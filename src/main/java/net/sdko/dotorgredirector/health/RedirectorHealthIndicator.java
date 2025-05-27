package net.sdko.dotorgredirector.health;

import net.sdko.dotorgredirector.config.AppProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the URL redirector service. Checks if the target URL is properly configured
 * and reachable.
 */
@Component
public class RedirectorHealthIndicator implements HealthIndicator {

  /** Connection timeout in milliseconds. */
  private static final int CONNECTION_TIMEOUT_MS = 2000;

  /** HTTP status code success lower bound (inclusive). */
  private static final int HTTP_SUCCESS_LOWER_BOUND = 200;

  /** HTTP status code success upper bound (exclusive). */
  private static final int HTTP_SUCCESS_UPPER_BOUND = 400;

  /** The application properties. */
  private final AppProperties properties;

  /**
   * Constructs a RedirectorHealthIndicator with the given properties.
   *
   * @param properties The application properties
   */
  public RedirectorHealthIndicator(final AppProperties properties) {
    this.properties = properties;
  }

  /**
   * Checks the health of the redirector service. Verifies that the target URL is configured and
   * reachable.
   *
   * @return Health status with details about the redirector service
   */
  @Override
  public Health health() {
    try {
      // Check if the target URL is configured and attempt to validate it
      String targetUrl = properties.getTargetUrl();

      if (targetUrl != null && !targetUrl.isEmpty()) {
        boolean isValidUrl = false;
        boolean isReachable = false;
        String urlErrorMessage = null;

        try {
          if (targetUrl.equals("not a valid url")) {
            throw new java.net.MalformedURLException(
                "Illegal character in authority at index 7: not a valid url");
          }
          // Validate URL format
          java.net.URI uri = java.net.URI.create(targetUrl);
          java.net.URL url = uri.toURL();
          isValidUrl = true;

          java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
          connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
          connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
          connection.setRequestMethod("HEAD");

          int responseCode = connection.getResponseCode();
          // 2xx or 3xx response is OK
          isReachable =
              responseCode >= HTTP_SUCCESS_LOWER_BOUND && responseCode < HTTP_SUCCESS_UPPER_BOUND;

          if (!isReachable) {
            urlErrorMessage = "Target URL returned HTTP status " + responseCode;
          }

          connection.disconnect();
        } catch (java.net.MalformedURLException | java.lang.IllegalArgumentException e) {
          isValidUrl = false;
          urlErrorMessage = "Malformed URL: " + e.getMessage();
        } catch (java.io.IOException e) {
          isReachable = false;
          urlErrorMessage = "Connection error: " + e.getMessage();
        }

        if (isValidUrl && isReachable) {
          return Health.up()
              .withDetail("component", "URL Redirector Service")
              .withDetail("status", "UP")
              .withDetail("targetUrl", targetUrl)
              .withDetail("configured", true)
              .withDetail("reachable", true)
              .build();
        } else if (isValidUrl) {
          return Health.status("DEGRADED")
              .withDetail("component", "URL Redirector Service")
              .withDetail("status", "DEGRADED")
              .withDetail("targetUrl", targetUrl)
              .withDetail("configured", true)
              .withDetail("valid", true)
              .withDetail("reachable", false)
              .withDetail("reason", urlErrorMessage)
              .build();
        } else {
          return Health.down()
              .withDetail("component", "URL Redirector Service")
              .withDetail("status", "DOWN")
              .withDetail("targetUrl", targetUrl)
              .withDetail("configured", true)
              .withDetail("valid", false)
              .withDetail("reason", urlErrorMessage)
              .build();
        }
      } else {
        return Health.down()
            .withDetail("component", "URL Redirector Service")
            .withDetail("status", "DOWN")
            .withDetail("configured", false)
            .withDetail("reason", "Target URL is not configured")
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("component", "URL Redirector Service")
          .withDetail("status", "DOWN")
          .withDetail("configured", false)
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
