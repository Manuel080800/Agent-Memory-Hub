package io.agentmemoryhub.core.exception;

import java.util.List;
import java.util.Arrays;
import lombok.Getter;

@Getter
public class HubException extends RuntimeException {
    private final String code;
    private final String messageKey;
    private final List<Object> messageArgs;
    private final List<String> details;
    private final int httpStatus;

    public HubException(String code, String messageKey, int httpStatus, Object... messageArgs) {
        this(code, messageKey, httpStatus, List.of(), null, messageArgs);
    }

    public HubException(String code, String messageKey, int httpStatus, List<String> details, Throwable cause, Object... messageArgs) {
        super(messageKey, cause);
        this.code = code;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
        this.details = List.copyOf(details);
        this.messageArgs = List.copyOf(Arrays.asList(messageArgs.clone()));
    }
}
