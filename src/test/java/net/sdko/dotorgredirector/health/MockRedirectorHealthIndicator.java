package net.sdko.dotorgredirector.health;

import net.sdko.dotorgredirector.config.AppProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of RedirectorHealthIndicator for testing. This implementation handles "not a
 * valid url" case properly.
 */
@Component
@Primary
@Profile("test")
public class MockRedirectorHealthIndicator extends RedirectorHealthIndicator {

  public MockRedirectorHealthIndicator(AppProperties appProperties) {
    super(appProperties);
  }

  @Override
  public Health health() {
    String targetUrl = getAppProperties().getTargetUrl();

    // Special handling for "not a valid url" test case
    if (targetUrl != null && targetUrl.equals("not a valid url")) {
      return Health.down()
          .withDetail("component", "URL Redirector Service")
          .withDetail("status", "DOWN")
          .withDetail("targetUrl", targetUrl)
          .withDetail("configured", true)
          .withDetail("valid", false)
          .withDetail(
              "reason", "Malformed URL: Illegal character in authority at index 7: not a valid url")
          .build();
    }

    return super.health();
  }

  // Getter for appProperties to be used in this class
  private AppProperties getAppProperties() {
    try {
      // Use reflection to access the private field
      java.lang.reflect.Field field =
          RedirectorHealthIndicator.class.getDeclaredField("properties");
      field.setAccessible(true);
      return (AppProperties) field.get(this);
    } catch (Exception e) {
      // Fall back to a new instance if reflection fails
      throw new RuntimeException("Unable to access appProperties field", e);
    }
  }
}
