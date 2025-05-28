package net.sdko.dotorgredirector.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import net.sdko.dotorgredirector.RedirectFilter;
import net.sdko.dotorgredirector.core.RedirectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.AbstractController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for application-wide settings, including security.
 */
@Configuration
@EnableWebSecurity
public class AppConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    private final Environment environment;
    private final AppProperties appProperties;
    private final VersionProvider versionProvider;
    
    @Value("${backend.auth.username}")
    private String backendUsername;
    
    @Value("${backend.auth.password}")
    private String backendPassword;
    
    /**
     * Constructs AppConfig with required dependencies.
     *
     * @param environment The Spring environment
     * @param appProperties The application properties
     * @param versionProvider The version provider
     */
    public AppConfig(
            final Environment environment,
            final AppProperties appProperties,
            final VersionProvider versionProvider) {
        this.environment = environment;
        this.appProperties = appProperties;
        this.versionProvider = versionProvider;
        
        // Initialize the app version from the version provider
        appProperties.setVersion(versionProvider.getVersion());
    }
    
    /**
     * Provides the application environment name.
     *
     * @return The environment name (development or production)
     */
    @Bean
    public String applicationEnvironment() {
        // First check for DOTORG_ENV environment variable
        String env = System.getenv("DOTORG_ENV");
        
        // Fall back to Spring active profiles
        if (env == null || env.isEmpty()) {
            env = Arrays.stream(environment.getActiveProfiles())
                    .filter(profile -> "dev".equals(profile))
                    .findFirst()
                    .map(profile -> "development")
                    .orElse("production");
        }
        
        LOGGER.info("Application environment: {}", env);
        return env;
    }
    
    /**
     * Configures and registers the redirect filter.
     *
     * @param redirectHandler The redirect handler
     * @return The filter registration bean
     */
    @Bean
    public FilterRegistrationBean<RedirectFilter> redirectFilter(
            final RedirectHandler redirectHandler) {
        
        RedirectFilter filter = new RedirectFilter(redirectHandler);
        
        FilterRegistrationBean<RedirectFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        
        // Configure URL patterns
        registrationBean.addUrlPatterns("/*");
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
    
    /**
     * Adds a handler for testing Sentry integration.
     * 
     * @return The handler mapping for Sentry test
     */
    @Bean
    public AbstractHandlerMapping sentryTestHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Integer.MAX_VALUE - 1);
        
        Map<String, Object> urlMap = new HashMap<>();
        urlMap.put("/backend/test-sentry", new AbstractController() {
            @Override
            protected org.springframework.web.servlet.ModelAndView handleRequestInternal(
                    HttpServletRequest request, HttpServletResponse response) throws Exception {
                
                LOGGER.info("Handling test-sentry request");
                
                try {
                    // Test if Sentry is enabled
                    if (!Sentry.isEnabled()) {
                        LOGGER.warn("Sentry is not enabled");
                        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"status\":\"error\",\"message\":\"Sentry is not enabled\"}");
                        return null;
                    }
                    
                    // Send a test message to Sentry
                    LOGGER.info("Sending test message to Sentry");
                    Sentry.captureMessage("Test message from /backend/test-sentry endpoint", io.sentry.SentryLevel.INFO);
                    
                    // Simulate an exception for testing error reporting
                    try {
                        throw new RuntimeException("Test exception from /backend/test-sentry endpoint");
                    } catch (Exception e) {
                        Sentry.captureException(e);
                    }
                    
                    // Return success response
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                            "{\"status\":\"success\",\"message\":\"Test events sent to Sentry\"}");
                    
                } catch (Exception e) {
                    LOGGER.error("Error in test-sentry endpoint", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                            "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
                }
                
                return null;
            }
        });
        
        mapping.setUrlMap(urlMap);
        return mapping;
    }
}
