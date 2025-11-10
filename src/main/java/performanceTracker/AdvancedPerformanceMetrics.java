package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import base.DriverManager;

import java.util.*;

/**
 * Advanced Performance Metrics Collector
 * 
 * Captures cutting-edge performance metrics:
 * - FID (First Input Delay) - User interaction responsiveness
 * - INP (Interaction to Next Paint) - Overall interactivity (NEW 2024 Core Web Vital)
 * - CLS (Cumulative Layout Shift) - Visual stability
 * - Performance Observer API - Real-time performance monitoring
 * - Long Tasks API - Identifies blocking operations
 * - Resource Timing API - Detailed resource performance
 * - Navigation Timing API - Complete navigation breakdown
 * 
 * Google's Core Web Vitals (2024):
 * - LCP (Largest Contentful Paint) - Already implemented
 * - INP (Interaction to Next Paint) - NEW! Replaces FID
 * - CLS (Cumulative Layout Shift) - Already implemented
 * 
 * Usage:
 * <pre>
 * AdvancedPerformanceMetrics metrics = new AdvancedPerformanceMetrics(driver);
 * AdvancedMetrics result = metrics.captureAdvancedMetrics();
 * result.printSummary();
 * </pre>
 */
public class AdvancedPerformanceMetrics {
    
    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final Gson gson;
    
    public AdvancedPerformanceMetrics() {
    	WebDriver driver = DriverManager.getDriver();
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.gson = new Gson();
    }
    
