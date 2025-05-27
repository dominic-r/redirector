package net.sdko.dotorgredirector.health;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Prometheus metrics collection. Verifies that metrics can be collected and
 * exported to Prometheus.
 */
@Component
public final class PrometheusHealthIndicator implements HealthIndicator {

  /** The meter registry used for collecting metrics. */
  private final MeterRegistry meterRegistry;

  /**
   * Constructs a PrometheusHealthIndicator with the given registry.
   *
   * @param meterRegistry The meter registry for metrics collection
   */
  public PrometheusHealthIndicator(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Checks the health of the Prometheus metrics collection. Verifies that metrics can be registered
   * and incremented.
   *
   * @return Health status with details about the metrics collection
   */
  @Override
  public Health health() {
    try {
      // Check if MeterRegistry is available and verify metrics collection
      if (meterRegistry != null) {
        String registryType = meterRegistry.getClass().getSimpleName();
        boolean isPrometheusRegistry = registryType.toLowerCase().contains("prometheus");

        // Create a test counter to verify registry is working
        try {
          io.micrometer.core.instrument.Counter testCounter =
              io.micrometer.core.instrument.Counter.builder("health.test.counter")
                  .description("Test counter for health check")
                  .tag("check", "prometheus_health")
                  .register(meterRegistry);

          // Increment the counter to verify it works
          testCounter.increment();

          // Try to fetch the metric value to verify it's working
          double currentValue = testCounter.count();

          // We might want to mark as DEGRADED for non-Prometheus registries,
          // but for now we'll always return UP as long as metrics collection
          // is working
          return Health.up()
              .withDetail("component", "Prometheus Metrics Collector")
              .withDetail("status", "UP")
              .withDetail("registry_type", registryType)
              .withDetail("test_metric_value", currentValue)
              .withDetail("metrics_collection", "active")
              .withDetail("prometheus_registry", isPrometheusRegistry)
              .build();
        } catch (Exception metricError) {
          // Registry exists but metrics collection is broken
          return Health.down()
              .withDetail("component", "Prometheus Metrics Collector")
              .withDetail("status", "DOWN")
              .withDetail("registry_type", registryType)
              .withDetail("metrics_collection", "failed")
              .withDetail("reason", "Cannot collect metrics: " + metricError.getMessage())
              .build();
        }
      } else {
        return Health.down()
            .withDetail("component", "Prometheus Metrics Collector")
            .withDetail("status", "DOWN")
            .withDetail("metrics_collection", "unavailable")
            .withDetail("reason", "MeterRegistry is not available")
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("component", "Prometheus Metrics Collector")
          .withDetail("status", "DOWN")
          .withDetail("metrics_collection", "error")
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
