package accessibility;

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
 * Generates accessibility reports
 */
public class AccessibilityReporter {
    
    private final String reportDirectory;
    
    public AccessibilityReporter() {
        this("test-reports/accessibility");
    }
    
    public AccessibilityReporter(String reportDirectory) {
        this.reportDirectory = reportDirectory;
    }
    
    /**
     * Generate HTML report
     */
    public void generateHtmlReport(AccessibilityReport report) {
        try {
            Path reportPath = Paths.get(reportDirectory);
            Files.createDirectories(reportPath);
            
            String fileName = "accessibility-report-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".html";
            Path filePath = reportPath.resolve(fileName);
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write("<!DOCTYPE html>\n");
                writer.write("<html><head><title>Accessibility Report</title>");
                writer.write("<style>");
                writer.write("body { font-family: Arial, sans-serif; margin: 20px; }");
                writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }");
                writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
                writer.write("th { background-color: #4CAF50; color: white; }");
                writer.write("tr:nth-child(even) { background-color: #f2f2f2; }");
                writer.write(".error { background-color: #ffcccc; }");
                writer.write(".warning { background-color: #fff4cc; }");
                writer.write(".score { font-size: 24px; font-weight: bold; }");
                writer.write(".passed { color: green; }");
                writer.write(".failed { color: red; }");
                writer.write("</style></head><body>");
                
                writer.write("<h1>Accessibility Report</h1>");
                writer.write("<p>Generated: " + LocalDateTime.now() + "</p>");
                
                // Score section
                writer.write("<div class='score " + (report.isPassed() ? "passed" : "failed") + "'>");
                writer.write("Score: " + String.format("%.1f", report.getScore()) + "/100");
                writer.write("</div>");
                
                writer.write("<p>Total Checks: " + report.getTotalChecks() + "</p>");
                writer.write("<p>Violations: " + report.getViolationCount() + "</p>");
                
                // Violations by level
                Map<AccessibilityViolation.WCAGLevel, Integer> byLevel = report.getViolationsByLevel();
                if (!byLevel.isEmpty()) {
                    writer.write("<h2>Violations by WCAG Level</h2>");
                    writer.write("<table>");
                    writer.write("<tr><th>Level</th><th>Count</th></tr>");
                    for (Map.Entry<AccessibilityViolation.WCAGLevel, Integer> entry : byLevel.entrySet()) {
                        writer.write("<tr>");
                        writer.write("<td>" + entry.getKey() + "</td>");
                        writer.write("<td>" + entry.getValue() + "</td>");
                        writer.write("</tr>");
                    }
                    writer.write("</table>");
                }
                
                // All violations
                List<AccessibilityViolation> violations = report.getViolations();
                if (!violations.isEmpty()) {
                    writer.write("<h2>All Violations</h2>");
                    writer.write("<table>");
                    writer.write("<tr><th>Level</th><th>Criterion</th><th>Issue</th><th>Element</th><th>Recommendation</th></tr>");
                    
                    for (AccessibilityViolation violation : violations) {
                        String rowClass = violation.getLevel() == AccessibilityViolation.WCAGLevel.A ? 
                            "error" : "warning";
                        writer.write("<tr class='" + rowClass + "'>");
                        writer.write("<td>" + violation.getLevel() + "</td>");
                        writer.write("<td>" + violation.getCriterion() + " - " + violation.getCriterionName() + "</td>");
                        writer.write("<td>" + violation.getIssue() + "</td>");
                        writer.write("<td>" + violation.getElement() + "</td>");
                        writer.write("<td>" + violation.getRecommendation() + "</td>");
                        writer.write("</tr>");
                    }
                    writer.write("</table>");
                }
                
                writer.write("</body></html>");
            }
            
            TestLogManager.info("Accessibility report generated: " + filePath.toString());
        } catch (IOException e) {
            TestLogManager.error("Failed to generate accessibility report", e);
        }
    }
    
    /**
     * Report to ExtentReports
     */
    public void reportToExtent(AccessibilityReport report) {
        if (report.isPassed()) {
            ExtentManager.passTest("Accessibility check passed: " + 
                String.format("%.1f", report.getScore()) + "/100");
        } else {
            ExtentManager.failTest("Accessibility check failed: " + 
                String.format("%.1f", report.getScore()) + "/100");
        }
        
        // Add violations summary
        if (report.getViolationCount() > 0) {
            ExtentManager.warningTest("Violations found: " + report.getViolationCount());
            
            Map<AccessibilityViolation.WCAGLevel, Integer> byLevel = report.getViolationsByLevel();
            for (Map.Entry<AccessibilityViolation.WCAGLevel, Integer> entry : byLevel.entrySet()) {
                ExtentManager.warningTest("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
    
    /**
     * Generate console summary
     */
    public void generateConsoleSummary(AccessibilityReport report) {
        TestLogManager.info("=== Accessibility Report ===");
        TestLogManager.info(report.getSummary());
        
        if (report.getViolationCount() > 0) {
            TestLogManager.warning("Violations found:");
            for (AccessibilityViolation violation : report.getViolations()) {
                TestLogManager.warning("  " + violation);
            }
        }
    }
}

