package io.agentmemoryhub.webapp.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ErrorDetailRecord;
import io.agentmemoryhub.core.model.record.ErrorResponseRecord;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class WebAppErrorAdvice {
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @ExceptionHandler(HubException.class)
    public ResponseEntity<ErrorResponseRecord> handleHubException(HubException hubException, Locale locale) {
        var localizedMessage = messageSource.getMessage(hubException.getMessageKey(), hubException.getMessageArgs().toArray(), hubException.getMessageKey(), locale);
        var detailRecordList = hubException.getDetails().stream().map(detail -> new ErrorDetailRecord(null, detail)).toList();
        var errorResponseRecord = new ErrorResponseRecord(hubException.getCode(), localizedMessage, detailRecordList);
        return ResponseEntity.status(hubException.getHttpStatus()).body(errorResponseRecord);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonNode> handleInvalidRequest(IllegalArgumentException exception, Locale locale) {
        var errorResponse = objectMapper.createObjectNode();
        errorResponse.put("message", messageSource.getMessage("webapp.error.invalidRequest", null, locale));
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(errorResponse);
    }
}
