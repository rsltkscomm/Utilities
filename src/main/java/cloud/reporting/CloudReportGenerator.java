package cloud.reporting;

import cloud.CloudConfiguration;
import cloud.execution.CloudExecutionEngine;
import cloud.session.CloudSessionInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import reporting.TestLogManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Cloud Report Generator
 * Generates comprehensive reports for cloud testing activities
 */
public class CloudReportGenerator {
    
    private final CloudConfiguration cloudConfig;
    private final String reportDirectory;
    private final DateTimeFormatter dateTimeFormatter;
    
    public CloudReportGenerator() {
        this.cloudConfig = new CloudConfiguration();
        this.reportDirectory = "reports/cloud";
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        
        // Create report directory if it doesn't exist
        createReportDirectory();
    }
    
    /**
     * Generate comprehensive cloud testing report
     */
    public String generateCloudReport(CloudExecutionEngine engine, List<CloudSessionInfo> sessions) {
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        String reportFileName = String.format("cloud_report_%s.html", timestamp);
        String reportPath = reportDirectory + "/" + reportFileName;
        
        try {
            StringBuilder html = new StringBuilder();
            
            // HTML Header
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Cloud Testing Report</title>\n");
            html.append("    <style>\n");
            html.append(getReportStyles());
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            
            // Report Header
            html.append(generateReportHeader(timestamp));
            
            // Executive Summary
            html.append(generateExecutiveSummary(engine, sessions));
            
            // Cloud Provider Information
            html.append(generateProviderInfo());
            
            // Session Details
            html.append(generateSessionDetails(sessions));
            
            // Performance Metrics
            html.append(generatePerformanceMetrics(engine, sessions));
            
            // Cost Analysis
            html.append(generateCostAnalysis(sessions));
            
            // Recommendations
            html.append(generateRecommendations(engine, sessions));
            
            // Footer
            html.append(generateReportFooter());
            
            html.append("</body>\n");
            html.append("</html>");
            
            // Write report to file
            writeReportToFile(reportPath, html.toString());
            
            TestLogManager.success("Cloud report generated: " + reportPath);
            return reportPath;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate cloud report", e);
            throw new RuntimeException("Failed to generate cloud report", e);
        }
    }
    
    /**
     * Generate Excel report for cloud testing
     */
    public String generateExcelReport(List<CloudSessionInfo> sessions) {
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        String reportFileName = String.format("cloud_report_%s.xlsx", timestamp);
        String reportPath = reportDirectory + "/" + reportFileName;
        
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Summary Sheet
            createSummarySheet(workbook, sessions);
            
            // Session Details Sheet
            createSessionDetailsSheet(workbook, sessions);
            
            // Performance Metrics Sheet
            createPerformanceMetricsSheet(workbook, sessions);
            
            // Cost Analysis Sheet
            createCostAnalysisSheet(workbook, sessions);
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(reportPath)) {
                workbook.write(fileOut);
            }
            
