package net.sdko.dotorgredirector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
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
  private MockFilterChain filterChain;

  @BeforeEach
  public void setUp() {
    redirectFilter = new RedirectFilter(TARGET_URL, "prod", VERSION);
    // Don't use metrics in test to avoid dependencies
    redirectFilter.setMetrics(null);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();
  }

  @Test
  public void testRedirectToRootPath() throws ServletException, IOException {
    // Given
    request.setRequestURI("/");

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    assertEquals(302, response.getStatus());
    String redirectUrl = response.getRedirectedUrl();

    assertTrue(redirectUrl.startsWith(TARGET_URL + "/?"));
    assertTrue(redirectUrl.contains("x-sws-event=dot-org-redirect"));
    assertTrue(redirectUrl.contains("x-sws-env=prod"));
    assertTrue(redirectUrl.contains("x-sws-version=" + VERSION));
    assertTrue(redirectUrl.contains("x-sws-tracing-id="));
    assertTrue(redirectUrl.matches(".*x-sws-ts=\\d+.*"));
  }

  @Test
  public void testRedirectWithPath() throws ServletException, IOException {
    // Given
    request.setRequestURI("/test-path");

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    assertEquals(302, response.getStatus());
    String redirectUrl = response.getRedirectedUrl();

    assertTrue(redirectUrl.startsWith(TARGET_URL + "/test-path?"));
    assertTrue(redirectUrl.contains("x-sws-event=dot-org-redirect"));
    assertTrue(redirectUrl.contains("x-sws-tracing-id="));
  }

  @Test
  public void testRedirectWithQueryParameters() throws ServletException, IOException {
    // Given
    request.setRequestURI("/search");
    request.setParameter("q", "test");
    request.setParameter("page", "1");

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then
    assertEquals(302, response.getStatus());
    String redirectUrl = response.getRedirectedUrl();

    assertTrue(redirectUrl.startsWith(TARGET_URL + "/search?"));
    assertTrue(redirectUrl.contains("x-sws-event=dot-org-redirect"));
    assertTrue(redirectUrl.contains("x-sws-tracing-id="));
    assertTrue(redirectUrl.contains("q=test"));
    assertTrue(redirectUrl.contains("page=1"));
  }

  @Test
  public void testRedirectWithDevEnvironment() throws ServletException, IOException {
    // Given
    RedirectFilter devFilter = new RedirectFilter(TARGET_URL, "dev", VERSION);
    devFilter.setMetrics(null);
    request.setRequestURI("/test-path");

    // When
    devFilter.doFilter(request, response, filterChain);

    // Then
    assertEquals(302, response.getStatus());
    String redirectUrl = response.getRedirectedUrl();

    assertTrue(redirectUrl.contains("x-sws-env=dev"));
    assertTrue(redirectUrl.contains("x-sws-tracing-id="));
  }

  @Test
  public void testSkipRedirectForBackendPaths() throws ServletException, IOException {
    // Given
    request.setRequestURI("/backend/healthz");

    // Setup filter with exclude pattern
    redirectFilter = new RedirectFilter(TARGET_URL, "prod", VERSION);
    redirectFilter.setMetrics(null);

    // Create a mock FilterConfig with the excludePattern
    MockFilterConfig filterConfig = new MockFilterConfig();
    filterConfig.addInitParameter("excludePattern", "/backend/*");
    redirectFilter.init(filterConfig);

    // When
    redirectFilter.doFilter(request, response, filterChain);

    // Then - should not redirect but continue the filter chain
    assertEquals(200, response.getStatus());
    assertEquals(null, response.getRedirectedUrl());
  }
}
