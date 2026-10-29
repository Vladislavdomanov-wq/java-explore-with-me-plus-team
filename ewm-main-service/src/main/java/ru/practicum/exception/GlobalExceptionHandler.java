package ru.practicum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", "NOT_FOUND",
                        "reason", "The required object was not found.",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().format(FORMATTER),
                        "errors", Collections.emptyList()
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", "BAD_REQUEST",
                        "reason", "Incorrectly made request.",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().format(FORMATTER),
                        "errors", Collections.emptyList()
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", "CONFLICT",
                        "reason", "Integrity constraint has been violated.",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().format(FORMATTER),
                        "errors", Collections.emptyList()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "INTERNAL_SERVER_ERROR",
                        "reason", "Unexpected error.",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().format(FORMATTER),
                        "errors", Collections.emptyList()
                ));
    }
}