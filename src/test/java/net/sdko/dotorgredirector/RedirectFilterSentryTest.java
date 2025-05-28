package net.sdko.dotorgredirector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import net.sdko.dotorgredirector.core.RedirectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for the RedirectFilter. These tests don't require a Spring context. */
@Tag("unit")
public class RedirectFilterSentryTest {

  private RedirectFilter redirectFilter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  
  @Mock
  private FilterChain filterChain;

  @Mock private RedirectHandler mockRedirectHandler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    redirectFilter = new RedirectFilter(mockRedirectHandler);
    
    // Setup request and response
    request = new MockHttpServletRequest();
    request.setRequestURI("/test");
    request.addHeader("User-Agent", "Test User Agent");
    request.addHeader("Referer", "https://www.referrer.com");
    
    response = new MockHttpServletResponse();
  }

  @Test
  public void testFilterDelegationSuccess() throws ServletException, IOException {
    // Setup mock to handle the redirect
    when(mockRedirectHandler.handleRedirect(request, response)).thenReturn(true);
    
    // Execute filter
    redirectFilter.doFilter(request, response, filterChain);
    
    // Verify handler was called
    verify(mockRedirectHandler).handleRedirect(request, response);
    
    // Verify filter chain was not continued
    verifyNoInteractions(filterChain);
  }
  
  @Test
  public void testFilterDelegationNotHandled() throws ServletException, IOException {
    // Setup mock to not handle the redirect
    when(mockRedirectHandler.handleRedirect(request, response)).thenReturn(false);
    
    // Execute filter
    redirectFilter.doFilter(request, response, filterChain);
    
    // Verify handler was called
    verify(mockRedirectHandler).handleRedirect(request, response);
    
    // Verify filter chain was continued
    verify(filterChain).doFilter(request, response);
  }
  
  @Test
  public void testFilterDelegationWithException() throws ServletException, IOException {
    // Setup mock to throw exception
    when(mockRedirectHandler.handleRedirect(request, response))
        .thenThrow(new RuntimeException("Test exception"));
    
    // Execute filter - should propagate the exception
    assertThrows(RuntimeException.class, () -> {
      redirectFilter.doFilter(request, response, filterChain);
    });
    
    // Verify handler was called
    verify(mockRedirectHandler).handleRedirect(request, response);
    
    // Verify filter chain was not continued
    verifyNoInteractions(filterChain);
  }
} 