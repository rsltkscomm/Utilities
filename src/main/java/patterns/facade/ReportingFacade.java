package patterns.facade;

import patterns.repository.TestResult;
import patterns.repository.TestResultRepository;
import patterns.repository.TestExecutionStats;
import reporting.TestLogManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Facade for reporting operations.
 * This provides a simplified interface for complex reporting operations.
 */
public class ReportingFacade {
    
    private final TestResultRepository testResultRepository;
    private boolean reportingInitialized = false;
    
    public ReportingFacade(TestResultRepository testResultRepository) {
        this.testResultRepository = testResultRepository;
        TestLogManager.info("ReportingFacade initialized");
    }
    
    /**
     * Initializes reporting.
     */
    public void initializeReporting() {
        if (reportingInitialized) {
            TestLogManager.warning("Reporting already initialized");
            return;
        }
        
        TestLogManager.info("Initializing reporting system");
        reportingInitialized = true;
        TestLogManager.success("Reporting system initialized");
    }
    
    /**
     * Reports a test result.
     * @param testResult The test result to report
     */
    public void reportTestResult(TestResult testResult) {
        try {
            testResultRepository.saveTestResult(testResult);
            TestLogManager.info("Test result reported: " + testResult.getTestName() + " - " + testResult.getStatus());
        } catch (Exception e) {
            TestLogManager.error("Failed to report test result: " + testResult.getTestName(), e);
        }
    }
    
    /**
     * Generates a summary report.
     * @return Summary report as a string
     */
    public String generateSummaryReport() {
        try {
            TestExecutionStats stats = testResultRepository.getExecutionStats();
            
            StringBuilder summary = new StringBuilder();
            summary.append("=== Test Execution Summary ===\n");
            summary.append("Total Tests: ").append(stats.getTotalTests()).append("\n");
            summary.append("Passed: ").append(stats.getPassedTests()).append("\n");
            summary.append("Failed: ").append(stats.getFailedTests()).append("\n");
            summary.append("Skipped: ").append(stats.getSkippedTests()).append("\n");
            summary.append("Errors: ").append(stats.getErrorTests()).append("\n");
            summary.append("Success Rate: ").append(String.format("%.2f", stats.getSuccessRate())).append("%\n");
            summary.append("Total Execution Time: ").append(stats.getTotalExecutionTime()).append("ms\n");
            summary.append("Average Execution Time: ").append(String.format("%.2f", stats.getAverageExecutionTime())).append("ms\n");
            
            String summaryStr = summary.toString();
            TestLogManager.info("Summary report generated");
            return summaryStr;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate summary report", e);
            return "Failed to generate summary report: " + e.getMessage();
        }
    }
    
    /**
     * Generates a detailed report.
     * @return Detailed report as a string
     */
    public String generateDetailedReport() {
        try {
            List<TestResult> allResults = testResultRepository.getAllTestResults();
            
            StringBuilder report = new StringBuilder();
            report.append("=== Detailed Test Report ===\n");
            report.append("Generated at: ").append(LocalDateTime.now()).append("\n\n");
            
            for (TestResult result : allResults) {
                report.append("Test: ").append(result.getTestName()).append("\n");
                report.append("Status: ").append(result.getStatus()).append("\n");
                report.append("Start Time: ").append(result.getStartTime()).append("\n");
                report.append("End Time: ").append(result.getEndTime()).append("\n");
                report.append("Duration: ").append(result.getDurationInMillis()).append("ms\n");
                
                if (result.hasError()) {
                    report.append("Error: ").append(result.getErrorMessage()).append("\n");
                }
                
                report.append("Additional Info: ").append(result.getAdditionalInfo()).append("\n");
                report.append("---\n");
            }
            
            String reportStr = report.toString();
            TestLogManager.info("Detailed report generated");
            return reportStr;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate detailed report", e);
            return "Failed to generate detailed report: " + e.getMessage();
        }
    }
    
