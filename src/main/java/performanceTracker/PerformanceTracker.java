package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;

import base.DriverManager;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Performance Tracker for Functional Tests
 * Automatically tracks page load times and API response times during functional tests
 * 
 * Usage: Automatically used by BaseSeleniumTest when performance monitoring is enabled
 */
public class PerformanceTracker {
    
    private final WebDriver driver;
    private final ConfigurationManager config;
    private final List<PageLoadMetric> pageLoads;
    private final List<WebVitalsCapture.WebVitals> webVitalsList;
    private final List<ResourceUsageMonitor.ResourceMetrics> resourceMetricsList;
    private final List<AdvancedPerformanceMetrics.AdvancedMetrics> advancedMetricsList;
    private NetworkPerformanceMonitor networkMonitor;
    private NetworkTransactionCapture transactionCapture;
    private WebVitalsCapture webVitalsCapture;
    private PerformanceScreenshotCapture screenshotCapture;
    private ResourceUsageMonitor resourceMonitor;
    private AdvancedPerformanceMetrics advancedMetrics;
    private long testStartTime;
    
    protected BrowserMobNetworkCapture networkCapture;
    protected static PerformanceTracker performanceTracker;
    
    public PerformanceTracker() {
        this.driver = DriverManager.getDriver();
        this.config = ConfigurationManager.getInstance();
        this.pageLoads = new ArrayList<>();
        this.webVitalsList = new ArrayList<>();
        this.resourceMetricsList = new ArrayList<>();
        this.advancedMetricsList = new ArrayList<>();
        this.webVitalsCapture = new WebVitalsCapture();
        this.screenshotCapture = new PerformanceScreenshotCapture();
        this.resourceMonitor = new ResourceUsageMonitor();
        this.advancedMetrics = new AdvancedPerformanceMetrics();
        this.testStartTime = System.currentTimeMillis();
    }
    
    /**
     * Record page load time
     */
    public void recordPageLoad(String url, long loadTimeMs) {
        if (!config.isCapturePageLoadTimes()) {
            return;
        }
        
        PageLoadMetric metric = new PageLoadMetric(url, loadTimeMs);
        pageLoads.add(metric);
        
        // Check threshold
        if (loadTimeMs > config.getPerformanceThresholdPageLoadMs()) {
            System.out.println("   ⚠️  PERFORMANCE WARNING: Page load time " + loadTimeMs + "ms exceeds threshold " + 
                              config.getPerformanceThresholdPageLoadMs() + "ms");
            
            // Automatically capture screenshot
            if (screenshotCapture != null) {
                screenshotCapture.captureSlowPageLoad(url, loadTimeMs);
            }
        }
    }
    
