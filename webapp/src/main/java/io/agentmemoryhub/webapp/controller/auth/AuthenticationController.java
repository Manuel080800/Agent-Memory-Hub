package io.agentmemoryhub.webapp.controller.auth;

import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import io.agentmemoryhub.core.model.record.LoginInputRecord;
import io.agentmemoryhub.core.service.auth.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final WebAppSessionHandler webAppSessionHandler;
    private final MessageSource messageSource;

    @GetMapping("/")
    public String getHome(HttpSession httpSession) {
        return webAppSessionHandler.hasUser(httpSession) ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String getLoginPage(HttpSession httpSession, Model model) {
        model.addAttribute("csrfToken", webAppSessionHandler.getOrCreateCsrfToken(httpSession));
        return webAppSessionHandler.hasUser(httpSession) ? "redirect:/dashboard" : "login";
    }

    @PostMapping("/login")
    public String loginByEmailAndPassword(@RequestParam String email, @RequestParam String password, @RequestParam String csrfToken, HttpServletRequest httpServletRequest, HttpSession httpSession, Model model, Locale locale) {
        if (!webAppSessionHandler.isValidCsrfToken(httpSession, csrfToken)) {
            model.addAttribute("errorMessage", messageSource.getMessage("webapp.login.sessionExpired", null, locale));
            model.addAttribute("csrfToken", webAppSessionHandler.getOrCreateCsrfToken(httpSession));
            return "login";
        }

        try {
            var userRecord = authenticationService.authenticateByCredentials(new LoginInputRecord(email, password));
            httpServletRequest.changeSessionId();
            webAppSessionHandler.storeUser(httpSession, userRecord);
            return "redirect:/dashboard";
        } catch (RuntimeException exception) {
            model.addAttribute("errorMessage", messageSource.getMessage("webapp.login.failed", null, locale));
            model.addAttribute("csrfToken", webAppSessionHandler.getOrCreateCsrfToken(httpSession));
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logoutBySession(@RequestParam String csrfToken, HttpSession httpSession) {
        if (!webAppSessionHandler.isValidCsrfToken(httpSession, csrfToken)) {
            return "redirect:/dashboard";
        }

        httpSession.invalidate();
        return "redirect:/login";
    }
}
