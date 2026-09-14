package org.insurance.insuranceservice.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return problem(NOT_FOUND, "not-found", "Resource not found", ex.getMessage());
  }

  @ExceptionHandler(InvalidApplicationStateException.class)
  ProblemDetail handleInvalidState(InvalidApplicationStateException ex) {
    log.warn("Invalid application state transition: {}", ex.getMessage());
    return problem(CONFLICT, "invalid-state", "Invalid state transition", ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return problem(BAD_REQUEST, "validation", "Request validation failed", detail);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String detail = "Parameter '%s' has invalid value '%s'".formatted(ex.getName(), ex.getValue());
    return problem(BAD_REQUEST, "bad-request", "Invalid request parameter", detail);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
    return problem(
        BAD_REQUEST,
        "bad-request",
        "Malformed request body",
        "The request body could not be parsed");
  }

  private ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create("https://insurance.local/errors/" + type));
    return problem;
  }
}
