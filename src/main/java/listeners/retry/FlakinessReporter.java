package listeners.retry;

import reporting.ExtentManager;
import reporting.TestLogManager;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Reports flaky tests and stability metrics
 */
public class FlakinessReporter {
    
    private final FlakyTestDetector detector;
    private final TestStabilityScorer scorer;
    private final String reportDirectory;
    
    public FlakinessReporter(FlakyTestDetector detector, TestStabilityScorer scorer) {
        this.detector = detector;
        this.scorer = scorer;
        this.reportDirectory = "test-reports/flakiness";
    }
    
    public FlakinessReporter(FlakyTestDetector detector, TestStabilityScorer scorer, String reportDirectory) {
        this.detector = detector;
        this.scorer = scorer;
        this.reportDirectory = reportDirectory;
    }
    
    /**
     * Generate flakiness report
     */
    public void generateReport() {
        TestLogManager.info("Generating flakiness report...");
        
        try {
            // Ensure report directory exists
            Path reportPath = Paths.get(reportDirectory);
            Files.createDirectories(reportPath);
            
            // Generate HTML report
            generateHtmlReport(reportPath);
            
            // Generate JSON report
            generateJsonReport(reportPath);
            
            // Generate summary
            generateSummary();
            
            TestLogManager.info("Flakiness report generated in: " + reportDirectory);
        } catch (IOException e) {
            TestLogManager.error("Failed to generate flakiness report", e);
        }
    }
    
    /**
     * Generate HTML report
     */
    private void generateHtmlReport(Path reportPath) throws IOException {
        String fileName = "flakiness-report-" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".html";
        Path filePath = reportPath.resolve(fileName);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html><head><title>Flakiness Report</title>");
            writer.write("<style>");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }");
            writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            writer.write("th { background-color: #4CAF50; color: white; }");
            writer.write("tr:nth-child(even) { background-color: #f2f2f2; }");
            writer.write(".flaky { background-color: #ffcccc; }");
            writer.write(".stable { background-color: #ccffcc; }");
            writer.write("</style></head><body>");
            
            writer.write("<h1>Flakiness Report</h1>");
            writer.write("<p>Generated: " + LocalDateTime.now() + "</p>");
            
            // Flaky tests section
            List<FlakyTestDetector.FlakyTestInfo> flakyTests = detector.getFlakyTests();
            writer.write("<h2>Flaky Tests (" + flakyTests.size() + ")</h2>");
            writer.write("<table>");
            writer.write("<tr><th>Test</th><th>Total</th><th>Passed</th><th>Failed</th><th>Failure Rate</th><th>Common Exceptions</th></tr>");
            
            for (FlakyTestDetector.FlakyTestInfo flaky : flakyTests) {
                writer.write("<tr class='flaky'>");
                writer.write("<td>" + flaky.getTestKey() + "</td>");
                writer.write("<td>" + flaky.getTotalExecutions() + "</td>");
                writer.write("<td>" + flaky.getPassedCount() + "</td>");
                writer.write("<td>" + flaky.getFailedCount() + "</td>");
                writer.write("<td>" + String.format("%.1f%%", flaky.getFailureRate() * 100) + "</td>");
                writer.write("<td>" + flaky.getFailurePatterns().toString() + "</td>");
                writer.write("</tr>");
            }
            writer.write("</table>");
            
            // Stability scores section
            Map<String, TestStabilityScorer.StabilityScore> scores = scorer.getAllStabilityScores();
            writer.write("<h2>Stability Scores (" + scores.size() + ")</h2>");
            writer.write("<table>");
            writer.write("<tr><th>Test</th><th>Score</th><th>Level</th><th>Total</th><th>Passed</th><th>Failed</th></tr>");
            
            for (TestStabilityScorer.StabilityScore score : scores.values()) {
                String rowClass = score.getLevel() == TestStabilityScorer.StabilityLevel.STABLE ? 
                    "stable" : "flaky";
                writer.write("<tr class='" + rowClass + "'>");
                writer.write("<td>" + score.getTestKey() + "</td>");
                writer.write("<td>" + String.format("%.1f", score.getScore()) + "%</td>");
                writer.write("<td>" + score.getLevel() + "</td>");
                writer.write("<td>" + score.getTotalExecutions() + "</td>");
                writer.write("<td>" + score.getPassedCount() + "</td>");
                writer.write("<td>" + score.getFailedCount() + "</td>");
                writer.write("</tr>");
            }
            writer.write("</table>");
            
