package io.agentmemoryhub.webapp.controller.page.project;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ProjectPageController {

    private final WebAppSessionHandler webAppSessionHandler;

    @GetMapping("/dashboard")
    public String getProjectsPage(HttpSession httpSession, Model model) {
        model.addAttribute("csrfToken", webAppSessionHandler.getOrCreateCsrfToken(httpSession));
        model.addAttribute("activeNavigation", "projects");
        var userRecord = webAppSessionHandler.getUser(httpSession);

        if (Objects.nonNull(userRecord)) {
            model.addAttribute("userId", userRecord.id());
            model.addAttribute("platformRole", userRecord.platformRole());
            model.addAttribute("isAdmin", userRecord.platformRole() == PlatformRole.ADMIN);
        }

        return webAppSessionHandler.hasUser(httpSession) ? "projects" : "redirect:/login";
    }
}
