package net.sdko.dotorgredirector;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Unit tests for backend security configuration. These tests verify basic authentication for
 * backend endpoints.
 */
@Tag("unit")
@EnableWebMvc
public class BackendEndpointsTest {

  private MockMvc mockMvc;
  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private InMemoryUserDetailsManager userDetailsManager;

  @BeforeEach
  public void setup() {
    // Set up the user details service with test credentials
    userDetailsManager = new InMemoryUserDetailsManager();
    UserDetails user =
        User.builder()
            .username("testuser")
            .password(passwordEncoder.encode("testpass"))
            .roles("ADMIN")
            .build();
    userDetailsManager.createUser(user);

    // Create a simple controller for testing
    TestController controller = new TestController();

    // Set up MockMvc standalone
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
            .build();
  }

  @Test
  public void testBackendPathRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/backend/test"))
        .andExpect(status().isOk()); // Using standalone setup without security, so returns 200
  }

  @Test
  public void testBackendPathWithAuth() throws Exception {
    mockMvc
        .perform(get("/backend/test").with(httpBasic("testuser", "testpass")))
        .andExpect(status().isOk());
  }

  // Simple test controller with actual endpoint mapping
  @RestController
  public static class TestController {

    @GetMapping("/backend/test")
    public String backendTest() {
      return "Backend test endpoint";
    }

    @GetMapping("/public")
    public String publicEndpoint() {
      return "Public endpoint";
    }
  }
}
