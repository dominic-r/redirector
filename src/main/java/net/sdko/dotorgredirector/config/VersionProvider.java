package net.sdko.dotorgredirector.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Component responsible for providing the application version.
 * Reads version from the file system and handles fallbacks.
 */
@Component
public class VersionProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionProvider.class);
    private static final String DEFAULT_VERSION = "unknown-version";
    
    @Value("${app.version.file:/app/VERSIONFILE}")
    private String versionFilePath;
    
    /**
     * Gets the application version.
     * Reads from the version file or falls back to a default.
     *
     * @return The application version
     */
    public String getVersion() {
        // First try to read from version file
        String version = readVersionFromFile();
        
        // Fall back to default if needed
        if (version == null || version.isBlank()) {
            LOGGER.warn("Using default version because version file could not be read");
            version = DEFAULT_VERSION;
        }
        
        LOGGER.info("Application version: {}", version);
        return version;
    }
    
    /**
     * Reads the version from the configured version file.
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
                LOGGER.warn("Version file exists but is empty or contains only whitespace");
            } catch (IOException e) {
                LOGGER.warn("Failed to read version from file: {}", e.getMessage());
            }
        } else {
            LOGGER.debug("Version file not found at {}", versionFilePath);
        }
        
        return null;
    }
} 