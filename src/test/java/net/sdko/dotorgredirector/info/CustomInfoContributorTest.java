package net.sdko.dotorgredirector.info;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Map;
import net.sdko.dotorgredirector.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.info.Info;

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
  }

  @Test
  void testContribute() {
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
  }
}
