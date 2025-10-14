package cloud.session;

import org.openqa.selenium.WebDriver;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cloud Session Interface
 * Represents a cloud testing session
 */
public interface CloudSession {
    
    /**
     * Get session ID
     */
    String getSessionId();
    
    /**
     * Get session name
     */
    String getSessionName();
    
    /**
     * Get session status
     */
    SessionStatus getStatus();
    
    /**
     * Get WebDriver instance
     */
    WebDriver getWebDriver();
    
    /**
     * Get session information
     */
    CloudSessionInfo getSessionInfo();
    
    /**
     * Update session status
     */
    boolean updateStatus(String status, String reason);
    
    /**
     * Stop the session
     */
    boolean stop();
    
    /**
     * Check if session is active
     */
    boolean isActive();
    
    /**
     * Get session start time
     */
    LocalDateTime getStartTime();
    
    /**
     * Get session end time
     */
    LocalDateTime getEndTime();
    
    /**
     * Get session duration in seconds
     */
    long getDurationSeconds();
    
    /**
     * Get session metadata
     */
    Map<String, Object> getMetadata();
    
    /**
     * Add metadata to session
     */
    void addMetadata(String key, Object value);
    
    /**
     * Get session logs
     */
    Map<String, String> getLogs();
    
    /**
     * Get session video URL
     */
    String getVideoUrl();
    
    /**
     * Get session screenshot URL
     */
    String getScreenshotUrl();
    
    /**
     * Get session performance metrics
     */
    Map<String, Object> getPerformanceMetrics();
    
    /**
     * Session status enumeration
     */
    enum SessionStatus {
        CREATING("Creating"),
        ACTIVE("Active"),
        PAUSED("Paused"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        STOPPED("Stopped"),
        TIMEOUT("Timeout"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        SessionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
