package patterns.builder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for test execution settings.
 * This class holds all the configuration parameters needed for test execution.
 */
public class TestConfiguration {
    
    private final String browser;
    private final boolean headless;
    private final String environment;
    private final Duration timeout;
    private final String baseUrl;
    private final boolean remote;
    private final String remoteUrl;
    private final int threadCount;
    private final boolean retryEnabled;
    private final int maxRetries;
    private final boolean screenshotOnFailure;
    private final boolean videoRecording;
    private final Map<String, String> customProperties;
    
    private TestConfiguration(Builder builder) {
        this.browser = builder.browser;
        this.headless = builder.headless;
        this.environment = builder.environment;
        this.timeout = builder.timeout;
        this.baseUrl = builder.baseUrl;
        this.remote = builder.remote;
        this.remoteUrl = builder.remoteUrl;
        this.threadCount = builder.threadCount;
        this.retryEnabled = builder.retryEnabled;
        this.maxRetries = builder.maxRetries;
        this.screenshotOnFailure = builder.screenshotOnFailure;
        this.videoRecording = builder.videoRecording;
        this.customProperties = new HashMap<>(builder.customProperties);
    }
    
    // Getters
    public String getBrowser() { return browser; }
    public boolean isHeadless() { return headless; }
    public String getEnvironment() { return environment; }
    public Duration getTimeout() { return timeout; }
    public String getBaseUrl() { return baseUrl; }
    public boolean isRemote() { return remote; }
    public String getRemoteUrl() { return remoteUrl; }
    public int getThreadCount() { return threadCount; }
    public boolean isRetryEnabled() { return retryEnabled; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isScreenshotOnFailure() { return screenshotOnFailure; }
    public boolean isVideoRecording() { return videoRecording; }
    public Map<String, String> getCustomProperties() { return new HashMap<>(customProperties); }
    
    public String getCustomProperty(String key) {
        return customProperties.get(key);
    }
    
    public String getCustomProperty(String key, String defaultValue) {
        return customProperties.getOrDefault(key, defaultValue);
    }
    
    /**
     * Builder class for TestConfiguration.
     */
    public static class Builder {
        private String browser = "chrome";
        private boolean headless = false;
        private String environment = "test";
        private Duration timeout = Duration.ofSeconds(30);
        private String baseUrl = "";
        private boolean remote = false;
        private String remoteUrl = "";
        private int threadCount = 1;
        private boolean retryEnabled = true;
        private int maxRetries = 1;
        private boolean screenshotOnFailure = true;
        private boolean videoRecording = false;
        private Map<String, String> customProperties = new HashMap<>();
        
        public Builder browser(String browser) {
            this.browser = browser;
            return this;
        }
        
        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }
        
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }
        
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder timeoutSeconds(int seconds) {
            this.timeout = Duration.ofSeconds(seconds);
            return this;
        }
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder remote(boolean remote) {
            this.remote = remote;
            return this;
        }
        
        public Builder remoteUrl(String remoteUrl) {
            this.remoteUrl = remoteUrl;
            this.remote = true;
            return this;
        }
        
        public Builder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }
        
        public Builder retryEnabled(boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder screenshotOnFailure(boolean screenshotOnFailure) {
            this.screenshotOnFailure = screenshotOnFailure;
            return this;
        }
        
        public Builder videoRecording(boolean videoRecording) {
            this.videoRecording = videoRecording;
            return this;
        }
        
        public Builder customProperty(String key, String value) {
            this.customProperties.put(key, value);
            return this;
        }
        
        public Builder customProperties(Map<String, String> properties) {
            this.customProperties.putAll(properties);
            return this;
        }
        
        public TestConfiguration build() {
            validate();
            return new TestConfiguration(this);
        }
        
        private void validate() {
            if (browser == null || browser.trim().isEmpty()) {
                throw new IllegalArgumentException("Browser cannot be null or empty");
            }
            if (environment == null || environment.trim().isEmpty()) {
                throw new IllegalArgumentException("Environment cannot be null or empty");
            }
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            if (threadCount < 1) {
                throw new IllegalArgumentException("Thread count must be at least 1");
            }
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries cannot be negative");
            }
            if (remote && (remoteUrl == null || remoteUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("Remote URL is required when remote is enabled");
            }
        }
    }
    
    @Override
    public String toString() {
        return "TestConfiguration{" +
                "browser='" + browser + '\'' +
                ", headless=" + headless +
                ", environment='" + environment + '\'' +
                ", timeout=" + timeout +
                ", baseUrl='" + baseUrl + '\'' +
                ", remote=" + remote +
                ", threadCount=" + threadCount +
                ", retryEnabled=" + retryEnabled +
                ", maxRetries=" + maxRetries +
                '}';
    }
}
