package patterns.repository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents execution statistics for test runs.
 * This class holds aggregated statistics about test execution.
 */
public class TestExecutionStats {
    
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;
    private final int errorTests;
    private final long totalExecutionTime;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Map<String, Object> additionalStats;
    
    public TestExecutionStats(int totalTests, int passedTests, int failedTests, 
                             int skippedTests, int errorTests, long totalExecutionTime,
                             LocalDateTime startTime, LocalDateTime endTime,
                             Map<String, Object> additionalStats) {
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.errorTests = errorTests;
        this.totalExecutionTime = totalExecutionTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.additionalStats = additionalStats != null ? additionalStats : Map.of();
    }
    
    /**
     * Gets the total number of tests.
     * @return Total number of tests
     */
    public int getTotalTests() {
        return totalTests;
    }
    
    /**
     * Gets the number of passed tests.
     * @return Number of passed tests
     */
    public int getPassedTests() {
        return passedTests;
    }
    
    /**
     * Gets the number of failed tests.
     * @return Number of failed tests
     */
    public int getFailedTests() {
        return failedTests;
    }
    
    /**
     * Gets the number of skipped tests.
     * @return Number of skipped tests
     */
    public int getSkippedTests() {
        return skippedTests;
    }
    
    /**
     * Gets the number of error tests.
     * @return Number of error tests
     */
    public int getErrorTests() {
        return errorTests;
    }
    
    /**
     * Gets the total execution time in milliseconds.
     * @return Total execution time in milliseconds
     */
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    /**
     * Gets the start time.
     * @return Start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the end time.
     * @return End time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * Gets additional statistics.
     * @return Map of additional statistics
     */
    public Map<String, Object> getAdditionalStats() {
        return additionalStats;
    }
    
    /**
     * Gets the success rate as a percentage.
     * @return Success rate percentage
     */
    public double getSuccessRate() {
        if (totalTests == 0) {
            return 0.0;
        }
        return (double) passedTests / totalTests * 100.0;
    }
    
    /**
     * Gets the failure rate as a percentage.
     * @return Failure rate percentage
     */
    public double getFailureRate() {
        if (totalTests == 0) {
            return 0.0;
        }
        return (double) (failedTests + errorTests) / totalTests * 100.0;
    }
    
    /**
     * Gets the skip rate as a percentage.
     * @return Skip rate percentage
     */
    public double getSkipRate() {
        if (totalTests == 0) {
            return 0.0;
        }
        return (double) skippedTests / totalTests * 100.0;
    }
    
    /**
     * Gets the average execution time per test in milliseconds.
     * @return Average execution time per test in milliseconds
     */
    public double getAverageExecutionTime() {
        if (totalTests == 0) {
            return 0.0;
        }
        return (double) totalExecutionTime / totalTests;
    }
    
    /**
     * Checks if all tests passed.
     * @return true if all tests passed, false otherwise
     */
    public boolean isAllPassed() {
        return totalTests > 0 && failedTests == 0 && errorTests == 0 && skippedTests == 0;
    }
    
    /**
     * Checks if there are any failures.
     * @return true if there are failures, false otherwise
     */
    public boolean hasFailures() {
        return failedTests > 0 || errorTests > 0;
    }
    
    /**
     * Gets the duration of the test execution.
     * @return Duration in milliseconds, or 0 if start/end times are not available
     */
    public long getDuration() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    @Override
    public String toString() {
        return "TestExecutionStats{" +
                "totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", skippedTests=" + skippedTests +
                ", errorTests=" + errorTests +
                ", successRate=" + String.format("%.2f", getSuccessRate()) + "%" +
                ", totalExecutionTime=" + totalExecutionTime + "ms" +
                ", averageExecutionTime=" + String.format("%.2f", getAverageExecutionTime()) + "ms" +
                '}';
    }
}