            // Degrading tests section
            List<TestStabilityScorer.DegradingTest> degrading = scorer.getDegradingTests();
            if (!degrading.isEmpty()) {
                writer.write("<h2>Degrading Tests (" + degrading.size() + ")</h2>");
                writer.write("<table>");
                writer.write("<tr><th>Test</th><th>Current Score</th><th>Initial</th><th>Latest</th><th>Degradation Rate</th></tr>");
                
                for (TestStabilityScorer.DegradingTest test : degrading) {
                    writer.write("<tr class='flaky'>");
                    writer.write("<td>" + test.getTestKey() + "</td>");
                    writer.write("<td>" + String.format("%.1f%%", test.getCurrentScore().getScore()) + "</td>");
                    writer.write("<td>" + String.format("%.1f%%", test.getInitialScore()) + "</td>");
                    writer.write("<td>" + String.format("%.1f%%", test.getLatestScore()) + "</td>");
                    writer.write("<td>" + String.format("%.2f", test.getDegradationRate()) + "</td>");
                    writer.write("</tr>");
                }
                writer.write("</table>");
            }
            
            writer.write("</body></html>");
        }
    }
    
    /**
     * Generate JSON report
     */
    private void generateJsonReport(Path reportPath) throws IOException {
        String fileName = "flakiness-report-" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        Path filePath = reportPath.resolve(fileName);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("{\n");
            writer.write("  \"generated\": \"" + LocalDateTime.now() + "\",\n");
            writer.write("  \"flakyTests\": [\n");
            
            List<FlakyTestDetector.FlakyTestInfo> flakyTests = detector.getFlakyTests();
            for (int i = 0; i < flakyTests.size(); i++) {
                FlakyTestDetector.FlakyTestInfo flaky = flakyTests.get(i);
                writer.write("    {\n");
                writer.write("      \"testKey\": \"" + flaky.getTestKey() + "\",\n");
                writer.write("      \"totalExecutions\": " + flaky.getTotalExecutions() + ",\n");
                writer.write("      \"passedCount\": " + flaky.getPassedCount() + ",\n");
                writer.write("      \"failedCount\": " + flaky.getFailedCount() + ",\n");
                writer.write("      \"failureRate\": " + flaky.getFailureRate() + ",\n");
                writer.write("      \"failurePatterns\": " + flaky.getFailurePatterns().toString() + "\n");
                writer.write("    }");
                if (i < flakyTests.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            
            writer.write("  ]\n");
            writer.write("}\n");
        }
    }
    
    /**
     * Generate summary to console
     */
    private void generateSummary() {
        List<FlakyTestDetector.FlakyTestInfo> flakyTests = detector.getFlakyTests();
        Map<String, TestStabilityScorer.StabilityScore> scores = scorer.getAllStabilityScores();
        List<TestStabilityScorer.DegradingTest> degrading = scorer.getDegradingTests();
        
        TestLogManager.info("=== Flakiness Report Summary ===");
        TestLogManager.info("Total Tests Tracked: " + scores.size());
        TestLogManager.info("Flaky Tests: " + flakyTests.size());
        TestLogManager.info("Degrading Tests: " + degrading.size());
        
        if (!flakyTests.isEmpty()) {
            TestLogManager.warning("Flaky Tests Detected:");
            for (FlakyTestDetector.FlakyTestInfo flaky : flakyTests) {
                TestLogManager.warning("  - " + flaky);
            }
        }
        
        if (!degrading.isEmpty()) {
            TestLogManager.warning("Degrading Tests:");
            for (TestStabilityScorer.DegradingTest test : degrading) {
                TestLogManager.warning("  - " + test);
            }
        }
    }
    
    /**
     * Alert on flaky tests
     */
    public void alertOnFlakyTests() {
        List<FlakyTestDetector.FlakyTestInfo> flakyTests = detector.getFlakyTests();
        
        if (!flakyTests.isEmpty()) {
            ExtentManager.warningTest("Flaky tests detected: " + flakyTests.size());
            for (FlakyTestDetector.FlakyTestInfo flaky : flakyTests) {
                ExtentManager.warningTest("Flaky: " + flaky.getTestKey() + 
                    " (" + String.format("%.1f%%", flaky.getFailureRate() * 100) + " failure rate)");
            }
        }
    }
}

