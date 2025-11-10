package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import base.DriverManager;

import java.util.Map;

/**
 * Resource Usage Monitor
 * 
 * Monitors browser and page resource usage:
 * - Memory consumption (JavaScript heap size)
 * - DOM complexity (element count, depth)
 * - JavaScript execution time
 * - Resource counts (scripts, stylesheets, images)
 * - Detects potential memory leaks
 * - Identifies performance bottlenecks
 * 
 * Usage:
 * ResourceUsageMonitor monitor = new ResourceUsageMonitor(driver);
 * ResourceMetrics metrics = monitor.captureMetrics();
 * metrics.printSummary();
 */
public class ResourceUsageMonitor {
    
    private final WebDriver driver;
    private final ConfigurationManager config;
    
    // Thresholds
    private static final long MEMORY_WARNING_THRESHOLD_MB = 100;  // 100 MB
    private static final long MEMORY_CRITICAL_THRESHOLD_MB = 200; // 200 MB
    private static final int DOM_SIZE_WARNING_THRESHOLD = 1500;   // 1500 elements
    private static final int DOM_SIZE_CRITICAL_THRESHOLD = 3000;  // 3000 elements
    private static final int DOM_DEPTH_WARNING_THRESHOLD = 15;    // 15 levels
    
    public ResourceUsageMonitor() {
        this.driver = DriverManager.getDriver();
        this.config = ConfigurationManager.getInstance();
    }
    
    /**
     * Capture all resource usage metrics
     */
    public ResourceMetrics captureMetrics() {
        if (driver == null || !(driver instanceof JavascriptExecutor)) {
            System.err.println("⚠️  WebDriver does not support JavaScript execution");
            return new ResourceMetrics();
        }
        
        JavascriptExecutor js = (JavascriptExecutor) driver;
        ResourceMetrics metrics = new ResourceMetrics();
        
        try {
            // Capture memory metrics
            captureMemoryMetrics(js, metrics);
            
            // Capture DOM metrics
            captureDomMetrics(js, metrics);
            
            // Capture resource counts
            captureResourceCounts(js, metrics);
            
            // Capture timing metrics
            captureTimingMetrics(js, metrics);
            
            metrics.analyze();
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing resource metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * Capture memory metrics
     */
    private void captureMemoryMetrics(JavascriptExecutor js, ResourceMetrics metrics) {
        try {
            // Check if performance.memory is available (Chrome-specific)
            Object memoryAvailable = js.executeScript("return typeof performance.memory !== 'undefined';");
            
            if (Boolean.TRUE.equals(memoryAvailable)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memoryData = (Map<String, Object>) js.executeScript(
                    "return {" +
                    "  usedJSHeapSize: performance.memory.usedJSHeapSize," +
                    "  totalJSHeapSize: performance.memory.totalJSHeapSize," +
                    "  jsHeapSizeLimit: performance.memory.jsHeapSizeLimit" +
                    "};"
                );
                
                if (memoryData != null) {
                    metrics.setUsedMemoryBytes(getLong(memoryData, "usedJSHeapSize"));
                    metrics.setTotalMemoryBytes(getLong(memoryData, "totalJSHeapSize"));
                    metrics.setMemoryLimitBytes(getLong(memoryData, "jsHeapSizeLimit"));
                }
            }
        } catch (Exception e) {
            // Memory API not available in this browser
            metrics.setMemorySupported(false);
        }
    }
    
    /**
     * Capture DOM metrics
     */
    private void captureDomMetrics(JavascriptExecutor js, ResourceMetrics metrics) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> domData = (Map<String, Object>) js.executeScript(
                "function getMaxDepth(node) {" +
                "  if (!node || !node.children || node.children.length === 0) return 0;" +
                "  var maxChildDepth = 0;" +
                "  for (var i = 0; i < node.children.length; i++) {" +
                "    maxChildDepth = Math.max(maxChildDepth, getMaxDepth(node.children[i]));" +
                "  }" +
                "  return maxChildDepth + 1;" +
                "}" +
                "return {" +
                "  totalElements: document.getElementsByTagName('*').length," +
                "  maxDepth: getMaxDepth(document.body)," +
                "  bodyChildren: document.body ? document.body.children.length : 0" +
                "};"
            );
            
            if (domData != null) {
                metrics.setTotalElements(getInt(domData, "totalElements"));
                metrics.setMaxDomDepth(getInt(domData, "maxDepth"));
                metrics.setBodyChildren(getInt(domData, "bodyChildren"));
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing DOM metrics: " + e.getMessage());
        }
    }
    
    /**
     * Capture resource counts
     */
    private void captureResourceCounts(JavascriptExecutor js, ResourceMetrics metrics) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resourceData = (Map<String, Object>) js.executeScript(
                "return {" +
                "  scripts: document.getElementsByTagName('script').length," +
                "  stylesheets: document.getElementsByTagName('link[rel=\"stylesheet\"]').length + " +
                "               document.getElementsByTagName('style').length," +
                "  images: document.getElementsByTagName('img').length," +
                "  iframes: document.getElementsByTagName('iframe').length," +
                "  videos: document.getElementsByTagName('video').length" +
                "};"
            );
            
            if (resourceData != null) {
                metrics.setScriptCount(getInt(resourceData, "scripts"));
                metrics.setStylesheetCount(getInt(resourceData, "stylesheets"));
                metrics.setImageCount(getInt(resourceData, "images"));
                metrics.setIframeCount(getInt(resourceData, "iframes"));
                metrics.setVideoCount(getInt(resourceData, "videos"));
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing resource counts: " + e.getMessage());
        }
    }
    
