package patterns.repository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data class representing test execution statistics.
 */
public class TestExecutionStats {
    
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;
    private final int errorTests;
    private final long totalExecutionTime;
    private final double passRate;
    private final double failRate;
    private final LocalDateTime firstExecution;
    private final LocalDateTime lastExecution;
    private final Map<String, Integer> statusCounts;
    
    public TestExecutionStats(int totalTests, int passedTests, int failedTests, 
                            int skippedTests, int errorTests, long totalExecutionTime,
                            LocalDateTime firstExecution, LocalDateTime lastExecution,
                            Map<String, Integer> statusCounts) {
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.errorTests = errorTests;
        this.totalExecutionTime = totalExecutionTime;
        this.firstExecution = firstExecution;
        this.lastExecution = lastExecution;
        this.statusCounts = statusCounts;
        
        // Calculate rates
        this.passRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0.0;
        this.failRate = totalTests > 0 ? (double) (failedTests + errorTests) / totalTests * 100 : 0.0;
    }
    
    // Getters
    public int getTotalTests() { return totalTests; }
    public int getPassedTests() { return passedTests; }
    public int getFailedTests() { return failedTests; }
    public int getSkippedTests() { return skippedTests; }
    public int getErrorTests() { return errorTests; }
    public long getTotalExecutionTime() { return totalExecutionTime; }
    public double getPassRate() { return passRate; }
    public double getFailRate() { return failRate; }
    public LocalDateTime getFirstExecution() { return firstExecution; }
    public LocalDateTime getLastExecution() { return lastExecution; }
    public Map<String, Integer> getStatusCounts() { return statusCounts; }
    
    public double getAverageExecutionTime() {
        return totalTests > 0 ? (double) totalExecutionTime / totalTests : 0.0;
    }
    
    public boolean hasFailures() {
        return failedTests > 0 || errorTests > 0;
    }
    
    public boolean isAllPassed() {
        return totalTests > 0 && failedTests == 0 && errorTests == 0 && skippedTests == 0;
    }
    
    @Override
    public String toString() {
        return "TestExecutionStats{" +
                "totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", skippedTests=" + skippedTests +
                ", errorTests=" + errorTests +
                ", passRate=" + String.format("%.2f", passRate) + "%" +
                ", failRate=" + String.format("%.2f", failRate) + "%" +
                ", averageExecutionTime=" + String.format("%.2f", getAverageExecutionTime()) + "ms" +
                '}';
    }
}
