package net.sdko.dotorgredirector.health;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Backend API services. This class checks the availability and health of
 * the application's API endpoints.
 */
@Component
public final class BackendApiHealthIndicator implements HealthIndicator {

  /** Minimum memory required in MB for API services to be considered healthy. */
  private static final int MIN_MEMORY_REQUIRED_MB = 10;

  /** Megabyte conversion factor. */
  private static final int MB_CONVERSION_FACTOR = 1024 * 1024;

  /**
   * Checks the health of the Backend API services. Verifies that Spring MVC components are
   * available and properly configured.
   *
   * @return Health status with details about the API services
   */
  @Override
  public Health health() {
    try {
      // Check if the application is properly configured for API endpoints
      Map<String, Object> endpointStatus = new HashMap<>();
      boolean apiServicesAvailable = false;

      try {
        // Verify Spring Framework classes are available
        // Just checking if these classes can be loaded - not using the variables
        Class.forName("org.springframework.web.servlet.DispatcherServlet");
        Class.forName("org.springframework.web.context.WebApplicationContext");

        endpointStatus.put("spring_mvc_available", true);

        // Get the WebApplicationContext if available
        try {
          Object webApplicationContext =
              Class.forName("org.springframework.web.context.ContextLoader")
                  .getMethod("getCurrentWebApplicationContext")
                  .invoke(null);

          if (webApplicationContext != null) {
            endpointStatus.put("web_context_available", true);

            // Check thread status
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            int apiThreads = 0;

            for (Thread thread : threads) {
              if (thread != null && thread.getName().contains("http")) {
                apiThreads++;
              }
            }

            endpointStatus.put("api_threads", apiThreads);
            endpointStatus.put("active_threads", Thread.activeCount());

            // Check if runtime has appropriate memory
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory() / MB_CONVERSION_FACTOR;
            long totalMemory = runtime.totalMemory() / MB_CONVERSION_FACTOR;

            endpointStatus.put("free_memory_mb", freeMemory);
            endpointStatus.put("total_memory_mb", totalMemory);

            // API is considered available if we have web context and memory
            apiServicesAvailable = apiThreads > 0 && freeMemory > MIN_MEMORY_REQUIRED_MB;
          } else {
            endpointStatus.put("web_context_available", false);
            endpointStatus.put("reason", "WebApplicationContext not initialized");
          }
        } catch (Exception contextError) {
          endpointStatus.put("web_context_available", false);
          endpointStatus.put("context_error", contextError.getMessage());
        }
      } catch (ClassNotFoundException e) {
        endpointStatus.put("spring_mvc_available", false);
        endpointStatus.put("class_error", e.getMessage());
      }

      if (apiServicesAvailable) {
        return Health.up()
            .withDetail("component", "Backend API Services")
            .withDetail("status", "UP")
            .withDetail("endpoints_available", true)
            .withDetails(endpointStatus)
            .build();
      } else {
        return Health.down()
            .withDetail("component", "Backend API Services")
            .withDetail("status", "DOWN")
            .withDetail("endpoints_available", false)
            .withDetails(endpointStatus)
            .withDetail("reason", "Backend API services unavailable")
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("component", "Backend API Services")
          .withDetail("status", "DOWN")
          .withDetail("endpoints_available", false)
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
