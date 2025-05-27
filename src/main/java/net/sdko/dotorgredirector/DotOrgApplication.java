package net.sdko.dotorgredirector;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import net.sdko.dotorgredirector.config.AppProperties;
import net.sdko.dotorgredirector.metrics.RedirectMetrics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Main application class for the .org redirector. This is the entry point for the Spring Boot
 * application.
 */
@SpringBootApplication
public class DotOrgApplication {

  /** The application environment. */
  private final String environment;

  /** The application properties. */
  private final AppProperties appProperties;

  /** The metrics for redirect operations. */
  private final RedirectMetrics redirectMetrics;

  /**
   * Constructs a DotOrgApplication with the given dependencies.
   *
   * @param applicationEnvironment The application environment
   * @param properties The application properties
   * @param metrics The redirect metrics
   */
  public DotOrgApplication(
      @Qualifier("applicationEnvironment") final String applicationEnvironment,
      final AppProperties properties,
      final RedirectMetrics metrics) {
    this.environment = applicationEnvironment;
    this.appProperties = properties;
    this.redirectMetrics = metrics;
  }

  /**
   * Main method to start the application.
   *
   * @param args Command-line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(DotOrgApplication.class, args);
  }

  /**
   * Configures and registers the redirect filter.
   *
   * @return The filter registration bean
   */
  @Bean
  public FilterRegistrationBean<RedirectFilter> redirectFilter() {
    RedirectFilter filter =
        new RedirectFilter(appProperties.getTargetUrl(), environment, appProperties.getVersion());

    // Manually set the metrics (we can't use @Autowired in a Filter directly)
    filter.setMetrics(redirectMetrics);

    FilterRegistrationBean<RedirectFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(filter);

    // Skip redirecting for backend paths
    registrationBean.addUrlPatterns("/*");
    registrationBean.addInitParameter("excludePattern", "/backend/*");
    registrationBean.setOrder(1);

    return registrationBean;
  }

  /**
   * Enables the @Timed annotation for Prometheus metrics.
   *
   * @param registry The meter registry
   * @return The timed aspect
   */
  @Bean
  public TimedAspect timedAspect(final MeterRegistry registry) {
    return new TimedAspect(registry);
  }
}
