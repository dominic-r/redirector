package net.sdko.dotorgredirector.config;

import io.sentry.IHub;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.protocol.SentryId;
import io.sentry.spring.jakarta.SentryExceptionResolver;
import io.sentry.spring.jakarta.SentryTaskDecorator;
import io.sentry.spring.jakarta.tracing.TransactionNameProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Configuration for Sentry error reporting.
 * Initializes and configures Sentry for the application.
 */
@Configuration
public class SentryConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SentryConfig.class);
    
    @Value("${backend.sentry.dsn:}")
    private String sentryDsn;
    
    private final AppProperties appProperties;
    private final String applicationEnvironment;
    
    /**
     * Constructs a SentryConfig with the required dependencies.
     *
     * @param appProperties The application properties
     * @param applicationEnvironment The application environment
     */
    public SentryConfig(
            final AppProperties appProperties,
            final String applicationEnvironment) {
        this.appProperties = appProperties;
        this.applicationEnvironment = applicationEnvironment;
    }
    
    /**
     * Initializes Sentry when the application starts.
     */
    @PostConstruct
    public void initializeSentry() {
        LOGGER.info("Initializing Sentry with DSN: {}", maskSentryDsn(sentryDsn));
        
        try {
            if (sentryDsn == null || sentryDsn.isEmpty()) {
                LOGGER.warn("No Sentry DSN configured. Sentry error reporting will be disabled.");
                return;
            }
            
            SentryOptions options = new SentryOptions();
            options.setDsn(sentryDsn);
            options.setEnvironment(applicationEnvironment);
            options.setRelease("dot-org@" + appProperties.getVersion());
            options.setTracesSampleRate(1.0);
            
            // Set debug to false to reduce log verbosity
            options.setDebug(false);
            
            options.setAttachStacktrace(true);
            options.setEnableExternalConfiguration(true);
            
            // Add a before-send callback to log events
            options.setBeforeSend((event, hint) -> {
                if (event.getLevel() != null && event.getLevel().ordinal() <= SentryLevel.INFO.ordinal()) {
                    // Log a simple message for INFO and DEBUG level events
                    LOGGER.debug("Sending {} level event to Sentry", event.getLevel());
                } else {
                    // Log more details for WARNING, ERROR and FATAL events
                    LOGGER.info("Sending {} level event to Sentry: {}", 
                        event.getLevel(), 
                        event.getMessage() != null ? event.getMessage().getMessage() : event.getEventId());
                }
                return event;
            });
            
            // Initialize Sentry with these options
            Sentry.init(options);
            
            // Verify initialization
            if (Sentry.isEnabled()) {
                LOGGER.info("Sentry successfully initialized and enabled");
                
                // Test event
                SentryId eventId = Sentry.captureMessage("Sentry initialized successfully", SentryLevel.INFO);
                LOGGER.info("Sent initialization test event with ID: {}", eventId);
            } else {
                LOGGER.warn("Sentry initialization completed but Sentry is not enabled");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Sentry: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Configures Sentry options.
     *
     * @return The configured Sentry options
     */
    @Bean
    public SentryOptions sentryOptions() {
        SentryOptions options = new SentryOptions();
        options.setDsn(sentryDsn);
        options.setEnvironment(applicationEnvironment);
        options.setRelease("dot-org@" + appProperties.getVersion());
        options.setTracesSampleRate(1.0);
        
        // Set debug to false to reduce log verbosity
        options.setDebug(false);
        
        options.setAttachStacktrace(true);
        options.setEnableExternalConfiguration(true);
        
        // Add a before-send callback to log events
        options.setBeforeSend((event, hint) -> {
            if (event.getLevel() != null && event.getLevel().ordinal() <= SentryLevel.INFO.ordinal()) {
                // Log a simple message for INFO and DEBUG level events
                LOGGER.debug("Sending {} level event to Sentry", event.getLevel());
            } else {
                // Log more details for WARNING, ERROR and FATAL events
                LOGGER.info("Sending {} level event to Sentry: {}", 
                    event.getLevel(), 
                    event.getMessage() != null ? event.getMessage().getMessage() : event.getEventId());
            }
            return event;
        });
        
        return options;
    }
    
    /**
     * Creates a Sentry exception resolver for handling exceptions.
     *
     * @return The Sentry exception resolver
     */
    @Bean
    public HandlerExceptionResolver sentryExceptionResolver() {
        TransactionNameProvider transactionNameProvider = request -> 
                request.getMethod() + " " + request.getRequestURI();
        
        return new SentryExceptionResolver(
                Sentry.getCurrentHub(), 
                transactionNameProvider,
                Ordered.HIGHEST_PRECEDENCE);
    }
    
    /**
     * Creates a task decorator for Sentry to capture exceptions in async tasks.
     *
     * @return The Sentry task decorator
     */
    @Bean
    public TaskDecorator sentryTaskDecorator() {
        return new SentryTaskDecorator();
    }
    
    /**
     * Provides the Sentry hub for dependency injection.
     *
     * @return The current Sentry hub
     */
    @Bean
    public IHub sentryHub() {
        return Sentry.getCurrentHub();
    }
    
    /**
     * Masks the Sentry DSN for logging to avoid exposing sensitive information.
     *
     * @param dsn The Sentry DSN
     * @return A masked version of the DSN
     */
    private String maskSentryDsn(String dsn) {
        if (dsn == null || dsn.isEmpty()) {
            return "empty";
        }
        
        // Simple masking to avoid exposing full DSN in logs
        int atIndex = dsn.indexOf('@');
        if (atIndex > 0) {
            return dsn.substring(0, Math.min(8, atIndex)) + "..." + dsn.charAt(atIndex) + "...";
        }
        
        return dsn.substring(0, Math.min(8, dsn.length())) + "...";
    }
}
