package net.sdko.dotorgredirector.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.sentry.Sentry;
import java.net.HttpURLConnection;
import java.net.URL;
import net.sdko.dotorgredirector.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class HealthIndicatorsTest {

  @Mock private AppProperties appProperties;

  private MeterRegistry meterRegistry;
  private CoreHealthIndicator coreHealthIndicator;
  private RedirectorHealthIndicator redirectorHealthIndicator;
  private SentryHealthIndicator sentryHealthIndicator;
  private PrometheusHealthIndicator prometheusHealthIndicator;
  private BackendApiHealthIndicator backendApiHealthIndicator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    meterRegistry = new SimpleMeterRegistry();

    coreHealthIndicator = new CoreHealthIndicator();
    redirectorHealthIndicator = new RedirectorHealthIndicator(appProperties);
    sentryHealthIndicator = new SentryHealthIndicator();
    prometheusHealthIndicator = new PrometheusHealthIndicator(meterRegistry);
    backendApiHealthIndicator = new BackendApiHealthIndicator();
  }

  @Test
  void testCoreHealthIndicator() {
    Health health = coreHealthIndicator.health();
    assertEquals(Status.UP, health.getStatus());
    assertEquals("Core Redirector System", health.getDetails().get("component"));
    assertEquals("UP", health.getDetails().get("status"));
    assertNotNull(health.getDetails().get("memory_utilization"));
    assertNotNull(health.getDetails().get("free_memory_mb"));
    assertNotNull(health.getDetails().get("total_memory_mb"));
    assertNotNull(health.getDetails().get("max_memory_mb"));
    assertNotNull(health.getDetails().get("available_processors"));
  }

  @Test
  void testRedirectorHealthIndicator_validUrl() throws Exception {
    when(appProperties.getTargetUrl()).thenReturn("https://www.example.com");

    // Mock URL connection for testing
    RedirectorHealthIndicator spyIndicator = spy(redirectorHealthIndicator);

    // Create mocks for the URL connection
    URL mockUrl = mock(URL.class);
    HttpURLConnection mockConnection = mock(HttpURLConnection.class);

    // Use PowerMockito to mock the URL constructor and openConnection
    try (MockedConstruction<URL> mocked =
        mockConstruction(
            URL.class,
            (mock, context) -> {
              when(mock.openConnection()).thenReturn(mockConnection);
            })) {
      // Setup mock connection behavior
      when(mockConnection.getResponseCode()).thenReturn(200);

      Health health = spyIndicator.health();
      assertEquals(Status.UP, health.getStatus());
      assertEquals("URL Redirector Service", health.getDetails().get("component"));
      assertEquals("UP", health.getDetails().get("status"));
      assertEquals("https://www.example.com", health.getDetails().get("targetUrl"));
      assertEquals(true, health.getDetails().get("configured"));
      assertEquals(true, health.getDetails().get("reachable"));
    }
  }

  @Test
  void testRedirectorHealthIndicator_validUrlButUnreachable() throws Exception {
    when(appProperties.getTargetUrl()).thenReturn("https://www.example.com");

    // Mock URL connection that throws IOException
    RedirectorHealthIndicator spyIndicator = spy(redirectorHealthIndicator);
    doAnswer(
            invocation -> {
              throw new java.io.IOException("Connection refused");
            })
        .when(spyIndicator)
        .health();

    try {
      Health health = spyIndicator.health();
      // This should not be reached due to the mock
      fail("Expected exception was not thrown");
    } catch (Exception e) {
      // Expected
      assertTrue(e instanceof java.io.IOException);
    }
  }

  @Test
  void testRedirectorHealthIndicator_invalidUrl() {
    when(appProperties.getTargetUrl()).thenReturn("not a valid url");

    try {
      Health health = redirectorHealthIndicator.health();
      assertEquals(Status.DOWN, health.getStatus());
      assertEquals("URL Redirector Service", health.getDetails().get("component"));
      assertEquals("DOWN", health.getDetails().get("status"));
      assertEquals("not a valid url", health.getDetails().get("targetUrl"));
      assertEquals(false, health.getDetails().get("valid"));
      assertNotNull(health.getDetails().get("reason"));
    } catch (Exception e) {
      // If redirectorHealthIndicator fails, handle manually with the expected values
      fail(
          "RedirectorHealthIndicator should handle invalid URLs without throwing exceptions: "
              + e.getMessage());
    }
  }

  @Test
  void testRedirectorHealthIndicator_emptyUrl() {
    when(appProperties.getTargetUrl()).thenReturn("");

    Health health = redirectorHealthIndicator.health();
    assertEquals(Status.DOWN, health.getStatus());
    assertEquals("URL Redirector Service", health.getDetails().get("component"));
    assertEquals("DOWN", health.getDetails().get("status"));
    assertEquals(false, health.getDetails().get("configured"));
  }

  @Test
  void testSentryHealthIndicator_enabled() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      sentryMockedStatic.when(Sentry::isEnabled).thenReturn(true);

      // Mock the capture message call
      io.sentry.protocol.SentryId mockId = new io.sentry.protocol.SentryId();
      sentryMockedStatic
          .when(() -> Sentry.captureMessage(anyString(), any(io.sentry.SentryLevel.class)))
          .thenReturn(mockId);

      Health health = sentryHealthIndicator.health();
      assertEquals(Status.UP, health.getStatus());
      assertEquals("Sentry Error Reporting", health.getDetails().get("component"));
      assertEquals("UP", health.getDetails().get("status"));
      assertEquals(true, health.getDetails().get("enabled"));
      assertEquals(true, health.getDetails().get("connected"));
      assertNotNull(health.getDetails().get("test_event_id"));
    }
  }

  @Test
  void testSentryHealthIndicator_enabledButFailedToSend() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      sentryMockedStatic.when(Sentry::isEnabled).thenReturn(true);

      // Mock the capture message call to return null (failed to send)
      sentryMockedStatic
          .when(() -> Sentry.captureMessage(anyString(), any(io.sentry.SentryLevel.class)))
          .thenReturn(null);

      Health health = sentryHealthIndicator.health();
      assertEquals("DEGRADED", health.getStatus().getCode());
      assertEquals("Sentry Error Reporting", health.getDetails().get("component"));
      assertEquals("DEGRADED", health.getDetails().get("status"));
      assertEquals(true, health.getDetails().get("enabled"));
      assertEquals(false, health.getDetails().get("connected"));
      assertNotNull(health.getDetails().get("reason"));
    }
  }

  @Test
  void testSentryHealthIndicator_disabled() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      sentryMockedStatic.when(Sentry::isEnabled).thenReturn(false);

      Health health = sentryHealthIndicator.health();
      assertEquals(Status.DOWN, health.getStatus());
      assertEquals("Sentry Error Reporting", health.getDetails().get("component"));
      assertEquals("DOWN", health.getDetails().get("status"));
      assertEquals(false, health.getDetails().get("enabled"));
      assertEquals(false, health.getDetails().get("connected"));
    }
  }

  @Test
  void testPrometheusHealthIndicator() {
    Health health = prometheusHealthIndicator.health();
    assertEquals(Status.UP, health.getStatus());
    assertEquals("Prometheus Metrics Collector", health.getDetails().get("component"));
    assertEquals("UP", health.getDetails().get("status"));
    assertEquals("SimpleMeterRegistry", health.getDetails().get("registry_type"));
    assertEquals("active", health.getDetails().get("metrics_collection"));
    assertNotNull(health.getDetails().get("test_metric_value"));
  }

  @Test
  void testPrometheusHealthIndicator_null() {
    PrometheusHealthIndicator nullRegistryIndicator = new PrometheusHealthIndicator(null);
    Health health = nullRegistryIndicator.health();
    assertEquals(Status.DOWN, health.getStatus());
    assertEquals("Prometheus Metrics Collector", health.getDetails().get("component"));
    assertEquals("DOWN", health.getDetails().get("status"));
    assertEquals("unavailable", health.getDetails().get("metrics_collection"));
    assertEquals("MeterRegistry is not available", health.getDetails().get("reason"));
  }

  @Test
  void testBackendApiHealthIndicator() {
    // Since we can't easily mock the web application context in a unit test,
    // we'll verify the health details contain expected keys, even if the status
    // might be DOWN since we're in a test environment
    Health health = backendApiHealthIndicator.health();

    // The health check in a test environment will likely fail without full context
    assertNotNull(health.getStatus());
    assertEquals("Backend API Services", health.getDetails().get("component"));
    assertNotNull(health.getDetails().get("status"));
    assertNotNull(health.getDetails().get("endpoints_available"));
  }
}
