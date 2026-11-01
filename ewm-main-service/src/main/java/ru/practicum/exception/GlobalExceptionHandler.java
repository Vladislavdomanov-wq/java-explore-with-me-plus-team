package ru.practicum.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.practicum.dto.ErrorResponseDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.builder()
                        .status("NOT_FOUND")
                        .reason("The required object was not found.")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.builder()
                        .status("BAD_REQUEST")
                        .reason("Incorrectly made request.")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder()
                        .status("CONFLICT")
                        .reason("Integrity constraint has been violated.")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        String message = String.join(", ", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.builder()
                        .status("BAD_REQUEST")
                        .reason("Incorrectly made request.")
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .errors(errors)
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder()
                        .status("CONFLICT")
                        .reason("Integrity constraint has been violated.")
                        .message(ex.getMessage() == null ? "Integrity constraint has been violated." : ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            jakarta.validation.ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.builder()
                        .status("BAD_REQUEST")
                        .reason("Incorrectly made request.")
                        .message(ex.getMessage() == null ? "Bad request" : ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.builder()
                        .status("NOT_FOUND")
                        .reason("The required object was not found.")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.builder()
                        .status("INTERNAL_SERVER_ERROR")
                        .reason("Unexpected error.")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .errors(Collections.emptyList())
                        .build());
    }
}