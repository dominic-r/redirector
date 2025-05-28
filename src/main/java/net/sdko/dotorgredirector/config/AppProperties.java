package net.sdko.dotorgredirector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

/**
 * Application configuration properties loaded from application.properties.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {
  
  /**
   * The target URL to which requests will be redirected.
   */
  @NotBlank
  private String targetUrl = "https://www.d-roy.ca";
  
  /**
   * The application version.
   */
  private String version;
  
  /**
   * Flag to enable debug mode.
   */
  private boolean debug = false;
  
  /**
   * Pattern for paths that should be excluded from redirection.
   */
  private String excludePattern = "/backend/*";
  
  /**
   * HTTP status code to use for redirects.
   */
  private int redirectStatusCode = 302;

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
   * @param targetUrl The new target URL
   */
  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
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
   * Sets the application version.
   *
   * @param version The version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Checks if debug mode is enabled.
   *
   * @return true if debug is enabled, false otherwise
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * Sets the debug mode.
   *
   * @param debug The debug mode to set
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Gets the exclude pattern for paths that should not be redirected.
   *
   * @return The exclude pattern
   */
  public String getExcludePattern() {
    return excludePattern;
  }

  /**
   * Sets the exclude pattern.
   *
   * @param excludePattern The exclude pattern to set
   */
  public void setExcludePattern(String excludePattern) {
    this.excludePattern = excludePattern;
  }
  
  /**
   * Gets the HTTP status code to use for redirects.
   *
   * @return The HTTP status code
   */
  public int getRedirectStatusCode() {
    return redirectStatusCode;
  }
  
  /**
   * Sets the HTTP status code to use for redirects.
   *
   * @param redirectStatusCode The HTTP status code to set
   */
  public void setRedirectStatusCode(int redirectStatusCode) {
    this.redirectStatusCode = redirectStatusCode;
  }
}
