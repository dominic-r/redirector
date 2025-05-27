package net.sdko.dotorgredirector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Provides metrics for redirect operations. This class tracks the count and duration of redirects
 * performed by the application.
 */
@Component
public final class RedirectMetrics {

  /** Counter for tracking the total number of redirects. */
  private final Counter redirectCounter;

  /** Timer for measuring the duration of redirect operations. */
  private final Timer redirectTimer;

  /**
   * Constructs a RedirectMetrics instance with the given registry.
   *
   * @param registry The meter registry for recording metrics
   */
  public RedirectMetrics(final MeterRegistry registry) {
    this.redirectCounter =
        Counter.builder("dotorg.redirects.total")
            .description("Total number of redirects performed")
            .register(registry);

    this.redirectTimer =
        Timer.builder("dotorg.redirects.duration")
            .description("Time taken to process redirects")
            .register(registry);
  }

  /** Increments the redirect counter by one. */
  public void incrementRedirectCount() {
    redirectCounter.increment();
  }

  /**
   * Returns the timer used for measuring redirect durations.
   *
   * @return The redirect timer
   */
  public Timer getRedirectTimer() {
    return redirectTimer;
  }
}
