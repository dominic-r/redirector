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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Core service responsible for building redirect URLs and managing redirect logic.
 */
@Service
public class RedirectService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectService.class);
    
    private final AppProperties appProperties;
    private final String environment;
    
    private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of(
        "x-sws-event", "x-sws-tracing-id", "x-sws-env", "x-sws-version", "x-sws-ts"
    );
    
    private static final Pattern X_SWS_EVENT_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-]{1,50}$");
    private static final Pattern X_SWS_TRACING_ID_PATTERN = Pattern.compile("^[a-fA-F0-9\\-]{36}$");
    private static final Pattern X_SWS_ENV_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-]{1,20}$");
    private static final Pattern X_SWS_VERSION_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-]{1,30}$");
    private static final Pattern X_SWS_TS_PATTERN = Pattern.compile("^[0-9]{1,15}$");
    
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
     * @throws SecurityException If the request contains dangerous patterns
     */
    public String buildRedirectUrl(HttpServletRequest request) throws URISyntaxException {
        String targetUrl = appProperties.getTargetUrl();
        String requestPath = request.getRequestURI();
        
        LOGGER.debug("Building redirect URL from {} to {}", requestPath, targetUrl);
        
        String sanitizedPath = sanitizePath(requestPath);
        
        URI targetUri = new URI(targetUrl);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(targetUri)
                .path(sanitizedPath);
        
        Map<String, String[]> filteredParams = filterQueryParameters(request.getParameterMap());
        
        String tracingId = UUID.randomUUID().toString();
        
        addTrackingParameters(builder, tracingId);
        
        filteredParams.forEach((key, values) -> {
            for (String value : values) {
                builder.queryParam(key, value);
            }
        });
        
        String redirectUrl = builder.build().toUriString();
        
        LOGGER.debug("Built redirect URL: {}", redirectUrl);
        return redirectUrl;
    }
    
    /**
     * Sanitizes the request path to prevent injection attacks.
     *
     * @param path The original request path
     * @return The sanitized path
     * @throws SecurityException If the path contains invalid characters
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "/";
        }
        
        if (!path.matches("^[a-zA-Z0-9._/\\-]*$")) {
            LOGGER.warn("Invalid characters detected in path: {}", path);
            throw new SecurityException("Invalid path contains unsafe characters");
        }
        
        path = path.replaceAll("\\.\\./", "");
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        path = path.replaceAll("//+", "/");
        
        return path;
    }
    
    /**
     * Filters query parameters to only allow whitelisted ones with proper validation.
     *
     * @param originalParams The original parameter map
     * @return Filtered parameter map
     */
    private Map<String, String[]> filterQueryParameters(Map<String, String[]> originalParams) {
        Map<String, String[]> filteredParams = new HashMap<>();
        
        for (Map.Entry<String, String[]> entry : originalParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            
            if (!ALLOWED_QUERY_PARAMS.contains(key)) {
                LOGGER.warn("Non-whitelisted query parameter detected, skipping: {}", key);
                continue;
            }
            
            List<String> validValues = new ArrayList<>();
            
            for (String value : values) {
                if (value != null && isValidParameterValue(key, value)) {
                    validValues.add(value);
                } else {
                    LOGGER.warn("Invalid parameter value detected, skipping: {}={}", key, value);
                }
            }
            
            if (!validValues.isEmpty()) {
                filteredParams.put(key, validValues.toArray(new String[0]));
            }
        }
        
        return filteredParams;
    }
    
    /**
     * Validates parameter values based on the parameter name and type.
     *
     * @param paramName The parameter name
     * @param value The parameter value to validate
     * @return true if the value is valid for this parameter type
     */
    private boolean isValidParameterValue(String paramName, String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        switch (paramName) {
            case "x-sws-tracing-id":
                return X_SWS_TRACING_ID_PATTERN.matcher(value).matches();
            
            case "x-sws-event":
                return X_SWS_EVENT_PATTERN.matcher(value).matches();
             
            case "x-sws-env":
                return X_SWS_ENV_PATTERN.matcher(value).matches();
             
            case "x-sws-version":
                return X_SWS_VERSION_PATTERN.matcher(value).matches();
            
            case "x-sws-ts":
                return X_SWS_TS_PATTERN.matcher(value).matches();
            
            default:
                return false;
        }
    }
    
    /**
     * Adds tracking parameters to the redirect URL.
     *
     * @param builder The URI builder
     * @param tracingId The tracing ID
     */
    private void addTrackingParameters(UriComponentsBuilder builder, String tracingId) {
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
        
        // Handle wildcard patterns (e.g., "/backend/*")
        if (excludePattern.endsWith("*")) {
            String prefix = excludePattern.substring(0, excludePattern.length() - 1);
            return requestPath.startsWith(prefix);
        } else {
            return requestPath.equals(excludePattern);
        }
    }
} 