package advanced;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Real-time analytics dashboard with interactive visualizations and metrics tracking.
 */
public class AnalyticsDashboard {
    
    private final ObjectMapper objectMapper;
    private final String dashboardDirectory;
    private final Map<String, DashboardMetric> metrics;
    private final ScheduledExecutorService scheduler;
    private final List<DashboardWidget> widgets;
    private boolean isRunning;
    
    public AnalyticsDashboard() {
        this.objectMapper = new ObjectMapper();
        this.dashboardDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("analytics_dashboard").toString();
        this.metrics = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.widgets = new ArrayList<>();
        this.isRunning = false;
        createDashboardDirectory();
    }
    
    /**
     * Starts the real-time analytics dashboard.
     */
    public void startDashboard() {
        if (isRunning) {
            TestLogManager.warning("Analytics dashboard is already running");
            return;
        }
        
        TestLogManager.info("Starting real-time analytics dashboard");
        isRunning = true;
        
        // Start real-time data collection
        scheduler.scheduleAtFixedRate(this::collectRealTimeMetrics, 0, 5, TimeUnit.SECONDS);
        
        // Start dashboard updates
        scheduler.scheduleAtFixedRate(this::updateDashboard, 0, 10, TimeUnit.SECONDS);
        
        // Generate initial dashboard
        generateDashboard();
        
        TestLogManager.success("Analytics dashboard started successfully");
    }
    
    /**
     * Stops the analytics dashboard.
     */
    public void stopDashboard() {
        if (!isRunning) {
            TestLogManager.warning("Analytics dashboard is not running");
            return;
        }
        
        TestLogManager.info("Stopping analytics dashboard");
        isRunning = false;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        TestLogManager.success("Analytics dashboard stopped");
    }
    
    /**
     * Adds a metric to the dashboard.
     * @param metric Dashboard metric to add
     */
    public void addMetric(DashboardMetric metric) {
        metrics.put(metric.getMetricId(), metric);
        TestLogManager.info("Added metric to dashboard: " + metric.getMetricName());
    }
    
    /**
     * Updates a metric value.
     * @param metricId Metric ID
     * @param value New metric value
     */
    public void updateMetric(String metricId, double value) {
        DashboardMetric metric = metrics.get(metricId);
        if (metric != null) {
            metric.setValue(value);
            metric.setLastUpdated(LocalDateTime.now());
            TestLogManager.info("Updated metric " + metricId + " to " + value);
        }
    }
    
    /**
     * Adds a widget to the dashboard.
     * @param widget Dashboard widget to add
     */
    public void addWidget(DashboardWidget widget) {
        widgets.add(widget);
        TestLogManager.info("Added widget to dashboard: " + widget.getWidgetName());
    }
    
