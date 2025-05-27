package net.sdko.dotorgredirector;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that provides essential beans for integration tests. This ensures that beans
 * requiring external resources are mocked or provided with test values.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

  /**
   * Provides a test version string. Overrides the default bean to avoid file system access during
   * tests.
   *
   * @return A test version string
   */
  @Bean
  @Primary
  public String applicationVersion() {
    return "test-version";
  }
}
