package net.sdko.dotorgredirector.info;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.SentryId;
import java.util.Map;
import net.sdko.dotorgredirector.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.info.Info;
import org.springframework.test.util.ReflectionTestUtils;

class CustomInfoContributorTest {

  @Mock private AppProperties appProperties;

  private String environment;
  private CustomInfoContributor infoContributor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    environment = "test";

    when(appProperties.getVersion()).thenReturn("1.0.0");
    when(appProperties.getTargetUrl()).thenReturn("https://www.example.com");

    infoContributor = new CustomInfoContributor(environment, appProperties);
    
    // Set a test Sentry DSN
    ReflectionTestUtils.setField(infoContributor, "sentryDsn", 
        "https://abcd1234@o1234.ingest.sentry.io/1234567");
  }

  @Test
  void testContribute() {
    try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
      // Mock Sentry.isEnabled() to return true
      sentryMock.when(Sentry::isEnabled).thenReturn(true);
      
      // Mock Sentry.captureMessage to return a non-null ID
      SentryId mockId = new SentryId();
      sentryMock
          .when(() -> Sentry.captureMessage(anyString(), any(SentryLevel.class)))
          .thenReturn(mockId);
      
      Info.Builder builder = new Info.Builder();
      infoContributor.contribute(builder);
      Info info = builder.build();

      // Check application info
      @SuppressWarnings("unchecked")
      Map<String, Object> appDetails = (Map<String, Object>) info.getDetails().get("application");
      assertNotNull(appDetails);
      assertEquals("1.0.0", appDetails.get("version"));
      assertEquals("test", appDetails.get("environment"));
      assertEquals("https://www.example.com", appDetails.get("targetUrl"));

      // Check runtime info
      @SuppressWarnings("unchecked")
      Map<String, Object> runtimeDetails = (Map<String, Object>) info.getDetails().get("runtime");
      assertNotNull(runtimeDetails);
      assertNotNull(runtimeDetails.get("jvmName"));
      assertNotNull(runtimeDetails.get("jvmVendor"));
      assertNotNull(runtimeDetails.get("jvmVersion"));
      assertNotNull(runtimeDetails.get("uptime"));
      assertNotNull(runtimeDetails.get("uptimeFormatted"));

      // Check memory info
      @SuppressWarnings("unchecked")
      Map<String, Object> memoryDetails = (Map<String, Object>) info.getDetails().get("memory");
      assertNotNull(memoryDetails);
      assertTrue(memoryDetails.containsKey("heapUsed"));
      assertTrue(memoryDetails.containsKey("heapMax"));
      assertTrue(memoryDetails.containsKey("heapUtilization"));
      assertTrue(memoryDetails.containsKey("nonHeapUsed"));
      
      // Check Sentry info
      @SuppressWarnings("unchecked")
      Map<String, Object> sentryDetails = (Map<String, Object>) info.getDetails().get("sentry");
      assertNotNull(sentryDetails);
      assertEquals(true, sentryDetails.get("enabled"));
      assertEquals(true, sentryDetails.get("configured"));
      assertNotNull(sentryDetails.get("dsn"));
      assertEquals("test", sentryDetails.get("environment"));
      assertEquals("dot-org@1.0.0", sentryDetails.get("release"));
      assertNotNull(sentryDetails.get("last_test_event_id"));
    }
  }
  
  @Test
  void testContribute_sentryDisabled() {
    try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
      // Mock Sentry.isEnabled() to return false
      sentryMock.when(Sentry::isEnabled).thenReturn(false);
      
      Info.Builder builder = new Info.Builder();
      infoContributor.contribute(builder);
      Info info = builder.build();
      
      // Check Sentry info
      @SuppressWarnings("unchecked")
      Map<String, Object> sentryDetails = (Map<String, Object>) info.getDetails().get("sentry");
      assertNotNull(sentryDetails);
      assertEquals(false, sentryDetails.get("enabled"));
      assertEquals(true, sentryDetails.get("configured"));
      assertNotNull(sentryDetails.get("dsn"));
      assertEquals("test", sentryDetails.get("environment"));
      assertEquals("dot-org@1.0.0", sentryDetails.get("release"));
      // No test event ID should be present
      assertFalse(sentryDetails.containsKey("last_test_event_id"));
    }
  }
  
  @Test
  void testContribute_noDsn() {
    // Set empty Sentry DSN
    ReflectionTestUtils.setField(infoContributor, "sentryDsn", "");
    
    try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
      // Mock Sentry.isEnabled() to return false
      sentryMock.when(Sentry::isEnabled).thenReturn(false);
      
      Info.Builder builder = new Info.Builder();
      infoContributor.contribute(builder);
      Info info = builder.build();
      
      // Check Sentry info
      @SuppressWarnings("unchecked")
      Map<String, Object> sentryDetails = (Map<String, Object>) info.getDetails().get("sentry");
      assertNotNull(sentryDetails);
      assertEquals(false, sentryDetails.get("enabled"));
      assertEquals(false, sentryDetails.get("configured"));
      assertEquals("not configured", sentryDetails.get("dsn"));
    }
  }
}
