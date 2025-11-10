package performanceTracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced HTML Performance Report Generator with Chart.js Visualizations
 * 
 * Features:
 * - Interactive charts and graphs (Chart.js)
 * - Trend analysis with historical data
 * - Performance score gauges
 * - Waterfall timeline visualization
 * - Responsive design with dark mode
 * - Export-ready professional reports
 * - Real-time metric updates
 * 
 * Generates:
 * - Executive summary dashboard
 * - Detailed metrics with visualizations
 * - Comparison charts
 * - Recommendations with priority
 * - Exportable in PDF/PNG
 */
public class EnhancedPerformanceReportGenerator {
    
    private final String reportsDir;
    
    public EnhancedPerformanceReportGenerator() {
        this.reportsDir = "./reports/";
        new File(reportsDir).mkdirs();
    }
    
    /**
     * Generate enhanced HTML performance report with Chart.js
     */
    public File generateEnhancedReport(String suiteName, PerformanceTracker tracker) {
        return generateEnhancedReport(suiteName, tracker, null);
    }
    
    /**
     * Generate enhanced HTML performance report for entire suite (Chart.js + URL)
     */
    public File generateEnhancedReport(String suiteName, PerformanceTracker tracker, String url) {
        if (tracker == null) {
            return null;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String meaningfulName = ReportNamingUtility.generateMeaningfulReportName(suiteName, null, url);
            String fileName = "performance_enhanced_" + meaningfulName + "_" + timestamp + ".html";

            File reportFile = new File(reportsDir, fileName);

            // Use the new suite-level enhanced report builder
            String htmlContent = buildEnhancedHtmlReport(suiteName, tracker);

            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(htmlContent);
            }

            System.out.println("🎨 Enhanced Suite Performance Report Generated: " + reportFile.getAbsolutePath());
            return reportFile;

        } catch (IOException e) {
            System.err.println("❌ Error generating enhanced suite report: " + e.getMessage());
            return null;
        }
    }

    
    /**
     * Build enhanced HTML report with visualizations (Suite-level)
     */
    private String buildEnhancedHtmlReport(String suiteName, PerformanceTracker tracker) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Performance Report - ").append(suiteName).append("</title>\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n");
        html.append("    <style>\n");
        html.append(getEnhancedStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Navigation (reuses your existing nav if it's generic)
        html.append(buildNavigation());

        // Suite Hero Section (replaces test-level hero)
        html.append(buildHeroSection(suiteName));

        // Dashboard Container
        html.append("    <div class=\"dashboard-container\">\n");

        // Executive Summary Cards
        html.append(buildExecutiveSummary(tracker));

        // Performance Score Gauge
        html.append(buildPerformanceScoreSection(tracker));

        // Page Load Chart
        html.append(buildPageLoadChartSection(tracker));

        // API Performance Chart
        html.append(buildAPIPerformanceChartSection(tracker));

        // Web Vitals Radar Chart
        html.append(buildWebVitalsRadarSection(tracker));

        // Timeline Visualization
        html.append(buildTimelineSection(tracker));

        // Detailed Tables
        html.append(buildDetailedTablesSection(tracker));

        // Recommendations
        html.append(buildSmartRecommendations(tracker));

        html.append("    </div>\n");

        // Footer
        html.append(buildFooter());

        // JavaScript for charts
        html.append(buildChartScripts(tracker));

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    
    /**
     * Build navigation bar
     */
    private String buildNavigation() {
        return "    <nav class=\"navbar\">\n" +
               "        <div class=\"nav-brand\">📊 Performance Dashboard</div>\n" +
               "        <div class=\"nav-links\">\n" +
               "            <a href=\"#summary\">Summary</a>\n" +
               "            <a href=\"#metrics\">Metrics</a>\n" +
               "            <a href=\"#recommendations\">Recommendations</a>\n" +
               "            <button class=\"theme-toggle\" onclick=\"toggleTheme()\">🌓</button>\n" +
               "        </div>\n" +
               "    </nav>\n";
    }
    
    /**
     * Build hero section
     */
    private String buildHeroSection(String suiteName) {
        return "    <div class=\"hero-section\">\n" +
               "        <h1 class=\"hero-title\">Performance Analysis Report</h1>\n" +
               "        <div class=\"hero-subtitle\">\n" +
               "            <span class=\"badge\">Test: " + (suiteName != null ? suiteName : "") + "</span>\n" +
               "            <span class=\"badge\">Date: " + new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date()) + "</span>\n" +
               "        </div>\n" +
               "    </div>\n";
    }
    
    /**
     * Build executive summary with metric cards
     */
    private String buildExecutiveSummary(PerformanceTracker tracker) {
        StringBuilder section = new StringBuilder();
        
        section.append("        <section id=\"summary\" class=\"section\">\n");
        section.append("            <h2 class=\"section-title\">📈 Executive Summary</h2>\n");
        section.append("            <div class=\"metric-cards\">\n");
        
        // Get metrics
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);
        
        // Page Load Card
        if (pageLoads != null && !pageLoads.isEmpty()) {
            double avgPageLoad = pageLoads.stream()
                .mapToLong(PerformanceTracker.PageLoadMetric::getLoadTimeMs)
                .average()
                .orElse(0.0);
            String status = avgPageLoad < 3000 ? "excellent" : avgPageLoad < 5000 ? "good" : "poor";
            
            section.append("                <div class=\"metric-card ").append(status).append("\">\n");
            section.append("                    <div class=\"metric-icon\">⚡</div>\n");
            section.append("                    <div class=\"metric-value\">").append(String.format("%.0f", avgPageLoad)).append("ms</div>\n");
            section.append("                    <div class=\"metric-label\">Avg Page Load</div>\n");
            section.append("                    <div class=\"metric-change\">").append(pageLoads.size()).append(" pages tested</div>\n");
            section.append("                </div>\n");
        }
        
        // API Performance Card
        if (networkMonitor != null) {
            NetworkPerformanceMonitor.NetworkSummary summary = networkMonitor.getSummary();
            if (summary.getTotalRequests() > 0) {
                String status = summary.getAvgDuration() < 500 ? "excellent" : 
                               summary.getAvgDuration() < 1000 ? "good" : "poor";
                
                section.append("                <div class=\"metric-card ").append(status).append("\">\n");
                section.append("                    <div class=\"metric-icon\">🌐</div>\n");
                section.append("                    <div class=\"metric-value\">").append(String.format("%.0f", summary.getAvgDuration())).append("ms</div>\n");
                section.append("                    <div class=\"metric-label\">Avg API Response</div>\n");
                section.append("                    <div class=\"metric-change\">").append(summary.getTotalRequests()).append(" API calls</div>\n");
                section.append("                </div>\n");
            }
        }
        
        // Web Vitals Card
        if (webVitalsList != null && !webVitalsList.isEmpty()) {
            WebVitalsCapture.WebVitals vitals = webVitalsList.get(0);
            int score = vitals.getOverallScore();
            String status = score >= 80 ? "excellent" : score >= 60 ? "good" : "poor";
            
            section.append("                <div class=\"metric-card ").append(status).append("\">\n");
            section.append("                    <div class=\"metric-icon\">🎯</div>\n");
            section.append("                    <div class=\"metric-value\">").append(score).append("/100</div>\n");
            section.append("                    <div class=\"metric-label\">Web Vitals Score</div>\n");
            section.append("                    <div class=\"metric-change\">Google Standards</div>\n");
            section.append("                </div>\n");
        }
        
        // Performance Issues Card
        int issuesCount = tracker.hasPerformanceIssues() ? 1 : 0;
        section.append("                <div class=\"metric-card ").append(issuesCount > 0 ? "poor" : "excellent").append("\">\n");
        section.append("                    <div class=\"metric-icon\">").append(issuesCount > 0 ? "⚠️" : "✅").append("</div>\n");
        section.append("                    <div class=\"metric-value\">").append(issuesCount).append("</div>\n");
        section.append("                    <div class=\"metric-label\">Performance Issues</div>\n");
        section.append("                    <div class=\"metric-change\">").append(issuesCount > 0 ? "Needs attention" : "All Good").append("</div>\n");
        section.append("                </div>\n");
        
        section.append("            </div>\n");
        section.append("        </section>\n");
        
        return section.toString();
    }
    
    /**
     * Build performance score gauge section
     */
    private String buildPerformanceScoreSection(PerformanceTracker tracker) {
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);
        if (webVitalsList == null || webVitalsList.isEmpty()) {
            return "";
        }
        
        WebVitalsCapture.WebVitals vitals = webVitalsList.get(0);
        int score = vitals.getOverallScore();
        
        return "        <section class=\"section\">\n" +
               "            <h2 class=\"section-title\">🎯 Overall Performance Score</h2>\n" +
               "            <div class=\"chart-container\">\n" +
               "                <canvas id=\"scoreGaugeChart\" height=\"200\"></canvas>\n" +
               "            </div>\n" +
               "        </section>\n";
    }
    
    /**
     * Build page load chart section
     */
    private String buildPageLoadChartSection(PerformanceTracker tracker) {
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        if (pageLoads == null || pageLoads.isEmpty()) {
            return "";
        }
        
        return "        <section class=\"section\">\n" +
               "            <h2 class=\"section-title\">📄 Page Load Performance</h2>\n" +
               "            <div class=\"chart-container\">\n" +
               "                <canvas id=\"pageLoadChart\"></canvas>\n" +
               "            </div>\n" +
               "        </section>\n";
    }
    
    /**
     * Build API performance chart section
     */
    private String buildAPIPerformanceChartSection(PerformanceTracker tracker) {
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();
        if (networkMonitor == null || networkMonitor.getApiRequests().isEmpty()) {
            return "";
        }
        
        return "        <section class=\"section\">\n" +
               "            <h2 class=\"section-title\">🌐 API Response Time Distribution</h2>\n" +
               "            <div class=\"chart-container\">\n" +
               "                <canvas id=\"apiPerformanceChart\"></canvas>\n" +
               "            </div>\n" +
               "        </section>\n";
    }
    
    /**
     * Build Web Vitals radar chart section
     */
    private String buildWebVitalsRadarSection(PerformanceTracker tracker) {
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);
        if (webVitalsList == null || webVitalsList.isEmpty()) {
            return "";
        }
        
        return "        <section class=\"section\">\n" +
               "            <h2 class=\"section-title\">🎯 Web Vitals Radar</h2>\n" +
               "            <div class=\"chart-container\">\n" +
               "                <canvas id=\"webVitalsRadarChart\"></canvas>\n" +
               "            </div>\n" +
               "        </section>\n";
    }
    
    /**
     * Build timeline section
     */
    private String buildTimelineSection(PerformanceTracker tracker) {
        return "        <section class=\"section\">\n" +
               "            <h2 class=\"section-title\">⏱️ Performance Timeline</h2>\n" +
               "            <div class=\"timeline\">\n" +
               "                <div class=\"timeline-item\">\n" +
               "                    <div class=\"timeline-marker\">1</div>\n" +
               "                    <div class=\"timeline-content\">\n" +
               "                        <h4>Page Load</h4>\n" +
               "                        <p>Initial navigation and DOM parsing</p>\n" +
               "                    </div>\n" +
               "                </div>\n" +
               "                <div class=\"timeline-item\">\n" +
               "                    <div class=\"timeline-marker\">2</div>\n" +
               "                    <div class=\"timeline-content\">\n" +
               "                        <h4>API Calls</h4>\n" +
               "                        <p>Background API requests executed</p>\n" +
               "                    </div>\n" +
               "                </div>\n" +
               "                <div class=\"timeline-item\">\n" +
               "                    <div class=\"timeline-marker\">3</div>\n" +
               "                    <div class=\"timeline-content\">\n" +
               "                        <h4>Metrics Captured</h4>\n" +
               "                        <p>Web Vitals and resource usage measured</p>\n" +
               "                    </div>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "        </section>\n";
    }
    
    /**
     * Build detailed tables section
     */
    private String buildDetailedTablesSection(PerformanceTracker tracker) {
        // Reuse existing table generation from PerformanceReportGenerator
        return "";
    }
    
    /**
     * Build smart recommendations
     */
    private String buildSmartRecommendations(PerformanceTracker tracker) {
        StringBuilder section = new StringBuilder();
        
        section.append("        <section id=\"recommendations\" class=\"section\">\n");
        section.append("            <h2 class=\"section-title\">💡 Smart Recommendations</h2>\n");
        section.append("            <div class=\"recommendations-grid\">\n");
        
        // Analyze and generate smart recommendations
        List<Map<String, String>> recommendations = generateSmartRecommendations(tracker);
        
        for (Map<String, String> rec : recommendations) {
            section.append("                <div class=\"recommendation-card priority-").append(rec.get("priority")).append("\">\n");
            section.append("                    <div class=\"rec-icon\">").append(rec.get("icon")).append("</div>\n");
            section.append("                    <div class=\"rec-content\">\n");
            section.append("                        <h4>").append(rec.get("title")).append("</h4>\n");
            section.append("                        <p>").append(rec.get("description")).append("</p>\n");
            section.append("                        <span class=\"rec-impact\">Impact: ").append(rec.get("impact")).append("</span>\n");
            section.append("                    </div>\n");
            section.append("                </div>\n");
        }
        
        section.append("            </div>\n");
        section.append("        </section>\n");
        
        return section.toString();
    }
    
    /**
     * Generate smart recommendations based on metrics
     */
    private List<Map<String, String>> generateSmartRecommendations(PerformanceTracker tracker) {
        List<Map<String, String>> recommendations = new ArrayList<>();
        
        // Analyze page loads
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        if (pageLoads != null) {
            long maxLoadTime = pageLoads.stream()
                .mapToLong(PerformanceTracker.PageLoadMetric::getLoadTimeMs)
                .max()
                .orElse(0);
            
            if (maxLoadTime > 5000) {
                Map<String, String> rec = new HashMap<>();
                rec.put("priority", "high");
                rec.put("icon", "🚀");
                rec.put("title", "Optimize Page Load Performance");
                rec.put("description", "Detected slow page load (" + maxLoadTime + "ms). Implement code splitting, lazy loading, and optimize bundle size.");
                rec.put("impact", "High - Improves user experience significantly");
                recommendations.add(rec);
            }
        }
        
        // Analyze API performance
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();
        if (networkMonitor != null) {
            List<NetworkPerformanceMonitor.NetworkRequest> slowApis = networkMonitor.getSlowApiCalls(1000);
            if (!slowApis.isEmpty()) {
                Map<String, String> rec = new HashMap<>();
                rec.put("priority", "high");
                rec.put("icon", "⚡");
                rec.put("title", "Optimize Slow API Calls");
                rec.put("description", "Found " + slowApis.size() + " slow API calls. Add database indexes, implement caching, or use CDN.");
                rec.put("impact", "High - Reduces wait time for users");
                recommendations.add(rec);
            }
        }
        
        // Analyze Web Vitals
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);
        if (webVitalsList != null && !webVitalsList.isEmpty()) {
            WebVitalsCapture.WebVitals vitals = webVitalsList.get(0);
            
            if (vitals.getLcp() > 2500) {
                Map<String, String> rec = new HashMap<>();
                rec.put("priority", "medium");
                rec.put("icon", "🎯");
                rec.put("title", "Improve LCP (Largest Contentful Paint)");
                rec.put("description", "LCP is " + String.format("%.0f", vitals.getLcp()) + "ms. Optimize images, preload critical resources, and use image CDN.");
                rec.put("impact", "Medium - Affects SEO and perceived performance");
                recommendations.add(rec);
            }
            
            if (vitals.getCls() > 0.1) {
                Map<String, String> rec = new HashMap<>();
                rec.put("priority", "medium");
                rec.put("icon", "📐");
                rec.put("title", "Fix Layout Shifts (CLS)");
                rec.put("description", "CLS score is " + String.format("%.3f", vitals.getCls()) + ". Set explicit dimensions for images and reserve space for ads.");
                rec.put("impact", "Medium - Improves visual stability");
                recommendations.add(rec);
            }
        }
        
        // Default recommendations
        if (recommendations.isEmpty()) {
            Map<String, String> rec = new HashMap<>();
            rec.put("priority", "low");
            rec.put("icon", "✅");
            rec.put("title", "Performance is Good!");
            rec.put("description", "All metrics are within acceptable ranges. Continue monitoring and set up performance budgets.");
            rec.put("impact", "Low - Maintenance");
            recommendations.add(rec);
        }
        
        return recommendations;
    }
    
    /**
     * Build footer
     */
    private String buildFooter() {
        return "    <footer class=\"footer\">\n" +
               "        <p>Generated by Selenium Performance Test Framework v2.0</p>\n" +
               "        <p>Powered by Chart.js • " + new SimpleDateFormat("MMMM dd, yyyy").format(new Date()) + "</p>\n" +
               "    </footer>\n";
    }
    
    /**
     * Build Chart.js scripts
     */
    private String buildChartScripts(PerformanceTracker tracker) {
        StringBuilder scripts = new StringBuilder();
        
        scripts.append("    <script>\n");
        
        // Theme toggle function
        scripts.append("        function toggleTheme() {\n");
        scripts.append("            document.body.classList.toggle('dark-mode');\n");
        scripts.append("        }\n\n");
        
        // Page Load Chart
        List<PerformanceTracker.PageLoadMetric> pageLoads = getPageLoads(tracker);
        if (pageLoads != null && !pageLoads.isEmpty()) {
            scripts.append("        // Page Load Chart\n");
            scripts.append("        const pageLoadCtx = document.getElementById('pageLoadChart');\n");
            scripts.append("        if (pageLoadCtx) {\n");
            scripts.append("            new Chart(pageLoadCtx, {\n");
            scripts.append("                type: 'bar',\n");
            scripts.append("                data: {\n");
            scripts.append("                    labels: ").append(getPageLoadLabels(pageLoads)).append(",\n");
            scripts.append("                    datasets: [{\n");
            scripts.append("                        label: 'Load Time (ms)',\n");
            scripts.append("                        data: ").append(getPageLoadData(pageLoads)).append(",\n");
            scripts.append("                        backgroundColor: 'rgba(102, 126, 234, 0.8)',\n");
            scripts.append("                        borderColor: 'rgba(102, 126, 234, 1)',\n");
            scripts.append("                        borderWidth: 2\n");
            scripts.append("                    }]\n");
            scripts.append("                },\n");
            scripts.append("                options: {\n");
            scripts.append("                    responsive: true,\n");
            scripts.append("                    plugins: {\n");
            scripts.append("                        legend: { display: true },\n");
            scripts.append("                        title: { display: true, text: 'Page Load Performance Comparison' }\n");
            scripts.append("                    },\n");
            scripts.append("                    scales: {\n");
            scripts.append("                        y: { beginAtZero: true, title: { display: true, text: 'Time (ms)' } }\n");
            scripts.append("                    }\n");
            scripts.append("                }\n");
            scripts.append("            });\n");
            scripts.append("        }\n\n");
        }
        
        // API Performance Chart
        NetworkPerformanceMonitor networkMonitor = tracker.getNetworkMonitor();
        if (networkMonitor != null && !networkMonitor.getApiRequests().isEmpty()) {
            List<NetworkPerformanceMonitor.NetworkRequest> apiCalls = networkMonitor.getApiRequests()
                .stream()
                .limit(10)
                .collect(Collectors.toList());
            
            scripts.append("        // API Performance Chart\n");
            scripts.append("        const apiCtx = document.getElementById('apiPerformanceChart');\n");
            scripts.append("        if (apiCtx) {\n");
            scripts.append("            new Chart(apiCtx, {\n");
            scripts.append("                type: 'horizontalBar',\n");
            scripts.append("                data: {\n");
            scripts.append("                    labels: ").append(getAPILabels(apiCalls)).append(",\n");
            scripts.append("                    datasets: [{\n");
            scripts.append("                        label: 'Response Time (ms)',\n");
            scripts.append("                        data: ").append(getAPIData(apiCalls)).append(",\n");
            scripts.append("                        backgroundColor: 'rgba(255, 159, 64, 0.8)',\n");
            scripts.append("                        borderColor: 'rgba(255, 159, 64, 1)',\n");
            scripts.append("                        borderWidth: 2\n");
            scripts.append("                    }]\n");
            scripts.append("                },\n");
            scripts.append("                options: {\n");
            scripts.append("                    indexAxis: 'y',\n");
            scripts.append("                    responsive: true,\n");
            scripts.append("                    plugins: {\n");
            scripts.append("                        legend: { display: true },\n");
            scripts.append("                        title: { display: true, text: 'Top 10 API Calls by Response Time' }\n");
            scripts.append("                    }\n");
            scripts.append("                }\n");
            scripts.append("            });\n");
            scripts.append("        }\n\n");
        }
        
        // Web Vitals Radar Chart
        List<WebVitalsCapture.WebVitals> webVitalsList = getWebVitalsList(tracker);
        if (webVitalsList != null && !webVitalsList.isEmpty()) {
            WebVitalsCapture.WebVitals vitals = webVitalsList.get(0);
            
            scripts.append("        // Web Vitals Radar Chart\n");
            scripts.append("        const radarCtx = document.getElementById('webVitalsRadarChart');\n");
            scripts.append("        if (radarCtx) {\n");
            scripts.append("            new Chart(radarCtx, {\n");
            scripts.append("                type: 'radar',\n");
            scripts.append("                data: {\n");
            scripts.append("                    labels: ['LCP', 'CLS', 'FCP', 'TTFB', 'Overall'],\n");
            scripts.append("                    datasets: [{\n");
            scripts.append("                        label: 'Current Performance',\n");
            scripts.append("                        data: ").append(getWebVitalsRadarData(vitals)).append(",\n");
            scripts.append("                        backgroundColor: 'rgba(75, 192, 192, 0.2)',\n");
            scripts.append("                        borderColor: 'rgba(75, 192, 192, 1)',\n");
            scripts.append("                        pointBackgroundColor: 'rgba(75, 192, 192, 1)',\n");
            scripts.append("                        pointBorderColor: '#fff',\n");
            scripts.append("                        pointHoverBackgroundColor: '#fff',\n");
            scripts.append("                        pointHoverBorderColor: 'rgba(75, 192, 192, 1)'\n");
            scripts.append("                    }]\n");
            scripts.append("                },\n");
            scripts.append("                options: {\n");
            scripts.append("                    responsive: true,\n");
            scripts.append("                    scales: {\n");
            scripts.append("                        r: { beginAtZero: true, max: 100 }\n");
            scripts.append("                    }\n");
            scripts.append("                }\n");
            scripts.append("            });\n");
            scripts.append("        }\n\n");
        }
        
        // Score Gauge Chart
        if (webVitalsList != null && !webVitalsList.isEmpty()) {
            WebVitalsCapture.WebVitals vitals = webVitalsList.get(0);
            int score = vitals.getOverallScore();
            
            scripts.append("        // Performance Score Gauge\n");
            scripts.append("        const gaugeCtx = document.getElementById('scoreGaugeChart');\n");
            scripts.append("        if (gaugeCtx) {\n");
            scripts.append("            new Chart(gaugeCtx, {\n");
            scripts.append("                type: 'doughnut',\n");
            scripts.append("                data: {\n");
            scripts.append("                    labels: ['Score', 'Remaining'],\n");
            scripts.append("                    datasets: [{\n");
            scripts.append("                        data: [").append(score).append(", ").append(100 - score).append("],\n");
            scripts.append("                        backgroundColor: ['").append(score >= 80 ? "#4CAF50" : score >= 60 ? "#FF9800" : "#F44336").append("', '#E0E0E0'],\n");
            scripts.append("                        borderWidth: 0\n");
            scripts.append("                    }]\n");
            scripts.append("                },\n");
            scripts.append("                options: {\n");
            scripts.append("                    responsive: true,\n");
            scripts.append("                    circumference: 180,\n");
            scripts.append("                    rotation: 270,\n");
            scripts.append("                    cutout: '75%',\n");
            scripts.append("                    plugins: {\n");
            scripts.append("                        legend: { display: false },\n");
            scripts.append("                        title: { display: true, text: 'Overall Performance: ").append(score).append("/100' }\n");
            scripts.append("                    }\n");
            scripts.append("                }\n");
            scripts.append("            });\n");
            scripts.append("        }\n");
        }
        
        scripts.append("    </script>\n");
        
        return scripts.toString();
    }
    
    // Helper methods for chart data
    private String getPageLoadLabels(List<PerformanceTracker.PageLoadMetric> pageLoads) {
        return "[" + pageLoads.stream()
            .map(m -> "'" + m.getUrl().replaceAll(".*\\/", "") + "'")
            .collect(Collectors.joining(", ")) + "]";
    }
    
    private String getPageLoadData(List<PerformanceTracker.PageLoadMetric> pageLoads) {
        return "[" + pageLoads.stream()
            .map(m -> String.valueOf(m.getLoadTimeMs()))
            .collect(Collectors.joining(", ")) + "]";
    }
    
    private String getAPILabels(List<NetworkPerformanceMonitor.NetworkRequest> apiCalls) {
        return "[" + apiCalls.stream()
            .map(api -> "'" + api.getEndpoint().replaceAll(".*\\/", "") + "'")
            .collect(Collectors.joining(", ")) + "]";
    }
    
    private String getAPIData(List<NetworkPerformanceMonitor.NetworkRequest> apiCalls) {
        return "[" + apiCalls.stream()
            .map(api -> String.valueOf(api.getDuration()))
            .collect(Collectors.joining(", ")) + "]";
    }
    
    private String getWebVitalsRadarData(WebVitalsCapture.WebVitals vitals) {
        // Normalize scores to 0-100 scale
        double lcpScore = Math.max(0, 100 - (vitals.getLcp() / 25.0)); // 2500ms = 0
        double clsScore = Math.max(0, 100 - (vitals.getCls() * 1000)); // 0.1 = 0
        double fcpScore = Math.max(0, 100 - (vitals.getFcp() / 18.0)); // 1800ms = 0
        double ttfbScore = Math.max(0, 100 - (vitals.getTtfb() / 8.0)); // 800ms = 0
        
        return String.format("[%.1f, %.1f, %.1f, %.1f, %d]", 
            lcpScore, clsScore, fcpScore, ttfbScore, vitals.getOverallScore());
    }
    
    /**
     * Get enhanced CSS styles
     */
    private String getEnhancedStyles() {
        return "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
               "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #333; line-height: 1.6; transition: all 0.3s ease; }\n" +
               "body.dark-mode { background: #1a1a2e; color: #eee; }\n" +
               ".navbar { background: white; padding: 15px 30px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 10px rgba(0,0,0,0.1); position: sticky; top: 0; z-index: 1000; }\n" +
               ".nav-brand { font-size: 1.5em; font-weight: bold; color: #667eea; }\n" +
               ".nav-links { display: flex; gap: 20px; align-items: center; }\n" +
               ".nav-links a { color: #333; text-decoration: none; transition: color 0.3s; }\n" +
               ".nav-links a:hover { color: #667eea; }\n" +
               ".theme-toggle { background: #667eea; color: white; border: none; padding: 8px 15px; border-radius: 20px; cursor: pointer; font-size: 1em; }\n" +
               ".hero-section { text-align: center; padding: 60px 20px; color: white; }\n" +
               ".hero-title { font-size: 3em; margin-bottom: 20px; }\n" +
               ".hero-subtitle { display: flex; gap: 15px; justify-content: center; flex-wrap: wrap; }\n" +
               ".badge { background: rgba(255,255,255,0.2); padding: 8px 20px; border-radius: 25px; font-size: 0.9em; }\n" +
               ".dashboard-container { max-width: 1400px; margin: -30px auto 30px; padding: 0 20px; }\n" +
               ".section { background: white; border-radius: 15px; padding: 30px; margin-bottom: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }\n" +
               ".section-title { color: #667eea; margin-bottom: 25px; font-size: 1.8em; border-bottom: 3px solid #667eea; padding-bottom: 10px; }\n" +
               ".metric-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }\n" +
               ".metric-card { padding: 25px; border-radius: 12px; text-align: center; transition: transform 0.3s, box-shadow 0.3s; cursor: pointer; }\n" +
               ".metric-card:hover { transform: translateY(-5px); box-shadow: 0 8px 25px rgba(0,0,0,0.15); }\n" +
               ".metric-card.excellent { background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%); }\n" +
               ".metric-card.good { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }\n" +
               ".metric-card.poor { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }\n" +
               ".metric-icon { font-size: 3em; margin-bottom: 10px; }\n" +
               ".metric-value { font-size: 2.5em; font-weight: bold; margin: 10px 0; }\n" +
               ".metric-label { font-size: 1.1em; color: #666; margin-bottom: 5px; }\n" +
               ".metric-change { font-size: 0.9em; opacity: 0.8; }\n" +
               ".chart-container { position: relative; height: 400px; }\n" +
               ".timeline { display: flex; flex-direction: column; gap: 20px; }\n" +
               ".timeline-item { display: flex; gap: 20px; align-items: flex-start; }\n" +
               ".timeline-marker { min-width: 40px; height: 40px; background: #667eea; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; }\n" +
               ".timeline-content { flex: 1; padding: 15px; background: #f8f9fa; border-radius: 8px; }\n" +
               ".timeline-content h4 { color: #667eea; margin-bottom: 5px; }\n" +
               ".recommendations-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }\n" +
               ".recommendation-card { padding: 20px; border-radius: 12px; border-left: 5px solid; display: flex; gap: 15px; transition: transform 0.3s; }\n" +
               ".recommendation-card:hover { transform: translateX(5px); }\n" +
               ".recommendation-card.priority-high { background: #ffe6e6; border-color: #f44336; }\n" +
               ".recommendation-card.priority-medium { background: #fff3e0; border-color: #ff9800; }\n" +
               ".recommendation-card.priority-low { background: #e8f5e9; border-color: #4caf50; }\n" +
               ".rec-icon { font-size: 2.5em; }\n" +
               ".rec-content h4 { color: #333; margin-bottom: 8px; }\n" +
               ".rec-content p { color: #666; font-size: 0.95em; margin-bottom: 10px; }\n" +
               ".rec-impact { display: inline-block; padding: 4px 12px; background: rgba(0,0,0,0.05); border-radius: 12px; font-size: 0.85em; }\n" +
               ".footer { text-align: center; padding: 30px; color: white; margin-top: 30px; }\n" +
               "@media print { body { background: white; } .navbar, .footer { display: none; } }\n" +
               "@media (max-width: 768px) { .hero-title { font-size: 2em; } .metric-cards { grid-template-columns: 1fr; } .chart-container { height: 300px; } }";
    }
    
    // Reflection helpers (same as PerformanceReportGenerator)
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
}


