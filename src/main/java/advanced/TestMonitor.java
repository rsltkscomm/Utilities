package advanced;

import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Advanced test execution monitoring utilities for real-time tracking, alerting, and analytics.
 */
public class TestMonitor {
    
    private final Map<String, TestExecution> activeTests;
    private final Map<String, TestExecution> completedTests;
    private final ScheduledExecutorService scheduler;
    private final String reportDirectory;
    private final List<TestAlert> alerts;
    private boolean isMonitoring;
    
    public TestMonitor() {
        this.activeTests = new ConcurrentHashMap<>();
        this.completedTests = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("monitoring_reports").toString();
        this.alerts = new ArrayList<>();
        this.isMonitoring = false;
        createReportDirectory();
    }
    
    /**
     * Starts test execution monitoring.
     */
    public void startTestMonitoring() {
        if (isMonitoring) {
            TestLogManager.warning("Test monitoring is already running");
            return;
        }
        
        TestLogManager.info("Starting test execution monitoring");
        isMonitoring = true;
        
        // Start periodic monitoring tasks
        scheduler.scheduleAtFixedRate(this::monitorActiveTests, 0, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkForAlerts, 0, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::generateMonitoringReport, 0, 60, TimeUnit.SECONDS);
        
        TestLogManager.success("Test monitoring started successfully");
    }
    
    /**
     * Stops test execution monitoring.
     */
    public void stopTestMonitoring() {
        if (!isMonitoring) {
            TestLogManager.warning("Test monitoring is not running");
            return;
        }
        
        TestLogManager.info("Stopping test execution monitoring");
        isMonitoring = false;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        generateFinalMonitoringReport();
        TestLogManager.success("Test monitoring stopped successfully");
    }
    
    /**
     * Tracks test execution metrics.
     * @param testName Name of the test
     * @param testType Type of test (unit, integration, e2e, etc.)
     * @return TestExecution object for tracking
     */
    public TestExecution trackTestExecution(String testName, String testType) {
        TestLogManager.info("Tracking test execution: " + testName);
        
        TestExecution execution = new TestExecution();
        execution.setTestName(testName);
        execution.setTestType(testType);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(TestExecution.Status.RUNNING);
        
        activeTests.put(testName, execution);
        
        TestLogManager.success("Test execution tracking started: " + testName);
        return execution;
    }
    
    /**
     * Updates test execution status.
     * @param testName Name of the test
     * @param status New status
     * @param message Optional message
     */
    public void updateTestStatus(String testName, TestExecution.Status status, String message) {
        TestExecution execution = activeTests.get(testName);
        if (execution != null) {
            execution.setStatus(status);
            execution.setEndTime(LocalDateTime.now());
            execution.setMessage(message);
            
            if (status == TestExecution.Status.COMPLETED || 
                status == TestExecution.Status.FAILED || 
                status == TestExecution.Status.SKIPPED) {
                
                // Move to completed tests
                completedTests.put(testName, execution);
                activeTests.remove(testName);
                
                TestLogManager.info("Test execution completed: " + testName + " - " + status);
            }
        } else {
            TestLogManager.warning("Test execution not found: " + testName);
        }
    }
    
    /**
     * Alerts on test failures.
     * @param testName Name of the failed test
     * @param errorMessage Error message
     * @param severity Alert severity
     */
    public void alertOnTestFailures(String testName, String errorMessage, AlertSeverity severity) {
        TestLogManager.info("Alerting on test failure: " + testName);
        
        TestAlert alert = new TestAlert();
        alert.setTestName(testName);
        alert.setAlertType("TEST_FAILURE");
        alert.setMessage("Test failed: " + testName + " - " + errorMessage);
        alert.setSeverity(severity);
        alert.setTimestamp(LocalDateTime.now());
        
        alerts.add(alert);
        
        // Send alert based on severity
        sendAlert(alert);
        
        TestLogManager.warning("Test failure alert sent: " + testName);
    }
    
