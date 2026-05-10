package com.sgitu.g4.exception;

import com.sgitu.g4.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
		return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
	}

	@ExceptionHandler({ BadRequestException.class, IllegalArgumentException.class, ConstraintViolationException.class })
	public ResponseEntity<ApiErrorResponse> badRequest(Exception ex, HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req, null);
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<ApiErrorResponse> forbiddenOp(ForbiddenOperationException ex, HttpServletRequest req) {
		return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), req, null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
		return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Accès refusé", req, null);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
		return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Identifiants invalides", req, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		List<ApiErrorResponse.FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(GlobalExceptionHandler::mapField)
				.collect(Collectors.toList());
		String msg = fieldErrors.isEmpty() ? "Erreur de validation" : fieldErrors.get(0).getMessage();
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg, req, fieldErrors);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
		return build(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION",
				"Contrainte base de données : " + ex.getMostSpecificCause().getMessage(), req, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> generic(Exception ex, HttpServletRequest req) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erreur interne", req, null);
	}

	private static ApiErrorResponse.FieldErrorDto mapField(FieldError fe) {
		return ApiErrorResponse.FieldErrorDto.builder()
				.field(fe.getField())
				.message(fe.getDefaultMessage())
				.rejectedValue(fe.getRejectedValue())
				.build();
	}

	private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message,
			HttpServletRequest req, List<ApiErrorResponse.FieldErrorDto> fields) {
		ApiErrorResponse body = ApiErrorResponse.builder()
				.status(status.value())
				.error(code)
				.message(message)
				.path(req.getRequestURI())
				.timestamp(Instant.now())
				.fieldErrors(fields)
				.build();
		return ResponseEntity.status(status).body(body);
	}
}
