package net.sdko.dotorgredirector.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Health indicator for core system resources. Monitors JVM memory usage and CPU availability. */
@Component
public final class CoreHealthIndicator implements HealthIndicator {

  /** Megabyte conversion factor. */
  private static final int MB_CONVERSION_FACTOR = 1024 * 1024;

  /** Maximum acceptable memory utilization (90%). */
  private static final double MAX_MEMORY_UTILIZATION = 0.9;

  /** Percentage multiplier for formatting. */
  private static final int PERCENT_MULTIPLIER = 100;

  /**
   * Checks the health of the core system resources. Verifies memory usage and CPU availability.
   *
   * @return Health status with details about system resources
   */
  @Override
  public Health health() {
    try {
      // Check if the JVM and runtime environment are healthy
      Runtime runtime = Runtime.getRuntime();
      long freeMemory = runtime.freeMemory() / MB_CONVERSION_FACTOR;
      long totalMemory = runtime.totalMemory() / MB_CONVERSION_FACTOR;
      long maxMemory = runtime.maxMemory() / MB_CONVERSION_FACTOR;

      // Check if we have at least 10% free memory
      double memoryUtilization = (double) (totalMemory - freeMemory) / totalMemory;
      boolean hasAdequateMemory = memoryUtilization < MAX_MEMORY_UTILIZATION;

      // Check available processors
      int availableProcessors = runtime.availableProcessors();
      boolean hasAdequateCpu = availableProcessors > 0;

      if (hasAdequateMemory && hasAdequateCpu) {
        return Health.up()
            .withDetail("component", "Core Redirector System")
            .withDetail("status", "UP")
            .withDetail(
                "memory_utilization",
                String.format("%.2f%%", memoryUtilization * PERCENT_MULTIPLIER))
            .withDetail("free_memory_mb", freeMemory)
            .withDetail("total_memory_mb", totalMemory)
            .withDetail("max_memory_mb", maxMemory)
            .withDetail("available_processors", availableProcessors)
            .build();
      } else {
        return Health.down()
            .withDetail("component", "Core Redirector System")
            .withDetail("status", "DOWN")
            .withDetail(
                "memory_utilization",
                String.format("%.2f%%", memoryUtilization * PERCENT_MULTIPLIER))
            .withDetail("free_memory_mb", freeMemory)
            .withDetail("total_memory_mb", totalMemory)
            .withDetail("max_memory_mb", maxMemory)
            .withDetail("available_processors", availableProcessors)
            .withDetail(
                "reason",
                hasAdequateMemory ? "Insufficient CPU resources" : "Memory usage too high")
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("component", "Core Redirector System")
          .withDetail("status", "DOWN")
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
