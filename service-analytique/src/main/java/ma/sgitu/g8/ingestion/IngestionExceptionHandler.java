package ma.sgitu.g8.ingestion;

import jakarta.validation.ConstraintViolationException;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@RestControllerAdvice
public class IngestionExceptionHandler {

    private static final String EMPTY_BODY_MESSAGE = "Request body must contain at least one event.";
    private static final String INVALID_BODY_MESSAGE = "Request body must be a valid non-empty JSON array.";

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            BindException.class
    })
    public ResponseEntity<BatchIngestionResponse> handleValidationExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(rejectedResponse(EMPTY_BODY_MESSAGE));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BatchIngestionResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(rejectedResponse(INVALID_BODY_MESSAGE));
    }

    private BatchIngestionResponse rejectedResponse(String reason) {
        return BatchIngestionResponse.builder()
                .totalReceived(0)
                .totalAccepted(0)
                .totalRejected(0)
                .rejectedReasons(List.of(reason))
                .status("REJECTED")
                .build();
    }
}
