package io.agentmemoryhub.webapp.handler.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.user.UserService;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

@Component
@RequiredArgsConstructor
public class WebAppAuthorizationInterceptor implements HandlerInterceptor {
    private final ProjectMemberDAO projectMemberDAO;
    private final WebAppSessionHandler webAppSessionHandler;
    private final UserService userService;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws IOException {
        var httpSession = httpServletRequest.getSession(false);
        var userRecord = Objects.isNull(httpSession) ? null : webAppSessionHandler.getUser(httpSession);

        if (Objects.isNull(userRecord)) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            userRecord = userService.getUserById(userRecord.id());
        } catch (HubException hubException) {
            if (hubException.getHttpStatus() == HttpServletResponse.SC_NOT_FOUND) {
                writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            throw hubException;
        }

        if (!userRecord.enabled()) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        webAppSessionHandler.storeUser(httpSession, userRecord);
        var requestPath = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());

        if (isUserResourcePath(requestPath) && userRecord.platformRole() != PlatformRole.ADMIN) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        var projectId = findProjectIdByPath(requestPath);

        if (projectId != null && !hasProjectAccess(httpServletRequest, requestPath, projectId, userRecord)) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        if (isMutation(httpServletRequest.getMethod()) && !webAppSessionHandler.isValidCsrfToken(httpSession, httpServletRequest.getHeader("X-CSRF-Token"))) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    private String findProjectIdByPath(String requestPath) {
        var pathPartList = requestPath.split("/");

        if (pathPartList.length >= 3 && "projects".equals(pathPartList[1])) {
            return pathPartList[2];
        }

        return null;
    }

    private boolean hasProjectAccess(HttpServletRequest httpServletRequest, String requestPath, String projectId, UserRecord userRecord) {
        var projectRoleOptional = projectMemberDAO.findProjectRoleByProjectIdAndUserId(projectId, userRecord.id());

        if (projectRoleOptional.isEmpty()) {
            return false;
        }

        var requestMethod = httpServletRequest.getMethod();

        if ("GET".equals(requestMethod) || "HEAD".equals(requestMethod)) {
            return true;
        }

        var projectRole = projectRoleOptional.get();
        if (requiresOwner(requestPath, requestMethod)) {
            return projectRole == ProjectRole.OWNER;
        }

        return projectRole == ProjectRole.OWNER || projectRole == ProjectRole.EDITOR;
    }

    private boolean requiresOwner(String requestPath, String requestMethod) {
        var pathPartList = requestPath.split("/");
        var projectResource = pathPartList.length > 3 ? pathPartList[3] : "";

        if ("members".equals(projectResource) || "settings".equals(projectResource) || "mcp-connections".equals(projectResource) || "active-profile".equals(projectResource) || requestPath.endsWith("/approve")) {
            return true;
        }

        if (!"DELETE".equals(requestMethod)) {
            return false;
        }

        return pathPartList.length == 3 || ("profiles".equals(projectResource) && pathPartList.length == 5);
    }

    private boolean isUserResourcePath(String requestPath) {
        return "/users".equals(requestPath) || requestPath.startsWith("/users/");
    }

    private boolean isMutation(String requestMethod) {
        return !"GET".equals(requestMethod) && !"HEAD".equals(requestMethod) && !"OPTIONS".equals(requestMethod);
    }

    private void writeAuthorizationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, int status) throws IOException {
        httpServletResponse.setStatus(status);
        httpServletResponse.setContentType("application/json");
        var code = status == HttpServletResponse.SC_UNAUTHORIZED ? "UNAUTHORIZED" : "FORBIDDEN";
        var messageKey = status == HttpServletResponse.SC_UNAUTHORIZED ? "error.unauthorized" : "error.forbidden";
        var message = messageSource.getMessage(messageKey, null, messageKey, RequestContextUtils.getLocale(httpServletRequest));
        var error = Map.of("code", code, "message", message, "details", List.of());
        var response = Map.of("success", false, "error", error);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.getWriter().write(objectMapper.writeValueAsString(response));
    }
}