    /**
     * Capture Web Vitals for current page
     */
    public void captureWebVitals() {
        if (webVitalsCapture == null) {
            return;
        }
        
        try {
            WebVitalsCapture.WebVitals vitals = webVitalsCapture.captureWebVitals();
            if (vitals != null) {
                webVitalsList.add(vitals);
                
                // Print summary if has issues
                if (vitals.hasPoorWebVitals()) {
                    System.out.println("   ❌ POOR WEB VITALS: " + vitals.getCompactSummary());
                    
                    // Automatically capture screenshot for poor Web Vitals
                    if (screenshotCapture != null) {
                        screenshotCapture.capturePoorWebVitals(vitals.getUrl(), vitals.getOverallScore());
                    }
                } else if (vitals.needsImprovement()) {
                    System.out.println("   ⚠️  WEB VITALS NEED IMPROVEMENT: " + vitals.getCompactSummary());
                } else {
                    System.out.println("   ✅ WEB VITALS: " + vitals.getCompactSummary());
                }
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error capturing Web Vitals: " + e.getMessage());
        }
    }
    
    /**
     * Capture API calls for current page
     */
    public void captureApiCalls() {
        if (!config.isCaptureApiResponseTimes()) {
            return;
        }
        
        if (networkMonitor == null) {
            networkMonitor = new NetworkPerformanceMonitor(driver);
        }
        
        networkMonitor.captureNetworkRequests();

        // Detailed API transaction capture via CDP (opt-in)
        if (transactionCapture == null && config.isCaptureApiDetailsEnabled()) {
            try {
                transactionCapture = new NetworkTransactionCapture(driver);
            } catch (Exception ignored) { }
        }
    }
    
    /**
     * Capture resource usage metrics
     */
    public void captureResourceUsage() {
        if (resourceMonitor == null) {
            return;
        }
        
        try {
            ResourceUsageMonitor.ResourceMetrics metrics = resourceMonitor.captureMetrics();
            if (metrics != null) {
                resourceMetricsList.add(metrics);
                
                // Print summary if has issues
                if (metrics.hasIssues()) {
                    System.out.println("   ⚠️  RESOURCE ISSUES: " + metrics.getCompactSummary());
                } else {
                    System.out.println("   ✅ RESOURCE USAGE: " + metrics.getCompactSummary());
                }
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error capturing resource usage: " + e.getMessage());
        }
    }
    
    /**
     * Capture Advanced Performance Metrics (FID, INP, Performance Observer API)
     */
    public void captureAdvancedMetrics() {
        if (advancedMetrics == null || !config.isPerformanceMonitoringEnabled()) {
            return;
        }
        
        try {
            AdvancedPerformanceMetrics.AdvancedMetrics metrics = advancedMetrics.captureAdvancedMetrics();
            if (metrics != null) {
                advancedMetricsList.add(metrics);
                
                // Print compact summary
                String summary = metrics.getCompactSummary();
                if (summary != null && !summary.isEmpty()) {
                    System.out.println("   🚀 ADVANCED METRICS: " + summary);
                }
            }
        } catch (Exception e) {
            System.err.println("   ⚠️  Error capturing advanced metrics: " + e.getMessage());
        }
    }
    
    /**
     * Get advanced metrics list
     */
    public List<AdvancedPerformanceMetrics.AdvancedMetrics> getAdvancedMetricsList() {
        return advancedMetricsList;
    }
    
    /**
     * Get performance summary
     */
    public String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("\n").append("=".repeat(80)).append("\n");
        summary.append("📊 PERFORMANCE SUMMARY\n");
        summary.append("=".repeat(80)).append("\n");
        
        // Page loads
        if (!pageLoads.isEmpty()) {
            summary.append("📄 PAGE LOADS (").append(pageLoads.size()).append("):\n");
            for (PageLoadMetric metric : pageLoads) {
                summary.append("   ").append(metric.getStatus()).append(" ")
                       .append(metric.getUrl()).append(" - ")
                       .append(metric.getLoadTimeMs()).append("ms\n");
            }
            summary.append("\n");
        }
        
        // API calls
        if (networkMonitor != null) {
            List<NetworkPerformanceMonitor.NetworkRequest> apiCalls = networkMonitor.getApiRequests();
            if (!apiCalls.isEmpty()) {
                NetworkPerformanceMonitor.NetworkSummary apiSummary = networkMonitor.getSummary();
                summary.append("🌐 API CALLS (").append(apiCalls.size()).append("):\n");
                summary.append("   Avg Response Time: ").append(String.format("%.2f", apiSummary.getAvgDuration())).append("ms\n");
                summary.append("   Slowest API: ").append(apiSummary.getMaxDuration()).append("ms\n");
                summary.append("   Fastest API: ").append(apiSummary.getMinDuration()).append("ms\n");
                
                // Show slow APIs
                List<NetworkPerformanceMonitor.NetworkRequest> slowApis = networkMonitor.getSlowApiCalls(
                    config.getPerformanceThresholdApiResponseMs()
                );
                if (!slowApis.isEmpty()) {
                    summary.append("\n   ⚠️  SLOW APIs (>").append(config.getPerformanceThresholdApiResponseMs()).append("ms):\n");
                    for (NetworkPerformanceMonitor.NetworkRequest api : slowApis) {
                        summary.append("      ❌ ").append(api.getEndpoint())
                               .append(" - ").append(api.getDuration()).append("ms\n");
                    }
                }
            }
        }
        
        // Web Vitals
        if (!webVitalsList.isEmpty()) {
            summary.append("\n🎯 WEB VITALS:\n");
            for (WebVitalsCapture.WebVitals vitals : webVitalsList) {
                summary.append("   ").append(vitals.getCompactSummary()).append("\n");
                if (vitals.hasPoorWebVitals()) {
                    summary.append("      ❌ POOR - Immediate optimization needed!\n");
                } else if (vitals.needsImprovement()) {
                    summary.append("      ⚠️  NEEDS IMPROVEMENT\n");
                }
            }
        }
        
        // Resource Usage
        if (!resourceMetricsList.isEmpty()) {
            summary.append("\n💻 RESOURCE USAGE:\n");
            for (ResourceUsageMonitor.ResourceMetrics metrics : resourceMetricsList) {
                summary.append("   ").append(metrics.getCompactSummary()).append("\n");
                if (metrics.hasIssues()) {
                    summary.append("      ⚠️  Resource issues detected - check memory and DOM complexity\n");
                }
            }
        }
        
        summary.append("=".repeat(80)).append("\n");
        
        return summary.toString();
    }
    
    /**
     * Get performance data for defect report
     */
    public String getPerformanceDataForDefect() {
        if (!config.isIncludePerformanceInDefect()) {
            return "";
        }
        
        StringBuilder data = new StringBuilder();
        
        data.append("\n\n### Performance Data\n\n");
        
        // Page loads
        if (!pageLoads.isEmpty()) {
            data.append("**Page Load Times:**\n");
            for (PageLoadMetric metric : pageLoads) {
                data.append("- ").append(metric.getUrl()).append(": ")
                    .append(metric.getLoadTimeMs()).append("ms");
                if (metric.isSlowLoad(config.getPerformanceThresholdPageLoadMs())) {
                    data.append(" ⚠️ SLOW");
                }
                data.append("\n");
            }
            data.append("\n");
        }
        
        // API calls summary
        if (networkMonitor != null) {
            List<NetworkPerformanceMonitor.NetworkRequest> apiCalls = networkMonitor.getApiRequests();
            if (!apiCalls.isEmpty()) {
                NetworkPerformanceMonitor.NetworkSummary summary = networkMonitor.getSummary();
                data.append("**API Performance:**\n");
                data.append("- Total API Calls: ").append(summary.getTotalRequests()).append("\n");
                data.append("- Avg Response Time: ").append(String.format("%.2f", summary.getAvgDuration())).append("ms\n");
                data.append("- Slowest API: ").append(summary.getMaxDuration()).append("ms\n\n");
                
                // List slow APIs
                List<NetworkPerformanceMonitor.NetworkRequest> slowApis = networkMonitor.getSlowApiCalls(
                    config.getPerformanceThresholdApiResponseMs()
                );
                if (!slowApis.isEmpty()) {
                    data.append("**Slow APIs (>").append(config.getPerformanceThresholdApiResponseMs()).append("ms):**\n");
                    for (NetworkPerformanceMonitor.NetworkRequest api : slowApis) {
                        data.append("- ").append(api.getEndpoint()).append(": ")
                            .append(api.getDuration()).append("ms\n");
                    }
                }
            }
        }
        
        // Detailed API transactions (sample drill-down)
        if (transactionCapture != null && config.isCaptureApiDetailsEnabled()) {
            java.util.List<NetworkTransactionCapture.ApiTransaction> txs = transactionCapture.getTransactions();
            if (!txs.isEmpty()) {
                data.append("\n**API Transactions (sample):**\n");
                int shown = 0;
                for (NetworkTransactionCapture.ApiTransaction tx : txs) {
                    if (shown++ >= 5) break;
                    data.append("- ").append(tx.method).append(" ").append(tx.path).append(" [").append(String.valueOf(tx.status)).append("]\n");
                    data.append("  - Request headers: ").append(String.valueOf(tx.requestHeaders)).append("\n");
                    if (tx.requestBody != null) {
                        data.append("  - Request body (truncated): \n\n");
                        data.append(tx.requestBody).append("\n\n");
                    }
                    data.append("  - Response headers: ").append(String.valueOf(tx.responseHeaders)).append("\n");
                    if (tx.responseBody != null) {
                        data.append("  - Response body (truncated): \n\n");
                        data.append(tx.responseBody).append("\n\n");
                    }
                }
            }
        }

        // Web Vitals
        if (!webVitalsList.isEmpty()) {
            data.append("\n**Web Vitals (Google Standards):**\n");
            for (WebVitalsCapture.WebVitals vitals : webVitalsList) {
                data.append("- ").append(vitals.getUrl()).append("\n");
                data.append("  - LCP: ").append(String.format("%.0f", vitals.getLcp())).append("ms ").append(vitals.getLcpEmoji()).append("\n");
                data.append("  - CLS: ").append(String.format("%.3f", vitals.getCls())).append(" ").append(vitals.getClsEmoji()).append("\n");
                data.append("  - FCP: ").append(String.format("%.0f", vitals.getFcp())).append("ms ").append(vitals.getFcpEmoji()).append("\n");
                data.append("  - TTFB: ").append(String.format("%.0f", vitals.getTtfb())).append("ms ").append(vitals.getTtfbEmoji()).append("\n");
                data.append("  - Overall Score: ").append(vitals.getOverallScore()).append("/100\n");
                
                if (vitals.hasPoorWebVitals()) {
                    data.append("  - ❌ VERDICT: Poor - Immediate optimization needed!\n");
                } else if (vitals.needsImprovement()) {
                    data.append("  - ⚠️ VERDICT: Needs Improvement\n");
                } else {
                    data.append("  - ✅ VERDICT: Good\n");
                }
            }
        }
        
        return data.toString();
    }
    
    /**
     * Check if any performance thresholds were violated
     */
    public boolean hasPerformanceIssues() {
        // Check page load times
        for (PageLoadMetric metric : pageLoads) {
            if (metric.isSlowLoad(config.getPerformanceThresholdPageLoadMs())) {
                return true;
            }
        }
        
        // Check API response times
        if (networkMonitor != null) {
            List<NetworkPerformanceMonitor.NetworkRequest> slowApis = networkMonitor.getSlowApiCalls(
                config.getPerformanceThresholdApiResponseMs()
            );
            if (!slowApis.isEmpty()) {
                return true;
            }
        }
        
        // Check Web Vitals
        for (WebVitalsCapture.WebVitals vitals : webVitalsList) {
            if (vitals.hasPoorWebVitals() || vitals.needsImprovement()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get network monitor
     */
    public NetworkPerformanceMonitor getNetworkMonitor() {
        return networkMonitor;
    }
    
    public List<PageLoadMetric> getPageLoadMetrics() {
        return new ArrayList<>(pageLoads);
    }
    
    public List<WebVitalsCapture.WebVitals> getWebVitalsList() {
        return new ArrayList<>(webVitalsList);
    }
    
    public List<ResourceUsageMonitor.ResourceMetrics> getResourceMetricsList() {
        return new ArrayList<>(resourceMetricsList);
    }
    
    public String getTestCaseKey() {
        // This would typically be set during test execution
        // For now, return a default or extract from test context
        return "TEST-CASE";
    }
    
    public long getTestStartTime() {
        return testStartTime;
    }
    
    /**
     * Get screenshot capture
     */
    public PerformanceScreenshotCapture getScreenshotCapture() {
        return screenshotCapture;
    }
    
    /**
     * Get all performance screenshots captured during the test
     */
    public List<File> getPerformanceScreenshots() {
        if (screenshotCapture != null) {
            return screenshotCapture.getCapturedScreenshots();
        }
        return new ArrayList<>();
    }
    
    /**
     * Generate network waterfall visualization
     */
    public File generateWaterfallVisualization(String testCaseKey) {
        if (networkMonitor == null) {
            return null;
        }
        
        List<NetworkPerformanceMonitor.NetworkRequest> requests = networkMonitor.getApiRequests();
        if (requests.isEmpty()) {
            return null;
        }
        
        NetworkWaterfallVisualizer visualizer = new NetworkWaterfallVisualizer();
        return visualizer.generateWaterfall(testCaseKey, requests);
    }
    
    /**
     * Page Load Metric Model
     */
    public static class PageLoadMetric {
        private final String url;
        private final long loadTimeMs;
        private final long timestamp;
        
        public PageLoadMetric(String url, long loadTimeMs) {
            this.url = url;
            this.loadTimeMs = loadTimeMs;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getUrl() {
            return url;
        }
        
        public long getLoadTimeMs() {
            return loadTimeMs;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isSlowLoad(int thresholdMs) {
            return loadTimeMs > thresholdMs;
        }
        
        public String getStatus() {
            if (loadTimeMs < 2000) return "✅";
            if (loadTimeMs < 5000) return "⚠️ ";
            return "❌";
        }
    }
    
    /**
     * Suite-level driver: collects all ITestResult and generates reports after the whole suite.
     */
    public static void generatePerformanceReportsForSuite(ISuite suite) {
        ConfigurationManager config = ConfigurationManager.getInstance();

        if (!config.isPerformanceMonitoringEnabled() || !config.isGenerateHtmlPerformanceReport()) {
            System.out.println("ℹ️ Performance reporting disabled. Skipping suite-level generation.");
            return;
        }

        try {
            System.out.println("📦 Suite complete. Generating performance reports for all tests...");

            // Collect unique results across passed/failed/skipped (avoids duplicates from retries)
            Set<ITestResult> allResults = new LinkedHashSet<>();
            for (ISuiteResult sr : suite.getResults().values()) {
                ITestContext ctx = sr.getTestContext();
                allResults.addAll(ctx.getPassedTests().getAllResults());
                allResults.addAll(ctx.getFailedTests().getAllResults());
                allResults.addAll(ctx.getSkippedTests().getAllResults());
            }

            for (ITestResult result : allResults) {
                generatePerformanceReportsForResult(result);
            }

            System.out.println("✅ Suite-level performance report generation finished.");
        } catch (Exception e) {
            System.err.println("❌ Error generating performance reports after suite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate performance reports per test result (called at suite end).
     */
    private static void generatePerformanceReportsForResult(ITestResult result) {
        ConfigurationManager config = ConfigurationManager.getInstance();

        if (result == null) {
            System.err.println("⚠️ Skipping report generation: ITestResult is null.");
            return;
        }
        if (!config.isPerformanceMonitoringEnabled() || !config.isGenerateHtmlPerformanceReport()) {
            return;
        }

        try {
            // Suite name + URL
            String suiteName = result.getTestContext().getSuite().getName();
            String url = extractUrlFromTracker(performanceTracker);

            System.out.println("📊 Generating performance reports for suite: " + suiteName);

            // --- HTML Report ---
            PerformanceReportGenerator reportGenerator = new PerformanceReportGenerator();

            // If your generator signature is: generateReport(String suiteName, PerformanceTracker tracker, String url)
            File reportFile = reportGenerator.generateReport(suiteName, performanceTracker, url);
            System.setProperty("performanceReportPath", reportFile.getAbsolutePath());

            // If your current signature is generateReport(String suiteName, PerformanceTracker tracker)
            // use this instead:
            // File reportFile = reportGenerator.generateReport(suiteName, performanceTracker);

            if (reportFile != null) {
                System.out.println("✅ HTML Performance Report Generated: " + reportFile.getName());
                result.setAttribute("performanceReport", reportFile);
            }

            // --- Enhanced Report ---
            EnhancedPerformanceReportGenerator enhancedGenerator = new EnhancedPerformanceReportGenerator();
            File enhancedReportFile = enhancedGenerator.generateEnhancedReport(suiteName, performanceTracker, url);
            if (enhancedReportFile != null) {
                System.out.println("✅ Enhanced Performance Report Generated: " + enhancedReportFile.getName());
                result.setAttribute("enhancedPerformanceReport", enhancedReportFile);
            }

            // If you re-enable waterfall/har, ensure you have a suite-level key/name to save files under.
            // Example:
            // File waterfallFile = performanceTracker.generateWaterfallVisualization(suiteName);
            // if (waterfallFile != null) {
            //     System.out.println("✅ Network Waterfall Visualization Generated: " + waterfallFile.getName());
            //     result.setAttribute("waterfallVisualization", waterfallFile);
            // }

        } catch (Exception e) {
            System.err.println("❌ Error generating performance reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract URL from performance tracker (from first page load)
     */
    private static String extractUrlFromTracker(PerformanceTracker tracker) {
        if (tracker == null) {
            return null;
        }
        try {
            List<PerformanceTracker.PageLoadMetric> pageLoads = tracker.getPageLoadMetrics();
            if (pageLoads != null && !pageLoads.isEmpty()) {
                return pageLoads.get(0).getUrl(); // base URL (first page load)
            }
        } catch (Exception ignore) {}
        return null;
    }
    
    public void capturedPerformanceMetrics(long loadTime)
	{
    	String baseUrl = System.getProperty("Environment").toUpperCase() + "_" + System.getProperty("ReleaseVersion");
    	if (performanceTracker == null)
		{
    		ConfigurationManager config = ConfigurationManager.getInstance();
    		 if (config.isPerformanceMonitoringEnabled()) {
    	            performanceTracker = new PerformanceTracker();
    	            System.out.println("📊 Performance monitoring enabled");
    	        }
		}
    	
    	if (performanceTracker != null) {
            performanceTracker.recordPageLoad(baseUrl, loadTime);
            try {
                performanceTracker.captureWebVitals();
                performanceTracker.captureApiCalls();
                performanceTracker.captureResourceUsage();
                performanceTracker.captureAdvancedMetrics();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }

	}
}

