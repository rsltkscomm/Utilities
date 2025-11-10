package performanceTracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Network Waterfall Visualization
 * 
 * Generates interactive HTML waterfall chart showing:
 * - Timeline of all network requests
 * - Parallel vs sequential loading
 * - Resource types (HTML, CSS, JS, API, images)
 * - Request/response phases
 * - Blocking resources identification
 * - Visual performance analysis
 * 
 * Similar to Chrome DevTools Network tab!
 */
public class NetworkWaterfallVisualizer {
    
    private final ConfigurationManager config;
    private final String reportsDir;
    
    public NetworkWaterfallVisualizer() {
        this.config = ConfigurationManager.getInstance();
        this.reportsDir = "./reports/";
        
        // Create reports directory if it doesn't exist
        File dir = new File(reportsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Generate waterfall visualization HTML
     */
    public File generateWaterfall(String testCaseKey, List<NetworkPerformanceMonitor.NetworkRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "network_waterfall_" + (testCaseKey != null ? testCaseKey + "_" : "") + timestamp + ".html";
            File waterfallFile = new File(reportsDir + fileName);
            
            String htmlContent = buildWaterfallHtml(testCaseKey, requests);
            
            try (FileWriter writer = new FileWriter(waterfallFile)) {
                writer.write(htmlContent);
            }
            
            System.out.println("🌊 Network Waterfall Visualization Generated: " + waterfallFile.getAbsolutePath());
            
            return waterfallFile;
            
        } catch (IOException e) {
            System.err.println("❌ Error generating waterfall visualization: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Build waterfall HTML content
     */
    private String buildWaterfallHtml(String testCaseKey, List<NetworkPerformanceMonitor.NetworkRequest> requests) {
        StringBuilder html = new StringBuilder();
        
        // Sort requests by start time
        List<NetworkPerformanceMonitor.NetworkRequest> sortedRequests = new ArrayList<>(requests);
        sortedRequests.sort(Comparator.comparingLong(NetworkPerformanceMonitor.NetworkRequest::getStartTime));
        
        // Find overall timeline boundaries
        long firstStart = sortedRequests.stream()
            .mapToLong(NetworkPerformanceMonitor.NetworkRequest::getStartTime)
            .min().orElse(0);
        long lastEnd = sortedRequests.stream()
            .mapToLong(r -> r.getStartTime() + r.getDuration())
            .max().orElse(0);
        long totalTime = lastEnd - firstStart;
        
        // HTML document start
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Network Waterfall - ").append(testCaseKey != null ? testCaseKey : "Test").append("</title>\n");
        html.append("    <style>\n");
        html.append(getWaterfallStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // Header
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>🌊 Network Waterfall Visualization</h1>\n");
        html.append("        <p class=\"subtitle\">Timeline of Network Requests</p>\n");
        html.append("    </div>\n");
        
        // Container
        html.append("    <div class=\"container\">\n");
        
        // Summary section
        html.append(buildSummarySection(testCaseKey, requests, totalTime));
        
        // Waterfall section
        html.append("        <div class=\"section\">\n");
        html.append("            <h2>🌊 Request Timeline</h2>\n");
        html.append("            <div class=\"waterfall-container\">\n");
        
        // Timeline header
        html.append("                <div class=\"timeline-header\">\n");
        html.append("                    <div class=\"timeline-label\">Request</div>\n");
        html.append("                    <div class=\"timeline-bars\">\n");
        html.append("                        <div class=\"time-markers\">\n");
        for (int i = 0; i <= 10; i++) {
            long time = (totalTime * i) / 10;
            html.append("                            <span>").append(time).append("ms</span>\n");
        }
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        
        // Timeline rows
        for (NetworkPerformanceMonitor.NetworkRequest request : sortedRequests) {
            html.append(buildWaterfallRow(request, firstStart, totalTime));
        }
        
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        // Legend section
        html.append(buildLegendSection());
        
        // Detailed table section
        html.append(buildDetailedTableSection(sortedRequests));
        
        html.append("    </div>\n");
        
        // Footer
        html.append("    <div class=\"footer\">\n");
        html.append("        <p>Generated on " + new SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss").format(new Date()) + "</p>\n");
        html.append("        <p>Network Waterfall Analyzer</p>\n");
        html.append("    </div>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    /**
     * Build summary section
     */
    private String buildSummarySection(String testCaseKey, List<NetworkPerformanceMonitor.NetworkRequest> requests, long totalTime) {
        StringBuilder section = new StringBuilder();
        
        // Group by type
        Map<String, Long> typeGroups = requests.stream()
            .collect(Collectors.groupingBy(
                NetworkPerformanceMonitor.NetworkRequest::getType,
                Collectors.counting()
            ));
        
        section.append("        <div class=\"section\">\n");
        section.append("            <h2>📊 Summary</h2>\n");
        section.append("            <div class=\"summary-grid\">\n");
        
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(requests.size()).append("</div>\n");
        section.append("                    <div class=\"summary-label\">Total Requests</div>\n");
        section.append("                </div>\n");
        
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(totalTime).append("ms</div>\n");
        section.append("                    <div class=\"summary-label\">Total Timeline</div>\n");
        section.append("                </div>\n");
        
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(typeGroups.getOrDefault("xhr", 0L)).append("</div>\n");
        section.append("                    <div class=\"summary-label\">API Calls</div>\n");
        section.append("                </div>\n");
        
        section.append("                <div class=\"summary-card\">\n");
        section.append("                    <div class=\"summary-value\">").append(typeGroups.getOrDefault("script", 0L)).append("</div>\n");
        section.append("                    <div class=\"summary-label\">Scripts</div>\n");
        section.append("                </div>\n");
        
        section.append("            </div>\n");
        section.append("        </div>\n");
        
        return section.toString();
    }
    
    /**
     * Build waterfall row for a single request
     */
    private String buildWaterfallRow(NetworkPerformanceMonitor.NetworkRequest request, long firstStart, long totalTime) {
        StringBuilder row = new StringBuilder();
        
        long relativeStart = request.getStartTime() - firstStart;
        long duration = request.getDuration();
        
        // Calculate percentages for positioning
        double startPercent = (relativeStart * 100.0) / totalTime;
        double widthPercent = (duration * 100.0) / totalTime;
        
        // Ensure minimum width for visibility
        widthPercent = Math.max(widthPercent, 0.5);
        
        String resourceType = request.getType();
        String barClass = getResourceTypeClass(resourceType);
        
        row.append("                <div class=\"timeline-row\">\n");
        row.append("                    <div class=\"timeline-label\" title=\"").append(request.getUrl()).append("\">\n");
        row.append("                        <span class=\"type-badge ").append(barClass).append("\">").append(resourceType.toUpperCase()).append("</span>\n");
        row.append("                        <span class=\"endpoint\">").append(request.getEndpoint()).append("</span>\n");
        row.append("                    </div>\n");
        row.append("                    <div class=\"timeline-bars\">\n");
        row.append("                        <div class=\"bar ").append(barClass).append("\" ");
        row.append("style=\"left: ").append(String.format("%.2f", startPercent)).append("%; ");
        row.append("width: ").append(String.format("%.2f", widthPercent)).append("%;\" ");
        row.append("title=\"Start: ").append(relativeStart).append("ms, Duration: ").append(duration).append("ms\">\n");
        row.append("                            <span class=\"duration-label\">").append(duration).append("ms</span>\n");
        row.append("                        </div>\n");
        row.append("                    </div>\n");
        row.append("                </div>\n");
        
        return row.toString();
    }
    
    /**
     * Build legend section
     */
    private String buildLegendSection() {
        return "        <div class=\"section legend-section\">\n" +
               "            <h2>📋 Legend</h2>\n" +
               "            <div class=\"legend-grid\">\n" +
               "                <div class=\"legend-item\"><span class=\"color-box xhr\"></span> XHR/API Calls</div>\n" +
               "                <div class=\"legend-item\"><span class=\"color-box script\"></span> JavaScript</div>\n" +
               "                <div class=\"legend-item\"><span class=\"color-box css\"></span> Stylesheets</div>\n" +
               "                <div class=\"legend-item\"><span class=\"color-box img\"></span> Images</div>\n" +
               "                <div class=\"legend-item\"><span class=\"color-box font\"></span> Fonts</div>\n" +
               "                <div class=\"legend-item\"><span class=\"color-box other\"></span> Other</div>\n" +
               "            </div>\n" +
               "        </div>\n";
    }
    
    /**
     * Build detailed table section
     */
    private String buildDetailedTableSection(List<NetworkPerformanceMonitor.NetworkRequest> requests) {
        StringBuilder section = new StringBuilder();
        
        section.append("        <div class=\"section\">\n");
        section.append("            <h2>📋 Detailed Request Data</h2>\n");
        section.append("            <table class=\"data-table\">\n");
        section.append("                <thead>\n");
        section.append("                    <tr>\n");
        section.append("                        <th>Type</th>\n");
        section.append("                        <th>URL</th>\n");
        section.append("                        <th>Start Time</th>\n");
        section.append("                        <th>Duration</th>\n");
        section.append("                        <th>Size</th>\n");
        section.append("                    </tr>\n");
        section.append("                </thead>\n");
        section.append("                <tbody>\n");
        
        long firstStart = requests.stream()
            .mapToLong(NetworkPerformanceMonitor.NetworkRequest::getStartTime)
            .min().orElse(0);
        
        for (NetworkPerformanceMonitor.NetworkRequest request : requests) {
            long relativeStart = request.getStartTime() - firstStart;
            
            section.append("                    <tr>\n");
            section.append("                        <td><span class=\"type-badge ").append(getResourceTypeClass(request.getType()))
                    .append("\">").append(request.getType()).append("</span></td>\n");
            section.append("                        <td class=\"url-cell\" title=\"").append(request.getUrl()).append("\">")
                    .append(truncateUrl(request.getUrl(), 60)).append("</td>\n");
            section.append("                        <td>").append(relativeStart).append("ms</td>\n");
            section.append("                        <td><strong>").append(request.getDuration()).append("ms</strong></td>\n");
            section.append("                        <td>").append(formatBytes(request.getTransferSize())).append("</td>\n");
            section.append("                    </tr>\n");
        }
        
        section.append("                </tbody>\n");
        section.append("            </table>\n");
        section.append("        </div>\n");
        
        return section.toString();
    }
    
    /**
     * Get CSS class for resource type
     */
    private String getResourceTypeClass(String type) {
        if (type == null) return "other";
        
        switch (type.toLowerCase()) {
            case "xhr":
            case "fetch":
                return "xhr";
            case "script":
                return "script";
            case "css":
            case "stylesheet":
                return "css";
            case "img":
            case "image":
                return "img";
            case "font":
                return "font";
            default:
                return "other";
        }
    }
    
    /**
     * Format bytes to human-readable
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String prefix = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), prefix);
    }
    
    /**
     * Truncate URL for display
     */
    private String truncateUrl(String url, int maxLength) {
        if (url == null || url.length() <= maxLength) {
            return url;
        }
        return url.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Get waterfall CSS styles
     */
    private String getWaterfallStyles() {
        return
            "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #333; line-height: 1.6; }\n" +
            ".header { background: white; padding: 40px 20px; text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
            ".header h1 { font-size: 2.5em; color: #667eea; margin-bottom: 10px; }\n" +
            ".subtitle { font-size: 1.2em; color: #666; }\n" +
            ".container { max-width: 1400px; margin: 30px auto; padding: 0 20px; }\n" +
            ".section { background: white; border-radius: 10px; padding: 30px; margin-bottom: 30px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }\n" +
            ".section h2 { color: #667eea; margin-bottom: 20px; font-size: 1.8em; border-bottom: 3px solid #667eea; padding-bottom: 10px; }\n" +
            ".summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }\n" +
            ".summary-card { padding: 25px; text-align: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }\n" +
            ".summary-value { font-size: 2.5em; font-weight: bold; margin-bottom: 10px; }\n" +
            ".summary-label { font-size: 1em; opacity: 0.9; }\n" +
            ".waterfall-container { background: #f8f9fa; border-radius: 8px; padding: 20px; overflow-x: auto; }\n" +
            ".timeline-header { display: flex; border-bottom: 2px solid #667eea; padding-bottom: 10px; margin-bottom: 10px; font-weight: bold; }\n" +
            ".timeline-row { display: flex; padding: 8px 0; border-bottom: 1px solid #e0e0e0; transition: background 0.2s; }\n" +
            ".timeline-row:hover { background: white; }\n" +
            ".timeline-label { width: 300px; padding-right: 20px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n" +
            ".timeline-bars { flex: 1; position: relative; height: 30px; }\n" +
            ".time-markers { display: flex; justify-content: space-between; font-size: 0.8em; color: #666; margin-bottom: 5px; }\n" +
            ".bar { position: absolute; height: 24px; border-radius: 4px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: transform 0.2s; }\n" +
            ".bar:hover { transform: scaleY(1.2); z-index: 10; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }\n" +
            ".duration-label { color: white; font-size: 0.85em; font-weight: bold; text-shadow: 0 1px 2px rgba(0,0,0,0.3); }\n" +
            ".type-badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 0.75em; font-weight: bold; color: white; margin-right: 8px; }\n" +
            ".endpoint { font-size: 0.9em; color: #333; }\n" +
            ".xhr { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }\n" +
            ".script { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }\n" +
            ".css { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }\n" +
            ".img { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }\n" +
            ".font { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }\n" +
            ".other { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); }\n" +
            ".legend-section { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 50%, #ffecd2 100%); }\n" +
            ".legend-grid { display: flex; flex-wrap: wrap; gap: 20px; justify-content: center; }\n" +
            ".legend-item { display: flex; align-items: center; gap: 10px; padding: 10px 20px; background: white; border-radius: 8px; }\n" +
            ".color-box { width: 30px; height: 20px; border-radius: 4px; }\n" +
            ".data-table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n" +
            ".data-table thead { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }\n" +
            ".data-table th { padding: 15px; text-align: left; font-weight: 600; }\n" +
            ".data-table td { padding: 12px 15px; border-bottom: 1px solid #e0e0e0; }\n" +
            ".data-table tbody tr:hover { background: #f8f9fa; }\n" +
            ".url-cell { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n" +
            ".footer { text-align: center; padding: 30px; color: white; font-size: 0.9em; margin-top: 30px; }\n" +
            "@media print { body { background: white; } }\n";
    }
}


