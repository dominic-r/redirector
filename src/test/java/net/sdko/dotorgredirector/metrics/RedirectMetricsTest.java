package net.sdko.dotorgredirector.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the RedirectMetrics class. These tests don't require a Spring context. */
@Tag("unit")
public class RedirectMetricsTest {

  private RedirectMetrics redirectMetrics;
  private MeterRegistry meterRegistry;

  @BeforeEach
  public void setup() {
    // Use a simple meter registry for testing
    meterRegistry = new SimpleMeterRegistry();
    redirectMetrics = new RedirectMetrics(meterRegistry);
  }

  @Test
  public void testMetricsRegistration() {
    assertNotNull(redirectMetrics);
    assertNotNull(meterRegistry);

    // Check counter registration
    assertNotNull(meterRegistry.find("dotorg.redirects.total").counter());

    // Check timer registration
    assertNotNull(meterRegistry.find("dotorg.redirects.duration").timer());
  }

  @Test
  public void testIncrementRedirectCount() {
    // Get initial count (should be 0)
    double initialCount = meterRegistry.find("dotorg.redirects.total").counter().count();
    assertEquals(0.0, initialCount);

    // Increment the counter
    redirectMetrics.incrementRedirectCount();

    // Check that the counter was incremented
    assertEquals(1.0, meterRegistry.find("dotorg.redirects.total").counter().count());

    // Increment again
    redirectMetrics.incrementRedirectCount();

    // Check that the counter was incremented again
    assertEquals(2.0, meterRegistry.find("dotorg.redirects.total").counter().count());
  }
}
