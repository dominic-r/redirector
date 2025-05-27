package net.sdko.dotorgredirector.config;

import io.sentry.SentryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Application configuration class. Provides beans and configuration settings for the application.
 */
@Configuration
public class AppConfig {

  /** The Spring environment. */
  private final Environment environment;

  /** Application properties. */
  private final AppProperties appProperties;

  /** Username for backend authentication. */
  @Value("${backend.auth.username}")
  private String backendUsername;

  /** Password for backend authentication. */
  @Value("${backend.auth.password}")
  private String backendPassword;

  /** Sentry DSN for error reporting. */
  @Value("${backend.sentry.dsn}")
  private String sentryDsn;

  /**
   * Constructs an AppConfig with the given environment and properties.
   *
   * @param environment The Spring environment
   * @param appProperties The application properties
   */
  public AppConfig(final Environment environment, final AppProperties appProperties) {
    this.environment = environment;
    this.appProperties = appProperties;
  }

  /**
   * Provides the application environment name. Gets environment from DOTORG_ENV or falls back to
   * Spring active profiles.
   *
   * @return The environment name (development or production)
   */
  @Bean
  public String applicationEnvironment() {
    // Get environment from DOTORG_ENV or fallback to Spring active profiles
    String env = System.getenv("DOTORG_ENV");
    if (env == null || env.isEmpty()) {
      env =
          environment.getActiveProfiles().length > 0
                  && environment.getActiveProfiles()[0].equals("dev")
              ? "development"
              : "production";
    }
    return env;
  }

  /**
   * Configures Sentry options for error reporting.
   *
   * @return The configured Sentry options
   */
  @Bean
  public SentryOptions sentryOptions() {
    SentryOptions options = new SentryOptions();
    options.setDsn(sentryDsn);

    String env = applicationEnvironment();
    options.setEnvironment(env);

    options.setRelease("dot-org@" + appProperties.getVersion());
    options.setTracesSampleRate(1.0);
    options.setDebug(env.equals("development"));
    options.setAttachStacktrace(true);

    return options;
  }

  /**
   * Provides a password encoder for secure password storage.
   *
   * @return The password encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures user details for authentication.
   *
   * @param passwordEncoder The password encoder
   * @return The user details service
   */
  @Bean
  public InMemoryUserDetailsManager userDetailsService(final PasswordEncoder passwordEncoder) {
    UserDetails user =
        User.builder()
            .username(backendUsername)
            .password(passwordEncoder.encode(backendPassword))
            .roles("ADMIN")
            .build();
    return new InMemoryUserDetailsManager(user);
  }

  /**
   * Configures security filter chain for HTTP requests.
   *
   * @param http The HTTP security configuration
   * @return The security filter chain
   * @throws Exception If an error occurs during configuration
   */
  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/backend/**").authenticated().anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults())
        .build();
  }
}
