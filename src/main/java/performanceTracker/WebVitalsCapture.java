package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import base.DriverManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Web Vitals Capture - Industry-Standard Performance Metrics
 * (Updated: consolidated JS snapshot to reduce stale/identical metric captures)
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
     * Capture all Web Vitals metrics in a single JS snapshot.
     *
     * Note: best accuracy for LCP/CLS is achieved when a PerformanceObserver runs while page loads.
     * This method captures what's available at the moment of invocation as a consistent snapshot.
     */
    public WebVitals captureWebVitals() {
        if (!(driver instanceof JavascriptExecutor)) {
            System.err.println("⚠️  WebDriver does not support JavaScript execution");
            return new WebVitals();
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // Consolidated JS that returns a map/object with all metrics
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) js.executeScript(
                "try {\n" +
                "  var t = performance.timing || {};\n" +
                "  var navStart = t.navigationStart || 0;\n" +
                "\n" +
                "  // Paint timings (FCP)\n" +
                "  var paints = performance.getEntriesByType && performance.getEntriesByType('paint') || [];\n" +
                "  var fcp = 0;\n" +
                "  for (var i = 0; i < paints.length; i++) {\n" +
                "    if (paints[i].name === 'first-contentful-paint') {\n" +
                "      fcp = Math.round(paints[i].startTime || 0);\n                }\n                }\n" +
                "\n" +
                "  // Largest Contentful Paint (last entry if present)\n" +
                "  var lcpEntries = performance.getEntriesByType ? performance.getEntriesByType('largest-contentful-paint') : [];\n" +
                "  var lcp = null;\n" +
                "  if (lcpEntries && lcpEntries.length > 0) {\n" +
                "    var e = lcpEntries[lcpEntries.length - 1];\n" +
                "    lcp = Math.round(e.renderTime || e.loadTime || e.startTime || 0);\n" +
                "  }\n" +
                "\n" +
                "  // CLS: sum layout-shift entries (ignore hadRecentInput)\n" +
                "  var clsEntries = performance.getEntriesByType ? performance.getEntriesByType('layout-shift') : [];\n" +
                "  var cls = 0;\n" +
                "  for (var j = 0; j < clsEntries.length; j++) {\n" +
                "    try {\n" +
                "      var ent = clsEntries[j];\n" +
                "      if (!ent.hadRecentInput) {\n" +
                "        cls += (ent.value || 0);\n" +
                "      }\n" +
                "    } catch (err) { /* ignore entry errors */ }\n" +
                "  }\n" +
                "\n" +
                "  // Navigation timing derived metrics (defensive fallback to zero)\n" +
                "  var responseStart = t.responseStart || 0;\n" +
                "  var responseEnd = t.responseEnd || 0;\n" +
                "  var requestStart = t.requestStart || 0;\n" +
                "  var domainLookupStart = t.domainLookupStart || 0;\n" +
                "  var domainLookupEnd = t.domainLookupEnd || 0;\n" +
                "  var connectStart = t.connectStart || 0;\n" +
                "  var connectEnd = t.connectEnd || 0;\n" +
                "  var domContentLoadedEventEnd = t.domContentLoadedEventEnd || 0;\n" +
                "  var loadEventEnd = t.loadEventEnd || 0;\n" +
                "  var domInteractive = t.domInteractive || 0;\n" +
                "\n" +
                "  var ttfb = navStart ? Math.max(0, responseStart - navStart) : 0;\n" +
                "  var domContentLoaded = navStart ? Math.max(0, domContentLoadedEventEnd - navStart) : 0;\n" +
                "  var fullPageLoad = navStart ? Math.max(0, loadEventEnd - navStart) : 0;\n" +
                "  var dnsLookup = Math.max(0, domainLookupEnd - domainLookupStart);\n" +
                "  var tcpConnection = Math.max(0, connectEnd - connectStart);\n" +
                "  var serverResponse = Math.max(0, responseEnd - requestStart);\n" +
                "  var domProcessing = Math.max(0, domInteractive - responseEnd);\n" +
                "\n" +
                "  return {\n" +
                "    lcp: lcp === null ? null : lcp,\n" +
                "    cls: Math.round(cls * 1000) / 1000,\n" +
                "    fcp: fcp || 0,\n" +
                "    ttfb: ttfb || 0,\n" +
                "    domContentLoaded: domContentLoaded || 0,\n" +
                "    fullPageLoad: fullPageLoad || 0,\n" +
                "    dnsLookup: dnsLookup || 0,\n" +
                "    tcpConnection: tcpConnection || 0,\n" +
                "    serverResponse: serverResponse || 0,\n" +
                "    domProcessing: domProcessing || 0\n" +
                "  };\n" +
                "} catch (e) { return { lcp: null, cls: 0, fcp: 0, ttfb: 0, domContentLoaded:0, fullPageLoad:0, dnsLookup:0, tcpConnection:0, serverResponse:0, domProcessing:0 }; }"
            );

            WebVitals vitals = new WebVitals();

            if (raw != null) {
                // LCP: can be null if not available yet
                Object lcpObj = raw.get("lcp");
                if (lcpObj instanceof Number) {
                    vitals.setLcp(((Number) lcpObj).doubleValue());
                } else {
                    // leave at default 0.0 to make it explicit when not available
                    vitals.setLcp(0.0);
                }

                Object clsObj = raw.get("cls");
                vitals.setCls(asDouble(clsObj));

                Object fcpObj = raw.get("fcp");
                vitals.setFcp(asDouble(fcpObj));

                Object ttfbObj = raw.get("ttfb");
                vitals.setTtfb(asDouble(ttfbObj));

                Object domContentLoadedObj = raw.get("domContentLoaded");
                vitals.setDomContentLoaded(asDouble(domContentLoadedObj));

                Object fullPageLoadObj = raw.get("fullPageLoad");
                vitals.setFullPageLoad(asDouble(fullPageLoadObj));

                Object dnsObj = raw.get("dnsLookup");
                vitals.setDnsLookup(asDouble(dnsObj));

                Object tcpObj = raw.get("tcpConnection");
                vitals.setTcpConnection(asDouble(tcpObj));

                Object serverRespObj = raw.get("serverResponse");
                vitals.setServerResponse(asDouble(serverRespObj));

                Object domProcObj = raw.get("domProcessing");
                vitals.setDomProcessing(asDouble(domProcObj));
            }

            // metadata
            try {
                vitals.setUrl(driver.getCurrentUrl());
            } catch (Exception ex) {
                vitals.setUrl(null);
            }
            vitals.setTimestamp(System.currentTimeMillis());

            return vitals;

        } catch (Exception e) {
            System.err.println("⚠️  Error capturing Web Vitals: " + e.getMessage());
            return new WebVitals();
        }
    }

    // helper to handle Number -> double conversion defensively
    private double asDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ----------------------
    // Web Vitals Data Model
    // (unchanged from your original; left intact)
    // ----------------------
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

        // Rating helpers (unchanged)
        public String getLcpRating() {
            if (lcp < LCP_GOOD_THRESHOLD) return "Good";
            if (lcp < LCP_POOR_THRESHOLD) return "Needs Improvement";
            return "Poor";
        }
        public String getLcpEmoji() {
            if (lcp < LCP_GOOD_THRESHOLD) return "✅";
            if (lcp < LCP_POOR_THRESHOLD) return "⚠️";
            return "❌";
        }
        public String getClsRating() {
            if (cls < CLS_GOOD_THRESHOLD) return "Good";
            if (cls < CLS_POOR_THRESHOLD) return "Needs Improvement";
            return "Poor";
        }
        public String getClsEmoji() {
            if (cls < CLS_GOOD_THRESHOLD) return "✅";
            if (cls < CLS_POOR_THRESHOLD) return "⚠️";
            return "❌";
        }
        public String getFcpRating() {
            if (fcp < FCP_GOOD_THRESHOLD) return "Good";
            return "Needs Improvement";
        }
        public String getFcpEmoji() {
            if (fcp < FCP_GOOD_THRESHOLD) return "✅";
            return "⚠️";
        }
        public String getTtfbRating() {
            if (ttfb < TTFB_GOOD_THRESHOLD) return "Good";
            return "Needs Improvement";
        }
        public String getTtfbEmoji() {
            if (ttfb < TTFB_GOOD_THRESHOLD) return "✅";
            return "⚠️";
        }

        public boolean hasPoorWebVitals() {
            return lcp >= LCP_POOR_THRESHOLD ||
                   cls >= CLS_POOR_THRESHOLD;
        }

        public boolean needsImprovement() {
            return (lcp >= LCP_GOOD_THRESHOLD && lcp < LCP_POOR_THRESHOLD) ||
                   (cls >= CLS_GOOD_THRESHOLD && cls < CLS_POOR_THRESHOLD);
        }

        public int getOverallScore() {
            int lcpScore = lcp < LCP_GOOD_THRESHOLD ? 100 :
                          lcp < LCP_POOR_THRESHOLD ? 60 : 30;
            int clsScore = cls < CLS_GOOD_THRESHOLD ? 100 :
                          cls < CLS_POOR_THRESHOLD ? 60 : 30;
            int fcpScore = fcp < FCP_GOOD_THRESHOLD ? 100 : 60;
            int ttfbScore = ttfb < TTFB_GOOD_THRESHOLD ? 100 : 60;

            return (lcpScore + clsScore + fcpScore + ttfbScore) / 4;
        }

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
