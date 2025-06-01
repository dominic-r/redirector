package net.sdko.dotorgredirector.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import net.sdko.dotorgredirector.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Security-focused unit tests for the RedirectService.
 * These tests verify that all security measures are working correctly.
 */
@Tag("unit")
public class RedirectServiceSecurityTest {

    private static final String TARGET_URL = "https://www.d-roy.ca";
    private static final String TEST_ENVIRONMENT = "test";

    private RedirectService redirectService;
    private AppProperties appProperties;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        appProperties = new AppProperties();
        appProperties.setTargetUrl(TARGET_URL);
        appProperties.setVersion("1.0.0-test");
        
        redirectService = new RedirectService(appProperties, TEST_ENVIRONMENT);
    }

    // ========== PATH SANITIZATION TESTS ==========

    @Test
    public void testSanitizePath_NormalPath() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/normal/path");
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.startsWith(TARGET_URL + "/normal/path"));
    }

    @Test
    public void testSanitizePath_PathTraversalBlocked() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/../../etc/passwd");
        
        assertDoesNotThrow(() -> {
            String result = redirectService.buildRedirectUrl(request);
            // Path traversal should be removed
            assertTrue(result.contains("/etc/passwd"));
            assertFalse(result.contains("../"));
        });
    }

    @Test
    public void testSanitizePath_JavaScriptInjectionBlocked() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/javascript:alert('xss')");
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            redirectService.buildRedirectUrl(request);
        });
        
        assertTrue(exception.getMessage().contains("unsafe characters"));
    }

    @Test
    public void testSanitizePath_DataUrlBlocked() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/data:text/html,<script>alert('xss')</script>");
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            redirectService.buildRedirectUrl(request);
        });
        
        assertTrue(exception.getMessage().contains("unsafe characters"));
    }

    @Test
    public void testSanitizePath_ScriptTagBlocked() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/<script>alert('xss')</script>");
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            redirectService.buildRedirectUrl(request);
        });
        
        assertTrue(exception.getMessage().contains("unsafe characters"));
    }

    @Test
    public void testSanitizePath_OnloadAttributeBlocked() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test?onload=alert('xss')");
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            redirectService.buildRedirectUrl(request);
        });
        
        assertTrue(exception.getMessage().contains("unsafe characters"));
    }

    @Test
    public void testSanitizePath_DoubleSlashesRemoved() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("//test//path//");
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("/test/path/"));
        // Check that the URL doesn't contain double slashes after the domain
        String pathPart = result.substring(result.indexOf("://") + 3);
        pathPart = pathPart.substring(pathPart.indexOf("/"));
        assertFalse(pathPart.contains("//"));
    }

    @Test
    public void testSanitizePath_NullPathHandled() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(null);
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.startsWith(TARGET_URL + "/"));
    }

    // ========== QUERY PARAMETER FILTERING TESTS ==========

    @Test
    public void testQueryParameterFiltering_WhitelistedParamsAllowed() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "test-event");
        request.setParameter("x-sws-env", "test");
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-event=test-event"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_NonWhitelistedParamsBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "test-event"); // allowed
        request.setParameter("utm_source", "google"); // not allowed
        request.setParameter("custom_param", "value"); // not allowed
        request.setParameter("user_id", "12345"); // not allowed
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-event=test-event"));
        assertFalse(result.contains("utm_source"));
        assertFalse(result.contains("custom_param"));
        assertFalse(result.contains("user_id"));
    }

    @Test
    public void testQueryParameterValidation_XSwsEventValidation() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "valid-event_123");
        request.setParameter("x-sws-env", "invalid@env"); // invalid chars
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-event=valid-event_123"));
        assertFalse(result.contains("x-sws-env=invalid@env"));
    }

    @Test
    public void testQueryParameterValidation_TracingIdValidation() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        // Valid UUID format
        request.setParameter("x-sws-tracing-id", "123e4567-e89b-12d3-a456-426614174000");
        
        String result = redirectService.buildRedirectUrl(request);
        
        // Should contain the valid tracing ID
        assertTrue(result.contains("x-sws-tracing-id"));
    }

    @Test
    public void testQueryParameterValidation_InvalidTracingId() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        // Invalid UUID format
        request.setParameter("x-sws-tracing-id", "invalid-uuid");
        request.setParameter("x-sws-env", "test"); // should still work
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-env=test"));
        // The invalid tracing ID should be filtered out, but automatic one should be added
        assertTrue(result.contains("x-sws-tracing-id"));
    }

    @Test
    public void testQueryParameterValidation_TimestampValidation() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        long currentTime = System.currentTimeMillis() / 1000;
        request.setParameter("x-sws-ts", String.valueOf(currentTime));
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-ts"));
    }

    @Test
    public void testQueryParameterValidation_InvalidTimestamp() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        // Invalid timestamp format
        request.setParameter("x-sws-ts", "not-a-number");
        request.setParameter("x-sws-env", "test"); // should still work
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-env=test"));
        // The invalid timestamp should be filtered out, but automatic one should be added
        assertTrue(result.contains("x-sws-ts"));
    }

    @Test
    public void testQueryParameterValidation_XSwsParams() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "valid-event");
        request.setParameter("x-sws-env", "production");
        request.setParameter("x-sws-version", "1.0.0-release");
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-event=valid-event"));
        assertTrue(result.contains("x-sws-env=production"));
        assertTrue(result.contains("x-sws-version=1.0.0-release"));
    }

    @Test
    public void testQueryParameterValidation_InvalidCharactersBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "invalid<script>event"); // invalid chars
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("x-sws-event=invalid"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterValidation_ValueLengthLimits() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "a".repeat(51)); // too long for event
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        // The long value should not be present, but automatic x-sws-event should be
        assertFalse(result.contains("a".repeat(51)));
        assertTrue(result.contains("x-sws-env=test"));
        assertTrue(result.contains("x-sws-event=dot-org-redirect")); // automatic value should be there
    }

    @Test
    public void testQueryParameterFiltering_NormalParams() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "redirect");
        request.setParameter("x-sws-env", "test");
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.contains("x-sws-event=redirect"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_JavaScriptInParamNameBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("javascript:alert", "value"); // not whitelisted
        request.setParameter("x-sws-env", "test"); // whitelisted
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("javascript:alert"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_JavaScriptInParamValueBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "javascript:alert"); // invalid chars blocked by regex
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("javascript:alert"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_ScriptTagInValueBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "<script>alert"); // invalid chars blocked by regex
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("<script"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_EncodedAttackBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "%3Cscript%3E"); // invalid chars blocked by regex
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("%3C"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_HTMLEntitiesBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("x-sws-event", "&lt;script&gt;"); // invalid chars blocked by regex
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("&lt;"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_LongValueBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        
        // Create a value longer than allowed for event (50 chars)
        String longValue = "a".repeat(51);
        request.setParameter("x-sws-event", longValue);
        request.setParameter("x-sws-env", "test"); // valid
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains(longValue));
        assertTrue(result.contains("x-sws-env=test"));
    }

    @Test
    public void testQueryParameterFiltering_InvalidParamNameBlocked() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setParameter("param with spaces", "value"); // not whitelisted
        request.setParameter("param@special", "value"); // not whitelisted
        request.setParameter("x-sws-env", "test"); // whitelisted
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertFalse(result.contains("param+with+spaces"));
        assertFalse(result.contains("param%40special"));
        assertTrue(result.contains("x-sws-env=test"));
    }

    // ========== URL VALIDATION TESTS ==========

    @Test
    public void testUrlBuilding_ValidRedirectUrl() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        
        String result = redirectService.buildRedirectUrl(request);
        
        assertTrue(result.startsWith("https://www.d-roy.ca"));
        assertTrue(result.contains("/test"));
    }

    // ========== EXCLUDE PATTERN TESTS ==========

    @Test
    public void testExcludePattern_BasicPrefixPattern() {
        assertTrue(redirectService.shouldExcludeFromRedirect("/backend/test", "/backend/*"));
        assertFalse(redirectService.shouldExcludeFromRedirect("/frontend/test", "/backend/*"));
    }

    @Test
    public void testExcludePattern_ExactMatch() {
        assertTrue(redirectService.shouldExcludeFromRedirect("/exact", "/exact"));
        assertFalse(redirectService.shouldExcludeFromRedirect("/exact/path", "/exact"));
    }

    @Test
    public void testExcludePattern_EmptyPattern() {
        assertFalse(redirectService.shouldExcludeFromRedirect("/any/path", ""));
        assertFalse(redirectService.shouldExcludeFromRedirect("/any/path", null));
    }

    @Test
    public void testExcludePattern_PrefixPattern() {
        assertTrue(redirectService.shouldExcludeFromRedirect("/api/v1/test", "/api/*"));
        assertTrue(redirectService.shouldExcludeFromRedirect("/api/v2/users", "/api/*"));
        assertFalse(redirectService.shouldExcludeFromRedirect("/web/api/test", "/api/*"));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    public void testBuildRedirectUrl_CompleteFlow() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/search");
        request.setParameter("x-sws-event", "redirect");
        request.setParameter("x-sws-env", "test");
        request.setParameter("malicious", "javascript:alert('xss')"); // Should be filtered out (not whitelisted)
        
        String result = redirectService.buildRedirectUrl(request);
        
        // Should contain valid whitelisted parameters
        assertTrue(result.contains("x-sws-event=redirect"));
        assertTrue(result.contains("x-sws-env=test"));
        
        // Should not contain non-whitelisted parameter
        assertFalse(result.contains("malicious"));
        
        // Should contain automatic tracking parameters
        assertTrue(result.contains("x-sws-event=dot-org-redirect"));
        assertTrue(result.contains("x-sws-env=" + TEST_ENVIRONMENT));
        assertTrue(result.contains("x-sws-version=1.0.0-test"));
        
        // Should start with target URL
        assertTrue(result.startsWith(TARGET_URL + "/search"));
    }

    @Test
    public void testBuildRedirectUrl_AllowedParameterTypes() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        
        // Test all allowed x-sws parameter types
        request.setParameter("x-sws-event", "custom-event");
        request.setParameter("x-sws-env", "staging");
        request.setParameter("x-sws-version", "2.0.0");
        request.setParameter("x-sws-tracing-id", "123e4567-e89b-12d3-a456-426614174000");
        request.setParameter("x-sws-ts", "1640995200");
        
        String result = redirectService.buildRedirectUrl(request);
        
        // All whitelisted parameters should be present
        assertTrue(result.contains("x-sws-event="));
        assertTrue(result.contains("x-sws-env="));
        assertTrue(result.contains("x-sws-version="));
        assertTrue(result.contains("x-sws-tracing-id"));
        assertTrue(result.contains("x-sws-ts"));
    }

    @Test
    public void testBuildRedirectUrl_ParameterValidationEdgeCases() throws URISyntaxException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        
        // Edge cases for parameter validation
        request.setParameter("x-sws-event", ""); // empty value - should be filtered
        request.setParameter("x-sws-env", "a".repeat(20)); // max length for env
        request.setParameter("x-sws-version", "valid.version-123_test"); // all valid chars
        request.setParameter("x-sws-tracing-id", "not-a-uuid"); // invalid format
        
        String result = redirectService.buildRedirectUrl(request);
        
        // Empty value should be filtered out
        assertFalse(result.contains("x-sws-event=&"));
        
        // Max length should work
        assertTrue(result.contains("x-sws-env=" + "a".repeat(20)));
        
        // Valid characters should work
        assertTrue(result.contains("x-sws-version=valid.version-123_test"));
        
        // Invalid tracing ID should be filtered, but automatic one added
        assertTrue(result.contains("x-sws-tracing-id"));
    }

    @Test
    public void testBuildRedirectUrl_AllDangerousPatternsBlocked() {
        String[] dangerousPaths = {
            "/javascript:alert('xss')",
            "/data:text/html,<script>",
            "/vbscript:msgbox('xss')",
            "/file:///etc/passwd",
            "/ftp://evil.com/",
            "/<script>alert('xss')</script>",
            "/</script><script>alert('xss')</script>",
            "/test?onload=alert('xss')",
            "/test?onerror=alert('xss')",
            "/test?onclick=alert('xss')"
        };
        
        for (String dangerousPath : dangerousPaths) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(dangerousPath);
            
            assertThrows(SecurityException.class, () -> {
                redirectService.buildRedirectUrl(request);
            }, "Failed to block dangerous path: " + dangerousPath);
        }
    }

    @Test
    public void testBuildRedirectUrl_SafePathsAllowed() throws URISyntaxException {
        String[] safePaths = {
            "/",
            "/home",
            "/search",
            "/api/users",
            "/docs/readme.txt",
            "/images/logo.png",
            "/css/style.css",
            "/js/app.js"
        };
        
        for (String safePath : safePaths) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(safePath);
            
            assertDoesNotThrow(() -> {
                String result = redirectService.buildRedirectUrl(request);
                assertTrue(result.startsWith(TARGET_URL));
                assertTrue(result.contains(safePath));
            }, "Safe path was incorrectly blocked: " + safePath);
        }
    }
} 