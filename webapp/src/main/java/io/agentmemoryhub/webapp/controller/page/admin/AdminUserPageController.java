package io.agentmemoryhub.webapp.controller.page.admin;

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
public class AdminUserPageController {

    private final WebAppSessionHandler webAppSessionHandler;

    @GetMapping("/admin/users")
    public String getUsersPage(HttpSession httpSession, Model model) {
        if (!webAppSessionHandler.hasUser(httpSession)) {
            return "redirect:/login";
        }

        var userRecord = webAppSessionHandler.getUser(httpSession);
        if (Objects.isNull(userRecord) || userRecord.platformRole() != PlatformRole.ADMIN) {
            return "redirect:/dashboard";
        }

        model.addAttribute("csrfToken", webAppSessionHandler.getOrCreateCsrfToken(httpSession));
        model.addAttribute("activeNavigation", "users");
        return "users";
    }
}
