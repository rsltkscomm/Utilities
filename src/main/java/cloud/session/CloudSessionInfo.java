package cloud.session;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cloud Session Information
 * Contains detailed information about a cloud testing session
 */
public class CloudSessionInfo {
    
    private final String sessionId;
    private final String sessionName;
    private final String provider;
    private final String browser;
    private final String platform;
    private final String version;
    private final String status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long durationSeconds;
    private final String videoUrl;
    private final String screenshotUrl;
    private final String publicUrl;
    private final String reason;
    private final Map<String, Object> metadata;
    private final Map<String, String> logs;
    private final Map<String, Object> performanceMetrics;
    
    private CloudSessionInfo(Builder builder) {
        this.sessionId = builder.sessionId;
        this.sessionName = builder.sessionName;
        this.provider = builder.provider;
        this.browser = builder.browser;
        this.platform = builder.platform;
        this.version = builder.version;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.durationSeconds = builder.durationSeconds;
        this.videoUrl = builder.videoUrl;
        this.screenshotUrl = builder.screenshotUrl;
        this.publicUrl = builder.publicUrl;
        this.reason = builder.reason;
        this.metadata = builder.metadata;
        this.logs = builder.logs;
        this.performanceMetrics = builder.performanceMetrics;
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getSessionName() { return sessionName; }
    public String getProvider() { return provider; }
    public String getBrowser() { return browser; }
    public String getPlatform() { return platform; }
    public String getVersion() { return version; }
    public String getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getDurationSeconds() { return durationSeconds; }
    public String getVideoUrl() { return videoUrl; }
    public String getScreenshotUrl() { return screenshotUrl; }
    public String getPublicUrl() { return publicUrl; }
    public String getReason() { return reason; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Map<String, String> getLogs() { return logs; }
    public Map<String, Object> getPerformanceMetrics() { return performanceMetrics; }
    
    /**
     * Check if session is completed
     */
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "stopped".equalsIgnoreCase(status);
    }
    
    /**
     * Check if session was successful
     */
    public boolean isSuccessful() {
        return "completed".equalsIgnoreCase(status);
    }
    
    /**
     * Check if session failed
     */
    public boolean isFailed() {
        return "failed".equalsIgnoreCase(status);
    }
    
    /**
     * Check if session is still running
     */
    public boolean isRunning() {
        return "active".equalsIgnoreCase(status) || "creating".equalsIgnoreCase(status);
    }
    
    /**
     * Get session summary
     */
    public String getSummary() {
        return String.format("Session %s: %s on %s %s %s - Status: %s", 
            sessionId, browser, platform, version, provider, status);
    }
    
    /**
     * Builder class
     */
    public static class Builder {
        private String sessionId;
        private String sessionName;
        private String provider;
        private String browser;
        private String platform;
        private String version;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationSeconds;
        private String videoUrl;
        private String screenshotUrl;
        private String publicUrl;
        private String reason;
        private Map<String, Object> metadata;
        private Map<String, String> logs;
        private Map<String, Object> performanceMetrics;
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder sessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }
        
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder browser(String browser) {
            this.browser = browser;
            return this;
        }
        
        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder durationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }
        
        public Builder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }
        
        public Builder screenshotUrl(String screenshotUrl) {
            this.screenshotUrl = screenshotUrl;
            return this;
        }
        
        public Builder publicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder logs(Map<String, String> logs) {
            this.logs = logs;
            return this;
        }
        
        public Builder performanceMetrics(Map<String, Object> performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
            return this;
        }
        
        public CloudSessionInfo build() {
            return new CloudSessionInfo(this);
        }
    }
}
