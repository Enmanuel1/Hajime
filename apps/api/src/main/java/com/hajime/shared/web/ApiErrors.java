package com.hajime.shared.web;

import com.hajime.shared.ApiException;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ApiException.class)
  ProblemDetail domain(ApiException e) {
    return problem(e.status(), e.code(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException e) {
    var p = problem(400, "VALIDATION_FAILED", "Request validation failed.");
    p.setProperty(
        "errors",
        e.getBindingResult().getFieldErrors().stream()
            .map(
                f ->
                    Map.of(
                        "field",
                        f.getField(),
                        "message",
                        Objects.requireNonNullElse(f.getDefaultMessage(), "Invalid value")))
            .toList());
    return p;
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    HandlerMethodValidationException.class
  })
  ProblemDetail malformed(Exception e) {
    return problem(400, "INVALID_REQUEST", "Invalid JSON, parameter type, or parameter value.");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail conflict(Exception e) {
    return problem(
        409,
        "CONCURRENT_MODIFICATION",
        "The record changed or conflicts with an existing record. Reload before retrying.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail integrity(Exception e) {
    return problem(409, "DATA_CONFLICT", "The request conflicts with stored data.");
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ProblemDetail forbidden(Exception e) {
    return problem(403, "ACCESS_DENIED", "Access denied.");
  }

  @ExceptionHandler({
    org.springframework.web.servlet.resource.NoResourceFoundException.class,
    org.springframework.web.servlet.NoHandlerFoundException.class
  })
  ProblemDetail notFound(Exception e) {
    return problem(404, "RESOURCE_NOT_FOUND", "The requested endpoint was not found.");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ProblemDetail unsupportedMethod(Exception e) {
    return problem(
        405, "METHOD_NOT_ALLOWED", "The HTTP method is not supported for this endpoint.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("Unhandled API failure", e);
    return problem(500, "INTERNAL_ERROR", "An unexpected error occurred.");
  }

  private ProblemDetail problem(int status, String code, String detail) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
    p.setTitle(code);
    p.setProperty("code", code);
    return p;
  }
}
