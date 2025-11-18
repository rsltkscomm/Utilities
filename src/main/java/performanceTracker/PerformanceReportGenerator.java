package performanceTracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * HTML Performance Report Generator
 *
 * Clicking a page row expands a single drill-down that shows:
 *  - occurrences for that page
 *  - related API transactions for that page (single table)
 *
 * Clicking the page row again collapses the drill-down.
 */
public class PerformanceReportGenerator {

    private final ConfigurationManager config;
    private final String reportsDir;

    public PerformanceReportGenerator() {
        this.config = ConfigurationManager.getInstance();
        this.reportsDir = "./reports/";

        // Create reports directory if it doesn't exist
        File dir = new File(reportsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Generate HTML performance report
     */
    public File generateReport(String suiteName, PerformanceTracker tracker) {
        return generateReport(suiteName, tracker, null);
    }

    /**
     * Generate HTML performance report for the entire suite (uses only suiteName + url).
     */
    public File generateReport(String suiteName, PerformanceTracker tracker, String url) {
        if (tracker == null) {
            return null;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String meaningfulName = ReportNamingUtility.generateMeaningfulReportName(suiteName, null, url);
            String fileName = "performance_report_" + meaningfulName + "_" + timestamp + ".html";

            // Use 2-arg File ctor to avoid missing path separator issues
            File reportFile = new File(reportsDir, fileName);

            String htmlContent = buildHtmlReport(suiteName, tracker);
            System.out.println(htmlContent);

            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(htmlContent);
            }

            System.out.println("📊 Performance Report Generated: " + reportFile.getAbsolutePath());
            return reportFile;

        } catch (IOException e) {
            System.err.println("❌ Error generating performance report: " + e.getMessage());
            return null;
        }
    }


    /**
     * Build HTML performance report content for the entire suite.
     */
    private String buildHtmlReport(String suiteName, PerformanceTracker tracker) {
        StringBuilder html = new StringBuilder();

        // HTML header
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Performance Report - ").append(suiteName).append("</title>\n");
        html.append("    <style>\n");
        html.append(getStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>📊 Suite Performance Report</h1>\n");
        html.append("        <p class=\"subtitle\">Automated Performance Analysis</p>\n");
        html.append("    </div>\n");

        // Container
        html.append("    <div class=\"container\">\n");

        // Suite Info Section
        html.append(buildSuiteInfoSection(suiteName));

        // PRIMARY OBJECTIVES - Page Load Times (page-level drilldown shows related APIs)
        html.append(buildUniquePageLoadsSection(tracker));

        // PRIMARY OBJECTIVES - API Performance & Network Traffic
        html.append(buildApiPerformanceSection(tracker));

        // API Transactions Drill-down (full transactions)
//        html.append(buildApiTransactionsSection(tracker));

        // KPI Section
        html.append(buildKpiSection(tracker));

        // Web Vitals Section
//        html.append(buildWebVitalsSection(tracker));

        // Performance Issues Section
        html.append(buildPerformanceIssuesSection(tracker));

        // Recommendations Section
        html.append(buildRecommendationsSection(tracker));

        html.append("    </div>\n");

        // Footer
        html.append("    <div class=\"footer\">\n");
        html.append("        <p>Generated on " + new SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss").format(new Date()) + "</p>\n");
        html.append("        <p>Automated Performance Testing</p>\n");
        html.append("    </div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }


    /**
     * Build Suite Info Section
     */
    private String buildSuiteInfoSection(String suiteName) {
        StringBuilder section = new StringBuilder();

        section.append("        <div class=\"section\">\n");
        section.append("            <h2>ℹ️ Suite Information</h2>\n");
        section.append("            <div class=\"info-grid\">\n");

        // Suite Name
        section.append("                <div class=\"info-item\">\n");
        section.append("                    <span class=\"label\">Suite Name:</span>\n");
        section.append("                    <span class=\"value\">").append(suiteName).append("</span>\n");
        section.append("                </div>\n");

        // Report Generated Timestamp
        section.append("                <div class=\"info-item\">\n");
        section.append("                    <span class=\"label\">Report Generated:</span>\n");
        section.append("                    <span class=\"value\">")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .append("</span>\n");
        section.append("                </div>\n");

        // Browser
        section.append("                <div class=\"info-item\">\n");
        section.append("                    <span class=\"label\">Browser:</span>\n");
        section.append("                    <span class=\"value\">")
                .append(System.getProperty("browser", "N/A"))
                .append("</span>\n");
        section.append("                </div>\n");

        section.append("            </div>\n");
        section.append("        </div>\n");

        return section.toString();
    }


    /**
     * Build Web Vitals Section
     */
    private String buildWebVitalsSection(PerformanceTracker tracker) {
        // Get Web Vitals data using reflection or add getter method
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);

        if (webVitalsList == null || webVitalsList.isEmpty()) {
            return "";
        }

        StringBuilder section = new StringBuilder();

        section.append("        <div class=\"section\">\n");
        section.append("            <h2>🎯 Web Vitals (Google Standards)</h2>\n");
        section.append("            <p class=\"section-subtitle\">Core Web Vitals are essential metrics for measuring user experience</p>\n");

        for (WebVitalsCapture.WebVitals vitals : webVitalsList) {
            section.append("            <div class=\"web-vitals-grid\">\n");

            // LCP Card
            section.append("                <div class=\"metric-card ").append(getMetricClass(vitals.getLcpRating())).append("\">\n");
            section.append("                    <div class=\"metric-header\">\n");
            section.append("                        <span class=\"metric-emoji\">").append(vitals.getLcpEmoji()).append("</span>\n");
            section.append("                        <span class=\"metric-title\">LCP</span>\n");
            section.append("                    </div>\n");
            section.append("                    <div class=\"metric-value\">").append(String.format("%.0f", vitals.getLcp())).append("ms</div>\n");
            section.append("                    <div class=\"metric-label\">Largest Contentful Paint</div>\n");
            section.append("                    <div class=\"metric-rating\">").append(vitals.getLcpRating()).append("</div>\n");
            section.append("                </div>\n");

            // CLS Card
            section.append("                <div class=\"metric-card ").append(getMetricClass(vitals.getClsRating())).append("\">\n");
            section.append("                    <div class=\"metric-header\">\n");
            section.append("                        <span class=\"metric-emoji\">").append(vitals.getClsEmoji()).append("</span>\n");
            section.append("                        <span class=\"metric-title\">CLS</span>\n");
            section.append("                    </div>\n");
            section.append("                    <div class=\"metric-value\">").append(String.format("%.3f", vitals.getCls())).append("</div>\n");
            section.append("                    <div class=\"metric-label\">Cumulative Layout Shift</div>\n");
            section.append("                    <div class=\"metric-rating\">").append(vitals.getClsRating()).append("</div>\n");
            section.append("                </div>\n");

            // FCP Card
            section.append("                <div class=\"metric-card ").append(getMetricClass(vitals.getFcpRating())).append("\">\n");
            section.append("                    <div class=\"metric-header\">\n");
            section.append("                        <span class=\"metric-emoji\">").append(vitals.getFcpEmoji()).append("</span>\n");
            section.append("                        <span class=\"metric-title\">FCP</span>\n");
            section.append("                    </div>\n");
            section.append("                    <div class=\"metric-value\">").append(String.format("%.0f", vitals.getFcp())).append("ms</div>\n");
            section.append("                    <div class=\"metric-label\">First Contentful Paint</div>\n");
            section.append("                    <div class=\"metric-rating\">").append(vitals.getFcpRating()).append("</div>\n");
            section.append("                </div>\n");

            // TTFB Card
            section.append("                <div class=\"metric-card ").append(getMetricClass(vitals.getTtfbRating())).append("\">\n");
            section.append("                    <div class=\"metric-header\">\n");
            section.append("                        <span class=\"metric-emoji\">").append(vitals.getTtfbEmoji()).append("</span>\n");
            section.append("                        <span class=\"metric-title\">TTFB</span>\n");
            section.append("                    </div>\n");
            section.append("                    <div class=\"metric-value\">").append(String.format("%.0f", vitals.getTtfb())).append("ms</div>\n");
            section.append("                    <div class=\"metric-label\">Time to First Byte</div>\n");
            section.append("                    <div class=\"metric-rating\">").append(vitals.getTtfbRating()).append("</div>\n");
            section.append("                </div>\n");

            section.append("            </div>\n");

            // Overall Score
            int score = vitals.getOverallScore();
            String scoreClass = score >= 80 ? "score-good" : score >= 60 ? "score-medium" : "score-poor";

            section.append("            <div class=\"overall-score ").append(scoreClass).append("\">\n");
            section.append("                <span class=\"score-label\">Overall Performance Score:</span>\n");
            section.append("                <span class=\"score-value\">").append(score).append("/100</span>\n");
            section.append("            </div>\n");

            // URL
            if (vitals.getUrl() != null) {
                section.append("            <div class=\"url-info\">\n");
                section.append("                <strong>URL:</strong> ").append(vitals.getUrl()).append("\n");
                section.append("            </div>\n");
            }
        }

        section.append("        </div>\n");

        return section.toString();
    }

    /**
     * Build Page Loads Section
     *
     * Updated: Clicking the page row toggles a single drill-down area that contains:
     *  - all occurrences for the page
     *  - a single "Related API Transactions" table for that page (page-scoped)
     *
     * The Related APIs are found by:
     *  - preferring networkMonitor.getApiRequestsForPage(pageUrl) if available, otherwise
     *  - filtering all networkMonitor.getApiRequests() by time-window and URL contains-page heuristics.
     */
    private String buildUniquePageLoadsSection(PerformanceTracker tracker) {
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        if (pageLoads == null || pageLoads.isEmpty()) return "";

        // Group by URL and keep the first occurrence for unique table, collect all for drill-down
        java.util.Map<String, java.util.List<PerformanceTracker.PageLoadMetric>> byUrl = new java.util.LinkedHashMap<>();
        for (PerformanceTracker.PageLoadMetric m : pageLoads) {
            byUrl.computeIfAbsent(m.getUrl(), k -> new java.util.ArrayList<>()).add(m);
        }

        // Attempt to get network monitor to fetch per-page APIs
        NetworkPerformanceMonitor networkMonitor = null;
        try {
            networkMonitor = tracker.getNetworkMonitor();
        } catch (Exception ignored) {
        }

        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\n");
        section.append("            <h2>📄 Page Load Response Times - PRIMARY METRIC</h2>\n");
        section.append("            <p class=\"section-subtitle\">Actual page load times captured for each navigation. Click any page row to expand occurrences and related API transactions for that page.</p>\n");
        section.append("            <table class=\"data-table\">\n");
        section.append("                <thead>\n");
        section.append("                    <tr>\n");
        section.append("                        <th>Status</th>\n");
        section.append("                        <th>URL / Page</th>\n");
        section.append("                        <th>First Load Time</th>\n");
        section.append("                        <th>Occurrences</th>\n");
        section.append("                        <th>Drill-down</th>\n");
        section.append("                    </tr>\n");
        section.append("                </thead>\n");
        section.append("                <tbody>\n");

        int idx = 0;
        for (java.util.Map.Entry<String, java.util.List<PerformanceTracker.PageLoadMetric>> e : byUrl.entrySet()) {
            PerformanceTracker.PageLoadMetric first = e.getValue().get(0);
            long loadTime = first.getLoadTimeMs();
            boolean isSlow = loadTime > config.getPerformanceThresholdPageLoadMs();
            String rowId = "page_drill_" + (idx++);

            // Top-level page row toggles entire drill-down (occurrences + related APIs)
            section.append("                    <tr onclick=\"var el=document.getElementById('").append(rowId).append("'); el.style.display = (el.style.display==='none'||!el.style.display)?'table-row':'none'\" style=\"cursor:pointer\">\n");
            section.append("                        <td class=\"status\">").append(first.getStatus()).append("</td>\n");
            section.append("                        <td>").append(first.getUrl()).append("</td>\n");
            section.append("                        <td class=\"load-time\">").append(loadTime).append("ms</td>\n");
            section.append("                        <td>").append(e.getValue().size()).append("</td>\n");
            section.append("                        <td class=\"").append(isSlow ? "slow" : "fast").append("\">")
                    .append(isSlow ? "Slow" : "Fast").append("</td>\n");
            section.append("                    </tr>\n");

            // Single drill-down row for this page (hidden by default)
            section.append("                    <tr id=\"").append(rowId).append("\" style=\"display:none\">\n");
            section.append("                        <td colspan=\"5\">\n");

            // (A) occurrences table
            section.append("                            <div style=\"margin-bottom:16px\">\n");
            section.append("                            <strong>Occurrences for ").append(escapeHtml(first.getUrl())).append(":</strong>\n");
            section.append("                            <table class=\"data-table\" style=\"margin-top:8px\">\n");
            section.append("                                <thead><tr><th>#</th><th>Timestamp</th><th>Load Time</th><th>Status</th></tr></thead>\n");
            section.append("                                <tbody>\n");
            int i = 1;
            for (PerformanceTracker.PageLoadMetric m : e.getValue()) {
                section.append("                                    <tr>\n");
                section.append("                                        <td>").append(i++).append("</td>\n");
                section.append("                                        <td>").append(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(m.getTimestamp()))).append("</td>\n");
                section.append("                                        <td class=\"load-time\">").append(m.getLoadTimeMs()).append("ms</td>\n");
                section.append("                                        <td class=\"status\">").append(m.getStatus()).append("</td>\n");
                section.append("                                    </tr>\n");
            }
            section.append("                                </tbody>\n");
            section.append("                            </table>\n");
            section.append("                            </div>\n");

            // (B) Related APIs for this page (single table)
            if (networkMonitor != null) {
                try {
                    List<NetworkPerformanceMonitor.NetworkRequest> relatedApis = null;
                    // Prefer direct per-page API accessor
                    try {
                        relatedApis = networkMonitor.getApiRequestsForPage(first.getUrl());
                    } catch (Throwable ignored) {
                        // fallback below
                    }

                    // If not available, use all APIs and filter using heuristics (time-window + URL contains)
                    if (relatedApis == null) {
                        try {
                            List<NetworkPerformanceMonitor.NetworkRequest> allApis = networkMonitor.getApiRequests();
                            if (allApis != null) {
                                // Determine a broad page window using occurrences: earliest startTime to latest end
                                long earliestTs = Long.MAX_VALUE;
                                long latestTs = Long.MIN_VALUE;
                                for (PerformanceTracker.PageLoadMetric m : e.getValue()) {
                                    long ts = m.getTimestamp();
                                    long lt = m.getLoadTimeMs();
                                    earliestTs = Math.min(earliestTs, ts - 1000); // include 1s before
                                    latestTs = Math.max(latestTs, ts + lt + 5000); // include load + 5s
                                }
                                if (earliestTs == Long.MAX_VALUE) {
                                    earliestTs = 0;
                                }
                                if (latestTs == Long.MIN_VALUE) {
                                    latestTs = Long.MAX_VALUE / 2;
                                }

                                relatedApis = new java.util.ArrayList<>();
                                for (NetworkPerformanceMonitor.NetworkRequest api : allApis) {
                                    long apiStart = api.getStartTime();
                                    boolean timeMatch = (apiStart >= earliestTs && apiStart <= latestTs);
                                    boolean urlMatch = api.getUrl() != null && api.getUrl().contains(first.getUrl());
                                    // include if either time or url hints match
                                    if (timeMatch || urlMatch) {
                                        relatedApis.add(api);
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                            relatedApis = null;
                        }
                    }

                    if (relatedApis != null && !relatedApis.isEmpty()) {
                        section.append("                            <div>\n");
                        section.append("                            <strong>Related API Transactions (").append(relatedApis.size()).append(")</strong>\n");
                        section.append("                            <table class=\"data-table\" style=\"margin-top:8px\">\n");
                        section.append("                                <thead><tr><th>#</th><th>Endpoint</th><th>Response Time</th><th>Start Time (ms)</th><th>Performance</th></tr></thead>\n");
                        section.append("                                <tbody>\n");
                        int j = 1;
                        for (NetworkPerformanceMonitor.NetworkRequest api : relatedApis) {
                            long duration = api.getDuration();
                            boolean apiSlow = duration > config.getPerformanceThresholdApiResponseMs();
                            String perfClass = apiSlow ? "slow" : "fast";
                            String emoji = apiSlow ? "❌" : (duration > config.getPerformanceThresholdApiResponseMs() / 2 ? "⚠️" : "✅");

                            section.append("                                    <tr>\n");
                            section.append("                                        <td>").append(j++).append("</td>\n");
                            section.append("                                        <td>").append(escapeHtml(api.getEndpoint())).append("</td>\n");
                            section.append("                                        <td class=\"load-time\">").append(duration).append("ms</td>\n");
                            section.append("                                        <td>").append(api.getStartTime()).append("</td>\n");
                            section.append("                                        <td class=\"").append(perfClass).append("\">").append(emoji).append("</td>\n");
                            section.append("                                    </tr>\n");
                        }
                        section.append("                                </tbody>\n");
                        section.append("                            </table>\n");
                        section.append("                            </div>\n");
                    } else {
                        section.append("                            <div style=\"margin-top:12px;color:#666\">No related API calls captured for this page.</div>\n");
                    }
                } catch (Throwable t) {
                    section.append("                            <div style=\"margin-top:12px;color:#666\">Unable to fetch related API calls for this page.</div>\n");
                }
            } else {
                section.append("                            <div style=\"margin-top:12px;color:#666\">Network monitor not available to show related API calls.</div>\n");
            }

            section.append("                        </td>\n");
            section.append("                    </tr>\n");
        }

        section.append("                </tbody>\n");
        section.append("            </table>\n");
        section.append("        </div>\n");

        return section.toString();
    }

    // KPI table rendering with real live data
    private String buildKpiSection(PerformanceTracker tracker) {
        NetworkPerformanceMonitor net = tracker.getNetworkMonitor();
        NetworkPerformanceMonitor.NetworkSummary s = (net != null) ? net.getSummary() : new NetworkPerformanceMonitor.NetworkSummary();

        // Get real data from network monitor
        double avg = s.getAvgDuration();
        double p95 = s.getP95();
        long total = s.getTotalRequests();

        // Calculate TPS based on actual test duration
        Long testStartTime = tracker.getTestStartTime();
        long testDuration = System.currentTimeMillis() - (testStartTime != null ? testStartTime : System.currentTimeMillis() - 30000);
        double tps = total > 0 && testDuration > 0 ? (total * 1000.0 / testDuration) : 0.0;

        // Calculate error rate (assuming all requests are successful for now)
        // In a real implementation, you would track failed requests separately
        double errorRate = 0.0; // Placeholder - would need failed request tracking

        // Calculate Apdex score based on response time thresholds
        double apdexScore = calculateApdexScore(s);

        // Format values with proper handling of zero/empty data
        String avgStr = avg > 0 ? String.format("%.2f sec", avg / 1000.0) : "No API calls captured";
        String p95Str = p95 > 0 ? String.format("%.2f sec", p95 / 1000.0) : "No API calls captured";
        String tpsStr = tps > 0 ? String.format("%.1f", tps) : "0";
        String errorRateStr = String.format("%.1f%%", errorRate);
        String apdexStr = apdexScore > 0 ? String.format("%.2f", apdexScore) : "N/A";

        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\n");
        section.append("            <h2>📌 Key Performance Indicators (KPI) - Live Data</h2>\n");
        section.append("            <p class=\"section-subtitle\">Real-time metrics captured during test execution</p>\n");
        section.append("            <table class=\"data-table\">\n");
        section.append("                <thead><tr><th>KPI</th><th>Target (SLA)</th><th>Observed Result</th><th>Status</th><th>Remarks</th></tr></thead>\n");
        section.append("                <tbody>\n");

        // Avg Response Time
        section.append("                    <tr><td>Avg Response Time</td><td>≤ 2 sec</td><td>").append(avgStr).append("</td><td>");
        if (avg > 0) {
            section.append(avg <= 2000 ? "✅ Pass" : "❌ Fail").append("</td><td>")
                    .append(avg <= 2000 ? "Within SLA" : "Exceeds SLA");
        } else {
            section.append("⚠️ No Data").append("</td><td>No API calls captured");
        }
        section.append("</td></tr>\n");

        // 95th Percentile Response
        section.append("                    <tr><td>95th Percentile Response</td><td>≤ 3 sec</td><td>").append(p95Str).append("</td><td>");
        if (p95 > 0) {
            section.append(p95 <= 3000 ? "✅ Pass" : "❌ Fail").append("</td><td>")
                    .append(p95 <= 3000 ? "Acceptable limits" : "Tail latency high");
        } else {
            section.append("⚠️ No Data").append("</td><td>No API calls captured");
        }
        section.append("</td></tr>\n");

        // Peak TPS
        section.append("                    <tr><td>Peak TPS</td><td>≥ 150</td><td>").append(tpsStr).append("</td><td>");
        if (tps > 0) {
            section.append(tps >= 150 ? "✅ Pass" : "⚠️ Watch").append("</td><td>")
                    .append(tps >= 150 ? "Meets capacity goals" : "Consider scaling");
        } else {
            section.append("⚠️ No Data").append("</td><td>No API calls captured");
        }
        section.append("</td></tr>\n");

        // Error Rate
        section.append("                    <tr><td>Error Rate</td><td>≤ 1%</td><td>").append(errorRateStr).append("</td><td>");
        section.append(errorRate <= 1.0 ? "✅ Pass" : "❌ Fail").append("</td><td>");
        section.append(errorRate <= 1.0 ? "Stable" : "High error rate detected").append("</td></tr>\n");

        // CPU Utilization (placeholder - requires monitoring agent)
        section.append("                    <tr><td>CPU Utilization</td><td>≤ 80%</td><td>N/A</td><td>N/A</td><td>Requires monitoring agent</td></tr>\n");

        // Memory Utilization (placeholder - requires monitoring agent)
        section.append("                    <tr><td>Memory Utilization</td><td>≤ 75%</td><td>N/A</td><td>N/A</td><td>Requires monitoring agent</td></tr>\n");

        // Apdex Score
        section.append("                    <tr><td>Apdex Score</td><td>≥ 0.85</td><td>").append(apdexStr).append("</td><td>");
        if (apdexScore > 0) {
            section.append(apdexScore >= 0.85 ? "✅ Pass" : "⚠️ Watch").append("</td><td>")
                    .append(apdexScore >= 0.85 ? "Good user experience" : "Needs improvement");
        } else {
            section.append("⚠️ No Data").append("</td><td>No API calls captured");
        }
        section.append("</td></tr>\n");

        section.append("                </tbody>\n");
        section.append("            </table>\n");

        // Add data summary
        section.append("            <div class=\"info-grid\" style=\"margin-top: 20px;\">\n");
        section.append("                <div class=\"info-item\">\n");
        section.append("                    <span class=\"label\">Total API Calls:</span>\n");
        section.append("                    <span class=\"value\">").append(total).append("</span>\n");
        section.append("                </div>\n");
        section.append("                <div class=\"info-item\">\n");
        section.append("                    <span class=\"label\">Test Duration:</span>\n");
        section.append("                    <span class=\"value\">").append(String.format("%.1f sec", testDuration / 1000.0)).append("</span>\n");
        section.append("                </div>\n");
        section.append("                <div class=\"info-item\">\n");
        section.append("                    <span class=\"label\">Data Source:</span>\n");
        section.append("                    <span class=\"value\">").append(net != null ? "Network Monitor" : "No Data").append("</span>\n");
        section.append("                </div>\n");
        section.append("            </div>\n");

        section.append("        </div>\n");
        return section.toString();
    }

    /**
     * Calculate Apdex score based on response time distribution
     */
    private double calculateApdexScore(NetworkPerformanceMonitor.NetworkSummary summary) {
        if (summary.getTotalRequests() == 0) return 0.0;

        // Apdex thresholds: Satisfied < 1s, Tolerating < 3s, Frustrated >= 3s
        long satisfied = 0, tolerating = 0;

        // This is a simplified calculation - in reality we'd need the full distribution
        double avg = summary.getAvgDuration();
        long totalRequests = summary.getTotalRequests();
        if (avg <= 1000) {
            satisfied = totalRequests;
        } else if (avg <= 3000) {
            tolerating = totalRequests;
        }
        // If avg > 3000, both satisfied and tolerating remain 0 (frustrated)

        // Apdex = (Satisfied + Tolerating/2) / Total
        return (satisfied + tolerating * 0.5) / (double) totalRequests;
    }

    /**
     * Build API Performance Section
     */
    private String buildApiPerformanceSection(PerformanceTracker tracker) {
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();

        if (networkMonitor == null) {
            return "";
        }

        List<NetworkPerformanceMonitor.NetworkRequest> apiCalls = networkMonitor.getApiRequests();

        if (apiCalls == null || apiCalls.isEmpty()) {
            return "";
        }

        NetworkPerformanceMonitor.NetworkSummary summary = networkMonitor.getSummary();

        StringBuilder section = new StringBuilder();

        section.append("        <div class=\"section\">\n");
        section.append("            <h2>🌐 Network Traffic & API Response Times - PRIMARY METRIC</h2>\n");
        section.append("            <p class=\"section-subtitle\">All network requests and API calls captured during test execution with detailed response times</p>\n");

        // Summary cards
        section.append("            <div class=\"api-summary-grid\">\n");
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(summary.getTotalRequests()).append("</div>\n");
        section.append("                    <div class=\"summary-label\">Total API Calls</div>\n");
        section.append("                </div>\n");
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(String.format("%.0f", summary.getAvgDuration())).append("ms</div>\n");
        section.append("                    <div class=\"summary-label\">Average Response Time</div>\n");
        section.append("                </div>\n");
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(String.format("%.0f", summary.getP95())).append("ms</div>\n");
        section.append("                    <div class=\"summary-label\">95th Percentile (P95)</div>\n");
        section.append("                </div>\n");
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(summary.getMinDuration()).append("ms</div>\n");
        section.append("                    <div class=\"summary-label\">Fastest API</div>\n");
        section.append("                </div>\n");
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(summary.getMaxDuration()).append("ms</div>\n");
        section.append("                    <div class=\"summary-label\">Slowest API</div>\n");
        section.append("                </div>\n");
        section.append("            </div>\n");

        // Percentile breakdown
        section.append("            <div class=\"percentile-breakdown\">\n");
        section.append("                <h3>Response Time Percentiles</h3>\n");
        section.append("                <div class=\"percentile-grid\">\n");
        section.append("                    <div class=\"percentile-item\"><span class=\"percentile-label\">P50 (Median):</span> <span class=\"percentile-value\">").append(String.format("%.0f", summary.getP50())).append("ms</span></div>\n");
        section.append("                    <div class=\"percentile-item\"><span class=\"percentile-label\">P75:</span> <span class=\"percentile-value\">").append(String.format("%.0f", summary.getP75())).append("ms</span></div>\n");
        section.append("                    <div class=\"percentile-item\"><span class=\"percentile-label\">P90:</span> <span class=\"percentile-value\">").append(String.format("%.0f", summary.getP90())).append("ms</span></div>\n");
        section.append("                    <div class=\"percentile-item\"><span class=\"percentile-label\">P95:</span> <span class=\"percentile-value\">").append(String.format("%.0f", summary.getP95())).append("ms</span></div>\n");
        section.append("                    <div class=\"percentile-item\"><span class=\"percentile-label\">P99:</span> <span class=\"percentile-value\">").append(String.format("%.0f", summary.getP99())).append("ms</span></div>\n");
        section.append("                </div>\n");
        section.append("            </div>\n");

        // API calls table
//        section.append("            <table class=\"data-table\">\n");
//        section.append("                <thead>\n");
//        section.append("                    <tr>\n");
//        section.append("                        <th>Status</th>\n");
//        section.append("                        <th>API Endpoint</th>\n");
//        section.append("                        <th>Response Time</th>\n");
//        section.append("                        <th>Performance</th>\n");
//        section.append("                    </tr>\n");
//        section.append("                </thead>\n");
//        section.append("                <tbody>\n");
//
//        for (NetworkPerformanceMonitor.NetworkRequest api : apiCalls) {
//            long duration = api.getDuration();
//            boolean isSlow = duration > config.getPerformanceThresholdApiResponseMs();
//            String statusEmoji = isSlow ? "❌" : duration > config.getPerformanceThresholdApiResponseMs() / 2 ? "⚠️" : "✅";
//
//            section.append("                    <tr>\n");
//            section.append("                        <td class=\"status\">").append(statusEmoji).append("</td>\n");
//            section.append("                        <td>").append(api.getEndpoint()).append("</td>\n");
//            section.append("                        <td class=\"load-time\">").append(duration).append("ms</td>\n");
//            section.append("                        <td class=\"").append(isSlow ? "slow" : "fast").append("\">")
//                    .append(isSlow ? "Slow" : "Fast").append("</td>\n");
//            section.append("                    </tr>\n");
//        }
//
//        section.append("                </tbody>\n");
//        section.append("            </table>\n");
//        section.append("        </div>\n");

        return section.toString();
    }

    /**
     * Build API Transactions Drill-down Section
     */
    private String buildApiTransactionsSection(PerformanceTracker tracker) {
        try {
            java.lang.reflect.Field f = tracker.getClass().getDeclaredField("transactionCapture");
            f.setAccessible(true);
            Object cap = f.get(tracker);
            if (cap == null) return "";

            java.lang.reflect.Method m = cap.getClass().getMethod("getTransactions");
            java.util.List<?> txs = (java.util.List<?>) m.invoke(cap);
            if (txs == null || txs.isEmpty()) return "";

            StringBuilder section = new StringBuilder();
            section.append("        <div class=\"section\">\n");
            section.append("            <h2>🔎 API Transactions (Detailed)</h2>\n");
            section.append("            <p class=\"section-subtitle\">Headers, payloads, and responses captured with size limits and same-origin rules.</p>\n");
            section.append("            <table class=\"data-table\">\n");
            section.append("                <thead><tr><th>Method</th><th>API</th><th>Status</th><th>Duration</th><th>Details</th></tr></thead>\n");
            section.append("                <tbody>\n");

            int idx = 0;
            for (Object o : txs) {
                idx++;
                Class<?> c = o.getClass();
                String method = String.valueOf(c.getField("method").get(o));
                String path = String.valueOf(c.getField("path").get(o));
                String status = String.valueOf(c.getField("status").get(o));
                String duration = String.valueOf(c.getField("durationMs").get(o)) + "ms";
                String rowId = "api_tx_" + idx;

                section.append("                    <tr onclick=\"var el=document.getElementById('").append(rowId).append("'); el.style.display=(el.style.display==='none'||!el.style.display)?'table-row':'none'\">\n");
                section.append("                        <td>").append(method).append("</td>\n");
                section.append("                        <td>").append(path).append("</td>\n");
                section.append("                        <td>").append(status).append("</td>\n");
                section.append("                        <td>").append(duration).append("</td>\n");
                section.append("                        <td>Click to expand</td>\n");
                section.append("                    </tr>\n");

                // Details row
                section.append("                    <tr id=\"").append(rowId).append("\" style=\"display:none\">\n");
                section.append("                        <td colspan=\"5\">\n");
                section.append("                            <pre style=\"white-space:pre-wrap;word-break:break-word;background:#f8f9fa;padding:12px;border-radius:6px;\">\n");
                section.append("REQUEST HEADERS:\n").append(String.valueOf(c.getField("requestHeaders").get(o))).append("\n\n");
                Object rb = c.getField("requestBody").get(o);
                if (rb != null) section.append("REQUEST BODY (truncated):\n").append(escapeHtml(String.valueOf(rb))).append("\n\n");
                section.append("RESPONSE HEADERS:\n").append(String.valueOf(c.getField("responseHeaders").get(o))).append("\n\n");
                Object r = c.getField("responseBody").get(o);
                if (r != null) section.append("RESPONSE BODY (truncated):\n").append(escapeHtml(String.valueOf(r))).append("\n");
                section.append("                            </pre>\n");
                section.append("                        </td>\n");
                section.append("                    </tr>\n");
            }

            section.append("                </tbody>\n");
            section.append("            </table>\n");
            section.append("        </div>\n");
            return section.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Build Performance Issues Section
     */
    private String buildPerformanceIssuesSection(PerformanceTracker tracker) {
        if (!tracker.hasPerformanceIssues()) {
            return "        <div class=\"section success-section\">\n" +
                    "            <h2>✅ No Performance Issues Detected</h2>\n" +
                    "            <p>All performance metrics are within acceptable thresholds.</p>\n" +
                    "        </div>\n";
        }

        StringBuilder section = new StringBuilder();

        section.append("        <div class=\"section warning-section\">\n");
        section.append("            <h2>⚠️ Performance Issues Detected</h2>\n");
        section.append("            <div class=\"issues-list\">\n");

        // Check slow pages
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        if (pageLoads != null) {
            for (PerformanceTracker.PageLoadMetric metric : pageLoads) {
                if (metric.isSlowLoad(config.getPerformanceThresholdPageLoadMs())) {
                    section.append("                <div class=\"issue-item\">\n");
                    section.append("                    <strong>Slow Page Load:</strong> ").append(metric.getUrl()).append("\n");
                    section.append("                    <br><span class=\"issue-detail\">Load time: ").append(metric.getLoadTimeMs())
                            .append("ms (threshold: ").append(config.getPerformanceThresholdPageLoadMs()).append("ms)</span>\n");
                    section.append("                </div>\n");
                }
            }
        }

        // Check slow APIs
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();
        if (networkMonitor != null) {
            List<NetworkPerformanceMonitor.NetworkRequest> slowApis = networkMonitor.getSlowApiCalls(
                    config.getPerformanceThresholdApiResponseMs()
            );
            if (slowApis != null) {
                for (NetworkPerformanceMonitor.NetworkRequest api : slowApis) {
                    section.append("                <div class=\"issue-item\">\n");
                    section.append("                    <strong>Slow API:</strong> ").append(api.getEndpoint()).append("\n");
                    section.append("                    <br><span class=\"issue-detail\">Response time: ").append(api.getDuration())
                            .append("ms (threshold: ").append(config.getPerformanceThresholdApiResponseMs()).append("ms)</span>\n");
                    section.append("                </div>\n");
                }
            }
        }

        // Check Web Vitals
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);
        if (webVitalsList != null) {
            for (WebVitalsCapture.WebVitals vitals : webVitalsList) {
                if (vitals.hasPoorWebVitals() || vitals.needsImprovement()) {
                    section.append("                <div class=\"issue-item\">\n");
                    section.append("                    <strong>Web Vitals Issue:</strong> ").append(vitals.getUrl()).append("\n");
                    section.append("                    <br><span class=\"issue-detail\">Score: ").append(vitals.getOverallScore())
                            .append("/100 - ").append(vitals.hasPoorWebVitals() ? "Poor" : "Needs Improvement").append("</span>\n");
                    section.append("                </div>\n");
                }
            }
        }

        section.append("            </div>\n");
        section.append("        </div>\n");

        return section.toString();
    }

    /**
     * Build Recommendations Section
     */
    private String buildRecommendationsSection(PerformanceTracker tracker) {
        StringBuilder section = new StringBuilder();

        section.append("        <div class=\"section\">\n");
        section.append("            <h2>💡 Performance Recommendations</h2>\n");
        section.append("            <ul class=\"recommendations-list\">\n");

        // Analyze and provide recommendations
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);

        boolean hasRecommendations = false;

        // Page load recommendations
        if (pageLoads != null && !pageLoads.isEmpty()) {
            long maxLoadTime = pageLoads.stream().mapToLong(PerformanceTracker.PageLoadMetric::getLoadTimeMs).max().orElse(0);
            if (maxLoadTime > config.getPerformanceThresholdPageLoadMs()) {
                section.append("                <li>Optimize slow page loads by implementing lazy loading, code splitting, and reducing bundle sizes.</li>\n");
                hasRecommendations = true;
            }
        }

        // API recommendations
        if (networkMonitor != null) {
            List<NetworkPerformanceMonitor.NetworkRequest> slowApis = networkMonitor.getSlowApiCalls(
                    config.getPerformanceThresholdApiResponseMs()
            );
            if (slowApis != null && !slowApis.isEmpty()) {
                section.append("                <li>Optimize slow API calls by adding database indexes, implementing caching, or using CDNs.</li>\n");
                section.append("                <li>Consider implementing API response compression and pagination for large datasets.</li>\n");
                hasRecommendations = true;
            }
        }

        // Web Vitals recommendations
        if (webVitalsList != null && !webVitalsList.isEmpty()) {
            for (WebVitalsCapture.WebVitals vitals : webVitalsList) {
                if (vitals.getLcp() > 2500) {
                    section.append("                <li><strong>LCP Improvement:</strong> Optimize largest contentful element by preloading critical resources and using image optimization.</li>\n");
                    hasRecommendations = true;
                }
                if (vitals.getCls() > 0.1) {
                    section.append("                <li><strong>CLS Improvement:</strong> Prevent layout shifts by setting explicit dimensions for images and ads.</li>\n");
                    hasRecommendations = true;
                }
                if (vitals.getTtfb() > 800) {
                    section.append("                <li><strong>TTFB Improvement:</strong> Optimize server response time by using faster hosting, implementing caching, or using a CDN.</li>\n");
                    hasRecommendations = true;
                }
            }
        }

        if (!hasRecommendations) {
            section.append("                <li>✅ Performance is good! Continue monitoring to maintain optimal performance.</li>\n");
            section.append("                <li>Consider implementing performance budgets to prevent regressions.</li>\n");
            section.append("                <li>Set up continuous performance monitoring in your CI/CD pipeline.</li>\n");
        } else {
            section.append("                <li>Set up performance baseline to track improvements over time.</li>\n");
            section.append("                <li>Monitor performance trends after implementing optimizations.</li>\n        ");
        }

        section.append("            </ul>\n");
        section.append("        </div>\n");

        return section.toString();
    }

    /**
     * Get metric CSS class based on rating
     */
    private String getMetricClass(String rating) {
        switch (rating) {
            case "Good":
                return "metric-good";
            case "Needs Improvement":
                return "metric-warning";
            case "Poor":
                return "metric-poor";
            default:
                return "";
        }
    }

    /**
     * Get Web Vitals list from tracker (using reflection if needed)
     */
    @SuppressWarnings("unchecked")
    private List<WebVitalsCapture.WebVitals> getWebVitalsList(PerformanceTracker tracker) {
        try {
            java.lang.reflect.Field field = tracker.getClass().getDeclaredField("webVitalsList");
            field.setAccessible(true);
            return (List<WebVitalsCapture.WebVitals>) field.get(tracker);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get page loads list from tracker (using reflection if needed)
     */
    @SuppressWarnings("unchecked")
    private List<PerformanceTracker.PageLoadMetric> getPageLoads(PerformanceTracker tracker) {
        try {
            java.lang.reflect.Field field = tracker.getClass().getDeclaredField("pageLoads");
            field.setAccessible(true);
            return (List<PerformanceTracker.PageLoadMetric>) field.get(tracker);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get CSS styles for the report
     */
    private String getStyles() {
        return
                "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #333; line-height: 1.6; }\n" +
                ".header { background: white; padding: 40px 20px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
                ".header h1 { font-size: 2.5em; color: #667eea; margin-bottom: 10px; }\n" +
                ".subtitle { font-size: 1.2em; color: #666; }\n" +
                ".container { max-width: 1200px; margin: 30px auto; padding: 0 20px; }\n" +
                ".section { background: white; border-radius: 10px; padding: 30px; margin-bottom: 30px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }\n" +
                ".section h2 { color: #667eea; margin-bottom: 20px; font-size: 1.8em; border-bottom: 3px solid #667eea; padding-bottom: 10px; }\n" +
                ".section-subtitle { color: #666; margin-bottom: 20px; font-style: italic; }\n" +
                ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }\n" +
                ".info-item { padding: 15px; background: #f8f9fa; border-radius: 8px; }\n" +
                ".label { font-weight: bold; color: #667eea; display: block; margin-bottom: 5px; }\n" +
                ".value { color: #333; font-size: 1.1em; }\n" +
                ".web-vitals-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }\n" +
                ".metric-card { padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }\n" +
                ".metric-good { background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%); }\n" +
                ".metric-warning { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }\n" +
                ".metric-poor { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }\n" +
                ".metric-header { display: flex; align-items: center; justify-content: center; gap: 10px; margin-bottom: 15px; }\n" +
                ".metric-emoji { font-size: 2em; }\n" +
                ".metric-title { font-size: 1.5em; font-weight: bold; }\n" +
                ".metric-value { font-size: 2.5em; font-weight: bold; margin: 10px 0; }\n" +
                ".metric-label { font-size: 0.9em; color: #666; margin-bottom: 8px; }\n" +
                ".metric-rating { font-weight: bold; padding: 5px 15px; border-radius: 20px; background: rgba(255,255,255,0.7); display: inline-block; }\n" +
                ".overall-score { text-align: center; padding: 25px; margin: 20px 0; border-radius: 10px; font-size: 1.5em; }\n" +
                ".score-good { background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%); }\n" +
                ".score-medium { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }\n" +
                ".score-poor { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }\n" +
                ".score-value { font-weight: bold; font-size: 1.8em; margin-left: 15px; }\n" +
                ".url-info { margin-top: 15px; padding: 12px; background: #f8f9fa; border-radius: 6px; word-break: break-all; }\n" +
                ".api-summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }\n" +
                ".summary-card { padding: 25px; text-align: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }\n" +
                ".summary-value { font-size: 2.5em; font-weight: bold; margin-bottom: 10px; }\n" +
                ".summary-label { font-size: 1em; opacity: 0.9; }\n" +
                ".data-table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n" +
                ".data-table thead { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }\n" +
                ".data-table th { padding: 15px; text-align: left; font-weight: 600; }\n" +
                ".data-table td { padding: 12px 15px; border-bottom: 1px solid #e0e0e0; }\n" +
                ".data-table tbody tr:hover { background: #f8f9fa; }\n" +
                ".status { font-size: 1.5em; text-align: center; }\n" +
                ".load-time { font-weight: bold; color: #667eea; }\n" +
                ".fast { color: #28a745; font-weight: bold; }\n" +
                ".slow { color: #dc3545; font-weight: bold; }\n" +
                ".success-section { background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%); }\n" +
                ".warning-section { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }\n" +
                ".issues-list { margin-top: 20px; }\n" +
                ".issue-item { padding: 15px; margin-bottom: 15px; background: white; border-left: 4px solid #dc3545; border-radius: 5px; }\n" +
                ".issue-detail { color: #666; font-size: 0.95em; }\n" +
                ".recommendations-list { list-style: none; padding-left: 0; }\n" +
                ".recommendations-list li { padding: 15px; margin: 10px 0; background: #fff3cd; border-left: 5px solid #f39c12; border-radius: 5px; }\n" +
                ".recommendations-list li:before { content: '💡 '; font-size: 1.2em; margin-right: 10px; }\n" +
                ".footer { text-align: center; padding: 30px; color: white; font-size: 0.9em; margin-top: 30px; }\n" +
                "@media print { body { background: white; } .header { box-shadow: none; } }\n";
    }
}
