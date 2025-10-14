package cloud.session.impl;

import cloud.CloudConfiguration;
import cloud.session.CloudSession;
import cloud.session.CloudSessionInfo;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import reporting.TestLogManager;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SauceLabs Cloud Session Implementation
 */
public class SauceLabsSession implements CloudSession {
    
    private final String sessionId;
    private final String sessionName;
    private final CloudConfiguration config;
    private final DesiredCapabilities capabilities;
    private final Map<String, Object> metadata;
    private final Map<String, String> logs;
    private final LocalDateTime startTime;
    
    private WebDriver webDriver;
    private SessionStatus status;
    private LocalDateTime endTime;
    private String reason;
    private String videoUrl;
    private String screenshotUrl;
    private String publicUrl;
    
    public SauceLabsSession(String sessionName, DesiredCapabilities capabilities, CloudConfiguration config) {
        this.sessionId = generateSessionId();
        this.sessionName = sessionName;
        this.config = config;
        this.capabilities = capabilities;
        this.metadata = new ConcurrentHashMap<>();
        this.logs = new ConcurrentHashMap<>();
        this.startTime = LocalDateTime.now();
        this.status = SessionStatus.CREATING;
        
        initializeSession();
    }
    
    /**
     * Initialize the session
     */
    private void initializeSession() {
        try {
            TestLogManager.info("Initializing SauceLabs session: " + sessionId);
            
            // Create remote WebDriver
            URL hubUrl = URI.create(config.getHubUrl()).toURL();
            this.webDriver = new RemoteWebDriver(hubUrl, capabilities);
            
            // Update status
            this.status = SessionStatus.ACTIVE;
            
            // Add metadata
            addMetadata("provider", "SauceLabs");
            addMetadata("sessionId", sessionId);
            addMetadata("sessionName", sessionName);
            addMetadata("startTime", startTime);
            addMetadata("capabilities", capabilities.asMap());
            
            TestLogManager.success("SauceLabs session initialized: " + sessionId);
            
        } catch (Exception e) {
            this.status = SessionStatus.FAILED;
            this.reason = "Failed to initialize session: " + e.getMessage();
            TestLogManager.error("Failed to initialize SauceLabs session", e);
            throw new RuntimeException("Failed to initialize SauceLabs session", e);
        }
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public String getSessionName() {
        return sessionName;
    }
    
    @Override
    public SessionStatus getStatus() {
        return status;
    }
    
    @Override
    public WebDriver getWebDriver() {
        return webDriver;
    }
    
    @Override
    public CloudSessionInfo getSessionInfo() {
        return new CloudSessionInfo.Builder()
            .sessionId(sessionId)
            .sessionName(sessionName)
            .provider("SauceLabs")
            .browser((String) capabilities.getCapability("browserName"))
            .platform((String) capabilities.getCapability("platform"))
            .version((String) capabilities.getCapability("version"))
            .status(status.getDisplayName())
            .startTime(startTime)
            .endTime(endTime)
            .durationSeconds(getDurationSeconds())
            .videoUrl(videoUrl)
            .screenshotUrl(screenshotUrl)
            .publicUrl(publicUrl)
            .reason(reason)
            .metadata(new HashMap<>(metadata))
            .logs(new HashMap<>(logs))
            .performanceMetrics(getPerformanceMetrics())
            .build();
    }
    
    @Override
    public boolean updateStatus(String status, String reason) {
        try {
            this.status = SessionStatus.valueOf(status.toUpperCase());
            this.reason = reason;
            
            addMetadata("lastStatusUpdate", LocalDateTime.now());
            addMetadata("statusReason", reason);
            
            TestLogManager.info("Updated SauceLabs session status: " + sessionId + " - " + status);
            return true;
            
        } catch (IllegalArgumentException e) {
            TestLogManager.warning("Invalid status: " + status);
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        try {
            TestLogManager.info("Stopping SauceLabs session: " + sessionId);
            
            if (webDriver != null) {
                webDriver.quit();
            }
            
            this.status = SessionStatus.STOPPED;
            this.endTime = LocalDateTime.now();
            this.reason = "Session stopped by user";
            
            addMetadata("endTime", endTime);
            addMetadata("stopReason", reason);
            
            TestLogManager.success("SauceLabs session stopped: " + sessionId);
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to stop SauceLabs session: " + sessionId, e);
            this.status = SessionStatus.FAILED;
            this.reason = "Failed to stop session: " + e.getMessage();
            return false;
        }
    }
    
    @Override
    public boolean isActive() {
        return status == SessionStatus.ACTIVE || status == SessionStatus.CREATING;
    }
    
    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    @Override
    public long getDurationSeconds() {
        if (endTime != null) {
            return java.time.Duration.between(startTime, endTime).getSeconds();
        } else {
            return java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        }
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    @Override
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    @Override
    public Map<String, String> getLogs() {
        return new HashMap<>(logs);
    }
    
    @Override
    public String getVideoUrl() {
        // TODO: Implement SauceLabs API call to get video URL
        return videoUrl;
    }
    
    @Override
    public String getScreenshotUrl() {
        // TODO: Implement SauceLabs API call to get screenshot URL
        return screenshotUrl;
    }
    
    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("duration", getDurationSeconds());
        metrics.put("status", status.getDisplayName());
        metrics.put("provider", "SauceLabs");
        
        // TODO: Add more performance metrics from SauceLabs
        return metrics;
    }
    
    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "sl-" + System.currentTimeMillis() + "-" + Thread.currentThread().threadId();
    }
    
    /**
     * Add log entry
     */
    public void addLog(String level, String message) {
        logs.put(LocalDateTime.now().toString(), level + ": " + message);
    }
    
    /**
     * Set video URL
     */
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    /**
     * Set screenshot URL
     */
    public void setScreenshotUrl(String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }
    
    /**
     * Set public URL
     */
    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }
}