    /**
     * Capture all advanced performance metrics
     */
    public AdvancedMetrics captureAdvancedMetrics() {
        AdvancedMetrics metrics = new AdvancedMetrics();
        
        try {
            // Capture FID (First Input Delay)
            metrics.fid = captureFID();
            
            // Capture INP (Interaction to Next Paint) - NEW Core Web Vital
            metrics.inp = captureINP();
            
            // Capture Long Tasks (blocking operations)
            metrics.longTasks = captureLongTasks();
            
            // Capture Resource Timing
            metrics.resourceTiming = captureResourceTiming();
            
            // Capture Navigation Timing (detailed breakdown)
            metrics.navigationTiming = captureNavigationTiming();
            
            // Capture Paint Timing
            metrics.paintTiming = capturePaintTiming();
            
            // Capture Layout Shift Events
            metrics.layoutShifts = captureLayoutShifts();
            
            // Capture Event Timing (user interactions)
            metrics.eventTiming = captureEventTiming();
            
            metrics.url = driver.getCurrentUrl();
            metrics.timestamp = System.currentTimeMillis();
            
        } catch (Exception e) {
            System.err.println("⚠️  Error capturing advanced metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * Capture FID (First Input Delay)
     * Measures time from user's first interaction to browser response
     */
    private Double captureFID() {
        try {
            String script = 
                "return new Promise((resolve) => {" +
                "  if (window.fidValue !== undefined) {" +
                "    resolve(window.fidValue);" +
                "    return;" +
                "  }" +
                "  " +
                "  new PerformanceObserver((list) => {" +
                "    for (const entry of list.getEntries()) {" +
                "      if (entry.name === 'first-input') {" +
                "        const fid = entry.processingStart - entry.startTime;" +
                "        window.fidValue = fid;" +
                "        resolve(fid);" +
                "      }" +
                "    }" +
                "  }).observe({type: 'first-input', buffered: true});" +
                "  " +
                "  setTimeout(() => resolve(null), 1000);" +
                "});";
            
            Object result = js.executeAsyncScript(script);
            
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            } else if (result instanceof Long) {
                return ((Long) result).doubleValue();
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  FID capture failed: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Capture INP (Interaction to Next Paint)
     * NEW Core Web Vital (2024) - Replaces FID
     * Measures overall responsiveness throughout page lifecycle
     */
    private Double captureINP() {
        try {
            String script = 
                "return new Promise((resolve) => {" +
                "  if (window.inpValue !== undefined) {" +
                "    resolve(window.inpValue);" +
                "    return;" +
                "  }" +
                "  " +
                "  let worstINP = 0;" +
                "  " +
                "  new PerformanceObserver((list) => {" +
                "    for (const entry of list.getEntries()) {" +
                "      if (entry.interactionId) {" +
                "        const inp = entry.processingEnd - entry.startTime;" +
                "        worstINP = Math.max(worstINP, inp);" +
                "      }" +
                "    }" +
                "  }).observe({type: 'event', buffered: true, durationThreshold: 16});" +
                "  " +
                "  setTimeout(() => {" +
                "    window.inpValue = worstINP;" +
                "    resolve(worstINP > 0 ? worstINP : null);" +
                "  }, 2000);" +
                "});";
            
            Object result = js.executeAsyncScript(script);
            
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            } else if (result instanceof Long) {
                return ((Long) result).doubleValue();
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  INP capture failed: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Capture Long Tasks (blocking operations > 50ms)
     * Identifies performance bottlenecks
     */
    private List<LongTask> captureLongTasks() {
        List<LongTask> longTasks = new ArrayList<>();
        
        try {
            String script = 
                "const longTasks = [];" +
                "if (window.performance && window.performance.getEntriesByType) {" +
                "  const entries = window.performance.getEntriesByType('longtask');" +
                "  entries.forEach(task => {" +
                "    longTasks.push({" +
                "      name: task.name," +
                "      duration: task.duration," +
                "      startTime: task.startTime" +
                "    });" +
                "  });" +
                "}" +
                "return JSON.stringify(longTasks);";
            
            Object result = js.executeScript(script);
            
            if (result instanceof String) {
                String json = (String) result;
                if (json != null && !json.equals("[]")) {
                    // Parse JSON array
                    com.google.gson.JsonArray array = gson.fromJson(json, com.google.gson.JsonArray.class);
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject obj = array.get(i).getAsJsonObject();
                        LongTask task = new LongTask();
                        task.name = obj.has("name") ? obj.get("name").getAsString() : "unknown";
                        task.duration = obj.has("duration") ? obj.get("duration").getAsDouble() : 0;
                        task.startTime = obj.has("startTime") ? obj.get("startTime").getAsDouble() : 0;
                        longTasks.add(task);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  Long Tasks capture failed: " + e.getMessage());
        }
        
        return longTasks;
    }
    
    /**
     * Capture Resource Timing (detailed resource performance)
     */
    private ResourceTimingSummary captureResourceTiming() {
        ResourceTimingSummary summary = new ResourceTimingSummary();
        
        try {
            String script = 
                "const resources = window.performance.getEntriesByType('resource');" +
                "const summary = {" +
                "  totalResources: resources.length," +
                "  scripts: 0, stylesheets: 0, images: 0, fonts: 0, xhr: 0," +
                "  totalDuration: 0, avgDuration: 0," +
                "  slowestResource: { name: '', duration: 0 }" +
                "};" +
                "resources.forEach(r => {" +
                "  summary.totalDuration += r.duration;" +
                "  if (r.duration > summary.slowestResource.duration) {" +
                "    summary.slowestResource = { name: r.name, duration: r.duration };" +
                "  }" +
                "  if (r.initiatorType === 'script') summary.scripts++;" +
                "  else if (r.initiatorType === 'css' || r.initiatorType === 'link') summary.stylesheets++;" +
                "  else if (r.initiatorType === 'img') summary.images++;" +
                "  else if (r.initiatorType === 'font') summary.fonts++;" +
                "  else if (r.initiatorType === 'xmlhttprequest' || r.initiatorType === 'fetch') summary.xhr++;" +
                "});" +
                "summary.avgDuration = summary.totalResources > 0 ? summary.totalDuration / summary.totalResources : 0;" +
                "return JSON.stringify(summary);";
            
            Object result = js.executeScript(script);
            
            if (result instanceof String) {
                JsonObject obj = gson.fromJson((String) result, JsonObject.class);
                summary.totalResources = obj.has("totalResources") ? obj.get("totalResources").getAsInt() : 0;
                summary.scripts = obj.has("scripts") ? obj.get("scripts").getAsInt() : 0;
                summary.stylesheets = obj.has("stylesheets") ? obj.get("stylesheets").getAsInt() : 0;
                summary.images = obj.has("images") ? obj.get("images").getAsInt() : 0;
                summary.fonts = obj.has("fonts") ? obj.get("fonts").getAsInt() : 0;
                summary.xhr = obj.has("xhr") ? obj.get("xhr").getAsInt() : 0;
                summary.avgDuration = obj.has("avgDuration") ? obj.get("avgDuration").getAsDouble() : 0;
                
                if (obj.has("slowestResource")) {
                    JsonObject slowest = obj.getAsJsonObject("slowestResource");
                    summary.slowestResourceName = slowest.has("name") ? slowest.get("name").getAsString() : "";
                    summary.slowestResourceDuration = slowest.has("duration") ? slowest.get("duration").getAsDouble() : 0;
                }
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  Resource Timing capture failed: " + e.getMessage());
        }
        
        return summary;
    }
    
    /**
     * Capture Navigation Timing (detailed page load breakdown)
     */
    private NavigationTiming captureNavigationTiming() {
        NavigationTiming timing = new NavigationTiming();
        
        try {
            String script = 
                "const perf = window.performance.getEntriesByType('navigation')[0];" +
                "if (perf) {" +
                "  return JSON.stringify({" +
                "    redirectTime: perf.redirectEnd - perf.redirectStart," +
                "    dnsTime: perf.domainLookupEnd - perf.domainLookupStart," +
                "    tcpTime: perf.connectEnd - perf.connectStart," +
                "    requestTime: perf.responseStart - perf.requestStart," +
                "    responseTime: perf.responseEnd - perf.responseStart," +
                "    domProcessingTime: perf.domComplete - perf.domInteractive," +
                "    loadCompleteTime: perf.loadEventEnd - perf.loadEventStart," +
                "    totalTime: perf.loadEventEnd - perf.fetchStart" +
                "  });" +
                "}" +
                "return null;";
            
            Object result = js.executeScript(script);
            
            if (result instanceof String) {
                JsonObject obj = gson.fromJson((String) result, JsonObject.class);
                timing.redirectTime = obj.has("redirectTime") ? obj.get("redirectTime").getAsDouble() : 0;
                timing.dnsTime = obj.has("dnsTime") ? obj.get("dnsTime").getAsDouble() : 0;
                timing.tcpTime = obj.has("tcpTime") ? obj.get("tcpTime").getAsDouble() : 0;
                timing.requestTime = obj.has("requestTime") ? obj.get("requestTime").getAsDouble() : 0;
                timing.responseTime = obj.has("responseTime") ? obj.get("responseTime").getAsDouble() : 0;
                timing.domProcessingTime = obj.has("domProcessingTime") ? obj.get("domProcessingTime").getAsDouble() : 0;
                timing.loadCompleteTime = obj.has("loadCompleteTime") ? obj.get("loadCompleteTime").getAsDouble() : 0;
                timing.totalTime = obj.has("totalTime") ? obj.get("totalTime").getAsDouble() : 0;
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  Navigation Timing capture failed: " + e.getMessage());
        }
        
        return timing;
    }
    
    /**
     * Capture Paint Timing (FP, FCP)
     */
    private PaintTiming capturePaintTiming() {
        PaintTiming timing = new PaintTiming();
        
        try {
            String script = 
                "const paintEntries = window.performance.getEntriesByType('paint');" +
                "const result = {};" +
                "paintEntries.forEach(entry => {" +
                "  if (entry.name === 'first-paint') result.fp = entry.startTime;" +
                "  if (entry.name === 'first-contentful-paint') result.fcp = entry.startTime;" +
                "});" +
                "return JSON.stringify(result);";
            
            Object result = js.executeScript(script);
            
            if (result instanceof String) {
                JsonObject obj = gson.fromJson((String) result, JsonObject.class);
                timing.firstPaint = obj.has("fp") ? obj.get("fp").getAsDouble() : null;
                timing.firstContentfulPaint = obj.has("fcp") ? obj.get("fcp").getAsDouble() : null;
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  Paint Timing capture failed: " + e.getMessage());
        }
        
        return timing;
    }
    
    /**
     * Capture Layout Shift Events (detailed CLS analysis)
     */
    private List<LayoutShiftEvent> captureLayoutShifts() {
        List<LayoutShiftEvent> shifts = new ArrayList<>();
        
        try {
            String script = 
                "const shifts = [];" +
                "if (window.performance && window.performance.getEntriesByType) {" +
                "  const entries = window.performance.getEntriesByType('layout-shift');" +
                "  entries.forEach(entry => {" +
                "    if (!entry.hadRecentInput) {" +  // Only count shifts not caused by user input
                "      shifts.push({" +
                "        value: entry.value," +
                "        startTime: entry.startTime," +
                "        hadRecentInput: entry.hadRecentInput" +
                "      });" +
                "    }" +
                "  });" +
                "}" +
                "return JSON.stringify(shifts);";
            
            Object result = js.executeScript(script);
            
            if (result instanceof String) {
                String json = (String) result;
                if (json != null && !json.equals("[]")) {
                    com.google.gson.JsonArray array = gson.fromJson(json, com.google.gson.JsonArray.class);
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject obj = array.get(i).getAsJsonObject();
                        LayoutShiftEvent shift = new LayoutShiftEvent();
                        shift.value = obj.has("value") ? obj.get("value").getAsDouble() : 0;
                        shift.startTime = obj.has("startTime") ? obj.get("startTime").getAsDouble() : 0;
                        shift.hadRecentInput = obj.has("hadRecentInput") && obj.get("hadRecentInput").getAsBoolean();
                        shifts.add(shift);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  Layout Shifts capture failed: " + e.getMessage());
        }
        
        return shifts;
    }
    
    /**
     * Capture Event Timing (user interactions)
     */
    private List<EventTimingEntry> captureEventTiming() {
        List<EventTimingEntry> events = new ArrayList<>();
        
        try {
            String script = 
                "const events = [];" +
                "if (window.performance && window.performance.getEntriesByType) {" +
                "  const entries = window.performance.getEntriesByType('event');" +
                "  entries.forEach(entry => {" +
                "    events.push({" +
                "      name: entry.name," +
                "      duration: entry.duration," +
                "      startTime: entry.startTime," +
                "      processingStart: entry.processingStart," +
                "      processingEnd: entry.processingEnd," +
                "      interactionId: entry.interactionId" +
                "    });" +
                "  });" +
                "}" +
                "return JSON.stringify(events);";
            
            Object result = js.executeScript(script);
            
            if (result instanceof String) {
                String json = (String) result;
                if (json != null && !json.equals("[]")) {
                    com.google.gson.JsonArray array = gson.fromJson(json, com.google.gson.JsonArray.class);
                    for (int i = 0; i < Math.min(array.size(), 20); i++) { // Limit to 20 events
                        JsonObject obj = array.get(i).getAsJsonObject();
                        EventTimingEntry event = new EventTimingEntry();
                        event.name = obj.has("name") ? obj.get("name").getAsString() : "";
                        event.duration = obj.has("duration") ? obj.get("duration").getAsDouble() : 0;
                        event.startTime = obj.has("startTime") ? obj.get("startTime").getAsDouble() : 0;
                        events.add(event);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("   ⚠️  Event Timing capture failed: " + e.getMessage());
        }
        
        return events;
    }
    
    /**
     * Advanced Metrics Model
     */
    public static class AdvancedMetrics {
        public String url;
        public long timestamp;
        public Double fid;  // First Input Delay
        public Double inp;  // Interaction to Next Paint (NEW Core Web Vital 2024)
        public List<LongTask> longTasks;
        public ResourceTimingSummary resourceTiming;
        public NavigationTiming navigationTiming;
        public PaintTiming paintTiming;
        public List<LayoutShiftEvent> layoutShifts;
        public List<EventTimingEntry> eventTiming;
        
        public AdvancedMetrics() {
            this.longTasks = new ArrayList<>();
            this.layoutShifts = new ArrayList<>();
            this.eventTiming = new ArrayList<>();
            this.resourceTiming = new ResourceTimingSummary();
            this.navigationTiming = new NavigationTiming();
            this.paintTiming = new PaintTiming();
        }
        
        /**
         * Check if INP is good (< 200ms), needs improvement (200-500ms), or poor (> 500ms)
         */
        public String getINPRating() {
            if (inp == null || inp == 0) return "Not Available";
            if (inp < 200) return "Good";
            if (inp < 500) return "Needs Improvement";
            return "Poor";
        }
        
        /**
         * Check if FID is good (< 100ms), needs improvement (100-300ms), or poor (> 300ms)
         */
        public String getFIDRating() {
            if (fid == null || fid == 0) return "Not Available";
            if (fid < 100) return "Good";
            if (fid < 300) return "Needs Improvement";
            return "Poor";
        }
        
        /**
         * Get total CLS from layout shift events
         */
        public double getTotalCLS() {
            return layoutShifts.stream()
                .filter(shift -> !shift.hadRecentInput)
                .mapToDouble(shift -> shift.value)
                .sum();
        }
        
        /**
         * Check if has long tasks (blocking operations)
         */
        public boolean hasLongTasks() {
            return longTasks != null && !longTasks.isEmpty();
        }
        
        /**
         * Get total long task time
         */
        public double getTotalLongTaskTime() {
            return longTasks.stream()
                .mapToDouble(task -> task.duration)
                .sum();
        }
        
        /**
         * Print comprehensive summary
         */
        public void printSummary() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🚀 ADVANCED PERFORMANCE METRICS");
            System.out.println("=".repeat(80));
            System.out.println("URL: " + url);
            
            // Core Web Vitals 2024
            System.out.println("\n📊 CORE WEB VITALS (2024 Google Standards):");
            if (inp != null && inp > 0) {
                System.out.println("   🎯 INP (Interaction to Next Paint): " + String.format("%.0f", inp) + "ms [" + getINPRating() + "] 🆕");
            } else {
                System.out.println("   🎯 INP (Interaction to Next Paint): Not available yet");
            }
            
            if (fid != null && fid > 0) {
                System.out.println("   ⚡ FID (First Input Delay): " + String.format("%.0f", fid) + "ms [" + getFIDRating() + "]");
            } else {
                System.out.println("   ⚡ FID (First Input Delay): Not triggered (no user interaction yet)");
            }
            
            // Paint Timing
            if (paintTiming.firstPaint != null || paintTiming.firstContentfulPaint != null) {
                System.out.println("\n🎨 PAINT TIMING:");
                if (paintTiming.firstPaint != null) {
                    System.out.println("   🖼️  First Paint (FP): " + String.format("%.0f", paintTiming.firstPaint) + "ms");
                }
                if (paintTiming.firstContentfulPaint != null) {
                    System.out.println("   🎨 First Contentful Paint (FCP): " + String.format("%.0f", paintTiming.firstContentfulPaint) + "ms");
                }
            }
            
            // Layout Shifts
            if (!layoutShifts.isEmpty()) {
                System.out.println("\n📐 LAYOUT SHIFTS:");
                System.out.println("   Total CLS Score: " + String.format("%.3f", getTotalCLS()));
                System.out.println("   Shift Events: " + layoutShifts.size());
            }
            
            // Long Tasks
            if (hasLongTasks()) {
                System.out.println("\n⏱️  LONG TASKS (Blocking Operations > 50ms):");
                System.out.println("   Count: " + longTasks.size());
                System.out.println("   Total Blocking Time: " + String.format("%.0f", getTotalLongTaskTime()) + "ms");
                
                if (longTasks.size() <= 5) {
                    for (LongTask task : longTasks) {
                        System.out.println("   ⚠️  " + task.name + " - " + String.format("%.0f", task.duration) + "ms");
                    }
                } else {
                    System.out.println("   ⚠️  WARNING: " + longTasks.size() + " long tasks detected!");
                }
            }
            
            // Resource Timing
            if (resourceTiming.totalResources > 0) {
                System.out.println("\n📦 RESOURCE TIMING:");
                System.out.println("   Total Resources: " + resourceTiming.totalResources);
                System.out.println("   Scripts: " + resourceTiming.scripts + 
                                 " | Stylesheets: " + resourceTiming.stylesheets +
                                 " | Images: " + resourceTiming.images +
                                 " | XHR: " + resourceTiming.xhr);
                System.out.println("   Avg Load Time: " + String.format("%.0f", resourceTiming.avgDuration) + "ms");
                if (resourceTiming.slowestResourceDuration > 0) {
                    System.out.println("   Slowest: " + String.format("%.0f", resourceTiming.slowestResourceDuration) + "ms");
                }
            }
            
            // Navigation Timing
            if (navigationTiming.totalTime > 0) {
                System.out.println("\n🔄 NAVIGATION TIMING BREAKDOWN:");
                System.out.println("   DNS Lookup: " + String.format("%.0f", navigationTiming.dnsTime) + "ms");
                System.out.println("   TCP Connection: " + String.format("%.0f", navigationTiming.tcpTime) + "ms");
                System.out.println("   Request Time: " + String.format("%.0f", navigationTiming.requestTime) + "ms");
                System.out.println("   Response Time: " + String.format("%.0f", navigationTiming.responseTime) + "ms");
                System.out.println("   DOM Processing: " + String.format("%.0f", navigationTiming.domProcessingTime) + "ms");
                System.out.println("   Total Load Time: " + String.format("%.0f", navigationTiming.totalTime) + "ms");
            }
            
            // Event Timing
            if (!eventTiming.isEmpty()) {
                System.out.println("\n🖱️  USER INTERACTIONS:");
                System.out.println("   Total Events: " + eventTiming.size());
                
                double avgEventDuration = eventTiming.stream()
                    .mapToDouble(e -> e.duration)
                    .average()
                    .orElse(0);
                System.out.println("   Avg Interaction Time: " + String.format("%.0f", avgEventDuration) + "ms");
                
                // Show slow events
                List<EventTimingEntry> slowEvents = eventTiming.stream()
                    .filter(e -> e.duration > 100)
                    .sorted((a, b) -> Double.compare(b.duration, a.duration))
                    .limit(5)
                    .toList();
                
                if (!slowEvents.isEmpty()) {
                    System.out.println("   Slow Interactions (>100ms):");
                    for (EventTimingEntry event : slowEvents) {
                        System.out.println("      ⚠️  " + event.name + " - " + String.format("%.0f", event.duration) + "ms");
                    }
                }
            }
            
            System.out.println("=".repeat(80) + "\n");
        }
        
        /**
         * Get comprehensive summary for reporting
         */
        public String getCompactSummary() {
            StringBuilder summary = new StringBuilder();
            
            if (inp != null && inp > 0) {
                summary.append("INP: ").append(String.format("%.0f", inp)).append("ms ").append(getINPEmoji());
            }
            if (fid != null && fid > 0) {
                if (summary.length() > 0) summary.append(" | ");
                summary.append("FID: ").append(String.format("%.0f", fid)).append("ms ").append(getFIDEmoji());
            }
            if (hasLongTasks()) {
                if (summary.length() > 0) summary.append(" | ");
                summary.append("Long Tasks: ").append(longTasks.size());
            }
            
            return summary.toString();
        }
        
        private String getINPEmoji() {
            String rating = getINPRating();
            return switch (rating) {
                case "Good" -> "✅";
                case "Needs Improvement" -> "⚠️";
                case "Poor" -> "❌";
                default -> "ℹ️";
            };
        }
        
        private String getFIDEmoji() {
            String rating = getFIDRating();
            return switch (rating) {
                case "Good" -> "✅";
                case "Needs Improvement" -> "⚠️";
                case "Poor" -> "❌";
                default -> "ℹ️";
            };
        }
    }
    
    /**
     * Long Task Model
     */
    public static class LongTask {
        public String name;
        public double duration;
        public double startTime;
    }
    
    /**
     * Resource Timing Summary Model
     */
    public static class ResourceTimingSummary {
        public int totalResources;
        public int scripts;
        public int stylesheets;
        public int images;
        public int fonts;
        public int xhr;
        public double avgDuration;
        public String slowestResourceName;
        public double slowestResourceDuration;
    }
    
    /**
     * Navigation Timing Model
     */
    public static class NavigationTiming {
        public double redirectTime;
        public double dnsTime;
        public double tcpTime;
        public double requestTime;
        public double responseTime;
        public double domProcessingTime;
        public double loadCompleteTime;
        public double totalTime;
    }
    
    /**
     * Paint Timing Model
     */
    public static class PaintTiming {
        public Double firstPaint;
        public Double firstContentfulPaint;
    }
    
    /**
     * Layout Shift Event Model
     */
    public static class LayoutShiftEvent {
        public double value;
        public double startTime;
        public boolean hadRecentInput;
    }
    
    /**
     * Event Timing Entry Model
     */
    public static class EventTimingEntry {
        public String name;
        public double duration;
        public double startTime;
    }
}


