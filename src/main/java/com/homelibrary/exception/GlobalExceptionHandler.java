package com.homelibrary.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Date;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception e) {
    log.error("Exception raised: {}", e.getMessage());
    return ResponseEntity.internalServerError()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorDetails(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
  }

  @ExceptionHandler(HomeLibraryException.class)
  public ResponseEntity<?> handleException(HomeLibraryException e) {
    log.error("Session API exception raised: {}", e.getMessage());
    return ResponseEntity.status(e.getHttpStatus())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorDetails(LocalDateTime.now(), e.getHttpStatus(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleException(MethodArgumentNotValidException e) {
    log.error("MethodArgumentNotValidException raised: {}", e.getMessage());
    FieldError fieldError = e.getBindingResult().getFieldError();
    String field = fieldError != null ? fieldError.getField() : "<unknown>";
    String errorMessage = "Validation failed for parameter '" + field + "'";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorDetails(LocalDateTime.now(), HttpStatus.BAD_REQUEST, errorMessage));
  }
}
