package io.agentmemoryhub.webapp.handler;

import java.util.Locale;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class WebAppLocaleAdvice {
    @ModelAttribute
    public void addLanguageAttribute(Model model, Locale locale) {
        model.addAttribute("language", locale.getLanguage());
    }
}