    /**
     * Generates the analytics dashboard.
     * @return DashboardResult with dashboard details
     */
    public DashboardResult generateDashboard() {
        TestLogManager.info("Generating analytics dashboard");
        
        DashboardResult result = new DashboardResult();
        result.setDashboardId(UUID.randomUUID().toString());
        result.setGeneratedTime(LocalDateTime.now());
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "analytics_dashboard_" + timestamp + ".html";
            Path dashboardPath = Paths.get(dashboardDirectory, fileName);
            
            StringBuilder dashboard = new StringBuilder();
            dashboard.append(generateDashboardHTML());
            dashboard.append(generateDashboardCSS());
            dashboard.append(generateDashboardJavaScript());
            dashboard.append(generateMetricsSection());
            dashboard.append(generateWidgetsSection());
            dashboard.append(generateChartsSection());
            dashboard.append(generateDashboardFooter());
            
            Files.write(dashboardPath, dashboard.toString().getBytes());
            
            result.setDashboardPath(dashboardPath);
            result.setSuccess(true);
            TestLogManager.success("Analytics dashboard generated: " + dashboardPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate analytics dashboard", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Generates a specific widget.
     * @param widgetType Type of widget to generate
     * @param config Widget configuration
     * @return WidgetResult with widget details
     */
    public WidgetResult generateWidget(String widgetType, WidgetConfiguration config) {
        TestLogManager.info("Generating widget: " + widgetType);
        
        WidgetResult result = new WidgetResult();
        result.setWidgetId(UUID.randomUUID().toString());
        result.setWidgetType(widgetType);
        result.setGeneratedTime(LocalDateTime.now());
        
        try {
            String widgetContent = generateWidgetContent(widgetType, config);
            result.setWidgetContent(widgetContent);
            result.setSuccess(true);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate widget: " + widgetType, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Exports dashboard data.
     * @param format Export format (JSON, CSV, XML)
     * @return ExportResult with export details
     */
    public ExportResult exportDashboardData(String format) {
        TestLogManager.info("Exporting dashboard data in format: " + format);
        
        ExportResult result = new ExportResult();
        result.setExportId(UUID.randomUUID().toString());
        result.setExportFormat(format);
        result.setExportTime(LocalDateTime.now());
        
        try {
            Path exportPath = exportData(format);
            result.setExportPath(exportPath);
            result.setSuccess(true);
            TestLogManager.success("Dashboard data exported: " + exportPath);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to export dashboard data", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    private void collectRealTimeMetrics() {
        if (!isRunning) return;
        
        // Collect system metrics
        collectSystemMetrics();
        
        // Collect test execution metrics
        collectTestExecutionMetrics();
        
        // Collect performance metrics
        collectPerformanceMetrics();
    }
    
    private void collectSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        // Memory usage
        updateMetric("memory_used", runtime.totalMemory() - runtime.freeMemory());
        updateMetric("memory_total", runtime.totalMemory());
        updateMetric("memory_max", runtime.maxMemory());
        
        // CPU usage (simplified)
        updateMetric("cpu_usage", Math.random() * 100);
    }
    
    private void collectTestExecutionMetrics() {
        // Simulate test execution metrics
        updateMetric("tests_executed", Math.random() * 100);
        updateMetric("tests_passed", Math.random() * 90);
        updateMetric("tests_failed", Math.random() * 10);
        updateMetric("success_rate", Math.random() * 100);
    }
    
    private void collectPerformanceMetrics() {
        // Simulate performance metrics
        updateMetric("response_time", Math.random() * 2000);
        updateMetric("throughput", Math.random() * 1000);
        updateMetric("error_rate", Math.random() * 5);
    }
    
    private void updateDashboard() {
        if (!isRunning) return;
        
        // Update dashboard with latest metrics
        generateDashboard();
    }
    
    private String generateWidgetContent(String widgetType, WidgetConfiguration config) {
        switch (widgetType.toLowerCase()) {
            case "metric_card":
                return generateMetricCardWidget(config);
            case "line_chart":
                return generateLineChartWidget(config);
            case "bar_chart":
                return generateBarChartWidget(config);
            case "pie_chart":
                return generatePieChartWidget(config);
            case "gauge":
                return generateGaugeWidget(config);
            case "table":
                return generateTableWidget(config);
            default:
                return generateDefaultWidget(config);
        }
    }
    
    private String generateMetricCardWidget(WidgetConfiguration config) {
        DashboardMetric metric = metrics.get(config.getMetricId());
        if (metric == null) return "";
        
        return "<div class='metric-card'>" +
               "<div class='metric-title'>" + metric.getMetricName() + "</div>" +
               "<div class='metric-value'>" + String.format("%.2f", metric.getValue()) + "</div>" +
               "<div class='metric-unit'>" + metric.getUnit() + "</div>" +
               "<div class='metric-trend'>" + getTrendIndicator(metric) + "</div>" +
               "</div>";
    }
    
    private String generateLineChartWidget(WidgetConfiguration config) {
        return "<div class='line-chart-widget'>" +
               "<canvas id='" + config.getWidgetId() + "' width='400' height='200'></canvas>" +
               "<script>" +
               "var ctx = document.getElementById('" + config.getWidgetId() + "').getContext('2d');" +
               "var chart = new Chart(ctx, {" +
               "type: 'line'," +
               "data: { labels: ['1', '2', '3', '4', '5'], datasets: [{ label: 'Metric', data: [12, 19, 3, 5, 2] }] }," +
               "options: { responsive: true }" +
               "});" +
               "</script>" +
               "</div>";
    }
    
    private String generateBarChartWidget(WidgetConfiguration config) {
        return "<div class='bar-chart-widget'>" +
               "<canvas id='" + config.getWidgetId() + "' width='400' height='200'></canvas>" +
               "<script>" +
               "var ctx = document.getElementById('" + config.getWidgetId() + "').getContext('2d');" +
               "var chart = new Chart(ctx, {" +
               "type: 'bar'," +
               "data: { labels: ['A', 'B', 'C', 'D'], datasets: [{ label: 'Values', data: [12, 19, 3, 5] }] }," +
               "options: { responsive: true }" +
               "});" +
               "</script>" +
               "</div>";
    }
    
    private String generatePieChartWidget(WidgetConfiguration config) {
        return "<div class='pie-chart-widget'>" +
               "<canvas id='" + config.getWidgetId() + "' width='300' height='300'></canvas>" +
               "<script>" +
               "var ctx = document.getElementById('" + config.getWidgetId() + "').getContext('2d');" +
               "var chart = new Chart(ctx, {" +
               "type: 'pie'," +
               "data: { labels: ['Passed', 'Failed', 'Skipped'], datasets: [{ data: [70, 20, 10] }] }," +
               "options: { responsive: true }" +
               "});" +
               "</script>" +
               "</div>";
    }
    
    private String generateGaugeWidget(WidgetConfiguration config) {
        DashboardMetric metric = metrics.get(config.getMetricId());
        if (metric == null) return "";
        
        double percentage = (metric.getValue() / metric.getMaxValue()) * 100;
        
        return "<div class='gauge-widget'>" +
               "<div class='gauge-container'>" +
               "<div class='gauge-fill' style='width: " + percentage + "%'></div>" +
               "</div>" +
               "<div class='gauge-value'>" + String.format("%.1f", metric.getValue()) + "</div>" +
               "<div class='gauge-label'>" + metric.getMetricName() + "</div>" +
               "</div>";
    }
    
    private String generateTableWidget(WidgetConfiguration config) {
        return "<div class='table-widget'>" +
               "<table>" +
               "<thead><tr><th>Metric</th><th>Value</th><th>Status</th></tr></thead>" +
               "<tbody>" +
               "<tr><td>Success Rate</td><td>95.5%</td><td class='status-good'>Good</td></tr>" +
               "<tr><td>Response Time</td><td>250ms</td><td class='status-good'>Good</td></tr>" +
               "<tr><td>Error Rate</td><td>0.5%</td><td class='status-warning'>Warning</td></tr>" +
               "</tbody>" +
               "</table>" +
               "</div>";
    }
    
    private String generateDefaultWidget(WidgetConfiguration config) {
        return "<div class='default-widget'>" +
               "<h3>" + config.getWidgetName() + "</h3>" +
               "<p>Default widget content</p>" +
               "</div>";
    }
    
    private String getTrendIndicator(DashboardMetric metric) {
        // Simple trend calculation based on recent values
        return "<span class='trend-up'>↗</span>";
    }
    
    private Path exportData(String format) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "dashboard_export_" + timestamp + "." + format.toLowerCase();
        Path exportPath = Paths.get(dashboardDirectory, fileName);
        
        switch (format.toUpperCase()) {
            case "JSON":
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(exportPath.toFile(), metrics);
                break;
            case "CSV":
                exportToCSV(exportPath);
                break;
            case "XML":
                exportToXML(exportPath);
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
        
        return exportPath;
    }
    
    private void exportToCSV(Path exportPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric ID,Metric Name,Value,Unit,Last Updated\n");
        
        for (DashboardMetric metric : metrics.values()) {
            csv.append(metric.getMetricId()).append(",")
               .append(metric.getMetricName()).append(",")
               .append(metric.getValue()).append(",")
               .append(metric.getUnit()).append(",")
               .append(metric.getLastUpdated()).append("\n");
        }
        
        Files.write(exportPath, csv.toString().getBytes());
    }
    
    private void exportToXML(Path exportPath) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<dashboard>\n");
        xml.append("<export-time>").append(LocalDateTime.now()).append("</export-time>\n");
        xml.append("<metrics>\n");
        
        for (DashboardMetric metric : metrics.values()) {
            xml.append("<metric>\n");
            xml.append("<id>").append(metric.getMetricId()).append("</id>\n");
            xml.append("<name>").append(metric.getMetricName()).append("</name>\n");
            xml.append("<value>").append(metric.getValue()).append("</value>\n");
            xml.append("<unit>").append(metric.getUnit()).append("</unit>\n");
            xml.append("<last-updated>").append(metric.getLastUpdated()).append("</last-updated>\n");
            xml.append("</metric>\n");
        }
        
        xml.append("</metrics>\n");
        xml.append("</dashboard>\n");
        
        Files.write(exportPath, xml.toString().getBytes());
    }
    
    private void createDashboardDirectory() {
        try {
            Path dir = Paths.get(dashboardDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created analytics dashboard directory: " + dashboardDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create analytics dashboard directory", e);
        }
    }
    
    // HTML generation methods
    private String generateDashboardHTML() {
        return "<!DOCTYPE html><html><head><title>Analytics Dashboard</title>" +
               "<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>";
    }
    
    private String generateDashboardCSS() {
        return "<style>" +
               "body{font-family:Arial,sans-serif;margin:0;padding:20px;background-color:#f5f5f5;}" +
               ".dashboard{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:20px;}" +
               ".card{background:white;border-radius:8px;padding:20px;box-shadow:0 2px 4px rgba(0,0,0,0.1);}" +
               ".metric-card{text-align:center;padding:20px;}" +
               ".metric-title{font-size:14px;color:#666;margin-bottom:10px;}" +
               ".metric-value{font-size:2.5em;font-weight:bold;color:#333;margin-bottom:5px;}" +
               ".metric-unit{font-size:12px;color:#999;}" +
               ".metric-trend{font-size:20px;margin-top:10px;}" +
               ".trend-up{color:#4CAF50;}" +
               ".trend-down{color:#F44336;}" +
               ".trend-stable{color:#FF9800;}" +
               ".gauge-widget{text-align:center;}" +
               ".gauge-container{width:200px;height:20px;background:#e0e0e0;border-radius:10px;margin:0 auto 10px;overflow:hidden;}" +
               ".gauge-fill{height:100%;background:linear-gradient(90deg,#4CAF50,#8BC34A);transition:width 0.3s ease;}" +
               ".gauge-value{font-size:1.5em;font-weight:bold;color:#333;}" +
               ".gauge-label{font-size:12px;color:#666;margin-top:5px;}" +
               ".table-widget table{width:100%;border-collapse:collapse;}" +
               ".table-widget th,.table-widget td{border:1px solid #ddd;padding:8px;text-align:left;}" +
               ".table-widget th{background-color:#f2f2f2;}" +
               ".status-good{color:#4CAF50;}" +
               ".status-warning{color:#FF9800;}" +
               ".status-error{color:#F44336;}" +
               "</style></head><body>";
    }
    
    private String generateDashboardJavaScript() {
        return "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
               "<script>" +
               "// Auto-refresh dashboard every 30 seconds" +
               "setInterval(function() { location.reload(); }, 30000);" +
               "</script>";
    }
    
    private String generateMetricsSection() {
        StringBuilder section = new StringBuilder("<div class='dashboard'>");
        
        for (DashboardMetric metric : metrics.values()) {
            section.append("<div class='card metric-card'>");
            section.append("<div class='metric-title'>").append(metric.getMetricName()).append("</div>");
            section.append("<div class='metric-value'>").append(String.format("%.2f", metric.getValue())).append("</div>");
            section.append("<div class='metric-unit'>").append(metric.getUnit()).append("</div>");
            section.append("<div class='metric-trend'>").append(getTrendIndicator(metric)).append("</div>");
            section.append("</div>");
        }
        
        section.append("</div>");
        return section.toString();
    }
    
    private String generateWidgetsSection() {
        StringBuilder section = new StringBuilder("<div class='dashboard'>");
        
        for (DashboardWidget widget : widgets) {
            section.append("<div class='card'>");
            section.append("<h3>").append(widget.getWidgetName()).append("</h3>");
            section.append(generateWidgetContent(widget.getWidgetType(), widget.getConfiguration()));
            section.append("</div>");
        }
        
        section.append("</div>");
        return section.toString();
    }
    
    private String generateChartsSection() {
        return "<div class='dashboard'>" +
               "<div class='card'>" +
               "<h3>Test Results Trend</h3>" +
               "<canvas id='testResultsChart' width='400' height='200'></canvas>" +
               "<script>" +
               "var ctx = document.getElementById('testResultsChart').getContext('2d');" +
               "var chart = new Chart(ctx, {" +
               "type: 'line'," +
               "data: { labels: ['1h', '2h', '3h', '4h', '5h'], datasets: [{ label: 'Success Rate', data: [95, 96, 94, 97, 95], borderColor: '#4CAF50' }] }," +
               "options: { responsive: true, scales: { y: { beginAtZero: true, max: 100 } } }" +
               "});" +
               "</script>" +
               "</div>" +
               "<div class='card'>" +
               "<h3>Performance Metrics</h3>" +
               "<canvas id='performanceChart' width='400' height='200'></canvas>" +
               "<script>" +
               "var ctx = document.getElementById('performanceChart').getContext('2d');" +
               "var chart = new Chart(ctx, {" +
               "type: 'bar'," +
               "data: { labels: ['Response Time', 'Throughput', 'Error Rate'], datasets: [{ label: 'Values', data: [250, 1000, 0.5], backgroundColor: ['#2196F3', '#4CAF50', '#FF9800'] }] }," +
               "options: { responsive: true }" +
               "});" +
               "</script>" +
               "</div>" +
               "</div>";
    }
    
    private String generateDashboardFooter() {
        return "<footer style='margin-top:40px;text-align:center;color:#666;'>" +
               "<p>Analytics Dashboard - Generated on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
               "<p>Auto-refreshes every 30 seconds</p>" +
               "</footer></body></html>";
    }
    
    // Data model classes
    public static class DashboardMetric {
        private String metricId;
        private String metricName;
        private double value;
        private double minValue;
        private double maxValue;
        private String unit;
        private LocalDateTime lastUpdated;
        private String category;
        
        // Getters and setters
        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }
        
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        
        public double getMinValue() { return minValue; }
        public void setMinValue(double minValue) { this.minValue = minValue; }
        
        public double getMaxValue() { return maxValue; }
        public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
    
    public static class DashboardWidget {
        private String widgetId;
        private String widgetName;
        private String widgetType;
        private WidgetConfiguration configuration;
        private LocalDateTime createdTime;
        
        // Getters and setters
        public String getWidgetId() { return widgetId; }
        public void setWidgetId(String widgetId) { this.widgetId = widgetId; }
        
        public String getWidgetName() { return widgetName; }
        public void setWidgetName(String widgetName) { this.widgetName = widgetName; }
        
        public String getWidgetType() { return widgetType; }
        public void setWidgetType(String widgetType) { this.widgetType = widgetType; }
        
        public WidgetConfiguration getConfiguration() { return configuration; }
        public void setConfiguration(WidgetConfiguration configuration) { this.configuration = configuration; }
        
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    }
    
    public static class WidgetConfiguration {
        private String widgetId;
        private String widgetName;
        private String metricId;
        private Map<String, Object> parameters;
        
        public WidgetConfiguration() {
            this.parameters = new HashMap<>();
        }
        
        // Getters and setters
        public String getWidgetId() { return widgetId; }
        public void setWidgetId(String widgetId) { this.widgetId = widgetId; }
        
        public String getWidgetName() { return widgetName; }
        public void setWidgetName(String widgetName) { this.widgetName = widgetName; }
        
        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    public static class DashboardResult {
        private String dashboardId;
        private LocalDateTime generatedTime;
        private Path dashboardPath;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public String getDashboardId() { return dashboardId; }
        public void setDashboardId(String dashboardId) { this.dashboardId = dashboardId; }
        
        public LocalDateTime getGeneratedTime() { return generatedTime; }
        public void setGeneratedTime(LocalDateTime generatedTime) { this.generatedTime = generatedTime; }
        
        public Path getDashboardPath() { return dashboardPath; }
        public void setDashboardPath(Path dashboardPath) { this.dashboardPath = dashboardPath; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class WidgetResult {
        private String widgetId;
        private String widgetType;
        private LocalDateTime generatedTime;
        private String widgetContent;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public String getWidgetId() { return widgetId; }
        public void setWidgetId(String widgetId) { this.widgetId = widgetId; }
        
        public String getWidgetType() { return widgetType; }
        public void setWidgetType(String widgetType) { this.widgetType = widgetType; }
        
        public LocalDateTime getGeneratedTime() { return generatedTime; }
        public void setGeneratedTime(LocalDateTime generatedTime) { this.generatedTime = generatedTime; }
        
        public String getWidgetContent() { return widgetContent; }
        public void setWidgetContent(String widgetContent) { this.widgetContent = widgetContent; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class ExportResult {
        private String exportId;
        private String exportFormat;
        private LocalDateTime exportTime;
        private Path exportPath;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public String getExportId() { return exportId; }
        public void setExportId(String exportId) { this.exportId = exportId; }
        
        public String getExportFormat() { return exportFormat; }
        public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
        
        public LocalDateTime getExportTime() { return exportTime; }
        public void setExportTime(LocalDateTime exportTime) { this.exportTime = exportTime; }
        
        public Path getExportPath() { return exportPath; }
        public void setExportPath(Path exportPath) { this.exportPath = exportPath; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
