package patterns.facade;

import patterns.repository.TestResult;
import patterns.repository.TestResultRepository;
import reporting.ExtentManager;
import reporting.TestLogManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Facade class that provides a simplified interface for reporting operations.
 * This hides the complexity of coordinating multiple reporting systems.
 */
public class ReportingFacade {
    
    private final TestResultRepository resultRepository;
    private final boolean enableExtentReports;
    private final boolean enableCustomReports;
    
    public ReportingFacade(TestResultRepository resultRepository) {
        this(resultRepository, true, true);
    }
    
    public ReportingFacade(TestResultRepository resultRepository, boolean enableExtentReports, boolean enableCustomReports) {
        this.resultRepository = resultRepository;
        this.enableExtentReports = enableExtentReports;
        this.enableCustomReports = enableCustomReports;
    }
    
    /**
     * Initializes all reporting systems.
     */
    public void initializeReporting() {
        TestLogManager.info("Initializing reporting systems");
        
        if (enableExtentReports) {
            ExtentManager.initReports();
            TestLogManager.info("ExtentReports initialized");
        }
        
        if (enableCustomReports) {
            // Initialize custom reporting systems here
            TestLogManager.info("Custom reports initialized");
        }
        
        TestLogManager.success("All reporting systems initialized successfully");
    }
    
    /**
     * Reports a test result to all configured reporting systems.
     * @param testResult The test result to report
     */
    public void reportTestResult(TestResult testResult) {
        TestLogManager.info("Reporting test result: " + testResult.getTestName());
        
        // Save to repository
        resultRepository.saveTestResult(testResult);
        
        // Report to ExtentReports
        if (enableExtentReports) {
            reportToExtentReports(testResult);
        }
        
        // Report to custom systems
        if (enableCustomReports) {
            reportToCustomSystems(testResult);
        }
        
        TestLogManager.success("Test result reported successfully");
    }
    
    /**
     * Reports multiple test results.
     * @param testResults List of test results to report
     */
    public void reportTestResults(List<TestResult> testResults) {
        TestLogManager.info("Reporting " + testResults.size() + " test results");
        
        for (TestResult testResult : testResults) {
            reportTestResult(testResult);
        }
        
        TestLogManager.success("All test results reported successfully");
    }
    
    /**
     * Generates a summary report for the current test session.
     * @return ReportSummary object containing summary information
     */
    public ReportSummary generateSummaryReport() {
        TestLogManager.info("Generating summary report");
        
        var stats = resultRepository.getExecutionStats();
        
        ReportSummary summary = new ReportSummary(
                stats.getTotalTests(),
                stats.getPassedTests(),
                stats.getFailedTests(),
                stats.getSkippedTests(),
                stats.getPassRate(),
                stats.getTotalExecutionTime(),
                LocalDateTime.now()
        );
        
        TestLogManager.info("Summary report generated: " + summary);
        return summary;
    }
    
    /**
     * Generates a detailed report for a specific test.
     * @param testName The name of the test
     * @return DetailedReport object or null if test not found
     */
    public DetailedReport generateDetailedReport(String testName) {
        TestLogManager.info("Generating detailed report for: " + testName);
        
        var testResultOpt = resultRepository.getTestResult(testName);
        if (testResultOpt.isEmpty()) {
            TestLogManager.warning("Test result not found for: " + testName);
            return null;
        }
        
        TestResult testResult = testResultOpt.get();
        
        DetailedReport report = new DetailedReport(
                testResult.getTestName(),
                testResult.getStatus(),
                testResult.getStartTime(),
                testResult.getEndTime(),
                testResult.getDurationInMillis(),
                testResult.getErrorMessage(),
                testResult.getAdditionalInfo()
        );
        
        TestLogManager.info("Detailed report generated for: " + testName);
        return report;
    }
    
    /**
     * Generates a trend report for a specific date range.
     * @param startDate Start date
     * @param endDate End date
     * @return TrendReport object
     */
    public TrendReport generateTrendReport(LocalDateTime startDate, LocalDateTime endDate) {
        TestLogManager.info("Generating trend report from " + startDate + " to " + endDate);
        
        var stats = resultRepository.getExecutionStats(startDate, endDate);
        var testResults = resultRepository.getTestResultsByDateRange(startDate, endDate);
        
        TrendReport report = new TrendReport(
                startDate,
                endDate,
                stats,
                testResults
        );
        
        TestLogManager.info("Trend report generated");
        return report;
    }
    
