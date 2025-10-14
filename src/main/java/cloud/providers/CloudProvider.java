package cloud.providers;

import cloud.CloudConfiguration;
import cloud.session.CloudSession;
import cloud.session.CloudSessionInfo;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.Map;

/**
 * Base interface for cloud testing providers
 * Defines common operations for all cloud testing platforms
 */
public interface CloudProvider {
    
    /**
     * Get provider name
     */
    String getProviderName();
    
    /**
     * Get provider display name
     */
    String getDisplayName();
    
    /**
     * Initialize provider with configuration
     */
    void initialize(CloudConfiguration config);
    
    /**
     * Check if provider is properly configured
     */
    boolean isConfigured();
    
    /**
     * Validate provider credentials
     */
    boolean validateCredentials();
    
    /**
     * Create cloud session
     */
    CloudSession createSession(String sessionName, DesiredCapabilities capabilities);
    
    /**
     * Create cloud session with custom capabilities
     */
    CloudSession createSession(String sessionName, Map<String, Object> customCapabilities);
    
    /**
     * Get session information
     */
    CloudSessionInfo getSessionInfo(String sessionId);
    
    /**
     * Update session status
     */
    boolean updateSessionStatus(String sessionId, String status, String reason);
    
    /**
     * Stop session
     */
    boolean stopSession(String sessionId);
    
    /**
     * Get session video URL
     */
    String getSessionVideoUrl(String sessionId);
    
    /**
     * Get session screenshot URL
     */
    String getSessionScreenshotUrl(String sessionId);
    
    /**
     * Get session logs
     */
    Map<String, String> getSessionLogs(String sessionId);
    
    /**
     * Get available browsers
     */
    Map<String, Object> getAvailableBrowsers();
    
    /**
     * Get available platforms
     */
    Map<String, Object> getAvailablePlatforms();
    
    /**
     * Get provider status
     */
    ProviderStatus getProviderStatus();
    
    /**
     * Get provider statistics
     */
    Map<String, Object> getProviderStatistics();
    
    /**
     * Build capabilities for specific browser/platform combination
     */
    DesiredCapabilities buildCapabilities(String browser, String platform, String version);
    
    /**
     * Build mobile capabilities
     */
    DesiredCapabilities buildMobileCapabilities(String platform, String device, String version);
    
    /**
     * Get provider-specific configuration
     */
    Map<String, Object> getProviderConfiguration();
    
    /**
     * Set provider-specific configuration
     */
    void setProviderConfiguration(Map<String, Object> config);
    
    /**
     * Cleanup provider resources
     */
    void cleanup();
    
    /**
     * Provider status enumeration
     */
    enum ProviderStatus {
        ACTIVE("Active"),
        INACTIVE("Inactive"),
        MAINTENANCE("Maintenance"),
        ERROR("Error"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        ProviderStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
