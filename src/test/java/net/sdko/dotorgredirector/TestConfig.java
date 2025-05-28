package net.sdko.dotorgredirector;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.sentry.IHub;
import io.sentry.NoOpHub;
import io.sentry.SentryOptions;
import io.sentry.spring.jakarta.SentryExceptionResolver;
import io.sentry.spring.jakarta.SentryTaskDecorator;
import net.sdko.dotorgredirector.config.AppProperties;
import net.sdko.dotorgredirector.config.SentryConfig;
import net.sdko.dotorgredirector.core.MonitoringService;
import net.sdko.dotorgredirector.core.RedirectHandler;
import net.sdko.dotorgredirector.core.RedirectService;
import net.sdko.dotorgredirector.metrics.RedirectMetrics;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskDecorator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Test configuration that provides essential beans for integration tests. This ensures that beans
 * requiring external resources are mocked or provided with test values.
 */
@TestConfiguration
@Profile("test")
@ActiveProfiles("test")
public class TestConfig {

  /**
   * Provides a test version string. Overrides the default bean to avoid file system access during
   * tests.
   *
   * @return A test version string
   */
  @Bean
  @Qualifier("applicationVersion")
  public String applicationVersion() {
    return "test-version";
  }

  /**
   * Provides an empty Sentry DSN for testing.
   * 
   * @return An empty DSN to disable Sentry
   */
  @Bean
  @Qualifier("sentryDsn")
  public String sentryDsn() {
    // Empty DSN to disable Sentry in tests
    return "";
  }
  
  /**
   * Provides a test environment string.
   *
   * @return A test environment string
   */
  @Bean
  @Qualifier("applicationEnvironment")
  public String applicationEnvironment() {
    return "test";
  }
  
  /**
   * Provides disabled Sentry options for testing.
   * 
   * @return Disabled Sentry options
   */
  @Bean
  @Primary
  public SentryOptions sentryOptions() {
    SentryOptions options = new SentryOptions();
    options.setEnabled(false);
    options.setDsn("");
    return options;
  }
  
  /**
   * Provides a mock AppProperties bean for testing.
   * 
   * @return Configured AppProperties for testing
   */
  @Bean
  @Primary
  public AppProperties appProperties() {
    AppProperties properties = new AppProperties();
    properties.setVersion("test-version");
    properties.setTargetUrl("https://www.test-target.com");
    properties.setDebug(true);
    properties.setExcludePattern("/backend/*");
    properties.setRedirectStatusCode(302);
    return properties;
  }
  
  /**
   * Provides a test MeterRegistry for metrics testing.
   * 
   * @return A SimpleMeterRegistry for testing
   */
  @Bean
  @Primary
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
  
  /**
   * Provides a RedirectMetrics bean for testing.
   * 
   * @return RedirectMetrics instance
   */
  @Bean
  @Primary
  public RedirectMetrics redirectMetrics() {
    return new RedirectMetrics(meterRegistry());
  }
  
  /**
   * Provides a NoOp Sentry Hub for testing.
   * 
   * @return NoOpHub instance
   */
  @Bean
  @Primary
  public IHub sentryHub() {
    return NoOpHub.getInstance();
  }
  
  /**
   * Provides a MonitoringService with NoOp functionality for testing.
   * 
   * @return MonitoringService instance
   */
  @Bean
  @Primary
  public MonitoringService monitoringService() {
    return new MonitoringService(sentryHub());
  }
  
  /**
   * Provides a mock RedirectService bean for testing.
   * 
   * @return RedirectService instance
   */
  @Bean
  @Primary
  public RedirectService redirectService() {
    // Create the service with our test app properties and environment
    return new RedirectService(appProperties(), applicationEnvironment());
  }
  
  /**
   * Provides a RedirectHandler for testing.
   * 
   * @return RedirectHandler instance
   */
  @Bean
  @Primary
  public RedirectHandler redirectHandler() {
    return new RedirectHandler(
        redirectService(),
        monitoringService(),
        redirectMetrics(),
        appProperties()
    );
  }
  
  /**
   * Provides a mock SentryConfig bean for testing.
   * 
   * @return SentryConfig instance
   */
  @Bean
  @Primary
  public SentryConfig sentryConfig() {
    // Create with our test AppProperties and environment
    return new SentryConfig(appProperties(), applicationEnvironment());
  }
  
  /**
   * Provides a mock SentryExceptionResolver for testing.
   * 
   * @return Mock SentryExceptionResolver
   */
  @Bean
  @Primary
  public HandlerExceptionResolver sentryExceptionResolver() {
    return Mockito.mock(SentryExceptionResolver.class);
  }
  
  /**
   * Provides a mock SentryTaskDecorator for testing.
   * 
   * @return Mock SentryTaskDecorator
   */
  @Bean
  @Primary
  public TaskDecorator sentryTaskDecorator() {
    return Mockito.mock(SentryTaskDecorator.class);
  }
}
