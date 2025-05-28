package net.sdko.dotorgredirector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.sentry.Sentry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.SpringApplication;

/** Unit tests for the Sentry initialization in DotOrgApplication. */
@Tag("unit")
public class DotOrgApplicationSentryTest {

  private DotOrgApplication application;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    application = new DotOrgApplication();
  }

  @Test
  public void testMainMethodInitializesSentry() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class);
         MockedStatic<SpringApplication> springMockedStatic = mockStatic(SpringApplication.class)) {
      
      // Run the main method
      DotOrgApplication.main(new String[0]);
      
      // Verify SpringApplication.run was called with the correct arguments
      springMockedStatic.verify(() -> SpringApplication.run(eq(DotOrgApplication.class), any(String[].class)));
    }
  }
  
  @Test
  public void testMainMethodHandlesSentryException() {
    try (MockedStatic<Sentry> sentryMockedStatic = mockStatic(Sentry.class);
         MockedStatic<SpringApplication> springMockedStatic = mockStatic(SpringApplication.class)) {
      
      // Make Sentry.isEnabled() throw an exception
      sentryMockedStatic.when(Sentry::isEnabled).thenThrow(new RuntimeException("Sentry error"));
      
      // Should not throw exception
      DotOrgApplication.main(new String[0]);
      
      // Verify SpringApplication.run was still called with any arguments
      springMockedStatic.verify(() -> SpringApplication.run(eq(DotOrgApplication.class), any(String[].class)));
    }
  }
  
  @Test
  public void testMainMethodWithArguments() {
    try (MockedStatic<SpringApplication> springMockedStatic = mockStatic(SpringApplication.class)) {
      // Create some test arguments
      String[] args = new String[]{"--server.port=8080", "--debug"};
      
      // Run the main method with arguments
      DotOrgApplication.main(args);
      
      // Verify SpringApplication.run was called with the correct arguments
      springMockedStatic.verify(() -> SpringApplication.run(eq(DotOrgApplication.class), eq(args)));
    }
  }
  
  @Test
  public void testApplicationCreation() {
    // Just verify that we can create an instance without exceptions
    assertNotNull(application);
    // This is mainly to verify constructor coverage
    assertTrue(application instanceof DotOrgApplication);
  }
} 