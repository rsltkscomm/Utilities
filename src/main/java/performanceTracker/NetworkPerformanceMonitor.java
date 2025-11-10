package performanceTracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Network Performance Monitor
 * Captures and analyzes all background API calls and their response times
 * Uses browser's Performance API (Resource Timing API)
 */
public class NetworkPerformanceMonitor {
    
    private final WebDriver driver;
    private final Gson gson;
    private List<NetworkRequest> capturedRequests;
    private long captureStartTime;
    
    public NetworkPerformanceMonitor(WebDriver driver) {
        this.driver = driver;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.capturedRequests = new ArrayList<>();
        this.captureStartTime = System.currentTimeMillis();
    }
    
    /**
     * Capture all network requests using Performance API
     */
    public void captureNetworkRequests() {
        if (driver == null) {
            System.err.println("⚠️  WebDriver not available for network capture");
            return;
        }
        
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Use Performance API to get all resources
            String script = 
                "var perfEntries = window.performance.getEntriesByType('resource');" +
                "var results = [];" +
                "perfEntries.forEach(function(entry) {" +
                "  results.push({" +
                "    name: entry.name," +
                "    initiatorType: entry.initiatorType," +
                "    duration: Math.round(entry.duration)," +
                "    startTime: Math.round(entry.startTime)," +
                "    responseEnd: Math.round(entry.responseEnd)," +
                "    transferSize: entry.transferSize || 0," +
                "    encodedBodySize: entry.encodedBodySize || 0," +
                "    decodedBodySize: entry.decodedBodySize || 0" +
                "  });" +
                "});" +
                "return JSON.stringify(results);";
            
            Object result = js.executeScript(script);
            
            if (result != null) {
                String jsonResult = result.toString();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> rawRequests = gson.fromJson(jsonResult, listType);
                
                // Convert to NetworkRequest objects
                for (Map<String, Object> raw : rawRequests) {
                    NetworkRequest request = new NetworkRequest(raw);
                    
                    // Filter for API calls (XHR, fetch, or contains /api/)
                    if (request.isApiCall()) {
                        capturedRequests.add(request);
                    }
                }
                
                System.out.println("✅ Network requests captured: " + capturedRequests.size() + " API calls");
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing network requests: " + e.getMessage());
        }
    }
    
    /**
     * Get all captured API requests
     */
    public List<NetworkRequest> getApiRequests() {
        return new ArrayList<>(capturedRequests);
    }
    
