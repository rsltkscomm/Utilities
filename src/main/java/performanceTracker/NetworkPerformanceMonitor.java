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
    // Group captured requests by logical page name
    private final Map<String, List<NetworkRequest>> capturedRequestsByPage;
    private long captureStartTime;

    public NetworkPerformanceMonitor(WebDriver driver) {
        this.driver = driver;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.capturedRequestsByPage = new LinkedHashMap<>();
        this.captureStartTime = System.currentTimeMillis();
    }

    /**
     * Capture all network requests using Performance API and associate them with the provided page name.
     *
     * Behavior:
     *  - For the given page key, any existing list is REPLACED (not appended).
     *  - After capturing, performance.clearResourceTimings() is called so the next capture
     *    only sees new entries.
     *
     * @param page logical page name to group captured requests under (e.g., "login", "dashboard")
     */
    public void captureNetworkRequests(String page) {
        if (driver == null) {
            System.err.println("⚠️  WebDriver not available for network capture");
            return;
        }
        if (page == null || page.trim().isEmpty()) {
            page = "unknown";
        }

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Use Performance API to get all resources, then clear timings
            String script =
                "try {" +
                "  var perfEntries = window.performance.getEntriesByType('resource');" +
                "  var results = [];" +
                "  perfEntries.forEach(function(entry) {" +
                "    results.push({" +
                "      name: entry.name," +
                "      initiatorType: entry.initiatorType," +
                "      duration: Math.round(entry.duration || 0)," +
                "      startTime: Math.round(entry.startTime || 0)," +
                "      responseEnd: Math.round(entry.responseEnd || 0)," +
                "      transferSize: entry.transferSize || 0," +
                "      encodedBodySize: entry.encodedBodySize || 0," +
                "      decodedBodySize: entry.decodedBodySize || 0" +
                "    });" +
                "  });" +
                "  if (window.performance.clearResourceTimings) {" +
                "    window.performance.clearResourceTimings();" +
                "  }" +
                "  return JSON.stringify(results);" +
                "} catch (e) {" +
                "  return JSON.stringify([]);" +
                "}";

            Object result = js.executeScript(script);

            if (result != null) {
                String jsonResult = result.toString();
                Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                List<Map<String, Object>> rawRequests = gson.fromJson(jsonResult, listType);

                // Fresh list for THIS capture of THIS page (overwrite any previous one for same key)
                List<NetworkRequest> pageList = new ArrayList<>();
                capturedRequestsByPage.put(page, pageList);

                // Convert to NetworkRequest objects and store API calls for this page
                for (Map<String, Object> raw : rawRequests) {
                    NetworkRequest request = new NetworkRequest(raw);

                    // Filter for API calls (XHR, fetch, or contains /api/)
                    if (request.isApiCall()) {
                        pageList.add(request);
                    }
                }

                System.out.println("✅ Network requests captured for page '" + page + "': "
                        + pageList.size() + " API calls (latest capture for that page)");
            }

        } catch (Exception e) {
            System.err.println("⚠️  Error capturing network requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all captured API requests across all pages (concatenated).
     */
    public List<NetworkRequest> getApiRequests() {
        List<NetworkRequest> all = new ArrayList<>();
        for (List<NetworkRequest> list : capturedRequestsByPage.values()) {
            all.addAll(list);
        }
        return new ArrayList<>(all); // defensive copy
    }

    /**
     * Get captured API requests for a specific page name.
     *
     * @param page logical page name used in captureNetworkRequests
     * @return list (copy) of NetworkRequest for that page, or empty list if none
     */
    public List<NetworkRequest> getApiRequestsForPage(String page) {
        if (page == null) return Collections.emptyList();
        List<NetworkRequest> list = capturedRequestsByPage.get(page);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    /**
     * Get slow API calls (above threshold) across all pages
     */
    public List<NetworkRequest> getSlowApiCalls(int thresholdMs) {
        return getApiRequests().stream()
                .filter(req -> req.getDuration() > thresholdMs)
                .sorted(Comparator.comparingLong(NetworkRequest::getDuration).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get slow API calls for a specific page (above threshold)
     */
    public List<NetworkRequest> getSlowApiCallsForPage(String page, int thresholdMs) {
        return getApiRequestsForPage(page).stream()
                .filter(req -> req.getDuration() > thresholdMs)
                .sorted(Comparator.comparingLong(NetworkRequest::getDuration).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get API calls sorted by response time (slowest first) across all pages
     */
    public List<NetworkRequest> getApiCallsSortedByDuration() {
        return getApiRequests().stream()
                .sorted(Comparator.comparingLong(NetworkRequest::getDuration).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get API calls sorted by response time for a specific page (slowest first)
     */
    public List<NetworkRequest> getApiCallsSortedByDurationForPage(String page) {
        return getApiRequestsForPage(page).stream()
                .sorted(Comparator.comparingLong(NetworkRequest::getDuration).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get summary statistics across all pages
     */
    public NetworkSummary getSummary() {
        return computeSummary(getApiRequests());
    }

    /**
     * Get summary statistics for a specific page
     */
    public NetworkSummary getSummaryForPage(String page) {
        return computeSummary(getApiRequestsForPage(page));
    }

    /**
     * Print network performance summary (all pages aggregated)
     */
    public void printSummary() {
        NetworkSummary summary = getSummary();
        printSummaryInternal("ALL_PAGES", summary, getApiCallsSortedByDuration());
    }

    /**
     * Print network performance summary for a specific page
     */
    public void printSummaryForPage(String page) {
        NetworkSummary summary = getSummaryForPage(page);
        List<NetworkRequest> slowest = getApiCallsSortedByDurationForPage(page);
        printSummaryInternal(page == null ? "unknown" : page, summary, slowest);
    }

    /**
     * Internal summary printing helper
     */
    private void printSummaryInternal(String title, NetworkSummary summary, List<NetworkRequest> sortedByDuration) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🌐 NETWORK PERFORMANCE SUMMARY - " + title);
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

        if (!sortedByDuration.isEmpty()) {
            System.out.println("\n🐌 Top 5 Slowest API Calls:");
            for (int i = 0; i < Math.min(5, sortedByDuration.size()); i++) {
                NetworkRequest req = sortedByDuration.get(i);
                System.out.println("   " + (i + 1) + ". " + req.getEndpoint() + " - " + req.getDuration() + " ms");
            }
        }
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Compute summary from a list of NetworkRequest
     */
    private NetworkSummary computeSummary(List<NetworkRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new NetworkSummary();
        }

        long totalDuration = requests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .sum();

        double avgDuration = requests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .average()
                .orElse(0);

        long maxDuration = requests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .max()
                .orElse(0);

        long minDuration = requests.stream()
                .mapToLong(NetworkRequest::getDuration)
                .min()
                .orElse(0);

        long totalSize = requests.stream()
                .mapToLong(NetworkRequest::getTransferSize)
                .sum();

        // Calculate percentiles for performance distribution analysis
        List<Long> sortedDurations = requests.stream()
                .map(NetworkRequest::getDuration)
                .sorted()
                .collect(Collectors.toList());

        double p50 = calculatePercentile(sortedDurations, 50);
        double p75 = calculatePercentile(sortedDurations, 75);
        double p90 = calculatePercentile(sortedDurations, 90);
        double p95 = calculatePercentile(sortedDurations, 95);
        double p99 = calculatePercentile(sortedDurations, 99);

        return new NetworkSummary(
                requests.size(),
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
            this.url = safeGetString(data.get("name"));
            this.endpoint = extractEndpoint(this.url);
            this.initiatorType = safeGetString(data.get("initiatorType"));
            this.duration = getLongValue(data.get("duration"));
            this.startTime = getLongValue(data.get("startTime"));
            this.responseEnd = getLongValue(data.get("responseEnd"));
            this.transferSize = getLongValue(data.get("transferSize"));
            this.encodedBodySize = getLongValue(data.get("encodedBodySize"));
            this.decodedBodySize = getLongValue(data.get("decodedBodySize"));
        }

        private String safeGetString(Object o) {
            return o == null ? "" : o.toString();
        }

        private long getLongValue(Object value) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException ignored) {
                }
            }
            return 0;
        }

        private String extractEndpoint(String fullUrl) {
            if (fullUrl == null || fullUrl.isEmpty()) return "";
            try {
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
            String lowerUrl = url == null ? "" : url.toLowerCase();
            String initiator = initiatorType == null ? "" : initiatorType.toLowerCase();

            return initiator.equals("xmlhttprequest") ||
                   initiator.equals("fetch") ||
                   lowerUrl.contains("/api/") ||
                   lowerUrl.contains("/rest/") ||
                   lowerUrl.contains("/graphql") ||
                   (lowerUrl.contains(".json") && !lowerUrl.contains(".js"));
        }

        public String getUrl() { return url; }
        public String getEndpoint() { return endpoint; }
        public String getInitiatorType() { return initiatorType; }
        public long getDuration() { return duration; }
        public long getStartTime() { return startTime; }
        public long getResponseEnd() { return responseEnd; }
        public long getTransferSize() { return transferSize; }
        public long getEncodedBodySize() { return encodedBodySize; }
        public long getDecodedBodySize() { return decodedBodySize; }

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
        private double p50;
        private double p75;
        private double p90;
        private double p95;
        private double p99;

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

        public double getP50() { return p50; }
        public double getP75() { return p75; }
        public double getP90() { return p90; }
        public double getP95() { return p95; }
        public double getP99() { return p99; }

        public String getPercentilesFormatted() {
            return String.format(
                "p50: %.0fms | p75: %.0fms | p90: %.0fms | p95: %.0fms | p99: %.0fms",
                p50, p75, p90, p95, p99
            );
        }

        public boolean hasSignificantTailLatency() {
            if (p95 == 0) return false;
            return (p99 / p95) > 2.0;
        }

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
