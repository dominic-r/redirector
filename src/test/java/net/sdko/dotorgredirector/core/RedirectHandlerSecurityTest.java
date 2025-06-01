package net.sdko.dotorgredirector.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sdko.dotorgredirector.config.AppProperties;
import net.sdko.dotorgredirector.metrics.RedirectMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

/**
 * Security-focused unit tests for the RedirectHandler.
 * These tests verify that security exceptions are handled properly.
 */
@Tag("unit")
public class RedirectHandlerSecurityTest {

    private RedirectHandler redirectHandler;

    @Mock
    private RedirectService mockRedirectService;

    @Mock
    private MonitoringService mockMonitoringService;

    @Mock
    private RedirectMetrics mockRedirectMetrics;

    private AppProperties appProperties;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        appProperties = new AppProperties();
        appProperties.setTargetUrl("https://www.d-roy.ca");
        appProperties.setExcludePattern("/backend/*");
        appProperties.setRedirectStatusCode(302);

        redirectHandler = new RedirectHandler(
            mockRedirectService,
            mockMonitoringService,
            mockRedirectMetrics,
            appProperties
        );
    }

    @Test
    public void testHandleRedirect_SecurityException_ReturnsBadRequest() throws Exception {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/javascript:alert('xss')");

        // Mock exclude check to return false (not excluded)
        when(mockRedirectService.shouldExcludeFromRedirect("/javascript:alert('xss')", "/backend/*"))
            .thenReturn(false);

        // Mock transaction
        io.sentry.ITransaction mockTransaction = mock(io.sentry.ITransaction.class);
        when(mockMonitoringService.startRedirectTransaction(any(), anyString(), anyString()))
            .thenReturn(mockTransaction);

        // Mock buildRedirectUrl to throw SecurityException
        when(mockRedirectService.buildRedirectUrl(request))
            .thenThrow(new SecurityException("Invalid path contains dangerous pattern: javascript:"));

        // Execute
        boolean result = redirectHandler.handleRedirect(request, response);

        // Verify
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Invalid request"));
        assertTrue(response.getErrorMessage().contains("javascript:"));

        // Verify monitoring
        verify(mockMonitoringService, never()).captureException(any(SecurityException.class));
        verify(mockTransaction).finish();
    }

    @Test
    public void testHandleRedirect_OtherException_ReturnsInternalServerError() throws Exception {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/test");

        // Mock exclude check to return false (not excluded)
        when(mockRedirectService.shouldExcludeFromRedirect("/test", "/backend/*"))
            .thenReturn(false);

        // Mock transaction
        io.sentry.ITransaction mockTransaction = mock(io.sentry.ITransaction.class);
        when(mockMonitoringService.startRedirectTransaction(any(), anyString(), anyString()))
            .thenReturn(mockTransaction);

        // Mock buildRedirectUrl to throw RuntimeException
        when(mockRedirectService.buildRedirectUrl(request))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Execute
        boolean result = redirectHandler.handleRedirect(request, response);

        // Verify
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Redirect failed"));
        assertTrue(response.getErrorMessage().contains("Database connection failed"));

        // Verify monitoring
        verify(mockMonitoringService).captureException(any(RuntimeException.class));
        verify(mockTransaction).finish();
    }

    @Test
    public void testHandleRedirect_ExcludedPath_NotProcessed() throws Exception {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/backend/healthz");

        // Mock exclude check to return true (excluded)
        when(mockRedirectService.shouldExcludeFromRedirect("/backend/healthz", "/backend/*"))
            .thenReturn(true);

        // Execute
        boolean result = redirectHandler.handleRedirect(request, response);

        // Verify
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus()); // No error set

        // Verify that redirect service was not called
        verify(mockRedirectService, never()).buildRedirectUrl(any());
        verify(mockMonitoringService, never()).startRedirectTransaction(any(), anyString(), anyString());
    }

    @Test
    public void testHandleRedirect_SuccessfulRedirect() throws Exception {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/search");

        // Mock exclude check to return false (not excluded)
        when(mockRedirectService.shouldExcludeFromRedirect("/search", "/backend/*"))
            .thenReturn(false);

        // Mock transaction and spans
        io.sentry.ITransaction mockTransaction = mock(io.sentry.ITransaction.class);
        io.sentry.ISpan mockBuildSpan = mock(io.sentry.ISpan.class);
        io.sentry.ISpan mockRedirectSpan = mock(io.sentry.ISpan.class);
        
        when(mockMonitoringService.startRedirectTransaction(any(), anyString(), anyString()))
            .thenReturn(mockTransaction);
        when(mockMonitoringService.startSpan(mockTransaction, "build_redirect_url"))
            .thenReturn(mockBuildSpan);
        when(mockMonitoringService.startSpan(mockTransaction, "send_redirect"))
            .thenReturn(mockRedirectSpan);

        // Mock successful URL building
        String expectedUrl = "https://www.d-roy.ca/search?x-sws-event=dot-org-redirect";
        when(mockRedirectService.buildRedirectUrl(request))
            .thenReturn(expectedUrl);

        // Execute
        boolean result = redirectHandler.handleRedirect(request, response);

        // Verify
        assertTrue(result);
        assertEquals(302, response.getStatus());
        assertEquals(expectedUrl, response.getHeader("Location"));

        // Verify monitoring
        verify(mockMonitoringService).finishSpanSuccess(mockBuildSpan);
        verify(mockMonitoringService).finishSpanSuccess(mockRedirectSpan);
        verify(mockTransaction).finish();
        verify(mockTransaction).setData("redirect_url", expectedUrl);
    }

    @Test
    public void testHandleRedirect_SecurityExceptionWithMetrics() throws Exception {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/<script>alert('xss')</script>");

        // Mock exclude check to return false (not excluded)
        when(mockRedirectService.shouldExcludeFromRedirect("/<script>alert('xss')</script>", "/backend/*"))
            .thenReturn(false);

        // Mock transaction
        io.sentry.ITransaction mockTransaction = mock(io.sentry.ITransaction.class);
        when(mockMonitoringService.startRedirectTransaction(any(), anyString(), anyString()))
            .thenReturn(mockTransaction);

        // Mock metrics
        io.micrometer.core.instrument.Timer mockTimer = mock(io.micrometer.core.instrument.Timer.class);
        when(mockRedirectMetrics.getRedirectTimer()).thenReturn(mockTimer);
        when(mockTimer.recordCallable(any())).thenThrow(new SecurityException("Invalid path contains dangerous pattern: <script"));

        // Mock buildRedirectUrl to throw SecurityException
        when(mockRedirectService.buildRedirectUrl(request))
            .thenThrow(new SecurityException("Invalid path contains dangerous pattern: <script"));

        // Execute
        boolean result = redirectHandler.handleRedirect(request, response);

        // Verify
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Invalid request"));

        // Verify metrics were attempted to be recorded
        verify(mockRedirectMetrics).incrementRedirectCount();
        // SecurityExceptions are not reported to Sentry
        verify(mockMonitoringService, never()).captureException(any(SecurityException.class));
        verify(mockTransaction).finish();
    }

    @Test
    public void testHandleRedirect_MultipleSecurityViolations() throws Exception {
        String[] maliciousPaths = {
            "/javascript:alert('xss')",
            "/data:text/html,<script>",
            "/<script>alert('xss')</script>",
            "/vbscript:msgbox('xss')",
            "/file:///etc/passwd"
        };

        for (String maliciousPath : maliciousPaths) {
            // Setup
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI(maliciousPath);

            // Mock exclude check to return false (not excluded)
            when(mockRedirectService.shouldExcludeFromRedirect(maliciousPath, "/backend/*"))
                .thenReturn(false);

            // Mock transaction
            io.sentry.ITransaction mockTransaction = mock(io.sentry.ITransaction.class);
            when(mockMonitoringService.startRedirectTransaction(any(), anyString(), anyString()))
                .thenReturn(mockTransaction);

            // Mock buildRedirectUrl to throw SecurityException
            when(mockRedirectService.buildRedirectUrl(request))
                .thenThrow(new SecurityException("Invalid path contains dangerous pattern"));

            // Execute
            boolean result = redirectHandler.handleRedirect(request, response);

            // Verify
            assertFalse(result, "Failed for path: " + maliciousPath);
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), 
                "Wrong status code for path: " + maliciousPath);
            assertTrue(response.getErrorMessage().contains("Invalid request"), 
                "Wrong error message for path: " + maliciousPath);

            // Reset mocks for next iteration
            reset(mockRedirectService, mockMonitoringService, mockTransaction);
        }
    }

    @Test
    public void testHandleRedirect_IOExceptionDuringErrorHandling() throws Exception {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // Create a response that will throw IOException when sendError is called
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        doThrow(new IOException("Network error")).when(mockResponse).sendError(anyInt(), anyString());

        request.setRequestURI("/javascript:alert('xss')");

        // Mock exclude check to return false (not excluded)
        when(mockRedirectService.shouldExcludeFromRedirect("/javascript:alert('xss')", "/backend/*"))
            .thenReturn(false);

        // Mock transaction
        io.sentry.ITransaction mockTransaction = mock(io.sentry.ITransaction.class);
        when(mockMonitoringService.startRedirectTransaction(any(), anyString(), anyString()))
            .thenReturn(mockTransaction);

        // Mock buildRedirectUrl to throw SecurityException
        when(mockRedirectService.buildRedirectUrl(request))
            .thenThrow(new SecurityException("Invalid path contains dangerous pattern: javascript:"));

        // Execute - should not throw exception despite IOException in error handling
        boolean result = redirectHandler.handleRedirect(request, mockResponse);

        // Verify
        assertFalse(result);

        // Verify that exceptions were captured - SecurityExceptions are not reported to Sentry
        verify(mockMonitoringService, never()).captureException(any(SecurityException.class));
        verify(mockTransaction).finish();
    }
} 