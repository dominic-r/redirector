package net.sdko.dotorgredirector.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the application. This class holds configuration values that can be
 * set via application properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  /** The target URL to which requests will be redirected. */
  private String targetUrl = "https://www.d-roy.ca";

  /** The application version. */
  private String version;

  /**
   * Constructs an AppProperties instance with the given application version.
   *
   * @param applicationVersion The version of the application
   */
  @Autowired
  public AppProperties(@Qualifier("applicationVersion") final String applicationVersion) {
    this.version = applicationVersion;
  }

  /**
   * Gets the target URL.
   *
   * @return The target URL
   */
  public String getTargetUrl() {
    return targetUrl;
  }

  /**
   * Sets the target URL.
   *
   * @param newTargetUrl The new target URL
   */
  public void setTargetUrl(final String newTargetUrl) {
    this.targetUrl = newTargetUrl;
  }

  /**
   * Gets the application version.
   *
   * @return The application version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Required for Spring Boot property binding. The version is primarily set by the constructor
   * using the applicationVersion bean, but this setter allows overriding via application
   * properties.
   *
   * @param newVersion The version to set
   */
  public void setVersion(final String newVersion) {
    if (newVersion != null && !newVersion.equals("unknown-version")) {
      this.version = newVersion;
    }
  }
}
