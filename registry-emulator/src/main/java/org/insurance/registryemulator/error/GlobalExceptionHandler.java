package org.insurance.registryemulator.error;

import java.net.URI;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RegistryRecordNotFoundException.class)
  ProblemDetail handleNotFound(RegistryRecordNotFoundException ex) {
    return problem(NOT_FOUND, "not-found", "Registry record not found", ex.getMessage());
  }

  @ExceptionHandler(RegistryBusinessException.class)
  ProblemDetail handleBusiness(RegistryBusinessException ex) {
    log.warn("Business rejection: {}", ex.getMessage());
    return problem(
        UNPROCESSABLE_CONTENT, "business", "Business validation failed", ex.getMessage());
  }

  @ExceptionHandler(RegistryTechnicalException.class)
  ProblemDetail handleTechnical(RegistryTechnicalException ex) {
    log.error("Registry technical error", ex);
    return problem(INTERNAL_SERVER_ERROR, "technical", "Technical error", ex.getMessage());
  }

  @ExceptionHandler(RegistryUnavailableException.class)
  ProblemDetail handleUnavailable(RegistryUnavailableException ex) {
    log.warn("Registry unavailable: {}", ex.getMessage());
    return problem(
        SERVICE_UNAVAILABLE, "unavailable", "Registry temporarily unavailable", ex.getMessage());
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
