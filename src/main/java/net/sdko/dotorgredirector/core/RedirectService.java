package net.sdko.dotorgredirector.core;

import jakarta.servlet.http.HttpServletRequest;
import net.sdko.dotorgredirector.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core service responsible for building redirect URLs and managing redirect logic.
 */
@Service
public class RedirectService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectService.class);
    
    private final AppProperties appProperties;
    private final String environment;
    
    /**
     * Constructs a RedirectService with required dependencies.
     *
     * @param appProperties The application properties
     * @param environment The application environment
     */
    public RedirectService(AppProperties appProperties, String environment) {
        this.appProperties = appProperties;
        this.environment = environment;
    }
    
    /**
     * Builds a redirect URL for the given request.
     *
     * @param request The HTTP request to build a redirect URL for
     * @return The redirect URL
     * @throws URISyntaxException If the target URL is invalid
     */
    public String buildRedirectUrl(HttpServletRequest request) throws URISyntaxException {
        String targetUrl = appProperties.getTargetUrl();
        LOGGER.debug("Building redirect URL from {} to {}", request.getRequestURI(), targetUrl);
        
        // Start with target URL and add original path
        URI targetUri = new URI(targetUrl);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(targetUri)
                .path(request.getRequestURI());
        
        // Preserve original query parameters
        Map<String, String[]> originalParams = new HashMap<>(request.getParameterMap());
        
        // Generate UUID for tracing
        String tracingId = UUID.randomUUID().toString();
        
        // Add tracking parameters as x-sws-* parameters
        addTrackingParameters(builder, tracingId);
        
        // Add original query parameters
        originalParams.forEach((key, values) -> {
            for (String value : values) {
                builder.queryParam(key, value);
            }
        });
        
        String redirectUrl = builder.build().toUriString();
        LOGGER.debug("Built redirect URL: {}", redirectUrl);
        return redirectUrl;
    }
    
    /**
     * Adds tracking parameters to the redirect URL.
     *
     * @param builder The URI builder
     * @param tracingId The tracing ID
     */
    private void addTrackingParameters(UriComponentsBuilder builder, String tracingId) {
        // Add x-sws-* tracking parameters
        builder.queryParam("x-sws-event", "dot-org-redirect");
        builder.queryParam("x-sws-tracing-id", tracingId);
        builder.queryParam("x-sws-env", environment);
        builder.queryParam("x-sws-version", appProperties.getVersion());
        builder.queryParam("x-sws-ts", Instant.now().getEpochSecond());
    }
    
    /**
     * Checks if a request should be excluded from redirection.
     *
     * @param requestPath The request path to check
     * @param excludePattern The pattern for excluded paths
     * @return true if the request should be excluded, false otherwise
     */
    public boolean shouldExcludeFromRedirect(String requestPath, String excludePattern) {
        if (excludePattern == null || excludePattern.isEmpty()) {
            return false;
        }
        
        // Convert glob pattern to regex pattern (simple conversion)
        String regexPattern = excludePattern.replace("*", ".*");
        
        return requestPath.matches(regexPattern);
    }
} 