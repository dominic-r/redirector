package net.sdko.dotorgredirector.core;

import io.micrometer.core.annotation.Timed;
import io.sentry.ISpan;
import io.sentry.ITransaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sdko.dotorgredirector.config.AppProperties;
import net.sdko.dotorgredirector.metrics.RedirectMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Handler for processing redirect requests.
 * Coordinates the redirect process using the core services.
 */
@Component
public class RedirectHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectHandler.class);
    
    private final RedirectService redirectService;
    private final MonitoringService monitoringService;
    private final RedirectMetrics redirectMetrics;
    private final AppProperties appProperties;
    
    /**
     * Constructs a RedirectHandler with the required dependencies.
     *
     * @param redirectService The redirect service
     * @param monitoringService The monitoring service
     * @param redirectMetrics The redirect metrics
     * @param appProperties The application properties
     */
    public RedirectHandler(
            RedirectService redirectService, 
            MonitoringService monitoringService,
            RedirectMetrics redirectMetrics,
            AppProperties appProperties) {
        this.redirectService = redirectService;
        this.monitoringService = monitoringService;
        this.redirectMetrics = redirectMetrics;
        this.appProperties = appProperties;
    }
    
    /**
     * Handles a redirect request.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @return true if the request was handled, false otherwise
     */
    @Timed(value = "redirect.request", description = "Time taken to process redirect requests")
    public boolean handleRedirect(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String tracingId = UUID.randomUUID().toString();
        
        // Set the tracing ID for logging
        request.setAttribute("tracingId", tracingId);
        
        // Check if this request should be excluded from redirection
        if (shouldSkipRedirect(requestURI)) {
            LOGGER.debug("Skipping redirect for excluded path: {}", requestURI);
            return false;
        }
        
        // Start a Sentry transaction
        ITransaction transaction = monitoringService.startRedirectTransaction(
                request, tracingId, appProperties.getTargetUrl());
        
        try {
            // Record metrics if available and timer is not null
            if (redirectMetrics != null && redirectMetrics.getRedirectTimer() != null) {
                redirectMetrics.incrementRedirectCount();
                return redirectMetrics.getRedirectTimer().recordCallable(() -> 
                        performRedirect(request, response, transaction));
            } else {
                if (redirectMetrics != null) {
                    redirectMetrics.incrementRedirectCount();
                }
                return performRedirect(request, response, transaction);
            }
        } catch (SecurityException e) {
            LOGGER.warn("Security violation in redirect request: {}", e.getMessage());
            monitoringService.finishSpanError(transaction, e);
            return handleRedirectError(response, e);
        } catch (Exception e) {
            LOGGER.error("Error during redirect", e);
            monitoringService.captureException(e);
            monitoringService.finishSpanError(transaction, e);
            return handleRedirectError(response, e);
        } finally {
            transaction.finish();
        }
    }
    
    /**
     * Checks if a redirect should be skipped for this path.
     *
     * @param requestURI The request URI
     * @return true if redirect should be skipped, false otherwise
     */
    private boolean shouldSkipRedirect(String requestURI) {
        return redirectService.shouldExcludeFromRedirect(
                requestURI, appProperties.getExcludePattern());
    }
    
    /**
     * Performs the actual redirect.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param transaction The Sentry transaction
     * @return true if redirect was successful, false otherwise
     * @throws Exception if an error occurs
     */
    private boolean performRedirect(
            HttpServletRequest request, 
            HttpServletResponse response, 
            ITransaction transaction) throws Exception {
        
        // Create a span for building the URL
        ISpan buildUrlSpan = monitoringService.startSpan(transaction, "build_redirect_url");
        
        String redirectUrl;
        try {
            redirectUrl = redirectService.buildRedirectUrl(request);
            monitoringService.finishSpanSuccess(buildUrlSpan);
        } catch (Exception e) {
            monitoringService.finishSpanError(buildUrlSpan, e);
            throw e;
        }
        
        LOGGER.info("Redirecting to: {}", redirectUrl);
        
        // Set attribute for logging/monitoring
        request.setAttribute("redirected_to", redirectUrl);
        transaction.setData("redirect_url", redirectUrl);
        
        // Create a span for the actual redirect
        ISpan redirectSpan = monitoringService.startSpan(transaction, "send_redirect");
        try {
            // Perform the redirect with the configured status code
            response.setStatus(appProperties.getRedirectStatusCode());
            response.setHeader("Location", redirectUrl);
            response.getWriter().flush();
            monitoringService.finishSpanSuccess(redirectSpan);
            return true;
        } catch (Exception e) {
            monitoringService.finishSpanError(redirectSpan, e);
            throw e;
        }
    }
    
    /**
     * Handles errors during redirect.
     *
     * @param response The HTTP response
     * @param exception The exception that occurred
     * @return false to indicate the redirect failed
     */
    private boolean handleRedirectError(HttpServletResponse response, Exception exception) {
        try {
            if (exception instanceof SecurityException) {
                LOGGER.warn("Security violation in redirect request: {}", exception.getMessage());
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST, 
                        "Invalid request: " + exception.getMessage());
            } else {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Redirect failed: " + exception.getMessage());
            }
        } catch (IOException ioe) {
            LOGGER.error("Failed to send error response", ioe);
        }
        return false;
    }
} 