    /**
     * Monitors test performance metrics.
     * @param testName Name of the test
     * @param metrics Performance metrics
     */
    public void monitorTestPerformance(String testName, TestPerformanceMetrics metrics) {
        TestExecution execution = activeTests.get(testName);
        if (execution != null) {
            execution.setPerformanceMetrics(metrics);
            
            // Check for performance issues
            if (metrics.getExecutionTime() > 30000) { // 30 seconds
                TestAlert alert = new TestAlert();
                alert.setTestName(testName);
                alert.setAlertType("PERFORMANCE_ISSUE");
                alert.setMessage("Test execution time exceeded threshold: " + metrics.getExecutionTime() + "ms");
                alert.setSeverity(AlertSeverity.WARNING);
                alert.setTimestamp(LocalDateTime.now());
                
                alerts.add(alert);
                sendAlert(alert);
            }
        }
    }
    
    /**
     * Gets real-time test execution statistics.
     * @return TestExecutionStats with current statistics
     */
    public TestExecutionStats getRealTimeStats() {
        TestExecutionStats stats = new TestExecutionStats();
        
        stats.setActiveTests(activeTests.size());
        stats.setCompletedTests(completedTests.size());
        stats.setTotalTests(activeTests.size() + completedTests.size());
        
        // Calculate success rate
        long passedTests = completedTests.values().stream()
            .mapToLong(t -> t.getStatus() == TestExecution.Status.COMPLETED ? 1 : 0)
            .sum();
        
        if (completedTests.size() > 0) {
            stats.setSuccessRate((double) passedTests / completedTests.size() * 100);
        }
        
        // Calculate average execution time
        double avgTime = completedTests.values().stream()
            .mapToLong(t -> t.getExecutionTimeMillis())
            .average()
            .orElse(0.0);
        
        stats.setAverageExecutionTime(avgTime);
        
        // Count tests by type
        Map<String, Integer> testsByType = new HashMap<>();
        for (TestExecution test : completedTests.values()) {
            testsByType.merge(test.getTestType(), 1, Integer::sum);
        }
        stats.setTestsByType(testsByType);
        
        // Count tests by status
        Map<TestExecution.Status, Integer> testsByStatus = new HashMap<>();
        for (TestExecution test : completedTests.values()) {
            testsByStatus.merge(test.getStatus(), 1, Integer::sum);
        }
        stats.setTestsByStatus(testsByStatus);
        
        stats.setLastUpdated(LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * Gets test execution trends.
     * @param timeWindow Time window in minutes
     * @return TestTrends with trend analysis
     */
    public TestTrends getTestTrends(int timeWindow) {
        TestLogManager.info("Analyzing test trends for last " + timeWindow + " minutes");
        
        TestTrends trends = new TestTrends();
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeWindow);
        
        // Filter tests within time window
        List<TestExecution> recentTests = completedTests.values().stream()
            .filter(t -> t.getEndTime() != null && t.getEndTime().isAfter(cutoffTime))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        trends.setTimeWindow(timeWindow);
        trends.setTotalTests(recentTests.size());
        
        // Calculate trends
        long passedTests = recentTests.stream()
            .mapToLong(t -> t.getStatus() == TestExecution.Status.COMPLETED ? 1 : 0)
            .sum();
        
        trends.setPassedTests(passedTests);
        trends.setFailedTests(recentTests.size() - passedTests);
        
        if (recentTests.size() > 0) {
            trends.setSuccessRate((double) passedTests / recentTests.size() * 100);
        }
        
        // Calculate execution time trends
        double avgExecutionTime = recentTests.stream()
            .mapToLong(TestExecution::getExecutionTimeMillis)
            .average()
            .orElse(0.0);
        
        trends.setAverageExecutionTime(avgExecutionTime);
        
        // Identify flaky tests
        Map<String, Integer> testFailureCounts = new HashMap<>();
        for (TestExecution test : recentTests) {
            if (test.getStatus() == TestExecution.Status.FAILED) {
                testFailureCounts.merge(test.getTestName(), 1, Integer::sum);
            }
        }
        
        List<String> flakyTests = testFailureCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 2)
            .map(Map.Entry::getKey)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        trends.setFlakyTests(flakyTests);
        
        trends.setAnalysisTime(LocalDateTime.now());
        
        return trends;
    }
    
