package performanceTracker;

import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;

import base.DriverManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    // Gson for deep-copying WebVitals in case capture returns the same instance
    private final Gson gson;

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

        // init gson
        this.gson = new GsonBuilder().create();
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
            System.out.println("   ⚠️  PERFORMANCE WARNING: Page load time " + loadTimeMs + "ms exceeds threshold "
                    + config.getPerformanceThresholdPageLoadMs() + "ms");

            // Automatically capture screenshot
            if (screenshotCapture != null) {
                screenshotCapture.captureSlowPageLoad(url, loadTimeMs);
            }
        }
    }

    /**
     * Capture Web Vitals for current page
     *
     * Fixes an issue where the same WebVitals instance may be reused/overwritten by the capture method.
     * We deep-copy the returned object to ensure each sample is a distinct object and we attempt to
     * populate url/timestamp if missing so grouping/aggregation works correctly.
     */
    public void captureWebVitals() {
        if (webVitalsCapture == null || !config.isCaptureWebVitals()) {
            return;
        }

        try {
            WebVitalsCapture.WebVitals raw = webVitalsCapture.captureWebVitals();
            if (raw == null) {
                return;
            }

            // create a deep copy (to avoid repeated references to same instance)
            WebVitalsCapture.WebVitals copy = deepCopyWebVitals(raw);

            // If copy has no URL or empty url, try to fill it from last page load or driver
            try {
                String url = null;
                // try to get url field via reflection
                try {
                    Field urlField = null;
                    try {
                        urlField = copy.getClass().getDeclaredField("url");
                    } catch (NoSuchFieldException ignore) {
                    }
                    if (urlField != null) {
                        urlField.setAccessible(true);
                        Object val = urlField.get(copy);
                        if (val != null && val.toString().trim().length() > 0) {
                            url = val.toString();
                        }
                    }
                } catch (Throwable ignore) {
                }

                if (url == null || url.isEmpty()) {
                    // try last page load
                    List<PageLoadMetric> loads = this.getPageLoadMetrics();
                    if (loads != null && !loads.isEmpty()) {
                        url = loads.get(loads.size() - 1).getUrl();
                    }
                }

                if ((url == null || url.isEmpty()) && driver != null) {
                    try {
                        url = driver.getCurrentUrl();
                    } catch (Throwable ignore) {
                    }
                }

                if (url != null && !url.isEmpty()) {
                    // set 'url' on the copy if field exists and is blank
                    try {
                        Field urlField = copy.getClass().getDeclaredField("url");
                        urlField.setAccessible(true);
                        Object current = urlField.get(copy);
                        if (current == null || current.toString().trim().isEmpty()) {
                            urlField.set(copy, url);
                        }
                    } catch (NoSuchFieldException nsf) {
                        // no 'url' field - ignore
                    } catch (Throwable t) {
                        // ignore any reflection issues
                    }
                }
            } catch (Throwable t) {
                // ignore – best-effort only
            }

            // Try to set timestamp/capturedAt if such a field exists (useful for grouping)
            try {
                Field timestampField = null;
                try {
                    timestampField = copy.getClass().getDeclaredField("timestamp");
                } catch (NoSuchFieldException ignore) {
                    // try other common names
                    try {
                        timestampField = copy.getClass().getDeclaredField("capturedAt");
                    } catch (NoSuchFieldException ignore2) {
                    }
                }

                if (timestampField != null) {
                    timestampField.setAccessible(true);
                    Object existing = timestampField.get(copy);
                    if (existing == null) {
                        Class<?> ft = timestampField.getType();
                        if (ft.equals(long.class) || ft.equals(Long.class)) {
                            timestampField.set(copy, System.currentTimeMillis());
                        } else {
                            timestampField.set(copy, String.valueOf(System.currentTimeMillis()));
                        }
                    }
                }
            } catch (Throwable ignore) {
            }

            // Add the copy to the list of samples (distinct object)
            webVitalsList.add(copy);

            // Logging and screenshot logic remains the same (operate on 'copy')
            if (invokeHasPoorWebVitals(copy)) {
                System.out.println("   ❌ POOR WEB VITALS: " + invokeGetCompactSummary(copy));
                if (screenshotCapture != null) {
                    String sampleUrl = safeGetUrlFromWebVitals(copy);
                    screenshotCapture.capturePoorWebVitals(sampleUrl, invokeGetOverallScore(copy));
                }
            } else if (invokeNeedsImprovement(copy)) {
                System.out.println("   ⚠️  WEB VITALS NEED IMPROVEMENT: " + invokeGetCompactSummary(copy));
            } else {
                System.out.println("   ✅ WEB VITALS: " + invokeGetCompactSummary(copy));
            }

        } catch (Exception e) {
            System.err.println("   ⚠️  Error capturing Web Vitals: " + e.getMessage());
        }
    }

    /**
     * Deep copy webvitals using Gson serialization.
     * This avoids accidental reuse of the same object instance returned by the capture method.
     */
    private WebVitalsCapture.WebVitals deepCopyWebVitals(WebVitalsCapture.WebVitals raw) {
        try {
            String json = gson.toJson(raw);
            return gson.fromJson(json, WebVitalsCapture.WebVitals.class);
        } catch (Exception e) {
            // fallback: if serialization fails, return raw (still better than null)
            return raw;
        }
    }

    // -------------------------------------------------------------------------
    // Reflection helpers to call methods on WebVitals POJO without compile-time dependency
    // -------------------------------------------------------------------------
    private boolean invokeHasPoorWebVitals(WebVitalsCapture.WebVitals v) {
        try {
            return (boolean) v.getClass().getMethod("hasPoorWebVitals").invoke(v);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean invokeNeedsImprovement(WebVitalsCapture.WebVitals v) {
        try {
            return (boolean) v.getClass().getMethod("needsImprovement").invoke(v);
        } catch (Throwable t) {
            return false;
        }
    }

    private String invokeGetCompactSummary(WebVitalsCapture.WebVitals v) {
        try {
            Object o = v.getClass().getMethod("getCompactSummary").invoke(v);
            return o != null ? o.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private double invokeGetNumeric(WebVitalsCapture.WebVitals v, String methodName) {
        try {
            Object o = v.getClass().getMethod(methodName).invoke(v);
            if (o instanceof Number) return ((Number) o).doubleValue();
            return 0.0;
        } catch (Throwable t) {
            return 0.0;
        }
    }

    private String invokeGetEmoji(WebVitalsCapture.WebVitals v, String methodName) {
        try {
            Object o = v.getClass().getMethod(methodName).invoke(v);
            return o != null ? o.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    // ✅ FINAL AND ONLY VERSION
    private int invokeGetOverallScore(WebVitalsCapture.WebVitals v) {
        try {
            Object o = v.getClass().getMethod("getOverallScore").invoke(v);
            if (o instanceof Number) {
                return ((Number) o).intValue();
            }
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private String safeGetUrlFromWebVitals(WebVitalsCapture.WebVitals v) {
        try {
            // try getter first
            try {
                Object o = v.getClass().getMethod("getUrl").invoke(v);
                if (o != null) return o.toString();
            } catch (NoSuchMethodException ignore) {
            }
            // fallback to field
            Field f = v.getClass().getDeclaredField("url");
            f.setAccessible(true);
            Object o = f.get(v);
            return o != null ? o.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Capture API calls for current page
     */
    public void captureApiCalls(String pageUrl) {
        if (!config.isCaptureApiResponseTimes()) {
            return;
        }

        if (networkMonitor == null) {
            networkMonitor = new NetworkPerformanceMonitor(driver);
        }

        networkMonitor.captureNetworkRequests(pageUrl);

        // Detailed API transaction capture via CDP (opt-in)
        if (transactionCapture == null && config.isCaptureApiDetailsEnabled()) {
            try {
                transactionCapture = new NetworkTransactionCapture(driver);
            } catch (Exception ignored) {
            }
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
                summary.append("   ").append(invokeGetCompactSummary(vitals)).append("\n");
                if (invokeHasPoorWebVitals(vitals)) {
                    summary.append("      ❌ POOR - Immediate optimization needed!\n");
                } else if (invokeNeedsImprovement(vitals)) {
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
                data.append("- ").append(safeGetUrlFromWebVitals(vitals)).append("\n");
                data.append("  - LCP: ").append(String.format("%.0f", invokeGetNumeric(vitals, "getLcp"))).append("ms ").append(invokeGetEmoji(vitals, "getLcpEmoji")).append("\n");
                data.append("  - CLS: ").append(String.format("%.3f", invokeGetNumeric(vitals, "getCls"))).append(" ").append(invokeGetEmoji(vitals, "getClsEmoji")).append("\n");
                data.append("  - FCP: ").append(String.format("%.0f", invokeGetNumeric(vitals, "getFcp"))).append("ms ").append(invokeGetEmoji(vitals, "getFcpEmoji")).append("\n");
                data.append("  - TTFB: ").append(String.format("%.0f", invokeGetNumeric(vitals, "getTtfb"))).append("ms ").append(invokeGetEmoji(vitals, "getTtfbEmoji")).append("\n");
                data.append("  - Overall Score: ").append(invokeGetOverallScore(vitals)).append("/100\n");

                if (invokeHasPoorWebVitals(vitals)) {
                    data.append("  - ❌ VERDICT: Poor - Immediate optimization needed!\n");
                } else if (invokeNeedsImprovement(vitals)) {
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
            if (invokeHasPoorWebVitals(vitals) || invokeNeedsImprovement(vitals)) {
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
        } catch (Exception ignore) {
        }
        return null;
    }

    public void capturedPerformanceMetrics(String url, long loadTime) {
        String baseUrl = System.getProperty("Environment").toUpperCase() + "_" + System.getProperty("ReleaseVersion");
        if (performanceTracker == null) {
            ConfigurationManager config = ConfigurationManager.getInstance();
            if (config.isPerformanceMonitoringEnabled()) {
                performanceTracker = new PerformanceTracker();
                System.out.println("📊 Performance monitoring enabled");
            }
        }

        if (performanceTracker != null) {
            performanceTracker.recordPageLoad(url, loadTime);
            try {
                performanceTracker.captureWebVitals();
                performanceTracker.captureApiCalls(url);
                performanceTracker.captureResourceUsage();
                performanceTracker.captureAdvancedMetrics();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }

    }
}
