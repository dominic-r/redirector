package net.sdko.dotorgredirector.health;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

/**
 * Service for aggregating and reporting health status of application components. Manages multiple
 * health indicators and provides overall health reporting.
 */
@Service
public final class HealthService {

  /** Map of health indicators by component name. */
  private final Map<String, HealthIndicator> healthIndicators;

  /**
   * Constructs a HealthService with the given health indicators.
   *
   * @param coreHealthIndicator Core health indicator
   * @param redirectorHealthIndicator Redirector health indicator
   * @param sentryHealthIndicator Sentry health indicator
   * @param prometheusHealthIndicator Prometheus health indicator
   * @param backendApiHealthIndicator Backend API health indicator
   */
  public HealthService(
      final CoreHealthIndicator coreHealthIndicator,
      final RedirectorHealthIndicator redirectorHealthIndicator,
      final SentryHealthIndicator sentryHealthIndicator,
      final PrometheusHealthIndicator prometheusHealthIndicator,
      final BackendApiHealthIndicator backendApiHealthIndicator) {

    this.healthIndicators = new HashMap<>();
    this.healthIndicators.put("core", coreHealthIndicator);
    this.healthIndicators.put("redirector", redirectorHealthIndicator);
    this.healthIndicators.put("sentry", sentryHealthIndicator);
    this.healthIndicators.put("prometheus", prometheusHealthIndicator);
    this.healthIndicators.put("backendApi", backendApiHealthIndicator);
  }

  /**
   * Get the health of a specific component.
   *
   * @param component The component name
   * @return The health status (UP, DOWN, DEGRADED, UNKNOWN)
   */
  public String getComponentHealth(final String component) {
    HealthIndicator indicator = healthIndicators.get(component);
    if (indicator == null) {
      return "UNKNOWN";
    }

    Health health = indicator.health();
    return health.getStatus().getCode();
  }

  /**
   * Get the overall health status.
   *
   * @return UP if all components are UP, DOWN if any component is DOWN, DEGRADED if any component
   *     is DEGRADED but none are DOWN
   */
  public String getOverallHealth() {
    boolean hasDegraded = false;

    for (HealthIndicator indicator : healthIndicators.values()) {
      Health health = indicator.health();
      Status status = health.getStatus();

      if (Status.DOWN.equals(status)) {
        return "DOWN";
      } else if (!Status.UP.equals(status)) {
        hasDegraded = true;
      }
    }

    return hasDegraded ? "DEGRADED" : "UP";
  }

  /**
   * Get health details for all components.
   *
   * @return Map of component names to health status
   */
  public Map<String, String> getAllComponentHealth() {
    Map<String, String> result = new HashMap<>();

    for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
      Health health = entry.getValue().health();
      result.put(entry.getKey(), health.getStatus().getCode());
    }

    return result;
  }
}