    /**
     * Get slow API calls (above threshold)
     */
    public List<NetworkRequest> getSlowApiCalls(int thresholdMs) {
        return capturedRequests.stream()
                .filter(req -> req.getDuration() > thresholdMs)
                .sorted(Comparator.comparingLong(NetworkRequest::getDuration).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get API calls sorted by response time (slowest first)
     */
    public List<NetworkRequest> getApiCallsSortedByDuration() {
        return capturedRequests.stream()
                .sorted(Comparator.comparingLong(NetworkRequest::getDuration).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get summary statistics
     */
    public NetworkSummary getSummary() {
        if (capturedRequests.isEmpty()) {
            return new NetworkSummary();
        }
        
        long totalDuration = capturedRequests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .sum();
        
        double avgDuration = capturedRequests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .average()
                .orElse(0);
        
        long maxDuration = capturedRequests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .max()
                .orElse(0);
        
        long minDuration = capturedRequests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .min()
                .orElse(0);
        
        long totalSize = capturedRequests.stream()
                .mapToLong(NetworkRequest::getTransferSize)
                .sum();
        
        // Calculate percentiles for performance distribution analysis
        List<Long> sortedDurations = capturedRequests.stream()
                .map(NetworkRequest::getDuration)
                .sorted()
                .collect(Collectors.toList());
        
        double p50 = calculatePercentile(sortedDurations, 50);
        double p75 = calculatePercentile(sortedDurations, 75);
        double p90 = calculatePercentile(sortedDurations, 90);
        double p95 = calculatePercentile(sortedDurations, 95);
        double p99 = calculatePercentile(sortedDurations, 99);
        
        return new NetworkSummary(
            capturedRequests.size(),
            totalDuration,
            avgDuration,
            minDuration,
            maxDuration,
            totalSize,
            p50,
            p75,
            p90,
            p95,
            p99
        );
    }
    
    /**
     * Print network performance summary
     */
    public void printSummary() {
        NetworkSummary summary = getSummary();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🌐 NETWORK PERFORMANCE SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Total API Calls: " + summary.getTotalRequests());
        System.out.println("Total Response Time: " + summary.getTotalDuration() + " ms");
        System.out.println("Average Response Time: " + String.format("%.2f", summary.getAvgDuration()) + " ms");
        System.out.println("Min Response Time: " + summary.getMinDuration() + " ms");
        System.out.println("Max Response Time: " + summary.getMaxDuration() + " ms");
        System.out.println("Total Data Transferred: " + formatBytes(summary.getTotalSize()));
        
        // Display percentiles if we have enough data
        if (summary.getTotalRequests() >= 5) {
            System.out.println("\n📊 RESPONSE TIME PERCENTILES:");
            System.out.println("   p50 (Median):        " + String.format("%.0f", summary.getP50()) + " ms");
            System.out.println("   p75 (75th %ile):     " + String.format("%.0f", summary.getP75()) + " ms");
            System.out.println("   p90 (90th %ile):     " + String.format("%.0f", summary.getP90()) + " ms");
            System.out.println("   p95 (95th %ile):     " + String.format("%.0f", summary.getP95()) + " ms");
            System.out.println("   p99 (99th %ile):     " + String.format("%.0f", summary.getP99()) + " ms");
            System.out.println("   Consistency:         " + summary.getConsistencyRating());
            
            if (summary.hasSignificantTailLatency()) {
                System.out.println("   ⚠️  WARNING: Significant tail latency detected (p99 >> p95)");
                System.out.println("      Some requests are taking much longer than typical");
            }
        }
        
        System.out.println("=".repeat(80));
        
        if (!capturedRequests.isEmpty()) {
            System.out.println("\n🐌 Top 5 Slowest API Calls:");
            List<NetworkRequest> slowest = getApiCallsSortedByDuration();
            for (int i = 0; i < Math.min(5, slowest.size()); i++) {
                NetworkRequest req = slowest.get(i);
                System.out.println("   " + (i+1) + ". " + req.getEndpoint() + " - " + req.getDuration() + " ms");
            }
        }
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Network Request Model
     */
    public static class NetworkRequest {
        private String url;
        private String endpoint;
        private String initiatorType;
        private long duration;
        private long startTime;
        private long responseEnd;
        private long transferSize;
        private long encodedBodySize;
        private long decodedBodySize;
        
        public NetworkRequest(Map<String, Object> data) {
            this.url = (String) data.get("name");
            this.endpoint = extractEndpoint(this.url);
            this.initiatorType = (String) data.get("initiatorType");
            this.duration = getLongValue(data.get("duration"));
            this.startTime = getLongValue(data.get("startTime"));
            this.responseEnd = getLongValue(data.get("responseEnd"));
            this.transferSize = getLongValue(data.get("transferSize"));
            this.encodedBodySize = getLongValue(data.get("encodedBodySize"));
            this.decodedBodySize = getLongValue(data.get("decodedBodySize"));
        }
        
        private long getLongValue(Object value) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return 0;
        }
        
        private String extractEndpoint(String fullUrl) {
            try {
                // Extract path from URL
                int schemeEnd = fullUrl.indexOf("://");
                if (schemeEnd == -1) return fullUrl;
                
                int pathStart = fullUrl.indexOf("/", schemeEnd + 3);
                if (pathStart == -1) return fullUrl;
                
                String path = fullUrl.substring(pathStart);
                
                // Remove query parameters for cleaner display
                int queryStart = path.indexOf("?");
                if (queryStart != -1) {
                    path = path.substring(0, queryStart);
                }
                
                return path;
            } catch (Exception e) {
                return fullUrl;
            }
        }
        
        public boolean isApiCall() {
            // Consider as API call if:
            // 1. XHR or fetch type
            // 2. URL contains /api/
            // 3. URL contains common API patterns
            String lowerUrl = url.toLowerCase();
            return initiatorType.equals("xmlhttprequest") ||
                   initiatorType.equals("fetch") ||
                   lowerUrl.contains("/api/") ||
                   lowerUrl.contains("/rest/") ||
                   lowerUrl.contains("/graphql") ||
                   (lowerUrl.contains(".json") && !lowerUrl.contains(".js"));
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public String getInitiatorType() {
            return initiatorType;
        }
        
        public long getDuration() {
            return duration;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getResponseEnd() {
            return responseEnd;
        }
        
        public long getTransferSize() {
            return transferSize;
        }
        
        public long getEncodedBodySize() {
            return encodedBodySize;
        }
        
        public long getDecodedBodySize() {
            return decodedBodySize;
        }
        
        public String getPerformanceStatus() {
            if (duration < 100) return "✅ Excellent";
            if (duration < 500) return "✅ Good";
            if (duration < 1000) return "⚠️ Average";
            if (duration < 3000) return "⚠️ Slow";
            return "❌ Very Slow";
        }
        
        public String getStatusClass() {
            if (duration < 500) return "excellent";
            if (duration < 1000) return "good";
            if (duration < 3000) return "average";
            return "slow";
        }
        
        /**
         * Get clean resource type for visualization
         */
        public String getType() {
            if (initiatorType == null) {
                return "other";
            }
            
            switch (initiatorType.toLowerCase()) {
                case "xmlhttprequest":
                case "fetch":
                    return "xhr";
                case "script":
                    return "script";
                case "css":
                case "link":
                    return "css";
                case "img":
                    return "img";
                case "other":
                    // Check URL for type hints
                    if (url != null) {
                        String lowerUrl = url.toLowerCase();
                        if (lowerUrl.endsWith(".js")) return "script";
                        if (lowerUrl.endsWith(".css")) return "css";
                        if (lowerUrl.matches(".*\\.(png|jpg|jpeg|gif|svg|webp)")) return "img";
                        if (lowerUrl.matches(".*\\.(woff|woff2|ttf|eot)")) return "font";
                    }
                    return "other";
                default:
                    return initiatorType.toLowerCase();
            }
        }
    }
    
    /**
     * Network Summary Model
     */
    public static class NetworkSummary {
        private int totalRequests;
        private long totalDuration;
        private double avgDuration;
        private long minDuration;
        private long maxDuration;
        private long totalSize;
        
        // Percentiles for performance distribution analysis
        private double p50;  // Median - 50% of requests complete faster than this
        private double p75;  // 75th percentile
        private double p90;  // 90th percentile - Key metric for SLA compliance
        private double p95;  // 95th percentile - Common SLA threshold
        private double p99;  // 99th percentile - Tail latency
        
        public NetworkSummary() {
            this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        
        public NetworkSummary(int totalRequests, long totalDuration, double avgDuration,
                            long minDuration, long maxDuration, long totalSize,
                            double p50, double p75, double p90, double p95, double p99) {
            this.totalRequests = totalRequests;
            this.totalDuration = totalDuration;
            this.avgDuration = avgDuration;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
            this.totalSize = totalSize;
            this.p50 = p50;
            this.p75 = p75;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
        }
        
        public int getTotalRequests() { return totalRequests; }
        public long getTotalDuration() { return totalDuration; }
        public double getAvgDuration() { return avgDuration; }
        public long getMinDuration() { return minDuration; }
        public long getMaxDuration() { return maxDuration; }
        public long getTotalSize() { return totalSize; }
        
        // Percentile getters
        public double getP50() { return p50; }
        public double getP75() { return p75; }
        public double getP90() { return p90; }
        public double getP95() { return p95; }
        public double getP99() { return p99; }
        
        /**
         * Get formatted percentile summary
         */
        public String getPercentilesFormatted() {
            return String.format("p50: %.0fms | p75: %.0fms | p90: %.0fms | p95: %.0fms | p99: %.0fms",
                p50, p75, p90, p95, p99);
        }
        
        /**
         * Check if there's significant tail latency (p99 is much higher than p95)
         */
        public boolean hasSignificantTailLatency() {
            if (p95 == 0) return false;
            return (p99 / p95) > 2.0;  // p99 is more than 2x the p95
        }
        
        /**
         * Get performance consistency rating
         */
        public String getConsistencyRating() {
            if (totalRequests < 5) return "Insufficient data";
            
            double variance = maxDuration - minDuration;
            double range = avgDuration > 0 ? (variance / avgDuration) : 0;
            
            if (range < 0.5) return "Excellent (Very consistent)";
            if (range < 1.0) return "Good (Consistent)";
            if (range < 2.0) return "Fair (Some variation)";
            return "Poor (High variation)";
        }
    }
    
    /**
     * Calculate percentile from sorted list of values
     */
    private static double calculatePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }
        
        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);
        
        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }
        
        double lowerValue = sortedValues.get(lowerIndex);
        double upperValue = sortedValues.get(upperIndex);
        double fraction = index - lowerIndex;
        
        return lowerValue + (upperValue - lowerValue) * fraction;
    }
}