    /**
     * Generates comprehensive monitoring report.
     * @return Path to generated report
     */
    public Path generateMonitoringReport() {
        TestLogManager.info("Generating monitoring report");
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "test_monitoring_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader());
            report.append(generateMonitoringSummary());
            report.append(generateActiveTestsTable());
            report.append(generateCompletedTestsTable());
            report.append(generateAlertsTable());
            report.append(generateTrendsSection());
            report.append(generateHTMLFooter());
            
            Files.write(reportPath, report.toString().getBytes());
            TestLogManager.success("Monitoring report generated: " + reportPath);
            
            return reportPath;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate monitoring report", e);
            throw new RuntimeException("Monitoring report generation failed", e);
        }
    }
    
    /**
     * Exports monitoring data to JSON.
     * @return Path to exported JSON file
     */
    public Path exportMonitoringData() {
        TestLogManager.info("Exporting monitoring data to JSON");
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "monitoring_data_" + timestamp + ".json";
            Path exportPath = Paths.get(reportDirectory, fileName);
            
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("activeTests", activeTests);
            exportData.put("completedTests", completedTests);
            exportData.put("alerts", alerts);
            exportData.put("exportTime", LocalDateTime.now().toString());
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(exportPath.toFile(), exportData);
            
            TestLogManager.success("Monitoring data exported: " + exportPath);
            return exportPath;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to export monitoring data", e);
            throw new RuntimeException("Monitoring data export failed", e);
        }
    }
    
    private void monitorActiveTests() {
        if (!isMonitoring) return;
        
        LocalDateTime now = LocalDateTime.now();
        List<String> longRunningTests = new ArrayList<>();
        
        for (Map.Entry<String, TestExecution> entry : activeTests.entrySet()) {
            TestExecution execution = entry.getValue();
            long runningTime = java.time.Duration.between(execution.getStartTime(), now).toMinutes();
            
            // Alert for long-running tests
            if (runningTime > 10) { // 10 minutes
                longRunningTests.add(entry.getKey());
            }
        }
        
        // Send alerts for long-running tests
        for (String testName : longRunningTests) {
            TestAlert alert = new TestAlert();
            alert.setTestName(testName);
            alert.setAlertType("LONG_RUNNING_TEST");
            alert.setMessage("Test has been running for more than 10 minutes: " + testName);
            alert.setSeverity(AlertSeverity.WARNING);
            alert.setTimestamp(now);
            
            alerts.add(alert);
            sendAlert(alert);
        }
    }
    
    private void checkForAlerts() {
        if (!isMonitoring) return;
        
        // Check for high failure rate
        if (completedTests.size() > 10) {
            long failedTests = completedTests.values().stream()
                .mapToLong(t -> t.getStatus() == TestExecution.Status.FAILED ? 1 : 0)
                .sum();
            
            double failureRate = (double) failedTests / completedTests.size() * 100;
            
            if (failureRate > 50) { // 50% failure rate
                TestAlert alert = new TestAlert();
                alert.setTestName("SYSTEM");
                alert.setAlertType("HIGH_FAILURE_RATE");
                alert.setMessage("High test failure rate detected: " + String.format("%.2f", failureRate) + "%");
                alert.setSeverity(AlertSeverity.CRITICAL);
                alert.setTimestamp(LocalDateTime.now());
                
                alerts.add(alert);
                sendAlert(alert);
            }
        }
    }
    
    private void sendAlert(TestAlert alert) {
        // In a real implementation, this would send alerts via email, Slack, etc.
        TestLogManager.warning("ALERT [" + alert.getSeverity() + "]: " + alert.getMessage());
        
        // Log alert to file
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "alerts_" + timestamp + ".log";
            Path alertPath = Paths.get(reportDirectory, fileName);
            
            String alertLog = String.format("[%s] %s - %s: %s%n", 
                alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                alert.getSeverity(),
                alert.getAlertType(),
                alert.getMessage());
            
            Files.write(alertPath, alertLog.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to log alert", e);
        }
    }
    
    private void generateFinalMonitoringReport() {
        TestLogManager.info("Generating final monitoring report");
        generateMonitoringReport();
        exportMonitoringData();
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created monitoring report directory: " + reportDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create monitoring report directory", e);
        }
    }
    
    private String generateHTMLHeader() {
        return "<!DOCTYPE html><html><head><title>Test Monitoring Report</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:20px;}table{border-collapse:collapse;width:100%;}" +
               "th,td{border:1px solid #ddd;padding:8px;text-align:left;}th{background-color:#f2f2f2;}" +
               ".running{color:blue;}.completed{color:green;}.failed{color:red;}.skipped{color:orange;}" +
               ".critical{color:red;font-weight:bold;}.warning{color:orange;}.info{color:blue;}</style></head><body>";
    }
    
    private String generateMonitoringSummary() {
        TestExecutionStats stats = getRealTimeStats();
        
        return "<h1>Test Monitoring Report</h1>" +
               "<p>Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
               "<p>Active Tests: " + stats.getActiveTests() + "</p>" +
               "<p>Completed Tests: " + stats.getCompletedTests() + "</p>" +
               "<p>Total Tests: " + stats.getTotalTests() + "</p>" +
               "<p>Success Rate: " + String.format("%.2f", stats.getSuccessRate()) + "%</p>" +
               "<p>Average Execution Time: " + String.format("%.2f", stats.getAverageExecutionTime()) + "ms</p>";
    }
    
    private String generateActiveTestsTable() {
        StringBuilder table = new StringBuilder("<h2>Active Tests</h2><table><tr><th>Test Name</th><th>Type</th><th>Status</th><th>Start Time</th><th>Duration</th></tr>");
        
        for (TestExecution test : activeTests.values()) {
            long duration = java.time.Duration.between(test.getStartTime(), LocalDateTime.now()).toMillis();
            
            table.append("<tr>")
                 .append("<td>").append(test.getTestName()).append("</td>")
                 .append("<td>").append(test.getTestType()).append("</td>")
                 .append("<td class='running'>").append(test.getStatus()).append("</td>")
                 .append("<td>").append(test.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</td>")
                 .append("<td>").append(duration).append("ms</td>")
                 .append("</tr>");
        }
        
        table.append("</table>");
        return table.toString();
    }
    
    private String generateCompletedTestsTable() {
        StringBuilder table = new StringBuilder("<h2>Completed Tests</h2><table><tr><th>Test Name</th><th>Type</th><th>Status</th><th>Start Time</th><th>End Time</th><th>Duration</th><th>Message</th></tr>");
        
        for (TestExecution test : completedTests.values()) {
            String statusClass = test.getStatus().toString().toLowerCase();
            
            table.append("<tr>")
                 .append("<td>").append(test.getTestName()).append("</td>")
                 .append("<td>").append(test.getTestType()).append("</td>")
                 .append("<td class='").append(statusClass).append("'>").append(test.getStatus()).append("</td>")
                 .append("<td>").append(test.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</td>")
                 .append("<td>").append(test.getEndTime() != null ? test.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "N/A").append("</td>")
                 .append("<td>").append(test.getExecutionTimeMillis()).append("ms</td>")
                 .append("<td>").append(test.getMessage() != null ? test.getMessage() : "N/A").append("</td>")
                 .append("</tr>");
        }
        
        table.append("</table>");
        return table.toString();
    }
    
    private String generateAlertsTable() {
        StringBuilder table = new StringBuilder("<h2>Alerts</h2><table><tr><th>Timestamp</th><th>Severity</th><th>Type</th><th>Test Name</th><th>Message</th></tr>");
        
        for (TestAlert alert : alerts) {
            String severityClass = alert.getSeverity().toString().toLowerCase();
            
            table.append("<tr>")
                 .append("<td>").append(alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</td>")
                 .append("<td class='").append(severityClass).append("'>").append(alert.getSeverity()).append("</td>")
                 .append("<td>").append(alert.getAlertType()).append("</td>")
                 .append("<td>").append(alert.getTestName()).append("</td>")
                 .append("<td>").append(alert.getMessage()).append("</td>")
                 .append("</tr>");
        }
        
        table.append("</table>");
        return table.toString();
    }
    
    private String generateTrendsSection() {
        TestTrends trends = getTestTrends(60); // Last hour
        
        return "<h2>Test Trends (Last Hour)</h2>" +
               "<p>Total Tests: " + trends.getTotalTests() + "</p>" +
               "<p>Passed Tests: " + trends.getPassedTests() + "</p>" +
               "<p>Failed Tests: " + trends.getFailedTests() + "</p>" +
               "<p>Success Rate: " + String.format("%.2f", trends.getSuccessRate()) + "%</p>" +
               "<p>Average Execution Time: " + String.format("%.2f", trends.getAverageExecutionTime()) + "ms</p>" +
               "<p>Flaky Tests: " + trends.getFlakyTests().size() + "</p>";
    }
    
    private String generateHTMLFooter() {
        return "</body></html>";
    }
    
    /**
     * Test execution data model.
     */
    public static class TestExecution {
        public enum Status {
            RUNNING, COMPLETED, FAILED, SKIPPED
        }
        
        private String testName;
        private String testType;
        private Status status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String message;
        private TestPerformanceMetrics performanceMetrics;
        
        public long getExecutionTimeMillis() {
            if (endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMillis();
            } else {
                return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            }
        }
        
        // Getters and setters
        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }
        
        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }
        
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public TestPerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(TestPerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }
    }
    
    /**
     * Test performance metrics data model.
     */
    public static class TestPerformanceMetrics {
        private long executionTime;
        private long memoryUsed;
        private int cpuUsage;
        private int networkRequests;
        private long dataTransferred;
        
        // Getters and setters
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        
        public long getMemoryUsed() { return memoryUsed; }
        public void setMemoryUsed(long memoryUsed) { this.memoryUsed = memoryUsed; }
        
        public int getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(int cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public int getNetworkRequests() { return networkRequests; }
        public void setNetworkRequests(int networkRequests) { this.networkRequests = networkRequests; }
        
        public long getDataTransferred() { return dataTransferred; }
        public void setDataTransferred(long dataTransferred) { this.dataTransferred = dataTransferred; }
    }
    
    /**
     * Test alert data model.
     */
    public static class TestAlert {
        private String testName;
        private String alertType;
        private String message;
        private AlertSeverity severity;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }
        
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public AlertSeverity getSeverity() { return severity; }
        public void setSeverity(AlertSeverity severity) { this.severity = severity; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Alert severity enumeration.
     */
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    /**
     * Test execution statistics data model.
     */
    public static class TestExecutionStats {
        private int activeTests;
        private int completedTests;
        private int totalTests;
        private double successRate;
        private double averageExecutionTime;
        private Map<String, Integer> testsByType;
        private Map<TestExecution.Status, Integer> testsByStatus;
        private LocalDateTime lastUpdated;
        
        // Getters and setters
        public int getActiveTests() { return activeTests; }
        public void setActiveTests(int activeTests) { this.activeTests = activeTests; }
        
        public int getCompletedTests() { return completedTests; }
        public void setCompletedTests(int completedTests) { this.completedTests = completedTests; }
        
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        
        public Map<String, Integer> getTestsByType() { return testsByType; }
        public void setTestsByType(Map<String, Integer> testsByType) { this.testsByType = testsByType; }
        
        public Map<TestExecution.Status, Integer> getTestsByStatus() { return testsByStatus; }
        public void setTestsByStatus(Map<TestExecution.Status, Integer> testsByStatus) { this.testsByStatus = testsByStatus; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    /**
     * Test trends data model.
     */
    public static class TestTrends {
        private int timeWindow;
        private int totalTests;
        private long passedTests;
        private long failedTests;
        private double successRate;
        private double averageExecutionTime;
        private List<String> flakyTests;
        private LocalDateTime analysisTime;
        
        // Getters and setters
        public int getTimeWindow() { return timeWindow; }
        public void setTimeWindow(int timeWindow) { this.timeWindow = timeWindow; }
        
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        
        public long getPassedTests() { return passedTests; }
        public void setPassedTests(long passedTests) { this.passedTests = passedTests; }
        
        public long getFailedTests() { return failedTests; }
        public void setFailedTests(long failedTests) { this.failedTests = failedTests; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        
        public List<String> getFlakyTests() { return flakyTests; }
        public void setFlakyTests(List<String> flakyTests) { this.flakyTests = flakyTests; }
        
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
    }
}

