package net.sdko.dotorgredirector;

import net.sdko.dotorgredirector.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application class for the .org redirector.
 * Entry point for the Spring Boot application.
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DotOrgApplication {

  /**
   * Main method to start the application.
   *
   * @param args Command-line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(DotOrgApplication.class, args);
  }
}
