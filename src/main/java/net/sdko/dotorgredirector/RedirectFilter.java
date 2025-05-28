package net.sdko.dotorgredirector;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sdko.dotorgredirector.core.RedirectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter that redirects requests to a target URL.
 * Delegates actual handling to the RedirectHandler.
 */
public final class RedirectFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectFilter.class);

  private final RedirectHandler redirectHandler;

  /**
   * Constructs a RedirectFilter with the given handler.
   *
   * @param redirectHandler The redirect handler
   */
  public RedirectFilter(final RedirectHandler redirectHandler) {
    this.redirectHandler = redirectHandler;
  }

  /**
   * Initializes the filter with the given configuration.
   *
   * @param filterConfig The filter configuration
   */
  @Override
  public void init(final FilterConfig filterConfig) {
    LOGGER.info("Initializing RedirectFilter");
  }

  /**
   * Filters the request and delegates to the redirect handler.
   *
   * @param request The servlet request
   * @param response The servlet response
   * @param chain The filter chain
   * @throws IOException if an I/O error occurs
   * @throws ServletException if a servlet error occurs
   */
  @Override
  public void doFilter(
      final ServletRequest request, 
      final ServletResponse response, 
      final FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Delegate to the redirect handler
    boolean handled = redirectHandler.handleRedirect(httpRequest, httpResponse);
    
    // If not handled by the redirect handler, continue the filter chain
    if (!handled) {
      chain.doFilter(request, response);
    }
  }

  /**
   * Cleanup when filter is destroyed.
   */
  @Override
  public void destroy() {
    // No resources to clean up
  }
}
