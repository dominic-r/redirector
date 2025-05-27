package net.sdko.dotorgredirector.info;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import net.sdko.dotorgredirector.config.AppProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Custom InfoContributor for the Spring Boot Actuator info endpoint. Provides custom information
 * about the application, runtime, and memory usage.
 */
@Component
public final class CustomInfoContributor implements InfoContributor {

  /** Milliseconds in a second. */
  private static final int MILLISECONDS_PER_SECOND = 1000;

  /** Seconds in a minute. */
  private static final int SECONDS_PER_MINUTE = 60;

  /** Minutes in an hour. */
  private static final int MINUTES_PER_HOUR = 60;

  /** Hours in a day. */
  private static final int HOURS_PER_DAY = 24;

  /** Megabyte conversion factor. */
  private static final int MB_CONVERSION_FACTOR = 1024 * 1024;

  /** Percentage multiplier. */
  private static final int PERCENTAGE_MULTIPLIER = 100;

  /** The application environment. */
  private final String environment;

  /** The application properties. */
  private final AppProperties appProperties;

  /**
   * Constructs a CustomInfoContributor with the given parameters.
   *
   * @param applicationEnvironment The application environment
   * @param appProperties The application properties
   */
  public CustomInfoContributor(
      @Qualifier("applicationEnvironment") final String applicationEnvironment,
      final AppProperties appProperties) {
    this.environment = applicationEnvironment;
    this.appProperties = appProperties;
  }

  /**
   * Contributes custom information to the info endpoint.
   *
   * @param builder The info builder
   */
  @Override
  public void contribute(final Info.Builder builder) {
    // Get JVM runtime information
    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    // Calculate uptime
    long uptimeMs = runtimeMXBean.getUptime();
    long uptimeSec = uptimeMs / MILLISECONDS_PER_SECOND;
    long uptimeMin = uptimeSec / SECONDS_PER_MINUTE;
    long uptimeHours = uptimeMin / MINUTES_PER_HOUR;
    long uptimeDays = uptimeHours / HOURS_PER_DAY;

    // Create a formatted uptime string
    String uptimeFormatted =
        String.format(
            "%d days, %d hours, %d minutes, %d seconds",
            uptimeDays,
            uptimeHours % HOURS_PER_DAY,
            uptimeMin % MINUTES_PER_HOUR,
            uptimeSec % SECONDS_PER_MINUTE);

    // Memory information
    long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed() / MB_CONVERSION_FACTOR;
    long heapMax = memoryMXBean.getHeapMemoryUsage().getMax() / MB_CONVERSION_FACTOR;
    long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed() / MB_CONVERSION_FACTOR;

    // Application details
    Map<String, Object> appDetails = new HashMap<>();
    appDetails.put("version", appProperties.getVersion());
    appDetails.put("environment", environment);
    appDetails.put("targetUrl", appProperties.getTargetUrl());

    // Runtime statistics
    Map<String, Object> runtimeStats = new HashMap<>();
    runtimeStats.put("jvmName", runtimeMXBean.getVmName());
    runtimeStats.put("jvmVendor", runtimeMXBean.getVmVendor());
    runtimeStats.put("jvmVersion", runtimeMXBean.getVmVersion());
    runtimeStats.put("startTime", runtimeMXBean.getStartTime());
    runtimeStats.put("uptime", uptimeMs);
    runtimeStats.put("uptimeFormatted", uptimeFormatted);

    // Memory statistics
    Map<String, Object> memoryStats = new HashMap<>();
    memoryStats.put("heapUsed", heapUsed + "MB");
    memoryStats.put("heapMax", heapMax + "MB");
    memoryStats.put(
        "heapUtilization",
        String.format("%.2f%%", (double) heapUsed / heapMax * PERCENTAGE_MULTIPLIER));
    memoryStats.put("nonHeapUsed", nonHeapUsed + "MB");

    // Add all details to the info builder
    builder.withDetail("application", appDetails);
    builder.withDetail("runtime", runtimeStats);
    builder.withDetail("memory", memoryStats);
  }
}
