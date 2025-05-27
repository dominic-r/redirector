package net.sdko.dotorgredirector.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VersionConfigTest {

  @TempDir Path tempDir;

  private VersionConfig versionConfig;
  private Path versionFilePath;

  @BeforeEach
  void setUp() {
    versionConfig = new VersionConfig();
    versionFilePath = tempDir.resolve("VERSIONFILE");

    // Use reflection to replace the default path with our test path
    ReflectionTestUtils.setField(versionConfig, "versionFilePath", versionFilePath.toString());
  }

  @Test
  void shouldReadVersionFromFile() throws IOException {
    // Arrange
    Files.writeString(versionFilePath, "1.2.3");

    // Act
    String version = versionConfig.applicationVersion();

    // Assert
    assertEquals("1.2.3", version);
  }

  @Test
  void shouldTrimVersionFromFile() throws IOException {
    // Arrange
    Files.writeString(versionFilePath, "  1.2.3  \n");

    // Act
    String version = versionConfig.applicationVersion();

    // Assert
    assertEquals("1.2.3", version);
  }

  @Test
  void shouldUseDefaultVersionWhenFileIsEmpty() throws IOException {
    // Arrange
    Files.writeString(versionFilePath, "");

    // Act
    String version = versionConfig.applicationVersion();

    // Assert
    assertEquals("unknown-version", version);
  }

  @Test
  void shouldUseDefaultVersionWhenFileNotExists() {
    // Act
    String version = versionConfig.applicationVersion();

    // Assert
    assertEquals("unknown-version", version);
  }
}
