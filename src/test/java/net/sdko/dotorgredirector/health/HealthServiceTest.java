package net.sdko.dotorgredirector.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;

class HealthServiceTest {

  @Mock private CoreHealthIndicator coreHealthIndicator;

  @Mock private RedirectorHealthIndicator redirectorHealthIndicator;

  @Mock private SentryHealthIndicator sentryHealthIndicator;

  @Mock private PrometheusHealthIndicator prometheusHealthIndicator;

  @Mock private BackendApiHealthIndicator backendApiHealthIndicator;

  private HealthService healthService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    healthService =
        new HealthService(
            coreHealthIndicator,
            redirectorHealthIndicator,
            sentryHealthIndicator,
            prometheusHealthIndicator,
            backendApiHealthIndicator);
  }

  @Test
  void testGetComponentHealth() {
    when(coreHealthIndicator.health()).thenReturn(Health.up().build());

    String status = healthService.getComponentHealth("core");
    assertEquals("UP", status);
  }

  @Test
  void testGetComponentHealth_unknown() {
    String status = healthService.getComponentHealth("nonexistent");
    assertEquals("UNKNOWN", status);
  }

  @Test
  void testGetOverallHealth_allUp() {
    when(coreHealthIndicator.health()).thenReturn(Health.up().build());
    when(redirectorHealthIndicator.health()).thenReturn(Health.up().build());
    when(sentryHealthIndicator.health()).thenReturn(Health.up().build());
    when(prometheusHealthIndicator.health()).thenReturn(Health.up().build());
    when(backendApiHealthIndicator.health()).thenReturn(Health.up().build());

    String status = healthService.getOverallHealth();
    assertEquals("UP", status);
  }

  @Test
  void testGetOverallHealth_oneDegraded() {
    when(coreHealthIndicator.health()).thenReturn(Health.up().build());
    when(redirectorHealthIndicator.health()).thenReturn(Health.up().build());
    when(sentryHealthIndicator.health()).thenReturn(Health.status("DEGRADED").build());
    when(prometheusHealthIndicator.health()).thenReturn(Health.up().build());
    when(backendApiHealthIndicator.health()).thenReturn(Health.up().build());

    String status = healthService.getOverallHealth();
    assertEquals("DEGRADED", status);
  }

  @Test
  void testGetOverallHealth_oneDown() {
    when(coreHealthIndicator.health()).thenReturn(Health.up().build());
    when(redirectorHealthIndicator.health()).thenReturn(Health.down().build());
    when(sentryHealthIndicator.health()).thenReturn(Health.status("DEGRADED").build());
    when(prometheusHealthIndicator.health()).thenReturn(Health.up().build());
    when(backendApiHealthIndicator.health()).thenReturn(Health.up().build());

    String status = healthService.getOverallHealth();
    assertEquals("DOWN", status);
  }

  @Test
  void testGetAllComponentHealth() {
    when(coreHealthIndicator.health()).thenReturn(Health.up().build());
    when(redirectorHealthIndicator.health()).thenReturn(Health.down().build());
    when(sentryHealthIndicator.health()).thenReturn(Health.status("DEGRADED").build());
    when(prometheusHealthIndicator.health()).thenReturn(Health.up().build());
    when(backendApiHealthIndicator.health()).thenReturn(Health.up().build());

    Map<String, String> componentHealth = healthService.getAllComponentHealth();
    assertEquals(5, componentHealth.size());
    assertEquals("UP", componentHealth.get("core"));
    assertEquals("DOWN", componentHealth.get("redirector"));
    assertEquals("DEGRADED", componentHealth.get("sentry"));
    assertEquals("UP", componentHealth.get("prometheus"));
    assertEquals("UP", componentHealth.get("backendApi"));
  }
}
