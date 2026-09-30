
package com.alexbalutin.smartbattery.api;

import com.alexbalutin.smartbattery.api.dto.ApiDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> badRequest(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> notFound(
            NoSuchElementException ex,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> databaseUnavailable(
            DataAccessException ex,
            HttpServletRequest request) {
        log.error("Database request failed for {}", request.getRequestURI(), ex);
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The local telemetry database is temporarily unavailable",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiDtos.ErrorResponse> unexpected(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled API error for {}", request.getRequestURI(), ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request);
    }

    private ResponseEntity<ApiDtos.ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiDtos.ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()));
    }
}
