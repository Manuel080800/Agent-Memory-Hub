package io.agentmemoryhub.api.handler;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ErrorDetailRecord;
import io.agentmemoryhub.core.model.record.ErrorResponseRecord;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiErrorAdvice {
    private final MessageSource messageSource;

    @ExceptionHandler(HubException.class)
    public ResponseEntity<ErrorResponseRecord> handleHubException(HubException hubException, Locale locale) {
        var localizedMessage = messageSource.getMessage(hubException.getMessageKey(), hubException.getMessageArgs().toArray(), hubException.getMessageKey(), locale);
        var detailRecordList = hubException.getDetails().stream().map(detail -> new ErrorDetailRecord(null, detail)).toList();
        var errorResponseRecord = new ErrorResponseRecord(hubException.getCode(), localizedMessage, detailRecordList);
        return ResponseEntity.status(hubException.getHttpStatus()).body(errorResponseRecord);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponseRecord> handleInvalidRequest(Exception exception, Locale locale) {
        var localizedMessage = messageSource.getMessage("error.validation", null, "error.validation", locale);
        var errorResponseRecord = new ErrorResponseRecord("VALIDATION", localizedMessage, List.of());
        return ResponseEntity.badRequest().body(errorResponseRecord);
    }
}