    /**
     * Finalizes all reporting systems and generates final reports.
     */
    public void finalizeReporting() {
        TestLogManager.info("Finalizing reporting systems");
        
        if (enableExtentReports) {
            ExtentManager.flushReports();
            TestLogManager.info("ExtentReports finalized");
        }
        
        if (enableCustomReports) {
            // Finalize custom reporting systems
            TestLogManager.info("Custom reports finalized");
        }
        
        TestLogManager.success("All reporting systems finalized successfully");
    }
    
    private void reportToExtentReports(TestResult testResult) {
        try {
            switch (testResult.getStatus()) {
                case PASS:
                    ExtentManager.passTest("Test passed: " + testResult.getTestName());
                    break;
                case FAIL:
                case ERROR:
                    ExtentManager.failTest("Test failed: " + testResult.getTestName());
                    if (testResult.getErrorMessage() != null) {
                        ExtentManager.failTest("Error: " + testResult.getErrorMessage());
                    }
                    break;
                case SKIP:
                    ExtentManager.skipTest("Test skipped: " + testResult.getTestName());
                    break;
            }
            
            // Add additional information
            Map<String, String> additionalInfo = testResult.getAdditionalInfo();
            if (additionalInfo != null && !additionalInfo.isEmpty()) {
                ExtentManager.customReport(additionalInfo);
            }
            
        } catch (Exception e) {
            TestLogManager.error("Error reporting to ExtentReports", e);
        }
    }
    
    private void reportToCustomSystems(TestResult testResult) {
        try {
            // Implement custom reporting logic here
            // This could include database logging, email notifications, etc.
            
            TestLogManager.info("Reported to custom systems: " + testResult.getTestName());
            
        } catch (Exception e) {
            TestLogManager.error("Error reporting to custom systems", e);
        }
    }
    
    /**
     * Data class for report summary.
     */
    public static class ReportSummary {
        private final int totalTests;
        private final int passedTests;
        private final int failedTests;
        private final int skippedTests;
        private final double passRate;
        private final long totalExecutionTime;
        private final LocalDateTime generatedAt;
        
        public ReportSummary(int totalTests, int passedTests, int failedTests, int skippedTests,
                           double passRate, long totalExecutionTime, LocalDateTime generatedAt) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
            this.passRate = passRate;
            this.totalExecutionTime = totalExecutionTime;
            this.generatedAt = generatedAt;
        }
        
        // Getters
        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public int getSkippedTests() { return skippedTests; }
        public double getPassRate() { return passRate; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        
        @Override
        public String toString() {
            return "ReportSummary{" +
                    "totalTests=" + totalTests +
                    ", passedTests=" + passedTests +
                    ", failedTests=" + failedTests +
                    ", skippedTests=" + skippedTests +
                    ", passRate=" + String.format("%.2f", passRate) + "%" +
                    ", totalExecutionTime=" + totalExecutionTime + "ms" +
                    '}';
        }
    }
    
    /**
     * Data class for detailed report.
     */
    public static class DetailedReport {
        private final String testName;
        private final TestResult.Status status;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final long duration;
        private final String errorMessage;
        private final Map<String, String> additionalInfo;
        
        public DetailedReport(String testName, TestResult.Status status, LocalDateTime startTime,
                            LocalDateTime endTime, long duration, String errorMessage,
                            Map<String, String> additionalInfo) {
            this.testName = testName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.errorMessage = errorMessage;
            this.additionalInfo = additionalInfo;
        }
        
        // Getters
        public String getTestName() { return testName; }
        public TestResult.Status getStatus() { return status; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public long getDuration() { return duration; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, String> getAdditionalInfo() { return additionalInfo; }
        
        @Override
        public String toString() {
            return "DetailedReport{" +
                    "testName='" + testName + '\'' +
                    ", status=" + status +
                    ", duration=" + duration + "ms" +
                    '}';
        }
    }
    
    /**
     * Data class for trend report.
     */
    public static class TrendReport {
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;
        private final patterns.repository.TestExecutionStats stats;
        private final List<TestResult> testResults;
        
        public TrendReport(LocalDateTime startDate, LocalDateTime endDate,
                         patterns.repository.TestExecutionStats stats, List<TestResult> testResults) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.stats = stats;
            this.testResults = testResults;
        }
        
        // Getters
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public patterns.repository.TestExecutionStats getStats() { return stats; }
        public List<TestResult> getTestResults() { return testResults; }
        
        @Override
        public String toString() {
            return "TrendReport{" +
                    "startDate=" + startDate +
                    ", endDate=" + endDate +
                    ", stats=" + stats +
                    ", testResultsCount=" + testResults.size() +
                    '}';
        }
    }
}
