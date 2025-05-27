package net.sdko.dotorgredirector;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.sdko.dotorgredirector.config.AppProperties;
import net.sdko.dotorgredirector.metrics.RedirectMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the complete application. These tests verify that all components work
 * together correctly.
 */
@IntegrationTest
@AutoConfigureMockMvc
public class DotOrgApplicationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ApplicationContext context;

  @Autowired private AppProperties appProperties;

  @Autowired private RedirectMetrics redirectMetrics;

  @Test
  public void contextLoads() {
    assertNotNull(context);
    assertNotNull(appProperties);
    assertNotNull(redirectMetrics);
  }

  @Test
  public void testRedirectEndpoint() throws Exception {
    mockMvc
        .perform(get("/test"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header()
                .string(
                    "Location",
                    org.hamcrest.Matchers.containsString(appProperties.getTargetUrl())));
  }

  @Test
  public void testHealthEndpointWithAuth() throws Exception {
    mockMvc
        .perform(get("/backend/healthz").with(httpBasic("testuser", "testpass")))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              // Verify that we received a response with health data
              String content = result.getResponse().getContentAsString();
              org.junit.jupiter.api.Assertions.assertTrue(
                  content.contains("components"), "Health response should contain components");
            });
  }

  @Test
  public void testHealthEndpointWithoutAuth() throws Exception {
    mockMvc.perform(get("/backend/healthz")).andExpect(status().isUnauthorized());
  }

  @Test
  public void testMetricsEndpointWithAuth() throws Exception {
    mockMvc
        .perform(get("/backend/metrics").with(httpBasic("testuser", "testpass")))
        .andExpect(status().isOk());
  }
}
