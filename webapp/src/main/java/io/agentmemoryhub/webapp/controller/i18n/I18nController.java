package io.agentmemoryhub.webapp.controller.i18n;

import io.agentmemoryhub.webapp.handler.i18n.I18nHandler;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/i18n")
@RequiredArgsConstructor
public class I18nController {
    private final I18nHandler i18nHandler;

    @GetMapping({"/{module}", "/{module}/{moduleOptional}"})
    public Map<String, String> getMessages(Locale locale, @PathVariable String module, @PathVariable(required = false) String moduleOptional) {
        return i18nHandler.getMessagesModules(locale, module, moduleOptional);
    }
}
