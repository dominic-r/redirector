package net.sdko.dotorgredirector.core;

import io.sentry.IHub;
import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.TransactionOptions;
import io.sentry.protocol.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling application monitoring and Sentry integration.
 */
@Service
public class MonitoringService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringService.class);
    private final IHub sentryHub;
    
    /**
     * Constructs a MonitoringService with Sentry hub.
     *
     * @param sentryHub The Sentry hub
     */
    public MonitoringService(IHub sentryHub) {
        this.sentryHub = sentryHub;
    }
    
    /**
     * Starts a transaction for monitoring a redirect request.
     *
     * @param request The HTTP request
     * @param tracingId The tracing ID
     * @param targetUrl The target URL for redirection
     * @return A Sentry transaction
     */
    public ITransaction startRedirectTransaction(
            HttpServletRequest request, 
            String tracingId, 
            String targetUrl) {
        
        String requestURI = request.getRequestURI();
        
        // Configure transaction options
        TransactionOptions options = new TransactionOptions();
        options.setBindToScope(true);
        
        // Start the transaction
        ITransaction transaction = sentryHub.startTransaction(
                "Redirect " + requestURI, 
                "redirect", 
                options);
        
        // Configure the Sentry scope
        Sentry.configureScope(scope -> {
            // Add request information
            scope.setTag("request_uri", requestURI);
            scope.setTag("target_url", targetUrl);
            scope.setTag("tracing_id", tracingId);
            
            // Add user IP information
            User user = new User();
            user.setIpAddress(request.getRemoteAddr());
            scope.setUser(user);
            
            // Add request headers as context using setContexts
            Map<String, String> requestData = new HashMap<>();
            requestData.put("user_agent", request.getHeader("User-Agent"));
            requestData.put("referer", request.getHeader("Referer"));
            scope.setContexts("request", requestData);
        });
        
        LOGGER.debug("Started Sentry transaction for request: {}", requestURI);
        return transaction;
    }
    
    /**
     * Starts a child span within a transaction.
     *
     * @param transaction The parent transaction
     * @param operation The operation name
     * @return The created span
     */
    public ISpan startSpan(ITransaction transaction, String operation) {
        return transaction.startChild(operation);
    }
    
    /**
     * Completes a span with success status.
     *
     * @param span The span to complete
     */
    public void finishSpanSuccess(ISpan span) {
        span.setStatus(SpanStatus.OK);
        span.finish();
    }
    
    /**
     * Completes a span with error status.
     *
     * @param span The span to complete
     * @param throwable The error that occurred
     */
    public void finishSpanError(ISpan span, Throwable throwable) {
        span.setStatus(SpanStatus.INTERNAL_ERROR);
        span.setThrowable(throwable);
        span.finish();
    }
    
    /**
     * Captures an exception in Sentry.
     *
     * @param throwable The exception to capture
     */
    public void captureException(Throwable throwable) {
        Sentry.captureException(throwable);
    }
} 