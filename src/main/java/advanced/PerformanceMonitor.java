package advanced;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.File;
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
 * Advanced performance monitoring utilities for web applications.
 */
public class PerformanceMonitor {
    
    private final WebDriver driver;
    private final Map<String, PerformanceMetrics> metricsMap;
    private final ScheduledExecutorService scheduler;
    private final String reportDirectory;
    private boolean isMonitoring;
    
    public PerformanceMonitor(WebDriver driver) {
        this.driver = driver;
        this.metricsMap = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("performance_reports").toString();
        this.isMonitoring = false;
        createReportDirectory();
    }
    
    /**
     * Starts comprehensive performance monitoring.
     */
    public void startPerformanceMonitoring() {
        if (isMonitoring) {
            TestLogManager.warning("Performance monitoring is already running");
            return;
        }
        
        TestLogManager.info("Starting performance monitoring");
        isMonitoring = true;
        
        // Start periodic metrics collection
        scheduler.scheduleAtFixedRate(this::collectPerformanceMetrics, 0, 1, TimeUnit.SECONDS);
        
        // Start network monitoring
        scheduler.scheduleAtFixedRate(this::collectNetworkMetrics, 0, 2, TimeUnit.SECONDS);
        
        // Start memory monitoring
        scheduler.scheduleAtFixedRate(this::collectMemoryMetrics, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Stops performance monitoring and generates report.
     */
    public void stopPerformanceMonitoring() {
        if (!isMonitoring) {
            TestLogManager.warning("Performance monitoring is not running");
            return;
        }
        
        TestLogManager.info("Stopping performance monitoring");
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
        
        generatePerformanceReport();
    }
    
    /**
     * Gets current page load metrics.
     * @return PerformanceMetrics object with current metrics
     */
    public PerformanceMetrics getPageLoadMetrics() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Get navigation timing
            Map<String, Object> navigationTiming = (Map<String, Object>) js.executeScript(
                "return window.performance.timing;"
            );
            
            // Get resource timing
            List<Map<String, Object>> resourceTiming = (List<Map<String, Object>>) js.executeScript(
                "return window.performance.getEntriesByType('resource');"
            );
            
            // Calculate metrics
            long loadTime = calculateLoadTime(navigationTiming);
            long domContentLoaded = calculateDOMContentLoaded(navigationTiming);
            long firstPaint = getFirstPaint();
            long firstContentfulPaint = getFirstContentfulPaint();
            
            PerformanceMetrics metrics = new PerformanceMetrics();
            metrics.setLoadTime(loadTime);
            metrics.setDomContentLoaded(domContentLoaded);
            metrics.setFirstPaint(firstPaint);
            metrics.setFirstContentfulPaint(firstContentfulPaint);
            metrics.setResourceCount(resourceTiming.size());
            metrics.setTimestamp(LocalDateTime.now());
            
            return metrics;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to get page load metrics", e);
            return new PerformanceMetrics();
        }
    }
    
    /**
     * Measures performance of a specific action.
     * @param actionName Name of the action being measured
     * @param action Runnable action to measure
     * @return PerformanceMetrics for the action
     */
    public PerformanceMetrics measureAction(String actionName, Runnable action) {
        TestLogManager.info("Measuring performance for action: " + actionName);
        
        long startTime = System.currentTimeMillis();
        long startMemory = getCurrentMemoryUsage();
        
        try {
            action.run();
        } catch (Exception e) {
            TestLogManager.error("Action failed during performance measurement: " + actionName, e);
        }
        
        long endTime = System.currentTimeMillis();
        long endMemory = getCurrentMemoryUsage();
        
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setActionName(actionName);
        metrics.setExecutionTime(endTime - startTime);
        metrics.setMemoryUsed(endMemory - startMemory);
        metrics.setTimestamp(LocalDateTime.now());
        
        // Store metrics
        metricsMap.put(actionName + "_" + System.currentTimeMillis(), metrics);
        
        TestLogManager.info("Action '" + actionName + "' completed in " + metrics.getExecutionTime() + "ms");
        return metrics;
    }
    
