package net.sdko.dotorgredirector.health;

import io.sentry.Sentry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Sentry error reporting. Verifies that Sentry is properly configured and can
 * send test events.
 */
@Component
public final class SentryHealthIndicator implements HealthIndicator {

  /**
   * Checks the health of the Sentry error reporting service. Sends a test event to verify
   * connectivity.
   *
   * @return Health status with details about Sentry's availability
   */
  @Override
  public Health health() {
    try {
      // Check if Sentry is properly initialized and test connection
      boolean isEnabled = Sentry.isEnabled();

      if (isEnabled) {
        // Send a test event to verify connectivity
        boolean sentryConnected = false;
        String sentryResponse = null;

        try {
          // Create a test event with minimal impact
          io.sentry.protocol.SentryId eventId =
              Sentry.captureMessage("Health check test event", io.sentry.SentryLevel.DEBUG);

          // Non-null ID indicates event was accepted
          sentryConnected =
              eventId != null && !eventId.toString().equals("00000000-0000-0000-0000-000000000000");
          sentryResponse = eventId != null ? eventId.toString() : "null";

          if (!sentryConnected) {
            sentryResponse = "Failed to capture test event";
          }
        } catch (Exception sentryError) {
          sentryConnected = false;
          sentryResponse = sentryError.getMessage();
        }

        if (sentryConnected) {
          return Health.up()
              .withDetail("component", "Sentry Error Reporting")
              .withDetail("status", "UP")
              .withDetail("enabled", true)
              .withDetail("connected", true)
              .withDetail("test_event_id", sentryResponse)
              .build();
        } else {
          return Health.status("DEGRADED")
              .withDetail("component", "Sentry Error Reporting")
              .withDetail("status", "DEGRADED")
              .withDetail("enabled", true)
              .withDetail("connected", false)
              .withDetail("reason", "Could not send test event: " + sentryResponse)
              .build();
        }
      } else {
        return Health.down()
            .withDetail("component", "Sentry Error Reporting")
            .withDetail("status", "DOWN")
            .withDetail("enabled", false)
            .withDetail("connected", false)
            .withDetail("reason", "Sentry client is not enabled")
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("component", "Sentry Error Reporting")
          .withDetail("status", "DOWN")
          .withDetail("enabled", false)
          .withDetail("connected", false)
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
