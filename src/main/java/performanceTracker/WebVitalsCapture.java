package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import base.DriverManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Web Vitals Capture - Industry-Standard Performance Metrics
 * 
 * Captures Core Web Vitals and other important performance metrics:
 * 
 * CORE WEB VITALS (Google's key metrics):
 * - LCP (Largest Contentful Paint): User-perceived load speed
 * - FID (First Input Delay): Interactivity [Note: Can't capture in automation, use TBT instead]
 * - CLS (Cumulative Layout Shift): Visual stability
 * 
 * OTHER KEY METRICS:
 * - FCP (First Contentful Paint): Initial render
 * - TTFB (Time to First Byte): Server response
 * - DOM Content Loaded: DOM ready
 * - Full Page Load: Complete page load
 * - Total Blocking Time (TBT): Main thread blocking time
 * 
 * THRESHOLDS (Google Standards):
 * - LCP: < 2.5s (Good), 2.5-4.0s (Needs Improvement), > 4.0s (Poor)
 * - FID: < 100ms (Good), 100-300ms (Needs Improvement), > 300ms (Poor)
 * - CLS: < 0.1 (Good), 0.1-0.25 (Needs Improvement), > 0.25 (Poor)
 * 
 * Usage:
 * WebVitalsCapture vitals = new WebVitalsCapture(driver);
 * WebVitals metrics = vitals.captureWebVitals();
 * System.out.println(metrics.getSummary());
 */
public class WebVitalsCapture {
    
    private final WebDriver driver;
    private final ConfigurationManager config;
    
    // Google's thresholds for Core Web Vitals
    private static final double LCP_GOOD_THRESHOLD = 2500.0;      // 2.5 seconds
    private static final double LCP_POOR_THRESHOLD = 4000.0;      // 4.0 seconds
    private static final double FID_GOOD_THRESHOLD = 100.0;       // 100ms
    private static final double FID_POOR_THRESHOLD = 300.0;       // 300ms
    private static final double CLS_GOOD_THRESHOLD = 0.1;         // 0.1
    private static final double CLS_POOR_THRESHOLD = 0.25;        // 0.25
    private static final double FCP_GOOD_THRESHOLD = 1800.0;      // 1.8 seconds
    private static final double TTFB_GOOD_THRESHOLD = 800.0;      // 800ms
    
    public WebVitalsCapture() {
        this.driver = DriverManager.getDriver();
        this.config = ConfigurationManager.getInstance();
    }
    
    /**
     * Capture all Web Vitals metrics
     */
    public WebVitals captureWebVitals() {
        if (!(driver instanceof JavascriptExecutor)) {
            System.err.println("⚠️  WebDriver does not support JavaScript execution");
            return new WebVitals();
        }
        
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        try {
            // Capture Navigation Timing API metrics
            Map<String, Object> timingData = captureNavigationTiming(js);
            
            // Capture Paint Timing API metrics
            Map<String, Object> paintData = capturePaintTiming(js);
            
            // Capture Layout Shift (CLS)
            Double cls = captureLayoutShift(js);
            
            // Capture Largest Contentful Paint (LCP)
            Double lcp = captureLargestContentfulPaint(js);
            
            // Build WebVitals object
            WebVitals vitals = new WebVitals();
            
            // Set Core Web Vitals
            vitals.setLcp(lcp != null ? lcp : 0.0);
            vitals.setCls(cls != null ? cls : 0.0);
            
            // Set other metrics from Navigation Timing
            if (timingData != null) {
                vitals.setTtfb(getDouble(timingData, "ttfb"));
                vitals.setDomContentLoaded(getDouble(timingData, "domContentLoaded"));
                vitals.setFullPageLoad(getDouble(timingData, "fullPageLoad"));
                vitals.setDnsLookup(getDouble(timingData, "dnsLookup"));
                vitals.setTcpConnection(getDouble(timingData, "tcpConnection"));
                vitals.setServerResponse(getDouble(timingData, "serverResponse"));
                vitals.setDomProcessing(getDouble(timingData, "domProcessing"));
            }
            
            // Set Paint Timing metrics
            if (paintData != null) {
                vitals.setFcp(getDouble(paintData, "fcp"));
            }
            
            vitals.setUrl(driver.getCurrentUrl());
            vitals.setTimestamp(System.currentTimeMillis());
            
            return vitals;
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing Web Vitals: " + e.getMessage());
            return new WebVitals();
        }
    }
    
    /**
     * Capture Navigation Timing API metrics
     */
    private Map<String, Object> captureNavigationTiming(JavascriptExecutor js) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> timing = (Map<String, Object>) js.executeScript(
                "var t = performance.timing;" +
                "var nav = performance.navigation.type;" +
                "return {" +
                "  navigationStart: t.navigationStart," +
                "  domainLookupStart: t.domainLookupStart," +
                "  domainLookupEnd: t.domainLookupEnd," +
                "  connectStart: t.connectStart," +
                "  connectEnd: t.connectEnd," +
                "  requestStart: t.requestStart," +
                "  responseStart: t.responseStart," +
                "  responseEnd: t.responseEnd," +
                "  domContentLoadedEventStart: t.domContentLoadedEventStart," +
                "  domContentLoadedEventEnd: t.domContentLoadedEventEnd," +
                "  loadEventStart: t.loadEventStart," +
                "  loadEventEnd: t.loadEventEnd," +
                "  domInteractive: t.domInteractive" +
                "};"
            );
            
            if (timing == null) return null;
            
            // Calculate derived metrics
            Map<String, Object> metrics = new HashMap<>();
            long navStart = getLong(timing, "navigationStart");
            
            metrics.put("ttfb", getLong(timing, "responseStart") - navStart);
            metrics.put("domContentLoaded", getLong(timing, "domContentLoadedEventEnd") - navStart);
            metrics.put("fullPageLoad", getLong(timing, "loadEventEnd") - navStart);
            metrics.put("dnsLookup", getLong(timing, "domainLookupEnd") - getLong(timing, "domainLookupStart"));
            metrics.put("tcpConnection", getLong(timing, "connectEnd") - getLong(timing, "connectStart"));
            metrics.put("serverResponse", getLong(timing, "responseEnd") - getLong(timing, "requestStart"));
            metrics.put("domProcessing", getLong(timing, "domInteractive") - getLong(timing, "responseEnd"));
            
            return metrics;
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing Navigation Timing: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Capture Paint Timing API metrics (FCP)
     */
    private Map<String, Object> capturePaintTiming(JavascriptExecutor js) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paint = (Map<String, Object>) js.executeScript(
                "var paints = performance.getEntriesByType('paint');" +
                "var fcp = paints.find(function(p) { return p.name === 'first-contentful-paint'; });" +
                "return {" +
                "  fcp: fcp ? fcp.startTime : 0" +
                "};"
            );
            
            return paint;
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing Paint Timing: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Capture Largest Contentful Paint (LCP)
     */
    private Double captureLargestContentfulPaint(JavascriptExecutor js) {
        try {
            Object result = js.executeScript(
                "var lcpEntries = performance.getEntriesByType('largest-contentful-paint');" +
                "if (lcpEntries && lcpEntries.length > 0) {" +
                "  return lcpEntries[lcpEntries.length - 1].renderTime || lcpEntries[lcpEntries.length - 1].loadTime;" +
                "}" +
                "return null;"
            );
            
            if (result != null) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing LCP: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Capture Cumulative Layout Shift (CLS)
     */
    private Double captureLayoutShift(JavascriptExecutor js) {
        try {
            Object result = js.executeScript(
                "var clsEntries = performance.getEntriesByType('layout-shift');" +
                "var cls = 0;" +
                "if (clsEntries) {" +
                "  clsEntries.forEach(function(entry) {" +
                "    if (!entry.hadRecentInput) {" +
                "      cls += entry.value;" +
                "    }" +
                "  });" +
                "}" +
                "return cls;"
            );
            
            if (result != null) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            }
            
            return 0.0;
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing CLS: " + e.getMessage());
            return 0.0;
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
     * Helper to get double value from map
     */
    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * Web Vitals Data Model
     */
    public static class WebVitals {
        // Core Web Vitals
        private double lcp = 0.0;           // Largest Contentful Paint
        private double fid = 0.0;           // First Input Delay (not available in automation)
        private double cls = 0.0;           // Cumulative Layout Shift
        
        // Other Important Metrics
        private double fcp = 0.0;           // First Contentful Paint
        private double ttfb = 0.0;          // Time to First Byte
        private double domContentLoaded = 0.0;
        private double fullPageLoad = 0.0;
        
        // Detailed Timing
        private double dnsLookup = 0.0;
        private double tcpConnection = 0.0;
        private double serverResponse = 0.0;
        private double domProcessing = 0.0;
        
        // Metadata
        private String url;
        private long timestamp;
        
        // Getters and Setters
        public double getLcp() { return lcp; }
        public void setLcp(double lcp) { this.lcp = lcp; }
        
        public double getFid() { return fid; }
        public void setFid(double fid) { this.fid = fid; }
        
        public double getCls() { return cls; }
        public void setCls(double cls) { this.cls = cls; }
        
        public double getFcp() { return fcp; }
        public void setFcp(double fcp) { this.fcp = fcp; }
        
        public double getTtfb() { return ttfb; }
        public void setTtfb(double ttfb) { this.ttfb = ttfb; }
        
        public double getDomContentLoaded() { return domContentLoaded; }
        public void setDomContentLoaded(double domContentLoaded) { 
            this.domContentLoaded = domContentLoaded; 
        }
        
        public double getFullPageLoad() { return fullPageLoad; }
        public void setFullPageLoad(double fullPageLoad) { 
            this.fullPageLoad = fullPageLoad; 
        }
        
        public double getDnsLookup() { return dnsLookup; }
        public void setDnsLookup(double dnsLookup) { this.dnsLookup = dnsLookup; }
        
        public double getTcpConnection() { return tcpConnection; }
        public void setTcpConnection(double tcpConnection) { 
            this.tcpConnection = tcpConnection; 
        }
        
        public double getServerResponse() { return serverResponse; }
        public void setServerResponse(double serverResponse) { 
            this.serverResponse = serverResponse; 
        }
        
        public double getDomProcessing() { return domProcessing; }
        public void setDomProcessing(double domProcessing) { 
            this.domProcessing = domProcessing; 
        }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        /**
         * Get LCP rating (Good, Needs Improvement, Poor)
         */
        public String getLcpRating() {
            if (lcp < LCP_GOOD_THRESHOLD) return "Good";
            if (lcp < LCP_POOR_THRESHOLD) return "Needs Improvement";
            return "Poor";
        }
        
        /**
         * Get LCP emoji indicator
         */
        public String getLcpEmoji() {
            if (lcp < LCP_GOOD_THRESHOLD) return "✅";
            if (lcp < LCP_POOR_THRESHOLD) return "⚠️";
            return "❌";
        }
        
        /**
         * Get CLS rating
         */
        public String getClsRating() {
            if (cls < CLS_GOOD_THRESHOLD) return "Good";
            if (cls < CLS_POOR_THRESHOLD) return "Needs Improvement";
            return "Poor";
        }
        
        /**
         * Get CLS emoji indicator
         */
        public String getClsEmoji() {
            if (cls < CLS_GOOD_THRESHOLD) return "✅";
            if (cls < CLS_POOR_THRESHOLD) return "⚠️";
            return "❌";
        }
        
        /**
         * Get FCP rating
         */
        public String getFcpRating() {
            if (fcp < FCP_GOOD_THRESHOLD) return "Good";
            return "Needs Improvement";
        }
        
        /**
         * Get FCP emoji indicator
         */
        public String getFcpEmoji() {
            if (fcp < FCP_GOOD_THRESHOLD) return "✅";
            return "⚠️";
        }
        
        /**
         * Get TTFB rating
         */
        public String getTtfbRating() {
            if (ttfb < TTFB_GOOD_THRESHOLD) return "Good";
            return "Needs Improvement";
        }
        
        /**
         * Get TTFB emoji indicator
         */
        public String getTtfbEmoji() {
            if (ttfb < TTFB_GOOD_THRESHOLD) return "✅";
            return "⚠️";
        }
        
        /**
         * Check if any Core Web Vitals are in "Poor" state
         */
        public boolean hasPoorWebVitals() {
            return lcp >= LCP_POOR_THRESHOLD || 
                   cls >= CLS_POOR_THRESHOLD;
        }
        
        /**
         * Check if any Core Web Vitals need improvement
         */
        public boolean needsImprovement() {
            return (lcp >= LCP_GOOD_THRESHOLD && lcp < LCP_POOR_THRESHOLD) ||
                   (cls >= CLS_GOOD_THRESHOLD && cls < CLS_POOR_THRESHOLD);
        }
        
        /**
         * Get overall Web Vitals score (0-100)
         */
        public int getOverallScore() {
            int lcpScore = lcp < LCP_GOOD_THRESHOLD ? 100 : 
                          lcp < LCP_POOR_THRESHOLD ? 60 : 30;
            int clsScore = cls < CLS_GOOD_THRESHOLD ? 100 :
                          cls < CLS_POOR_THRESHOLD ? 60 : 30;
            int fcpScore = fcp < FCP_GOOD_THRESHOLD ? 100 : 60;
            int ttfbScore = ttfb < TTFB_GOOD_THRESHOLD ? 100 : 60;
            
            return (lcpScore + clsScore + fcpScore + ttfbScore) / 4;
        }
        
        /**
         * Get performance summary
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            
            sb.append("\n").append("=".repeat(80)).append("\n");
            sb.append("🎯 WEB VITALS SUMMARY\n");
            sb.append("=".repeat(80)).append("\n");
            sb.append("URL: ").append(url).append("\n");
            sb.append("Overall Score: ").append(getOverallScore()).append("/100\n");
            sb.append("-".repeat(80)).append("\n");
            
            sb.append("\n📊 CORE WEB VITALS (Google Standards):\n");
            sb.append(String.format("   %s LCP (Largest Contentful Paint): %.2fms [%s]\n", 
                getLcpEmoji(), lcp, getLcpRating()));
            sb.append(String.format("   %s CLS (Cumulative Layout Shift): %.3f [%s]\n", 
                getClsEmoji(), cls, getClsRating()));
            
            sb.append("\n⚡ OTHER KEY METRICS:\n");
            sb.append(String.format("   %s FCP (First Contentful Paint): %.2fms [%s]\n", 
                getFcpEmoji(), fcp, getFcpRating()));
            sb.append(String.format("   %s TTFB (Time to First Byte): %.2fms [%s]\n", 
                getTtfbEmoji(), ttfb, getTtfbRating()));
            sb.append(String.format("   ℹ️  DOM Content Loaded: %.2fms\n", domContentLoaded));
            sb.append(String.format("   ℹ️  Full Page Load: %.2fms\n", fullPageLoad));
            
            sb.append("\n🔍 DETAILED TIMING:\n");
            sb.append(String.format("   DNS Lookup: %.2fms\n", dnsLookup));
            sb.append(String.format("   TCP Connection: %.2fms\n", tcpConnection));
            sb.append(String.format("   Server Response: %.2fms\n", serverResponse));
            sb.append(String.format("   DOM Processing: %.2fms\n", domProcessing));
            
            if (hasPoorWebVitals()) {
                sb.append("\n❌ VERDICT: Poor - Immediate optimization needed!\n");
            } else if (needsImprovement()) {
                sb.append("\n⚠️  VERDICT: Needs Improvement - Consider optimizations\n");
            } else {
                sb.append("\n✅ VERDICT: Good - Meeting Google's standards\n");
            }
            
            sb.append("=".repeat(80)).append("\n");
            
            return sb.toString();
        }
        
        /**
         * Get compact summary for defect reports
         */
        public String getCompactSummary() {
            return String.format(
                "Web Vitals - LCP: %.0fms %s | CLS: %.3f %s | FCP: %.0fms %s | TTFB: %.0fms %s | Score: %d/100",
                lcp, getLcpEmoji(), 
                cls, getClsEmoji(),
                fcp, getFcpEmoji(),
                ttfb, getTtfbEmoji(),
                getOverallScore()
            );
        }
    }
}