    /**
     * Generates comprehensive performance report.
     */
    public void generatePerformanceReport() {
        TestLogManager.info("Generating performance report");
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "performance_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader());
            report.append(generateSummarySection());
            report.append(generateMetricsTable());
            report.append(generateChartsSection());
            report.append(generateRecommendationsSection());
            report.append(generateHTMLFooter());
            
            Files.write(reportPath, report.toString().getBytes());
            TestLogManager.success("Performance report generated: " + reportPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate performance report", e);
        }
    }
    
    /**
     * Gets performance statistics for a specific time period.
     * @param startTime Start time for statistics
     * @param endTime End time for statistics
     * @return PerformanceStatistics object
     */
    public PerformanceStatistics getPerformanceStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        List<PerformanceMetrics> filteredMetrics = new ArrayList<>();
        
        for (PerformanceMetrics metrics : metricsMap.values()) {
            if (metrics.getTimestamp().isAfter(startTime) && metrics.getTimestamp().isBefore(endTime)) {
                filteredMetrics.add(metrics);
            }
        }
        
        return calculateStatistics(filteredMetrics);
    }
    
    /**
     * Monitors Core Web Vitals.
     * @return CoreWebVitals object with current vitals
     */
    public CoreWebVitals getCoreWebVitals() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Get Largest Contentful Paint (LCP)
            Double lcp = (Double) js.executeScript(
                "return new Promise((resolve) => {" +
                "  new PerformanceObserver((list) => {" +
                "    const entries = list.getEntries();" +
                "    const lastEntry = entries[entries.length - 1];" +
                "    resolve(lastEntry.startTime);" +
                "  }).observe({entryTypes: ['largest-contentful-paint']});" +
                "});"
            );
            
            // Get First Input Delay (FID)
            Double fid = (Double) js.executeScript(
                "return new Promise((resolve) => {" +
                "  new PerformanceObserver((list) => {" +
                "    const entries = list.getEntries();" +
                "    entries.forEach((entry) => {" +
                "      resolve(entry.processingStart - entry.startTime);" +
                "    });" +
                "  }).observe({entryTypes: ['first-input']});" +
                "});"
            );
            
            // Get Cumulative Layout Shift (CLS)
            Double cls = (Double) js.executeScript(
                "return new Promise((resolve) => {" +
                "  let clsValue = 0;" +
                "  new PerformanceObserver((list) => {" +
                "    for (const entry of list.getEntries()) {" +
                "      if (!entry.hadRecentInput) {" +
                "        clsValue += entry.value;" +
                "      }" +
                "    }" +
                "    resolve(clsValue);" +
                "  }).observe({entryTypes: ['layout-shift']});" +
                "});"
            );
            
            CoreWebVitals vitals = new CoreWebVitals();
            vitals.setLargestContentfulPaint(lcp != null ? lcp.longValue() : 0);
            vitals.setFirstInputDelay(fid != null ? fid.longValue() : 0);
            vitals.setCumulativeLayoutShift(cls != null ? cls.doubleValue() : 0.0);
            vitals.setTimestamp(LocalDateTime.now());
            
            return vitals;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to get Core Web Vitals", e);
            return new CoreWebVitals();
        }
    }
    
    private void collectPerformanceMetrics() {
        if (!isMonitoring) return;
        
        try {
            PerformanceMetrics metrics = getPageLoadMetrics();
            String key = "page_load_" + System.currentTimeMillis();
            metricsMap.put(key, metrics);
        } catch (Exception e) {
            TestLogManager.error("Failed to collect performance metrics", e);
        }
    }
    
    private void collectNetworkMetrics() {
        if (!isMonitoring) return;
        
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
            for (LogEntry log : logs) {
                if (log.getMessage().contains("Network.responseReceived")) {
                    // Process network response metrics
                    TestLogManager.info("Network response: " + log.getMessage());
                }
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to collect network metrics", e);
        }
    }
    
    private void collectMemoryMetrics() {
        if (!isMonitoring) return;
        
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Map<String, Object> memoryInfo = (Map<String, Object>) js.executeScript(
                "return performance.memory;"
            );
            
            if (memoryInfo != null) {
                PerformanceMetrics metrics = new PerformanceMetrics();
                metrics.setMemoryUsed((Long) memoryInfo.get("usedJSHeapSize"));
                metrics.setMemoryTotal((Long) memoryInfo.get("totalJSHeapSize"));
                metrics.setMemoryLimit((Long) memoryInfo.get("jsHeapSizeLimit"));
                metrics.setTimestamp(LocalDateTime.now());
                
                String key = "memory_" + System.currentTimeMillis();
                metricsMap.put(key, metrics);
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to collect memory metrics", e);
        }
    }
    
    private long calculateLoadTime(Map<String, Object> navigationTiming) {
        try {
            long loadEventEnd = ((Number) navigationTiming.get("loadEventEnd")).longValue();
            long navigationStart = ((Number) navigationTiming.get("navigationStart")).longValue();
            return loadEventEnd - navigationStart;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private long calculateDOMContentLoaded(Map<String, Object> navigationTiming) {
        try {
            long domContentLoadedEventEnd = ((Number) navigationTiming.get("domContentLoadedEventEnd")).longValue();
            long navigationStart = ((Number) navigationTiming.get("navigationStart")).longValue();
            return domContentLoadedEventEnd - navigationStart;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private long getFirstPaint() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            List<Map<String, Object>> paintEntries = (List<Map<String, Object>>) js.executeScript(
                "return performance.getEntriesByType('paint');"
            );
            
            for (Map<String, Object> entry : paintEntries) {
                if ("first-paint".equals(entry.get("name"))) {
                    return ((Number) entry.get("startTime")).longValue();
                }
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to get first paint", e);
        }
        return 0;
    }
    
    private long getFirstContentfulPaint() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            List<Map<String, Object>> paintEntries = (List<Map<String, Object>>) js.executeScript(
                "return performance.getEntriesByType('paint');"
            );
            
            for (Map<String, Object> entry : paintEntries) {
                if ("first-contentful-paint".equals(entry.get("name"))) {
                    return ((Number) entry.get("startTime")).longValue();
                }
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to get first contentful paint", e);
        }
        return 0;
    }
    
    private long getCurrentMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private PerformanceStatistics calculateStatistics(List<PerformanceMetrics> metrics) {
        if (metrics.isEmpty()) {
            return new PerformanceStatistics();
        }
        
        PerformanceStatistics stats = new PerformanceStatistics();
        
        // Calculate averages
        double avgLoadTime = metrics.stream().mapToLong(PerformanceMetrics::getLoadTime).average().orElse(0);
        double avgExecutionTime = metrics.stream().mapToLong(PerformanceMetrics::getExecutionTime).average().orElse(0);
        
        stats.setAverageLoadTime(avgLoadTime);
        stats.setAverageExecutionTime(avgExecutionTime);
        stats.setTotalMeasurements(metrics.size());
        
        // Calculate percentiles
        List<Long> loadTimes = metrics.stream().map(PerformanceMetrics::getLoadTime).sorted().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        stats.setP95LoadTime(calculatePercentile(loadTimes, 95));
        stats.setP99LoadTime(calculatePercentile(loadTimes, 99));
        
        return stats;
    }
    
    private long calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        return values.get(Math.min(index, values.size() - 1));
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created performance report directory: " + reportDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create performance report directory", e);
        }
    }
    
    private String generateHTMLHeader() {
        return "<!DOCTYPE html><html><head><title>Performance Report</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:20px;}table{border-collapse:collapse;width:100%;}" +
               "th,td{border:1px solid #ddd;padding:8px;text-align:left;}th{background-color:#f2f2f2;}</style></head><body>";
    }
    
    private String generateSummarySection() {
        return "<h1>Performance Report Summary</h1>" +
               "<p>Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
               "<p>Total measurements: " + metricsMap.size() + "</p>";
    }
    
    private String generateMetricsTable() {
        StringBuilder table = new StringBuilder("<h2>Performance Metrics</h2><table><tr><th>Timestamp</th><th>Load Time (ms)</th><th>Execution Time (ms)</th><th>Memory Used (bytes)</th></tr>");
        
        for (PerformanceMetrics metrics : metricsMap.values()) {
            table.append("<tr>")
                 .append("<td>").append(metrics.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</td>")
                 .append("<td>").append(metrics.getLoadTime()).append("</td>")
                 .append("<td>").append(metrics.getExecutionTime()).append("</td>")
                 .append("<td>").append(metrics.getMemoryUsed()).append("</td>")
                 .append("</tr>");
        }
        
        table.append("</table>");
        return table.toString();
    }
    
    private String generateChartsSection() {
        return "<h2>Performance Charts</h2>" +
               "<p>Charts would be generated here using JavaScript libraries like Chart.js</p>";
    }
    
    private String generateRecommendationsSection() {
        return "<h2>Performance Recommendations</h2>" +
               "<ul>" +
               "<li>Optimize images and use appropriate formats</li>" +
               "<li>Minimize CSS and JavaScript files</li>" +
               "<li>Enable browser caching</li>" +
               "<li>Use CDN for static resources</li>" +
               "<li>Implement lazy loading for images</li>" +
               "</ul>";
    }
    
    private String generateHTMLFooter() {
        return "</body></html>";
    }
    
    /**
     * Performance metrics data model.
     */
    public static class PerformanceMetrics {
        private String actionName;
        private long loadTime;
        private long domContentLoaded;
        private long firstPaint;
        private long firstContentfulPaint;
        private long executionTime;
        private long memoryUsed;
        private long memoryTotal;
        private long memoryLimit;
        private int resourceCount;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public String getActionName() { return actionName; }
        public void setActionName(String actionName) { this.actionName = actionName; }
        
        public long getLoadTime() { return loadTime; }
        public void setLoadTime(long loadTime) { this.loadTime = loadTime; }
        
        public long getDomContentLoaded() { return domContentLoaded; }
        public void setDomContentLoaded(long domContentLoaded) { this.domContentLoaded = domContentLoaded; }
        
        public long getFirstPaint() { return firstPaint; }
        public void setFirstPaint(long firstPaint) { this.firstPaint = firstPaint; }
        
        public long getFirstContentfulPaint() { return firstContentfulPaint; }
        public void setFirstContentfulPaint(long firstContentfulPaint) { this.firstContentfulPaint = firstContentfulPaint; }
        
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        
        public long getMemoryUsed() { return memoryUsed; }
        public void setMemoryUsed(long memoryUsed) { this.memoryUsed = memoryUsed; }
        
        public long getMemoryTotal() { return memoryTotal; }
        public void setMemoryTotal(long memoryTotal) { this.memoryTotal = memoryTotal; }
        
        public long getMemoryLimit() { return memoryLimit; }
        public void setMemoryLimit(long memoryLimit) { this.memoryLimit = memoryLimit; }
        
        public int getResourceCount() { return resourceCount; }
        public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Performance statistics data model.
     */
    public static class PerformanceStatistics {
        private double averageLoadTime;
        private double averageExecutionTime;
        private long p95LoadTime;
        private long p99LoadTime;
        private int totalMeasurements;
        
        // Getters and setters
        public double getAverageLoadTime() { return averageLoadTime; }
        public void setAverageLoadTime(double averageLoadTime) { this.averageLoadTime = averageLoadTime; }
        
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        
        public long getP95LoadTime() { return p95LoadTime; }
        public void setP95LoadTime(long p95LoadTime) { this.p95LoadTime = p95LoadTime; }
        
        public long getP99LoadTime() { return p99LoadTime; }
        public void setP99LoadTime(long p99LoadTime) { this.p99LoadTime = p99LoadTime; }
        
        public int getTotalMeasurements() { return totalMeasurements; }
        public void setTotalMeasurements(int totalMeasurements) { this.totalMeasurements = totalMeasurements; }
    }
    
    /**
     * Core Web Vitals data model.
     */
    public static class CoreWebVitals {
        private long largestContentfulPaint;
        private long firstInputDelay;
        private double cumulativeLayoutShift;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public long getLargestContentfulPaint() { return largestContentfulPaint; }
        public void setLargestContentfulPaint(long largestContentfulPaint) { this.largestContentfulPaint = largestContentfulPaint; }
        
        public long getFirstInputDelay() { return firstInputDelay; }
        public void setFirstInputDelay(long firstInputDelay) { this.firstInputDelay = firstInputDelay; }
        
        public double getCumulativeLayoutShift() { return cumulativeLayoutShift; }
        public void setCumulativeLayoutShift(double cumulativeLayoutShift) { this.cumulativeLayoutShift = cumulativeLayoutShift; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}

