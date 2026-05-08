package com.serviceabonnement.exception;

import com.serviceabonnement.dto.response.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "serviceName", "service-abonnement");
    }

    @Test
    void handleResourceNotFoundException_ShouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, Objects.requireNonNull(response.getBody()).getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Not found", response.getBody().getMessage());
        assertEquals("service-abonnement", response.getBody().getServiceName());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleValidationException_ShouldReturn400() {
        ValidationException ex = new ValidationException("Invalid data");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, Objects.requireNonNull(response.getBody()).getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid data", response.getBody().getMessage());
    }

    @Test
    void handleConflictException_ShouldReturn409() {
        ConflictException ex = new ConflictException("Conflict");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, Objects.requireNonNull(response.getBody()).getStatus());
    }

    @Test
    void handleExternalServiceException_ShouldReturn503() {
        ExternalServiceException ex = new ExternalServiceException("Service down");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, Objects.requireNonNull(response.getBody()).getStatus());
    }

    @Test
    void handleGeneralException_ShouldReturn500() {
        Exception ex = new Exception("Random error");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, Objects.requireNonNull(response.getBody()).getStatus());
        assertEquals("An unexpected internal server error occurred", response.getBody().getMessage());
    }
}
