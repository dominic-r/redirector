package net.sdko.dotorgredirector;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.sdko.dotorgredirector.metrics.RedirectMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Filter that redirects requests to a target URL. This filter adds tracking parameters to the
 * redirect URL.
 */
public final class RedirectFilter implements Filter {

  /** Logger for this class. */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectFilter.class);

  /** The target URL to redirect to. */
  private final String targetUrl;

  /** The application environment. */
  private final String environment;

  /** The application version. */
  private final String version;

  /** Pattern for paths that should not be redirected. */
  private Pattern excludePattern;

  /** Metrics for tracking redirects. */
  private RedirectMetrics metrics;

  /**
   * Constructs a RedirectFilter with the given parameters.
   *
   * @param targetUrl The URL to redirect to
   * @param environment The application environment
   * @param version The application version
   */
  public RedirectFilter(final String targetUrl, final String environment, final String version) {
    this.targetUrl = targetUrl;
    this.environment = environment;
    this.version = version;
  }

  /**
   * Sets the metrics instance for tracking redirects.
   *
   * @param metrics The redirect metrics
   */
  public void setMetrics(final RedirectMetrics metrics) {
    this.metrics = metrics;
  }

  /**
   * Initializes the filter with the given configuration.
   *
   * @param filterConfig The filter configuration
   * @throws ServletException if an error occurs
   */
  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    String excludePatternStr = filterConfig.getInitParameter("excludePattern");
    if (excludePatternStr != null && !excludePatternStr.isEmpty()) {
      try {
        this.excludePattern = Pattern.compile(excludePatternStr.replace("*", ".*"));
      } catch (Exception e) {
        LOGGER.warn("Failed to compile exclude pattern: {}", excludePatternStr, e);
      }
    }
  }

  /**
   * Filters the request and performs the redirect.
   *
   * @param request The servlet request
   * @param response The servlet response
   * @param chain The filter chain
   * @throws IOException if an I/O error occurs
   * @throws ServletException if a servlet error occurs
   */
  @Override
  @Timed(value = "redirect.request", description = "Time taken to process redirect requests")
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String requestURI = httpRequest.getRequestURI();

    // Skip redirecting for excluded paths
    if (excludePattern != null && excludePattern.matcher(requestURI).matches()) {
      LOGGER.debug("Skipping redirect for excluded path: {}", requestURI);
      chain.doFilter(request, response);
      return;
    }

    // Record metrics if available
    if (metrics != null) {
      try {
        metrics.incrementRedirectCount();
        metrics
            .getRedirectTimer()
            .record(() -> performRedirectWithoutMetrics(httpRequest, httpResponse));
      } catch (Exception e) {
        LOGGER.error("Error during redirect with metrics", e);
        // Fallback to direct redirect without metrics
        performRedirectWithoutMetrics(httpRequest, httpResponse);
      }
    } else {
      performRedirectWithoutMetrics(httpRequest, httpResponse);
    }
  }

  /**
   * Performs the redirect without metrics recording.
   *
   * @param request The HTTP request
   * @param response The HTTP response
   */
  private void performRedirectWithoutMetrics(
      final HttpServletRequest request, final HttpServletResponse response) {
    try {
      String redirectUrl = buildRedirectUrl(request);
      LOGGER.info("Redirecting to: {}", redirectUrl);

      // Set attribute for logging/monitoring purposes
      request.setAttribute("redirected_to", redirectUrl);

      // Perform the redirect (302 Found)
      response.sendRedirect(redirectUrl);
    } catch (Exception e) {
      LOGGER.error("Failed to build redirect URL", e);
      try {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Redirect failed");
      } catch (IOException ioe) {
        LOGGER.error("Failed to send error response", ioe);
      }
    }
  }

  /**
   * Builds the redirect URL with tracking parameters.
   *
   * @param request The HTTP request
   * @return The redirect URL
   * @throws URISyntaxException if the target URL is invalid
   */
  private String buildRedirectUrl(final HttpServletRequest request) throws URISyntaxException {
    // Start with target URL and add original path
    URI targetUri = new URI(targetUrl);

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUri(targetUri).path(request.getRequestURI());

    // Preserve original query parameters
    Map<String, String[]> originalParams = new HashMap<>(request.getParameterMap());

    // Generate UUID for tracing
    String tracingId = UUID.randomUUID().toString();

    // Add tracking query parameters
    builder
        .queryParam("x-sws-event", "dot-org-redirect")
        .queryParam("x-sws-tracing-id", tracingId)
        .queryParam("x-sws-env", environment)
        .queryParam("x-sws-version", version)
        .queryParam("x-sws-ts", Instant.now().getEpochSecond());

    // Add original query parameters
    for (Map.Entry<String, String[]> entry : originalParams.entrySet()) {
      String key = entry.getKey();
      for (String value : entry.getValue()) {
        builder.queryParam(key, value);
      }
    }

    return builder.build().toUriString();
  }

  /** Cleans up resources when the filter is destroyed. */
  @Override
  public void destroy() {
    // No resources to clean up
  }
}