    /**
     * Capture timing metrics
     */
    private void captureTimingMetrics(JavascriptExecutor js, ResourceMetrics metrics) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> timingData = (Map<String, Object>) js.executeScript(
                "var t = performance.timing;" +
                "return {" +
                "  domInteractive: t.domInteractive - t.navigationStart," +
                "  domContentLoaded: t.domContentLoadedEventEnd - t.navigationStart," +
                "  domComplete: t.domComplete - t.navigationStart," +
                "  loadComplete: t.loadEventEnd - t.navigationStart" +
                "};"
            );
            
            if (timingData != null) {
                metrics.setDomInteractiveTime(getLong(timingData, "domInteractive"));
                metrics.setDomContentLoadedTime(getLong(timingData, "domContentLoaded"));
                metrics.setDomCompleteTime(getLong(timingData, "domComplete"));
                metrics.setLoadCompleteTime(getLong(timingData, "loadComplete"));
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing timing metrics: " + e.getMessage());
        }
    }
    
    /**
     * Helper to get long value from map
     */
    private long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
    
    /**
     * Helper to get int value from map
     */
    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    /**
     * Resource Metrics Model
     */
    public static class ResourceMetrics {
        // Memory metrics
        private long usedMemoryBytes = 0;
        private long totalMemoryBytes = 0;
        private long memoryLimitBytes = 0;
        private boolean memorySupported = true;
        
        // DOM metrics
        private int totalElements = 0;
        private int maxDomDepth = 0;
        private int bodyChildren = 0;
        
        // Resource counts
        private int scriptCount = 0;
        private int stylesheetCount = 0;
        private int imageCount = 0;
        private int iframeCount = 0;
        private int videoCount = 0;
        
        // Timing metrics
        private long domInteractiveTime = 0;
        private long domContentLoadedTime = 0;
        private long domCompleteTime = 0;
        private long loadCompleteTime = 0;
        
        // Analysis results
        private String memoryStatus = "OK";
        private String domComplexityStatus = "OK";
        private boolean hasIssues = false;
        
        // Getters and setters
        public long getUsedMemoryBytes() { return usedMemoryBytes; }
        public void setUsedMemoryBytes(long usedMemoryBytes) { this.usedMemoryBytes = usedMemoryBytes; }
        
        public long getTotalMemoryBytes() { return totalMemoryBytes; }
        public void setTotalMemoryBytes(long totalMemoryBytes) { this.totalMemoryBytes = totalMemoryBytes; }
        
        public long getMemoryLimitBytes() { return memoryLimitBytes; }
        public void setMemoryLimitBytes(long memoryLimitBytes) { this.memoryLimitBytes = memoryLimitBytes; }
        
        public boolean isMemorySupported() { return memorySupported; }
        public void setMemorySupported(boolean memorySupported) { this.memorySupported = memorySupported; }
        
        public int getTotalElements() { return totalElements; }
        public void setTotalElements(int totalElements) { this.totalElements = totalElements; }
        
        public int getMaxDomDepth() { return maxDomDepth; }
        public void setMaxDomDepth(int maxDomDepth) { this.maxDomDepth = maxDomDepth; }
        
        public int getBodyChildren() { return bodyChildren; }
        public void setBodyChildren(int bodyChildren) { this.bodyChildren = bodyChildren; }
        
        public int getScriptCount() { return scriptCount; }
        public void setScriptCount(int scriptCount) { this.scriptCount = scriptCount; }
        
        public int getStylesheetCount() { return stylesheetCount; }
        public void setStylesheetCount(int stylesheetCount) { this.stylesheetCount = stylesheetCount; }
        
        public int getImageCount() { return imageCount; }
        public void setImageCount(int imageCount) { this.imageCount = imageCount; }
        
        public int getIframeCount() { return iframeCount; }
        public void setIframeCount(int iframeCount) { this.iframeCount = iframeCount; }
        
        public int getVideoCount() { return videoCount; }
        public void setVideoCount(int videoCount) { this.videoCount = videoCount; }
        
        public long getDomInteractiveTime() { return domInteractiveTime; }
        public void setDomInteractiveTime(long domInteractiveTime) { 
            this.domInteractiveTime = domInteractiveTime; 
        }
        
        public long getDomContentLoadedTime() { return domContentLoadedTime; }
        public void setDomContentLoadedTime(long domContentLoadedTime) { 
            this.domContentLoadedTime = domContentLoadedTime; 
        }
        
        public long getDomCompleteTime() { return domCompleteTime; }
        public void setDomCompleteTime(long domCompleteTime) { 
            this.domCompleteTime = domCompleteTime; 
        }
        
        public long getLoadCompleteTime() { return loadCompleteTime; }
        public void setLoadCompleteTime(long loadCompleteTime) { 
            this.loadCompleteTime = loadCompleteTime; }
        
        public String getMemoryStatus() { return memoryStatus; }
        public String getDomComplexityStatus() { return domComplexityStatus; }
        public boolean hasIssues() { return hasIssues; }
        
        /**
         * Get used memory in MB
         */
        public double getUsedMemoryMB() {
            return usedMemoryBytes / (1024.0 * 1024.0);
        }
        
        /**
         * Get total memory in MB
         */
        public double getTotalMemoryMB() {
            return totalMemoryBytes / (1024.0 * 1024.0);
        }
        
        /**
         * Get memory limit in MB
         */
        public double getMemoryLimitMB() {
            return memoryLimitBytes / (1024.0 * 1024.0);
        }
        
        /**
         * Get memory usage percentage
         */
        public double getMemoryUsagePercent() {
            if (memoryLimitBytes == 0) return 0;
            return (usedMemoryBytes * 100.0) / memoryLimitBytes;
        }
        
        /**
         * Analyze metrics and set status
         */
        public void analyze() {
            // Analyze memory usage
            if (memorySupported) {
                double usedMB = getUsedMemoryMB();
                if (usedMB > MEMORY_CRITICAL_THRESHOLD_MB) {
                    memoryStatus = "CRITICAL";
                    hasIssues = true;
                } else if (usedMB > MEMORY_WARNING_THRESHOLD_MB) {
                    memoryStatus = "WARNING";
                    hasIssues = true;
                } else {
                    memoryStatus = "OK";
                }
            }
            
            // Analyze DOM complexity
            if (totalElements > DOM_SIZE_CRITICAL_THRESHOLD) {
                domComplexityStatus = "CRITICAL";
                hasIssues = true;
            } else if (totalElements > DOM_SIZE_WARNING_THRESHOLD) {
                domComplexityStatus = "WARNING";
                hasIssues = true;
            } else if (maxDomDepth > DOM_DEPTH_WARNING_THRESHOLD) {
                domComplexityStatus = "WARNING";
                hasIssues = true;
            } else {
                domComplexityStatus = "OK";
            }
        }
        
        /**
         * Print summary
         */
        public void printSummary() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("💻 RESOURCE USAGE SUMMARY");
            System.out.println("=".repeat(80));
            
            // Memory metrics
            if (memorySupported) {
                System.out.println("\n🧠 MEMORY USAGE:");
                System.out.println(String.format("   Used Memory:      %.2f MB / %.2f MB (%.1f%%)", 
                    getUsedMemoryMB(), getMemoryLimitMB(), getMemoryUsagePercent()));
                System.out.println(String.format("   Total Allocated:  %.2f MB", getTotalMemoryMB()));
                System.out.println("   Status:           " + getStatusIcon(memoryStatus) + " " + memoryStatus);
                
                if ("CRITICAL".equals(memoryStatus)) {
                    System.out.println("   ⚠️  WARNING: High memory usage - possible memory leak!");
                } else if ("WARNING".equals(memoryStatus)) {
                    System.out.println("   ⚠️  Memory usage is elevated");
                }
            } else {
                System.out.println("\n🧠 MEMORY USAGE: Not supported in this browser");
            }
            
            // DOM metrics
            System.out.println("\n📄 DOM COMPLEXITY:");
            System.out.println("   Total Elements:   " + totalElements + " elements");
            System.out.println("   Max DOM Depth:    " + maxDomDepth + " levels");
            System.out.println("   Body Children:    " + bodyChildren);
            System.out.println("   Status:           " + getStatusIcon(domComplexityStatus) + " " + domComplexityStatus);
            
            if ("CRITICAL".equals(domComplexityStatus)) {
                System.out.println("   ⚠️  WARNING: Very complex DOM - will impact performance!");
            } else if ("WARNING".equals(domComplexityStatus)) {
                System.out.println("   ⚠️  DOM complexity is elevated");
            }
            
            // Resource counts
            System.out.println("\n📦 RESOURCE COUNTS:");
            System.out.println("   Scripts:          " + scriptCount);
            System.out.println("   Stylesheets:      " + stylesheetCount);
            System.out.println("   Images:           " + imageCount);
            System.out.println("   IFrames:          " + iframeCount);
            System.out.println("   Videos:           " + videoCount);
            
            // Timing metrics
            if (domCompleteTime > 0) {
                System.out.println("\n⏱️  DOM TIMING:");
                System.out.println("   DOM Interactive:  " + domInteractiveTime + " ms");
                System.out.println("   DOM Content Loaded: " + domContentLoadedTime + " ms");
                System.out.println("   DOM Complete:     " + domCompleteTime + " ms");
                System.out.println("   Load Complete:    " + loadCompleteTime + " ms");
            }
            
            // Recommendations
            if (hasIssues) {
                System.out.println("\n💡 RECOMMENDATIONS:");
                if ("CRITICAL".equals(memoryStatus) || "WARNING".equals(memoryStatus)) {
                    System.out.println("   • Check for memory leaks (event listeners, timers, closures)");
                    System.out.println("   • Remove unused variables and references");
                    System.out.println("   • Use browser DevTools Memory Profiler");
                }
                if ("CRITICAL".equals(domComplexityStatus) || "WARNING".equals(domComplexityStatus)) {
                    System.out.println("   • Simplify DOM structure (reduce nesting)");
                    System.out.println("   • Use virtualization for long lists");
                    System.out.println("   • Remove unused DOM elements");
                    System.out.println("   • Consider lazy loading for off-screen content");
                }
            }
            
            System.out.println("=".repeat(80) + "\n");
        }
        
        /**
         * Get status icon
         */
        private String getStatusIcon(String status) {
            switch (status) {
                case "OK": return "✅";
                case "WARNING": return "⚠️";
                case "CRITICAL": return "❌";
                default: return "ℹ️";
            }
        }
        
        /**
         * Get compact summary for reports
         */
        public String getCompactSummary() {
            StringBuilder summary = new StringBuilder();
            
            if (memorySupported) {
                summary.append(String.format("Memory: %.1fMB ", getUsedMemoryMB()));
                summary.append(getStatusIcon(memoryStatus)).append(" | ");
            }
            
            summary.append("DOM: ").append(totalElements).append(" elements ");
            summary.append(getStatusIcon(domComplexityStatus));
            
            if (maxDomDepth > DOM_DEPTH_WARNING_THRESHOLD) {
                summary.append(" (depth: ").append(maxDomDepth).append(")");
            }
            
            return summary.toString();
        }
    }
}


