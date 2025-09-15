package patterns.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of a test execution.
 * This class holds all the information about a test run.
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
    
    private TestResult(Builder builder) {
        this.testName = builder.testName;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.errorMessage = builder.errorMessage;
        this.stackTrace = builder.stackTrace;
        this.additionalInfo = new HashMap<>(builder.additionalInfo);
    }
    
    /**
     * Gets the test name.
     * @return The test name
     */
    public String getTestName() {
        return testName;
    }
    
    /**
     * Gets the test status.
     * @return The test status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Gets the start time.
     * @return The start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the end time.
     * @return The end time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * Gets the error message.
     * @return The error message or null if no error
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Gets the stack trace.
     * @return The stack trace or null if no error
     */
    public String getStackTrace() {
        return stackTrace;
    }
    
    /**
     * Gets additional information.
     * @return Map of additional information
     */
    public Map<String, String> getAdditionalInfo() {
        return new HashMap<>(additionalInfo);
    }
    
    /**
     * Gets additional information by key.
     * @param key The information key
     * @return The information value or null if not found
     */
    public String getAdditionalInfo(String key) {
        return additionalInfo.get(key);
    }
    
    /**
     * Gets the duration in milliseconds.
     * @return The duration in milliseconds
     */
    public long getDurationInMillis() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    /**
     * Checks if the test passed.
     * @return true if passed, false otherwise
     */
    public boolean isPassed() {
        return status == Status.PASS;
    }
    
    /**
     * Checks if the test failed.
     * @return true if failed, false otherwise
     */
    public boolean isFailed() {
        return status == Status.FAIL;
    }
    
    /**
     * Checks if the test was skipped.
     * @return true if skipped, false otherwise
     */
    public boolean isSkipped() {
        return status == Status.SKIP;
    }
    
    /**
     * Checks if the test had an error.
     * @return true if error, false otherwise
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    /**
     * Checks if the test has an error message.
     * @return true if has error message, false otherwise
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
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
        
        public TestResult build() {
            if (testName == null || testName.trim().isEmpty()) {
                throw new IllegalArgumentException("Test name cannot be null or empty");
            }
            if (status == null) {
                throw new IllegalArgumentException("Status cannot be null");
            }
            if (startTime == null) {
                this.startTime = LocalDateTime.now();
            }
            if (endTime == null) {
                this.endTime = LocalDateTime.now();
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
                ", hasError=" + hasError() +
                '}';
    }
}