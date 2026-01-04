package listeners.retry;

import org.testng.ITestResult;
import reporting.TestLogManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects flaky tests by tracking test stability over time
 */
public class FlakyTestDetector {
    
    private final Map<String, TestExecutionHistory> testHistory;
    private final int minExecutionsForFlakiness;
    private final double flakinessThreshold;
    
    public FlakyTestDetector() {
        this(10, 0.3); // 30% failure rate indicates flakiness
    }
    
    public FlakyTestDetector(int minExecutionsForFlakiness, double flakinessThreshold) {
        this.testHistory = new ConcurrentHashMap<>();
        this.minExecutionsForFlakiness = minExecutionsForFlakiness;
        this.flakinessThreshold = flakinessThreshold;
    }
    
    /**
     * Record test execution result
     */
    public void recordExecution(ITestResult result) {
        String testKey = getTestKey(result);
        TestExecutionHistory history = testHistory.computeIfAbsent(
            testKey, k -> new TestExecutionHistory(testKey)
        );
        
        history.recordExecution(result);
        
        TestLogManager.info("Recorded execution for: " + testKey + 
            " (Total: " + history.getTotalExecutions() + 
            ", Passed: " + history.getPassedCount() + 
            ", Failed: " + history.getFailedCount() + ")");
    }
    
    /**
     * Check if test is flaky
     */
    public boolean isFlaky(String testKey) {
        TestExecutionHistory history = testHistory.get(testKey);
        if (history == null || history.getTotalExecutions() < minExecutionsForFlakiness) {
            return false;
        }
        
        double failureRate = history.getFailureRate();
        return failureRate > 0 && failureRate < 1.0 && failureRate >= flakinessThreshold;
    }
    
    /**
     * Get flaky tests
     */
    public List<FlakyTestInfo> getFlakyTests() {
        List<FlakyTestInfo> flakyTests = new ArrayList<>();
        
        for (Map.Entry<String, TestExecutionHistory> entry : testHistory.entrySet()) {
            if (isFlaky(entry.getKey())) {
                TestExecutionHistory history = entry.getValue();
                flakyTests.add(new FlakyTestInfo(
                    entry.getKey(),
                    history.getTotalExecutions(),
                    history.getPassedCount(),
                    history.getFailedCount(),
                    history.getFailureRate(),
                    history.getFailurePatterns()
                ));
            }
        }
        
        return flakyTests;
    }
    
    /**
     * Get test execution history
     */
    public TestExecutionHistory getHistory(String testKey) {
        return testHistory.get(testKey);
    }
    
    /**
     * Get all test keys
     */
    public Set<String> getAllTestKeys() {
        return new HashSet<>(testHistory.keySet());
    }
    
    /**
     * Generate test key from ITestResult
     */
    private String getTestKey(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        return className + "." + methodName;
    }
    
    /**
     * Test execution history
     */
    public static class TestExecutionHistory {
        private final String testKey;
        private final List<ExecutionRecord> executions;
        private final Map<String, Integer> failurePatterns;
        
        public TestExecutionHistory(String testKey) {
            this.testKey = testKey;
            this.executions = Collections.synchronizedList(new ArrayList<>());
            this.failurePatterns = new ConcurrentHashMap<>();
        }
        
        public void recordExecution(ITestResult result) {
            ExecutionRecord record = new ExecutionRecord(
                System.currentTimeMillis(),
                result.getStatus() == ITestResult.SUCCESS,
                result.getThrowable() != null ? result.getThrowable().getClass().getName() : null,
                result.getThrowable() != null ? result.getThrowable().getMessage() : null
            );
            
            executions.add(record);
            
            // Track failure patterns
            if (!record.isPassed() && record.getExceptionType() != null) {
                failurePatterns.merge(record.getExceptionType(), 1, Integer::sum);
            }
        }
        
        public int getTotalExecutions() {
            return executions.size();
        }
        
        public int getPassedCount() {
            return (int) executions.stream().filter(ExecutionRecord::isPassed).count();
        }
        
        public int getFailedCount() {
            return (int) executions.stream().filter(e -> !e.isPassed()).count();
        }
        
        public double getFailureRate() {
            if (executions.isEmpty()) {
                return 0.0;
            }
            return (double) getFailedCount() / getTotalExecutions();
        }
        
        public double getSuccessRate() {
            return 1.0 - getFailureRate();
        }
        
        public Map<String, Integer> getFailurePatterns() {
            return new HashMap<>(failurePatterns);
        }
        
        public List<ExecutionRecord> getRecentExecutions(int count) {
            int size = executions.size();
            int start = Math.max(0, size - count);
            return new ArrayList<>(executions.subList(start, size));
        }
        
        public List<ExecutionRecord> getAllExecutions() {
            return new ArrayList<>(executions);
        }
    }
    
    /**
     * Execution record
     */
    public static class ExecutionRecord {
        private final long timestamp;
        private final boolean passed;
        private final String exceptionType;
        private final String exceptionMessage;
        
        public ExecutionRecord(long timestamp, boolean passed, String exceptionType, String exceptionMessage) {
            this.timestamp = timestamp;
            this.passed = passed;
            this.exceptionType = exceptionType;
            this.exceptionMessage = exceptionMessage;
        }
        
        public long getTimestamp() { return timestamp; }
        public boolean isPassed() { return passed; }
        public String getExceptionType() { return exceptionType; }
        public String getExceptionMessage() { return exceptionMessage; }
    }
    
    /**
     * Flaky test information
     */
    public static class FlakyTestInfo {
        private final String testKey;
        private final int totalExecutions;
        private final int passedCount;
        private final int failedCount;
        private final double failureRate;
        private final Map<String, Integer> failurePatterns;
        
        public FlakyTestInfo(String testKey, int totalExecutions, int passedCount, 
                           int failedCount, double failureRate, Map<String, Integer> failurePatterns) {
            this.testKey = testKey;
            this.totalExecutions = totalExecutions;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.failureRate = failureRate;
            this.failurePatterns = failurePatterns;
        }
        
        public String getTestKey() { return testKey; }
        public int getTotalExecutions() { return totalExecutions; }
        public int getPassedCount() { return passedCount; }
        public int getFailedCount() { return failedCount; }
        public double getFailureRate() { return failureRate; }
        public Map<String, Integer> getFailurePatterns() { return failurePatterns; }
        
        @Override
        public String toString() {
            return String.format("FlakyTest[%s: %d/%d passed (%.1f%% failure rate)]", 
                testKey, passedCount, totalExecutions, failureRate * 100);
        }
    }
}

