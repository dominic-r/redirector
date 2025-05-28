package net.sdko.dotorgredirector.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  @Mock private AppProperties appProperties;
  @Mock private HttpServletRequest request;
  
  private GlobalExceptionHandler exceptionHandler;
  
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(appProperties.getVersion()).thenReturn("1.0.0");
    
    exceptionHandler = new GlobalExceptionHandler("test", appProperties);
    
    // Setup mock request
    when(request.getRequestURL()).thenReturn(new StringBuffer("https://example.com/test"));
    when(request.getMethod()).thenReturn("GET");
    when(request.getHeader("User-Agent")).thenReturn("Test User Agent");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
  }
  
  @Test
  void testHandleAllExceptionsWithSentry() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      // Create test exception
      Exception testException = new RuntimeException("Test exception");
      
      // Handle the exception
      ResponseEntity<Object> response = exceptionHandler.handleAllExceptions(testException, request);
      
      // Verify Sentry was called
      sentryMockedStatic.verify(() -> Sentry.configureScope(any()));
      sentryMockedStatic.verify(() -> Sentry.captureException(testException));
      
      // Verify response
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      
      @SuppressWarnings("unchecked")
      Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
      assertNotNull(responseBody);
      assertEquals("Internal Server Error", responseBody.get("error"));
      assertNotNull(responseBody.get("error_id"));
      assertEquals("An unexpected error occurred", responseBody.get("message"));
    }
  }
  
  @Test
  void testHandleAllExceptionsWithDifferentExceptionTypes() {
    // Test with different exception types
    Exception[] exceptions = {
      new IllegalArgumentException("Invalid argument"),
      new NullPointerException("Null pointer"),
      new UnsupportedOperationException("Unsupported operation")
    };
    
    for (Exception exception : exceptions) {
      try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
        // Handle the exception
        ResponseEntity<Object> response = exceptionHandler.handleAllExceptions(exception, request);
        
        // Verify Sentry was called with the specific exception
        sentryMockedStatic.verify(() -> Sentry.captureException(exception));
        
        // Verify response status
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      }
    }
  }
  
  @Test
  void testErrorIdIsUnique() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      // Create test exception
      Exception exception = new RuntimeException("Test exception");
      
      // Handle the exception twice
      ResponseEntity<Object> response1 = exceptionHandler.handleAllExceptions(exception, request);
      ResponseEntity<Object> response2 = exceptionHandler.handleAllExceptions(exception, request);
      
      // Get error IDs
      @SuppressWarnings("unchecked")
      String errorId1 = ((Map<String, Object>) response1.getBody()).get("error_id").toString();
      @SuppressWarnings("unchecked")
      String errorId2 = ((Map<String, Object>) response2.getBody()).get("error_id").toString();
      
      // Verify error IDs are different
      assertNotEquals(errorId1, errorId2);
    }
  }
} 