package net.sdko.dotorgredirector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Meta-annotation for integration tests. Can be used to mark tests that require full application
 * context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@SpringBootTest(classes = {DotOrgApplication.class, TestConfig.class})
@ActiveProfiles("test")
public @interface IntegrationTest {}
