package io.agentmemoryhub.api.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.auth.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

@Component
@RequiredArgsConstructor
public class ApiAuthorizationInterceptor implements HandlerInterceptor {
    private final AuthenticationService authenticationService;
    private final ProjectMemberDAO projectMemberDAO;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws IOException {
        var accessToken = getBearerToken(httpServletRequest);
        var authenticatedUser = authenticationService.findUserByAccessToken(accessToken);

        if (authenticatedUser.isEmpty()) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        var requestPath = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());

        if (requestPath.startsWith("/api/v1/users") && authenticatedUser.get().platformRole() != io.agentmemoryhub.core.common.PlatformRole.ADMIN) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        var projectId = findProjectIdByPath(requestPath);

        if (projectId.isPresent() && !hasProjectAccess(httpServletRequest, requestPath, projectId.get(), authenticatedUser.get())) {
            writeAuthorizationFailure(httpServletRequest, httpServletResponse, HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        httpServletRequest.setAttribute("authenticatedUser", authenticatedUser.get());
        return true;
    }

    private String getBearerToken(HttpServletRequest httpServletRequest) {
        var authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (Objects.isNull(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring("Bearer ".length()).strip();
    }

    private Optional<String> findProjectIdByPath(String requestPath) {
        var pathPartList = requestPath.split("/");

        if (pathPartList.length >= 5 && "projects".equals(pathPartList[3])) {
            return Optional.of(pathPartList[4]);
        }

        return Optional.empty();
    }

    private boolean hasProjectAccess(HttpServletRequest httpServletRequest, String requestPath, String projectId, UserRecord userRecord) {
        var projectRole = projectMemberDAO.findProjectRoleByProjectIdAndUserId(projectId, userRecord.id());

        if (projectRole.isEmpty()) {
            return false;
        }

        var requestMethod = httpServletRequest.getMethod();

        if ("GET".equals(requestMethod) || "HEAD".equals(requestMethod)) {
            return true;
        }

        if (requiresOwner(requestPath, requestMethod)) {
            return projectRole.get() == ProjectRole.OWNER;
        }

        return projectRole.get() == ProjectRole.OWNER || projectRole.get() == ProjectRole.EDITOR;
    }

    private boolean requiresOwner(String requestPath, String requestMethod) {
        var pathPartList = requestPath.split("/");
        var projectResource = pathPartList.length > 5 ? pathPartList[5] : "";

        if ("members".equals(projectResource) || "settings".equals(projectResource) || "mcp-connections".equals(projectResource) || "active-profile".equals(projectResource) || requestPath.endsWith("/approve")) {
            return true;
        }

        if (!"DELETE".equals(requestMethod)) {
            return false;
        }

        return pathPartList.length == 5 || ("profiles".equals(projectResource) && pathPartList.length == 7);
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
