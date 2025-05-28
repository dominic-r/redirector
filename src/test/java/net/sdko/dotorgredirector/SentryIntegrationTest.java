package net.sdko.dotorgredirector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.SentryId;
import java.util.Map;
import net.sdko.dotorgredirector.config.AppProperties;
import net.sdko.dotorgredirector.config.SentryConfig;
import net.sdko.dotorgredirector.health.SentryHealthIndicator;
import net.sdko.dotorgredirector.info.CustomInfoContributor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration test for Sentry with health and info endpoints.
 * Tests the complete integration of Sentry with both health and info endpoints.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class SentryIntegrationTest {

    @Mock private AppProperties appProperties;
    
    private SentryHealthIndicator sentryHealthIndicator;
    private CustomInfoContributor customInfoContributor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(appProperties.getVersion()).thenReturn("1.0.0-test");
        
        sentryHealthIndicator = new SentryHealthIndicator();
        customInfoContributor = new CustomInfoContributor("test", appProperties);
        
        // Set a test Sentry DSN
        ReflectionTestUtils.setField(customInfoContributor, "sentryDsn", 
            "https://abcd1234@o1234.ingest.sentry.io/1234567");
    }
    
    @Test
    void testSentryHealthIndicator_enabled() {
        try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
            // Mock Sentry.isEnabled() to return true
            sentryMock.when(Sentry::isEnabled).thenReturn(true);
            
            // Mock Sentry.captureMessage to return a non-null ID
            SentryId mockId = new SentryId();
            sentryMock
                .when(() -> Sentry.captureMessage(anyString(), any(SentryLevel.class)))
                .thenReturn(mockId);
            
            Health health = sentryHealthIndicator.health();
            
            assertEquals(Status.UP, health.getStatus());
            assertEquals("Sentry Error Reporting", health.getDetails().get("component"));
            assertEquals("UP", health.getDetails().get("status"));
            assertEquals(true, health.getDetails().get("enabled"));
            assertEquals(true, health.getDetails().get("connected"));
            assertNotNull(health.getDetails().get("test_event_id"));
        }
    }
    
    @Test
    void testSentryInfoContributor() {
        try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
            // Mock Sentry.isEnabled() to return true
            sentryMock.when(Sentry::isEnabled).thenReturn(true);
            
            // Mock Sentry.captureMessage to return a non-null ID
            SentryId mockId = new SentryId();
            sentryMock
                .when(() -> Sentry.captureMessage(anyString(), any(SentryLevel.class)))
                .thenReturn(mockId);
            
            // Create and populate the info builder
            Info.Builder builder = new Info.Builder();
            customInfoContributor.contribute(builder);
            Info info = builder.build();
            
            // Check that Sentry info is included
            @SuppressWarnings("unchecked")
            Map<String, Object> sentryInfo = (Map<String, Object>) info.getDetails().get("sentry");
            assertNotNull(sentryInfo);
            assertEquals(true, sentryInfo.get("enabled"));
            assertEquals(true, sentryInfo.get("configured"));
            assertNotNull(sentryInfo.get("dsn"));
            assertEquals("test", sentryInfo.get("environment"));
            assertEquals("dot-org@1.0.0-test", sentryInfo.get("release"));
            assertNotNull(sentryInfo.get("last_test_event_id"));
        }
    }
    
    @Test
    void testSentryConfigInitialization() {
        try (MockedStatic<Sentry> sentryMock = mockStatic(Sentry.class)) {
            // Mock Sentry.isEnabled() to return true after initialization
            sentryMock.when(Sentry::isEnabled).thenReturn(true);
            
            // Create SentryConfig
            SentryConfig sentryConfig = new SentryConfig(appProperties, "test");
            
            // Set DSN
            ReflectionTestUtils.setField(sentryConfig, "sentryDsn", 
                "https://abcd1234@o1234.ingest.sentry.io/1234567");
            
            // Initialize Sentry
            sentryConfig.initializeSentry();
            
            // Verify Sentry was initialized and enabled
            sentryMock.verify(Sentry::isEnabled);
        }
    }
} 