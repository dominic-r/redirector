package net.sdko.dotorgredirector;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

public class SecurityConfigTest {

  private MockMvc mockMvc;
  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @BeforeEach
  public void setup() {
    TestController controller = new TestController();

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
            .build();
  }

  @Test
  public void testAuthenticationSimulation() throws Exception {
    MvcResult result = mockMvc.perform(get("/public")).andExpect(status().isOk()).andReturn();

    MockHttpServletResponse response = result.getResponse();
    assert response.getContentAsString().equals("Public endpoint");
  }

  @Test
  public void testProtectedEndpointSimulation() throws Exception {
    MvcResult result = mockMvc.perform(get("/backend/test")).andExpect(status().isOk()).andReturn();

    MockHttpServletResponse response = result.getResponse();
    assert response.getContentAsString().equals("Protected endpoint");
  }

  @RestController
  static class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
      return "Public endpoint";
    }

    @GetMapping("/backend/test")
    public String protectedEndpoint() {
      return "Protected endpoint";
    }
  }
}
