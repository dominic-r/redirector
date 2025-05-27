package net.sdko.dotorgredirector.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for determining the application version. This class reads the version from a file
 * and provides it as a bean.
 */
@Configuration
public class VersionConfig {
  /** Logger for this class. */
  private static final Logger LOGGER = LoggerFactory.getLogger(VersionConfig.class);

  /** Path to the file containing the version information. */
  private static String versionFilePath = "/app/VERSIONFILE";

  /** Default version to use if the version cannot be determined. */
  private static final String DEFAULT_VERSION = "unknown-version";

  /**
   * Provides the application version as a bean. Reads the version from a file, falling back to a
   * default if necessary.
   *
   * @return The application version string
   */
  @Bean
  public String applicationVersion() {
    // Read from VERSIONFILE
    String version = readVersionFromFile();

    // If still not set, use default version
    if (version == null || version.isBlank()) {
      version = DEFAULT_VERSION;
    }

    LOGGER.info("Application version: {}", version);
    return version;
  }

  /**
   * Reads the version from the version file.
   *
   * @return The version string, or null if it could not be read
   */
  private String readVersionFromFile() {
    File versionFile = new File(versionFilePath);
    if (versionFile.exists() && versionFile.canRead()) {
      try {
        String version = Files.readString(Paths.get(versionFilePath)).trim();
        if (!version.isBlank()) {
          return version;
        }
      } catch (IOException e) {
        LOGGER.warn("Failed to read version from file: {}", e.getMessage());
      }
    } else {
      LOGGER.debug("VERSIONFILE not found at {}", versionFilePath);
    }
    return null;
  }
}
