package patterns.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Data class representing the result of a test execution.
 */
public class TestResult {
    
    public enum Status {
        PASS, FAIL, SKIP, ERROR
    }
    
    private final String testName;
    private final Status status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String errorMessage;
    private final String stackTrace;
    private final Map<String, String> additionalInfo;
    private final String screenshotPath;
    private final String videoPath;
    
    private TestResult(Builder builder) {
        this.testName = builder.testName;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.errorMessage = builder.errorMessage;
        this.stackTrace = builder.stackTrace;
        this.additionalInfo = new HashMap<>(builder.additionalInfo);
        this.screenshotPath = builder.screenshotPath;
        this.videoPath = builder.videoPath;
    }
    
    // Getters
    public String getTestName() { return testName; }
    public Status getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getErrorMessage() { return errorMessage; }
    public String getStackTrace() { return stackTrace; }
    public Map<String, String> getAdditionalInfo() { return new HashMap<>(additionalInfo); }
    public String getScreenshotPath() { return screenshotPath; }
    public String getVideoPath() { return videoPath; }
    
    public long getDurationInMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
    
    public boolean isPassed() {
        return status == Status.PASS;
    }
    
    public boolean isFailed() {
        return status == Status.FAIL || status == Status.ERROR;
    }
    
    public boolean isSkipped() {
        return status == Status.SKIP;
    }
    
    public String getAdditionalInfo(String key) {
        return additionalInfo.get(key);
    }
    
    public String getAdditionalInfo(String key, String defaultValue) {
        return additionalInfo.getOrDefault(key, defaultValue);
    }
    
    /**
     * Builder class for TestResult.
     */
    public static class Builder {
        private String testName;
        private Status status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String errorMessage;
        private String stackTrace;
        private Map<String, String> additionalInfo = new HashMap<>();
        private String screenshotPath;
        private String videoPath;
        
        public Builder testName(String testName) {
            this.testName = testName;
            return this;
        }
        
        public Builder status(Status status) {
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
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
        
        public Builder additionalInfo(String key, String value) {
            this.additionalInfo.put(key, value);
            return this;
        }
        
        public Builder additionalInfo(Map<String, String> additionalInfo) {
            this.additionalInfo.putAll(additionalInfo);
            return this;
        }
        
        public Builder screenshotPath(String screenshotPath) {
            this.screenshotPath = screenshotPath;
            return this;
        }
        
        public Builder videoPath(String videoPath) {
            this.videoPath = videoPath;
            return this;
        }
        
        public TestResult build() {
            if (testName == null || testName.trim().isEmpty()) {
                throw new IllegalArgumentException("Test name cannot be null or empty");
            }
            if (status == null) {
                throw new IllegalArgumentException("Status cannot be null");
            }
            return new TestResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "TestResult{" +
                "testName='" + testName + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + getDurationInMillis() + "ms" +
                '}';
    }
}
