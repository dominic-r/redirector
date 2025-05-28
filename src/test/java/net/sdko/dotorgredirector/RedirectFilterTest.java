package net.sdko.dotorgredirector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for the RedirectFilter. These tests don't require a Spring context. */
@Tag("unit")
public class RedirectFilterTest {

  private static final String TARGET_URL = "https://www.d-roy.ca";
  private static final String VERSION = "1.0.0";

  private RedirectFilter redirectFilter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  
  @Mock
  private FilterChain filterChain;
  
  @Mock
  private RedirectHandler mockRedirectHandler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    redirectFilter = new RedirectFilter(mockRedirectHandler);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  public void testRedirectToRootPath() throws ServletException, IOException {
    // Given
    request.setRequestURI("/");
    when(mockRedirectHandler.handleRedirect(request, response)).thenReturn(true);

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    verify(mockRedirectHandler).handleRedirect(request, response);
    // No verification on filterChain because the redirect was handled
    verifyNoInteractions(filterChain);
  }

  @Test
  public void testRedirectWithPath() throws ServletException, IOException {
    // Given
    request.setRequestURI("/test-path");
    when(mockRedirectHandler.handleRedirect(request, response)).thenReturn(true);

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    verify(mockRedirectHandler).handleRedirect(request, response);
    verifyNoInteractions(filterChain);
  }

  @Test
  public void testRedirectWithQueryParameters() throws ServletException, IOException {
    // Given
    request.setRequestURI("/search");
    request.setParameter("q", "test");
    request.setParameter("page", "1");
    when(mockRedirectHandler.handleRedirect(request, response)).thenReturn(true);

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    verify(mockRedirectHandler).handleRedirect(request, response);
    verifyNoInteractions(filterChain);
  }

  @Test
  public void testContinueFilterChainWhenNotHandled() throws ServletException, IOException {
    // Given
    request.setRequestURI("/backend/healthz");
    when(mockRedirectHandler.handleRedirect(request, response)).thenReturn(false);

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    verify(mockRedirectHandler).handleRedirect(request, response);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void testFilterInitialization() throws ServletException {
    // Create a mock FilterConfig
    MockFilterConfig filterConfig = new MockFilterConfig();
    filterConfig.addInitParameter("excludePattern", "/backend/*");
    
    // Initialize the filter
    redirectFilter.init(filterConfig);
    
    // Nothing to verify since init method doesn't do anything with the config yet
    // This test just ensures the method doesn't throw an exception
  }
  
  @Test
  public void testFilterDestroy() {
    // Call the destroy method
    redirectFilter.destroy();
    
    // Nothing to verify as the method has no functionality,
    // but this test ensures coverage
  }
}
