package net.sdko.dotorgredirector.config;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for capturing all unhandled exceptions
 * and reporting them to Sentry.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  
  private final String environment;
  private final String version;
  
  /**
   * Constructs a GlobalExceptionHandler with the given environment and version.
   *
   * @param applicationEnvironment The application environment
   * @param appProperties The application properties
   */
  public GlobalExceptionHandler(
      @Qualifier("applicationEnvironment") final String applicationEnvironment,
      final AppProperties appProperties) {
    this.environment = applicationEnvironment;
    this.version = appProperties.getVersion();
  }
  
  /**
   * Handles all exceptions not handled elsewhere.
   *
   * @param ex The exception
   * @param request The HTTP request
   * @return A response entity with error details
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleAllExceptions(Exception ex, HttpServletRequest request) {
    String errorId = UUID.randomUUID().toString();
    
    if (ex instanceof SecurityException) {
      LOGGER.warn("Security violation [{}]: {}", errorId, ex.getMessage());
    } else {
      LOGGER.error("Unhandled exception [{}]: {}", errorId, ex.getMessage(), ex);
      
      // Report non-security exceptions to Sentry
      Sentry.configureScope(scope -> {
        scope.setTag("error_id", errorId);
        scope.setTag("environment", environment);
        scope.setTag("version", version);
        
        // Add request data
        Map<String, String> requestData = new HashMap<>();
        requestData.put("url", request.getRequestURL().toString());
        requestData.put("method", request.getMethod());
        requestData.put("user_agent", request.getHeader("User-Agent"));
        requestData.put("remote_addr", request.getRemoteAddr());
        scope.setContexts("request", requestData);
      });
      
      Sentry.captureException(ex);
    }
    
    // Return appropriate response based on exception type
    Map<String, Object> errorResponse = new HashMap<>();
    
    if (ex instanceof SecurityException) {
      errorResponse.put("error", "Bad Request");
      errorResponse.put("error_id", errorId);
      errorResponse.put("message", ex.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } else {
      errorResponse.put("error", "Internal Server Error");
      errorResponse.put("error_id", errorId);
      errorResponse.put("message", "An unexpected error occurred");
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
} 