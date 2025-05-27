package net.sdko.dotorgredirector.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that overrides health indicators to always return UP status. This is needed
 * for integration tests where we want to test the endpoint access rather than actual health status.
 */
@Configuration
@Profile("test")
public class TestHealthConfig {

  @Bean
  @Primary
  public HealthIndicator coreHealthIndicator() {
    return () -> Health.up().withDetail("component", "Core Redirector System").build();
  }

  @Bean
  @Primary
  public HealthIndicator redirectorHealthIndicator() {
    return () -> Health.up().withDetail("component", "URL Redirector Service").build();
  }

  @Bean
  @Primary
  public HealthIndicator sentryHealthIndicator() {
    return () -> Health.up().withDetail("component", "Sentry Error Reporting").build();
  }

  @Bean
  @Primary
  public HealthIndicator prometheusHealthIndicator() {
    return () -> Health.up().withDetail("component", "Prometheus Metrics Collector").build();
  }

  @Bean
  @Primary
  public HealthIndicator backendApiHealthIndicator() {
    return () -> Health.up().withDetail("component", "Backend API Services").build();
  }
}
