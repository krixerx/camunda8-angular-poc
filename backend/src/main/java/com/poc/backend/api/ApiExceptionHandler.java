package com.poc.backend.api;

import com.poc.backend.api.dto.ApiError;
import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.command.ProblemException;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps Camunda client failures to structured JSON errors instead of opaque 500s. */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ProblemException.class)
  public ResponseEntity<ApiError> problem(ProblemException e) {
    int status = e.details() != null ? e.details().getStatus() : 502;
    String message = e.details() != null ? e.details().getDetail() : e.getMessage();
    log.warn("Camunda API problem ({}): {}", status, message);
    return ResponseEntity.status(status).body(new ApiError(status, message));
  }

  @ExceptionHandler(ClientException.class)
  public ResponseEntity<ApiError> client(ClientException e) {
    // unwrap async completion wrappers for a readable message
    Throwable cause = e.getCause() instanceof CompletionException ce ? ce.getCause() : e;
    log.warn("Camunda client error: {}", cause.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ApiError(502, cause.getMessage()));
  }
}
