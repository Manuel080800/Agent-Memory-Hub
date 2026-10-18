package io.agentmemoryhub.webapp.handler.i18n;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class I18nHandler {
    private static final String COMMON_MODULE = "error";
    private static final String MESSAGE_BUNDLE = "messages";

    private final MessageSource messageSource;

    public Map<String, String> getMessagesModules(Locale locale, String module, String moduleOptional) {
        var resourceBundle = ResourceBundle.getBundle(MESSAGE_BUNDLE, locale, I18nHandler.class.getClassLoader(), ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        var messageMap = new LinkedHashMap<String, String>();
        messageMap.putAll(getMessages(resourceBundle, locale, COMMON_MODULE));
        messageMap.putAll(getMessages(resourceBundle, locale, module));

        if (Objects.nonNull(moduleOptional) && !moduleOptional.isBlank()) {
            messageMap.putAll(getMessages(resourceBundle, locale, moduleOptional));
        }

        return messageMap;
    }

    private Map<String, String> getMessages(ResourceBundle resourceBundle, Locale locale, String module) {
        var messageMap = new LinkedHashMap<String, String>();
        resourceBundle.keySet().stream().filter(key -> key.startsWith(module + ".")).sorted().forEach(key -> messageMap.put(key, messageSource.getMessage(key, null, locale)));
        return messageMap;
    }
}