    /**
     * Generates a report for a specific date range.
     * @param startDate Start date
     * @param endDate End date
     * @return Report for the date range
     */
    public String generateDateRangeReport(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<TestResult> results = testResultRepository.getTestResultsByDateRange(startDate, endDate);
            TestExecutionStats stats = testResultRepository.getExecutionStats(startDate, endDate);
            
            StringBuilder report = new StringBuilder();
            report.append("=== Date Range Report ===\n");
            report.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
            report.append("Total Tests: ").append(stats.getTotalTests()).append("\n");
            report.append("Passed: ").append(stats.getPassedTests()).append("\n");
            report.append("Failed: ").append(stats.getFailedTests()).append("\n");
            report.append("Success Rate: ").append(String.format("%.2f", stats.getSuccessRate())).append("%\n");
            
            String reportStr = report.toString();
            TestLogManager.info("Date range report generated");
            return reportStr;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate date range report", e);
            return "Failed to generate date range report: " + e.getMessage();
        }
    }
    
    /**
     * Generates a failure report.
     * @return Failure report as a string
     */
    public String generateFailureReport() {
        try {
            List<TestResult> failedResults = testResultRepository.getTestResultsByStatus(TestResult.Status.FAIL);
            List<TestResult> errorResults = testResultRepository.getTestResultsByStatus(TestResult.Status.ERROR);
            
            StringBuilder report = new StringBuilder();
            report.append("=== Failure Report ===\n");
            report.append("Failed Tests: ").append(failedResults.size()).append("\n");
            report.append("Error Tests: ").append(errorResults.size()).append("\n\n");
            
            // Add failed tests
            for (TestResult result : failedResults) {
                report.append("FAILED: ").append(result.getTestName()).append("\n");
                if (result.hasError()) {
                    report.append("Error: ").append(result.getErrorMessage()).append("\n");
                }
                report.append("---\n");
            }
            
            // Add error tests
            for (TestResult result : errorResults) {
                report.append("ERROR: ").append(result.getTestName()).append("\n");
                if (result.hasError()) {
                    report.append("Error: ").append(result.getErrorMessage()).append("\n");
                }
                report.append("---\n");
            }
            
            String reportStr = report.toString();
            TestLogManager.info("Failure report generated");
            return reportStr;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate failure report", e);
            return "Failed to generate failure report: " + e.getMessage();
        }
    }
    
    /**
     * Exports test results to a specific format.
     * @param format Export format (JSON, CSV, XML)
     * @return Export result
     */
    public Map<String, Object> exportResults(String format) {
        Map<String, Object> exportResult = new HashMap<>();
        
        try {
            List<TestResult> allResults = testResultRepository.getAllTestResults();
            
            switch (format.toUpperCase()) {
                case "JSON":
                    exportResult.put("format", "JSON");
                    exportResult.put("data", convertToJson(allResults));
                    break;
                case "CSV":
                    exportResult.put("format", "CSV");
                    exportResult.put("data", convertToCsv(allResults));
                    break;
                case "XML":
                    exportResult.put("format", "XML");
                    exportResult.put("data", convertToXml(allResults));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
            
            exportResult.put("success", true);
            exportResult.put("count", allResults.size());
            TestLogManager.info("Results exported to " + format + " format");
            
        } catch (Exception e) {
            exportResult.put("success", false);
            exportResult.put("error", e.getMessage());
            TestLogManager.error("Failed to export results to " + format, e);
        }
        
        return exportResult;
    }
    
    /**
     * Finalizes reporting.
     */
    public void finalizeReporting() {
        TestLogManager.info("Finalizing reporting system");
        
        try {
            // Generate final summary
            String summary = generateSummaryReport();
            TestLogManager.info("Final summary:\n" + summary);
            
            reportingInitialized = false;
            TestLogManager.success("Reporting system finalized");
            
        } catch (Exception e) {
            TestLogManager.error("Error during reporting finalization", e);
        }
    }
    
    private String convertToJson(List<TestResult> results) {
        // Simple JSON conversion for demonstration
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"testResults\": [\n");
        
        for (int i = 0; i < results.size(); i++) {
            TestResult result = results.get(i);
            json.append("    {\n");
            json.append("      \"testName\": \"").append(result.getTestName()).append("\",\n");
            json.append("      \"status\": \"").append(result.getStatus()).append("\",\n");
            json.append("      \"duration\": ").append(result.getDurationInMillis()).append("\n");
            json.append("    }");
            
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n}");
        return json.toString();
    }
    
    private String convertToCsv(List<TestResult> results) {
        StringBuilder csv = new StringBuilder();
        csv.append("TestName,Status,Duration,StartTime,EndTime\n");
        
        for (TestResult result : results) {
            csv.append(result.getTestName()).append(",");
            csv.append(result.getStatus()).append(",");
            csv.append(result.getDurationInMillis()).append(",");
            csv.append(result.getStartTime()).append(",");
            csv.append(result.getEndTime()).append("\n");
        }
        
        return csv.toString();
    }
    
    private String convertToXml(List<TestResult> results) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testResults>\n");
        
        for (TestResult result : results) {
            xml.append("  <testResult>\n");
            xml.append("    <testName>").append(result.getTestName()).append("</testName>\n");
            xml.append("    <status>").append(result.getStatus()).append("</status>\n");
            xml.append("    <duration>").append(result.getDurationInMillis()).append("</duration>\n");
            xml.append("  </testResult>\n");
        }
        
        xml.append("</testResults>");
        return xml.toString();
    }
}