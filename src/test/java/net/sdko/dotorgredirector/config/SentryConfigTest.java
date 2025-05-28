package net.sdko.dotorgredirector.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.sentry.IHub;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.spring.jakarta.SentryExceptionResolver;
import io.sentry.spring.jakarta.SentryTaskDecorator;
import io.sentry.spring.jakarta.tracing.TransactionNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.TaskDecorator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

class SentryConfigTest {

  @Mock private AppProperties appProperties;
  @Mock private SentryOptions mockOptions;
  @Mock private IHub mockHub;
  
  private SentryConfig sentryConfig;
  
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(appProperties.getVersion()).thenReturn("1.0.0");
    when(appProperties.isDebug()).thenReturn(false);
    
    // Create a real SentryConfig instance
    sentryConfig = new SentryConfig(appProperties, "test");
    
    // Set the sentryDsn field via reflection
    ReflectionTestUtils.setField(sentryConfig, "sentryDsn", "https://test@sentry.io/123");
  }
  
  @Test
  void testSentryOptions() {
    // Get the Sentry options
    SentryOptions options = sentryConfig.sentryOptions();
    
    // Verify the options are configured correctly
    assertEquals("https://test@sentry.io/123", options.getDsn());
    assertEquals("test", options.getEnvironment());
    assertEquals("dot-org@1.0.0", options.getRelease());
    assertEquals(1.0, options.getTracesSampleRate());
    assertFalse(options.isDebug()); // Debug should be false now
    assertTrue(options.isAttachStacktrace());
    assertTrue(options.isEnableExternalConfiguration());
    
    // Verify that beforeSend callback is set
    assertNotNull(options.getBeforeSend());
  }
  
  @Test
  void testSentryOptionsWithDebugEnabled() {
    // Set debug to true
    when(appProperties.isDebug()).thenReturn(true);
    
    // Get the Sentry options
    SentryOptions options = sentryConfig.sentryOptions();
    
    // Verify debug is still false (we override it in the implementation)
    assertFalse(options.isDebug());
  }
  
  @Test
  void testSentryOptionsWithDevelopmentEnvironment() {
    // Create a new config with development environment
    SentryConfig devConfig = new SentryConfig(appProperties, "development");
    
    // Set the sentryDsn field via reflection
    ReflectionTestUtils.setField(devConfig, "sentryDsn", "https://test@sentry.io/123");
    
    // Get the Sentry options
    SentryOptions options = devConfig.sentryOptions();
    
    // Verify debug is still false (we override it in the implementation)
    assertFalse(options.isDebug());
  }
  
  @Test
  void testInitializeSentry() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      // Mock Sentry.isEnabled to return true
      sentryMockedStatic.when(Sentry::isEnabled).thenReturn(true);
      
      // Call the initialization method
      sentryConfig.initializeSentry();
      
      // Verify Sentry.isEnabled was called
      sentryMockedStatic.verify(Sentry::isEnabled);
    }
  }
  
  @Test
  void testInitializeSentryWithException() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      // Make Sentry.isEnabled throw an exception
      sentryMockedStatic.when(Sentry::isEnabled).thenThrow(new RuntimeException("Sentry error"));
      
      // Should not throw exception
      sentryConfig.initializeSentry();
      
      // Verify Sentry.isEnabled was called
      sentryMockedStatic.verify(Sentry::isEnabled);
    }
  }
  
  @Test
  void testMaskSentryDsnWithNullDsn() {
    // Test with null DSN
    String result = invokePrivateMaskMethod(null);
    assertEquals("empty", result);
  }
  
  @Test
  void testMaskSentryDsnWithEmptyDsn() {
    // Test with empty DSN
    String result = invokePrivateMaskMethod("");
    assertEquals("empty", result);
  }
  
  @Test
  void testMaskSentryDsnWithValidDsn() {
    // Test with valid DSN that contains an @ symbol
    String result = invokePrivateMaskMethod("https://abc123@sentry.io/123");
    assertEquals("https://...@...", result);
  }
  
  @Test
  void testMaskSentryDsnWithNoAtSymbol() {
    // Test with DSN that doesn't contain an @ symbol
    String result = invokePrivateMaskMethod("https://sentry.io/123");
    assertEquals("https://...", result);
  }
  
  @Test
  void testBeforeSendCallback() {
    // Get the Sentry options
    SentryOptions options = sentryConfig.sentryOptions();
    
    // Create a test event
    SentryEvent testEvent = new SentryEvent();
    testEvent.setLevel(SentryLevel.INFO);
    testEvent.setMessage(new io.sentry.protocol.Message());
    testEvent.getMessage().setMessage("Test message");
    
    // Execute the beforeSend callback
    SentryEvent result = options.getBeforeSend().execute(testEvent, null);
    
    // Verify the event was returned unmodified
    assertSame(testEvent, result);
  }
  
  @Test
  void testSentryExceptionResolver() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      // Mock getCurrentHub
      sentryMockedStatic.when(Sentry::getCurrentHub).thenReturn(mockHub);
      
      // Get the exception resolver bean
      HandlerExceptionResolver resolver = sentryConfig.sentryExceptionResolver();
      
      // Verify it's an instance of SentryExceptionResolver
      assertTrue(resolver instanceof SentryExceptionResolver);
    }
  }
  
  @Test
  void testSentryTaskDecorator() {
    // Get the task decorator bean
    TaskDecorator decorator = sentryConfig.sentryTaskDecorator();
    
    // Verify it's an instance of SentryTaskDecorator
    assertTrue(decorator instanceof SentryTaskDecorator);
  }
  
  @Test
  void testSentryHub() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class)) {
      // Mock getCurrentHub
      sentryMockedStatic.when(Sentry::getCurrentHub).thenReturn(mockHub);
      
      // Get the hub bean
      IHub hub = sentryConfig.sentryHub();
      
      // Verify it's the same hub returned by Sentry.getCurrentHub()
      assertSame(mockHub, hub);
    }
  }
  
  // Helper method to invoke the private maskSentryDsn method
  private String invokePrivateMaskMethod(String dsn) {
    return (String) ReflectionTestUtils.invokeMethod(sentryConfig, "maskSentryDsn", dsn);
  }
} 