            TestLogManager.success("Cloud Excel report generated: " + reportPath);
            return reportPath;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate Excel cloud report", e);
            throw new RuntimeException("Failed to generate Excel cloud report", e);
        }
    }
    
    /**
     * Generate JSON report for cloud testing
     */
    public String generateJsonReport(CloudExecutionEngine engine, List<CloudSessionInfo> sessions) {
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        String reportFileName = String.format("cloud_report_%s.json", timestamp);
        String reportPath = reportDirectory + "/" + reportFileName;
        
        try {
            Map<String, Object> reportData = new HashMap<>();
            
            // Report metadata
            reportData.put("reportType", "Cloud Testing Report");
            reportData.put("generatedAt", LocalDateTime.now().toString());
            reportData.put("provider", cloudConfig.getActiveProvider());
            
            // Execution statistics
            CloudExecutionEngine.ExecutionStatistics stats = engine.getStatistics();
            Map<String, Object> executionStats = new HashMap<>();
            executionStats.put("totalSessions", stats.getTotalSessions());
            executionStats.put("completedExecutions", stats.getCompletedExecutions());
            executionStats.put("failedExecutions", stats.getFailedExecutions());
            executionStats.put("runningExecutions", stats.getRunningExecutions());
            executionStats.put("successRate", stats.getSuccessRate());
            executionStats.put("maxParallelSessions", stats.getMaxParallelSessions());
            reportData.put("executionStatistics", executionStats);
            
            // Session data
            List<Map<String, Object>> sessionData = new ArrayList<>();
            for (CloudSessionInfo session : sessions) {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("sessionId", session.getSessionId());
                sessionMap.put("sessionName", session.getSessionName());
                sessionMap.put("provider", session.getProvider());
                sessionMap.put("browser", session.getBrowser());
                sessionMap.put("platform", session.getPlatform());
                sessionMap.put("version", session.getVersion());
                sessionMap.put("status", session.getStatus());
                sessionMap.put("startTime", session.getStartTime() != null ? session.getStartTime().toString() : null);
                sessionMap.put("endTime", session.getEndTime() != null ? session.getEndTime().toString() : null);
                sessionMap.put("durationSeconds", session.getDurationSeconds());
                sessionMap.put("videoUrl", session.getVideoUrl());
                sessionMap.put("screenshotUrl", session.getScreenshotUrl());
                sessionMap.put("publicUrl", session.getPublicUrl());
                sessionMap.put("reason", session.getReason());
                sessionData.add(sessionMap);
            }
            reportData.put("sessions", sessionData);
            
            // Performance metrics
            Map<String, Object> performanceMetrics = calculatePerformanceMetrics(sessions);
            reportData.put("performanceMetrics", performanceMetrics);
            
            // Cost analysis
            Map<String, Object> costAnalysis = calculateCostAnalysis(sessions);
            reportData.put("costAnalysis", costAnalysis);
            
            // Convert to JSON and write to file
            String jsonContent = convertToJson(reportData);
            writeReportToFile(reportPath, jsonContent);
            
            TestLogManager.success("Cloud JSON report generated: " + reportPath);
            return reportPath;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate JSON cloud report", e);
            throw new RuntimeException("Failed to generate JSON cloud report", e);
        }
    }
    
    /**
     * Generate dashboard data for real-time monitoring
     */
    public Map<String, Object> generateDashboardData(CloudExecutionEngine engine, List<CloudSessionInfo> sessions) {
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Real-time statistics
        CloudExecutionEngine.ExecutionStatistics stats = engine.getStatistics();
        dashboardData.put("executionStatistics", stats);
        
        // Active sessions
        List<Map<String, Object>> activeSessions = new ArrayList<>();
        for (CloudSessionInfo session : sessions) {
            if (session.isRunning()) {
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("sessionId", session.getSessionId());
                sessionData.put("sessionName", session.getSessionName());
                sessionData.put("provider", session.getProvider());
                sessionData.put("browser", session.getBrowser());
                sessionData.put("platform", session.getPlatform());
                sessionData.put("duration", session.getDurationSeconds());
                sessionData.put("status", session.getStatus());
                activeSessions.add(sessionData);
            }
        }
        dashboardData.put("activeSessions", activeSessions);
        
        // Provider statistics
        Map<String, Object> providerStats = calculateProviderStatistics(sessions);
        dashboardData.put("providerStatistics", providerStats);
        
        // Browser statistics
        Map<String, Object> browserStats = calculateBrowserStatistics(sessions);
        dashboardData.put("browserStatistics", browserStats);
        
        // Platform statistics
        Map<String, Object> platformStats = calculatePlatformStatistics(sessions);
        dashboardData.put("platformStatistics", platformStats);
        
        // Performance trends
        Map<String, Object> performanceTrends = calculatePerformanceTrends(sessions);
        dashboardData.put("performanceTrends", performanceTrends);
        
        return dashboardData;
    }
    
    // ===========================================
    // PRIVATE HELPER METHODS
    // ===========================================
    
    private void createReportDirectory() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(reportDirectory);
            if (!java.nio.file.Files.exists(path)) {
                java.nio.file.Files.createDirectories(path);
            }
        } catch (IOException e) {
            TestLogManager.warning("Failed to create report directory: " + reportDirectory);
        }
    }
    
    private void writeReportToFile(String filePath, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(content.getBytes());
        }
    }
    
    private String getReportStyles() {
        return """
            body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
            .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
            .header { text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 20px; margin-bottom: 30px; }
            .section { margin-bottom: 30px; }
            .section h2 { color: #007bff; border-left: 4px solid #007bff; padding-left: 10px; }
            .metric-card { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 15px; margin: 10px 0; }
            .metric-value { font-size: 24px; font-weight: bold; color: #007bff; }
            .metric-label { color: #6c757d; font-size: 14px; }
            .success { color: #28a745; }
            .warning { color: #ffc107; }
            .danger { color: #dc3545; }
            table { width: 100%; border-collapse: collapse; margin-top: 15px; }
            th, td { border: 1px solid #dee2e6; padding: 8px; text-align: left; }
            th { background-color: #007bff; color: white; }
            tr:nth-child(even) { background-color: #f8f9fa; }
            .status-completed { color: #28a745; font-weight: bold; }
            .status-failed { color: #dc3545; font-weight: bold; }
            .status-active { color: #007bff; font-weight: bold; }
            """;
    }
    
    private String generateReportHeader(String timestamp) {
        return String.format("""
            <div class="container">
                <div class="header">
                    <h1>Cloud Testing Report</h1>
                    <p>Generated on: %s</p>
                    <p>Provider: %s | Project: %s | Build: %s</p>
                </div>
            """, timestamp, cloudConfig.getActiveProvider(), cloudConfig.getProjectName(), cloudConfig.getBuildName());
    }
    
    private String generateExecutiveSummary(CloudExecutionEngine engine, List<CloudSessionInfo> sessions) {
        CloudExecutionEngine.ExecutionStatistics stats = engine.getStatistics();
        
        return String.format("""
            <div class="section">
                <h2>Executive Summary</h2>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Total Sessions</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value %s">%.1f%%</div>
                    <div class="metric-label">Success Rate</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Parallel Sessions</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Failed Sessions</div>
                </div>
            </div>
            """, 
            stats.getTotalSessions(),
            stats.getSuccessRate() >= 90 ? "success" : stats.getSuccessRate() >= 70 ? "warning" : "danger",
            stats.getSuccessRate(),
            stats.getMaxParallelSessions(),
            stats.getFailedExecutions()
        );
    }
    
    private String generateProviderInfo() {
        return String.format("""
            <div class="section">
                <h2>Cloud Provider Information</h2>
                <table>
                    <tr><td><strong>Provider</strong></td><td>%s</td></tr>
                    <tr><td><strong>Hub URL</strong></td><td>%s</td></tr>
                    <tr><td><strong>API URL</strong></td><td>%s</td></tr>
                    <tr><td><strong>Project</strong></td><td>%s</td></tr>
                    <tr><td><strong>Build</strong></td><td>%s</td></tr>
                    <tr><td><strong>Video Recording</strong></td><td>%s</td></tr>
                    <tr><td><strong>Screenshots</strong></td><td>%s</td></tr>
                </table>
            </div>
            """,
            cloudConfig.getActiveProvider(),
            cloudConfig.getHubUrl(),
            cloudConfig.getApiUrl(),
            cloudConfig.getProjectName(),
            cloudConfig.getBuildName(),
            cloudConfig.isVideoEnabled() ? "Enabled" : "Disabled",
            cloudConfig.isScreenshotEnabled() ? "Enabled" : "Disabled"
        );
    }
    
    private String generateSessionDetails(List<CloudSessionInfo> sessions) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("    <h2>Session Details</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th>Session ID</th>\n");
        html.append("            <th>Session Name</th>\n");
        html.append("            <th>Browser</th>\n");
        html.append("            <th>Platform</th>\n");
        html.append("            <th>Status</th>\n");
        html.append("            <th>Duration (s)</th>\n");
        html.append("            <th>Video</th>\n");
        html.append("            <th>Screenshot</th>\n");
        html.append("        </tr>\n");
        
        for (CloudSessionInfo session : sessions) {
            String statusClass = "status-" + session.getStatus().toLowerCase();
            html.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td class="%s">%s</td>
                    <td>%d</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """,
                session.getSessionId(),
                session.getSessionName(),
                session.getBrowser(),
                session.getPlatform(),
                statusClass,
                session.getStatus(),
                session.getDurationSeconds(),
                session.getVideoUrl() != null && !session.getVideoUrl().isEmpty() ? "✓" : "✗",
                session.getScreenshotUrl() != null && !session.getScreenshotUrl().isEmpty() ? "✓" : "✗"
            ));
        }
        
        html.append("    </table>\n");
        html.append("</div>\n");
        
        return html.toString();
    }
    
    private String generatePerformanceMetrics(CloudExecutionEngine engine, List<CloudSessionInfo> sessions) {
        Map<String, Object> metrics = calculatePerformanceMetrics(sessions);
        
        return String.format("""
            <div class="section">
                <h2>Performance Metrics</h2>
                <div class="metric-card">
                    <div class="metric-value">%.2f</div>
                    <div class="metric-label">Average Duration (seconds)</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Min Duration (seconds)</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Max Duration (seconds)</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Total Execution Time (seconds)</div>
                </div>
            </div>
            """,
            (Double) metrics.get("averageDuration"),
            (Integer) metrics.get("minDuration"),
            (Integer) metrics.get("maxDuration"),
            (Integer) metrics.get("totalExecutionTime")
        );
    }
    
    private String generateCostAnalysis(List<CloudSessionInfo> sessions) {
        Map<String, Object> costData = calculateCostAnalysis(sessions);
        
        return String.format("""
            <div class="section">
                <h2>Cost Analysis</h2>
                <div class="metric-card">
                    <div class="metric-value">$%.2f</div>
                    <div class="metric-label">Estimated Cost</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">%d</div>
                    <div class="metric-label">Total Minutes Used</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">$%.4f</div>
                    <div class="metric-label">Cost per Minute</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">$%.4f</div>
                    <div class="metric-label">Cost per Session</div>
                </div>
            </div>
            """,
            (Double) costData.get("estimatedCost"),
            (Integer) costData.get("totalMinutes"),
            (Double) costData.get("costPerMinute"),
            (Double) costData.get("costPerSession")
        );
    }
    
    private String generateRecommendations(CloudExecutionEngine engine, List<CloudSessionInfo> sessions) {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("<div class=\"section\">\n");
        recommendations.append("    <h2>Recommendations</h2>\n");
        recommendations.append("    <ul>\n");
        
        CloudExecutionEngine.ExecutionStatistics stats = engine.getStatistics();
        
        if (stats.getSuccessRate() < 90) {
            recommendations.append("        <li class=\"danger\">Success rate is below 90%. Review failed sessions and improve test stability.</li>\n");
        }
        
        if (stats.getMaxParallelSessions() > 5) {
            recommendations.append("        <li class=\"warning\">High parallel session count may impact performance. Consider optimizing test execution.</li>\n");
        }
        
        Map<String, Object> metrics = calculatePerformanceMetrics(sessions);
        double avgDuration = (Double) metrics.get("averageDuration");
        if (avgDuration > 300) { // 5 minutes
            recommendations.append("        <li class=\"warning\">Average session duration is high. Consider optimizing test execution time.</li>\n");
        }
        
        recommendations.append("        <li>Enable video recording for failed sessions to improve debugging.</li>\n");
        recommendations.append("        <li>Use parallel execution to reduce overall test execution time.</li>\n");
        recommendations.append("        <li>Monitor cloud provider costs regularly and optimize resource usage.</li>\n");
        
        recommendations.append("    </ul>\n");
        recommendations.append("</div>\n");
        
        return recommendations.toString();
    }
    
    private String generateReportFooter() {
        return """
            <div class="section">
                <hr>
                <p style="text-align: center; color: #6c757d;">
                    Cloud Testing Report generated by Utility Framework | 
                    <a href="https://github.com/your-repo/utilities">GitHub Repository</a>
                </p>
            </div>
            </div>
            """;
    }
    
    private void createSummarySheet(Workbook workbook, List<CloudSessionInfo> sessions) {
        Sheet sheet = workbook.createSheet("Summary");
        
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Metric");
        headerRow.createCell(1).setCellValue("Value");
        
        int rowNum = 1;
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Total Sessions");
        row.createCell(1).setCellValue(sessions.size());
        
        long successfulSessions = sessions.stream().mapToLong(s -> s.isSuccessful() ? 1 : 0).sum();
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Successful Sessions");
        row.createCell(1).setCellValue(successfulSessions);
        
        double successRate = sessions.size() > 0 ? (double) successfulSessions / sessions.size() * 100 : 0;
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Success Rate (%)");
        row.createCell(1).setCellValue(successRate);
        
        Map<String, Object> metrics = calculatePerformanceMetrics(sessions);
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Average Duration (seconds)");
        row.createCell(1).setCellValue((Double) metrics.get("averageDuration"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Total Execution Time (seconds)");
        row.createCell(1).setCellValue((Integer) metrics.get("totalExecutionTime"));
        
        // Auto-size columns
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createSessionDetailsSheet(Workbook workbook, List<CloudSessionInfo> sessions) {
        Sheet sheet = workbook.createSheet("Session Details");
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Session ID", "Session Name", "Provider", "Browser", "Platform", "Version", 
                           "Status", "Start Time", "End Time", "Duration (s)", "Reason"};
        
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        
        // Add session data
        int rowNum = 1;
        for (CloudSessionInfo session : sessions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(session.getSessionId());
            row.createCell(1).setCellValue(session.getSessionName());
            row.createCell(2).setCellValue(session.getProvider());
            row.createCell(3).setCellValue(session.getBrowser());
            row.createCell(4).setCellValue(session.getPlatform());
            row.createCell(5).setCellValue(session.getVersion());
            row.createCell(6).setCellValue(session.getStatus());
            row.createCell(7).setCellValue(session.getStartTime() != null ? session.getStartTime().toString() : "");
            row.createCell(8).setCellValue(session.getEndTime() != null ? session.getEndTime().toString() : "");
            row.createCell(9).setCellValue(session.getDurationSeconds());
            row.createCell(10).setCellValue(session.getReason() != null ? session.getReason() : "");
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createPerformanceMetricsSheet(Workbook workbook, List<CloudSessionInfo> sessions) {
        Sheet sheet = workbook.createSheet("Performance Metrics");
        
        Map<String, Object> metrics = calculatePerformanceMetrics(sessions);
        
        int rowNum = 0;
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Metric");
        row.createCell(1).setCellValue("Value");
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Average Duration (seconds)");
        row.createCell(1).setCellValue((Double) metrics.get("averageDuration"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Min Duration (seconds)");
        row.createCell(1).setCellValue((Integer) metrics.get("minDuration"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Max Duration (seconds)");
        row.createCell(1).setCellValue((Integer) metrics.get("maxDuration"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Total Execution Time (seconds)");
        row.createCell(1).setCellValue((Integer) metrics.get("totalExecutionTime"));
        
        // Auto-size columns
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createCostAnalysisSheet(Workbook workbook, List<CloudSessionInfo> sessions) {
        Sheet sheet = workbook.createSheet("Cost Analysis");
        
        Map<String, Object> costData = calculateCostAnalysis(sessions);
        
        int rowNum = 0;
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Cost Metric");
        row.createCell(1).setCellValue("Value");
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Estimated Cost ($)");
        row.createCell(1).setCellValue((Double) costData.get("estimatedCost"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Total Minutes Used");
        row.createCell(1).setCellValue((Integer) costData.get("totalMinutes"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Cost per Minute ($)");
        row.createCell(1).setCellValue((Double) costData.get("costPerMinute"));
        
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Cost per Session ($)");
        row.createCell(1).setCellValue((Double) costData.get("costPerSession"));
        
        // Auto-size columns
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private Map<String, Object> calculatePerformanceMetrics(List<CloudSessionInfo> sessions) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (sessions.isEmpty()) {
            metrics.put("averageDuration", 0.0);
            metrics.put("minDuration", 0);
            metrics.put("maxDuration", 0);
            metrics.put("totalExecutionTime", 0);
            return metrics;
        }
        
        long totalDuration = sessions.stream().mapToLong(CloudSessionInfo::getDurationSeconds).sum();
        double averageDuration = (double) totalDuration / sessions.size();
        
        long minDuration = sessions.stream().mapToLong(CloudSessionInfo::getDurationSeconds).min().orElse(0);
        long maxDuration = sessions.stream().mapToLong(CloudSessionInfo::getDurationSeconds).max().orElse(0);
        
        metrics.put("averageDuration", averageDuration);
        metrics.put("minDuration", (int) minDuration);
        metrics.put("maxDuration", (int) maxDuration);
        metrics.put("totalExecutionTime", (int) totalDuration);
        
        return metrics;
    }
    
    private Map<String, Object> calculateCostAnalysis(List<CloudSessionInfo> sessions) {
        Map<String, Object> costData = new HashMap<>();
        
        if (sessions.isEmpty()) {
            costData.put("estimatedCost", 0.0);
            costData.put("totalMinutes", 0);
            costData.put("costPerMinute", 0.0);
            costData.put("costPerSession", 0.0);
            return costData;
        }
        
        // Calculate total minutes used
        long totalSeconds = sessions.stream().mapToLong(CloudSessionInfo::getDurationSeconds).sum();
        int totalMinutes = (int) Math.ceil(totalSeconds / 60.0);
        
        // Cost per minute (varies by provider)
        double costPerMinute = getCostPerMinute();
        double estimatedCost = totalMinutes * costPerMinute;
        double costPerSession = estimatedCost / sessions.size();
        
        costData.put("estimatedCost", estimatedCost);
        costData.put("totalMinutes", totalMinutes);
        costData.put("costPerMinute", costPerMinute);
        costData.put("costPerSession", costPerSession);
        
        return costData;
    }
    
    private double getCostPerMinute() {
        // These are approximate costs - actual costs may vary
        switch (cloudConfig.getActiveProvider().toLowerCase()) {
            case "browserstack":
                return 0.05; // $0.05 per minute
            case "saucelabs":
                return 0.04; // $0.04 per minute
            case "lambdatest":
                return 0.03; // $0.03 per minute
            case "crossbrowsertesting":
                return 0.06; // $0.06 per minute
            default:
                return 0.05; // Default cost
        }
    }
    
    private Map<String, Object> calculateProviderStatistics(List<CloudSessionInfo> sessions) {
        Map<String, Object> providerStats = new HashMap<>();
        
        Map<String, Integer> providerCounts = new HashMap<>();
        for (CloudSessionInfo session : sessions) {
            String provider = session.getProvider();
            providerCounts.put(provider, providerCounts.getOrDefault(provider, 0) + 1);
        }
        
        providerStats.put("providerCounts", providerCounts);
        providerStats.put("totalProviders", providerCounts.size());
        
        return providerStats;
    }
    
    private Map<String, Object> calculateBrowserStatistics(List<CloudSessionInfo> sessions) {
        Map<String, Object> browserStats = new HashMap<>();
        
        Map<String, Integer> browserCounts = new HashMap<>();
        for (CloudSessionInfo session : sessions) {
            String browser = session.getBrowser();
            browserCounts.put(browser, browserCounts.getOrDefault(browser, 0) + 1);
        }
        
        browserStats.put("browserCounts", browserCounts);
        browserStats.put("totalBrowsers", browserCounts.size());
        
        return browserStats;
    }
    
    private Map<String, Object> calculatePlatformStatistics(List<CloudSessionInfo> sessions) {
        Map<String, Object> platformStats = new HashMap<>();
        
        Map<String, Integer> platformCounts = new HashMap<>();
        for (CloudSessionInfo session : sessions) {
            String platform = session.getPlatform();
            platformCounts.put(platform, platformCounts.getOrDefault(platform, 0) + 1);
        }
        
        platformStats.put("platformCounts", platformCounts);
        platformStats.put("totalPlatforms", platformCounts.size());
        
        return platformStats;
    }
    
    private Map<String, Object> calculatePerformanceTrends(List<CloudSessionInfo> sessions) {
        Map<String, Object> trends = new HashMap<>();
        
        // Calculate average duration over time
        List<Double> durationTrends = new ArrayList<>();
        for (CloudSessionInfo session : sessions) {
            durationTrends.add((double) session.getDurationSeconds());
        }
        
        trends.put("durationTrends", durationTrends);
        
        // Calculate success rate trend
        long successfulCount = sessions.stream().mapToLong(s -> s.isSuccessful() ? 1 : 0).sum();
        double successRate = sessions.size() > 0 ? (double) successfulCount / sessions.size() * 100 : 0;
        trends.put("successRate", successRate);
        
        return trends;
    }
    
    private String convertToJson(Map<String, Object> data) {
        // Simple JSON conversion - in production, use a proper JSON library like Jackson
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            
            json.append("  \"").append(entry.getKey()).append("\": ");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof List) {
                json.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(", ");
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        json.append(convertMapToJson((Map<?, ?>) item));
                    } else if (item instanceof String) {
                        json.append("\"").append(item).append("\"");
                    } else {
                        json.append(item);
                    }
                }
                json.append("]");
            } else if (value instanceof Map) {
                json.append(convertMapToJson((Map<?, ?>) value));
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    private String convertMapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\": ");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
}
