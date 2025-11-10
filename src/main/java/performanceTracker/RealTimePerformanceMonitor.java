package performanceTracker;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-Time Performance Monitor
 * 
 * Continuously monitors performance metrics and streams them to the dashboard
 * Features:
 * - Real-time metrics collection
 * - Performance alerting
 * - Historical data management
 * - Performance budget tracking
 * - System health monitoring
 */
@Component
public class RealTimePerformanceMonitor {
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    private final Map<String, List<PerformanceMetrics>> historicalData = new ConcurrentHashMap<>();
    private final Map<String, PerformanceMetrics> currentMetrics = new ConcurrentHashMap<>();
    private final Map<String, PerformanceAlert> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, PerformanceBudget> performanceBudgets = new ConcurrentHashMap<>();
    private final AtomicLong totalTests = new AtomicLong(0);
    private final AtomicLong completedTests = new AtomicLong(0);
    private final AtomicLong failedTests = new AtomicLong(0);
    
    /**
     * Scheduled task to collect and broadcast real-time metrics
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void collectAndBroadcastMetrics() {
        try {
            // Collect current performance metrics
            PerformanceMetrics metrics = collectCurrentMetrics();
            
            // Store in historical data
            String timestamp = String.valueOf(System.currentTimeMillis());
            historicalData.computeIfAbsent("global", k -> new ArrayList<>()).add(metrics);
            
            // Check for alerts
            checkPerformanceAlerts(metrics);
            
            // Broadcast to WebSocket clients
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/performance-metrics", metrics);
            }
            
            // Clean up old historical data (keep last 1000 entries)
            cleanupHistoricalData();
            
        } catch (Exception e) {
            System.err.println("Error in real-time performance monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Collect current performance metrics from various sources
     */
    private PerformanceMetrics collectCurrentMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // System metrics
        metrics.setTimestamp(System.currentTimeMillis());
        metrics.setActiveTests(getActiveTestCount());
        metrics.setCompletedTests((int) completedTests.get());
        metrics.setFailedTests((int) failedTests.get());
        
        // Performance metrics (simulated - in real implementation, collect from actual sources)
        metrics.setAveragePageLoadTime(getAveragePageLoadTime());
        metrics.setAverageApiResponseTime(getAverageApiResponseTime());
        metrics.setWebVitalsScore(getWebVitalsScore());
        metrics.setMemoryUsage(getMemoryUsage());
        metrics.setCpuUsage(getCpuUsage());
        metrics.setNetworkLatency(getNetworkLatency());
        
        // Performance budget compliance
        metrics.setBudgetCompliance(calculateBudgetCompliance());
        
        return metrics;
    }
    
    /**
     * Check for performance alerts and violations
     */
    private void checkPerformanceAlerts(PerformanceMetrics metrics) {
        for (Map.Entry<String, PerformanceBudget> entry : performanceBudgets.entrySet()) {
            String budgetName = entry.getKey();
            PerformanceBudget budget = entry.getValue();
            
            if (isBudgetViolated(budgetName, metrics, budget)) {
                createAlert(budgetName, metrics, budget);
            }
        }
    }
    
    /**
     * Check if a performance budget is violated
     */
    private boolean isBudgetViolated(String budgetName, PerformanceMetrics metrics, PerformanceBudget budget) {
        switch (budgetName.toLowerCase()) {
            case "page_load_time":
                return metrics.getAveragePageLoadTime() > budget.getThreshold();
            case "api_response_time":
                return metrics.getAverageApiResponseTime() > budget.getThreshold();
            case "web_vitals_score":
                return metrics.getWebVitalsScore() < budget.getThreshold();
            case "memory_usage":
                return metrics.getMemoryUsage() > budget.getThreshold();
            default:
                return false;
        }
    }
    
    /**
     * Create a performance alert
     */
    private void createAlert(String budgetName, PerformanceMetrics metrics, PerformanceBudget budget) {
        String alertId = budgetName + "_" + System.currentTimeMillis();
        PerformanceAlert alert = new PerformanceAlert();
        alert.setId(alertId);
        alert.setType("PERFORMANCE_BUDGET_VIOLATION");
        alert.setSeverity("WARNING");
        alert.setMessage(String.format("Performance budget violation: %s exceeded threshold %.2f (current: %.2f)", 
            budgetName, budget.getThreshold(), getMetricValue(budgetName, metrics)));
        alert.setTimestamp(System.currentTimeMillis());
        alert.setBudgetName(budgetName);
        alert.setCurrentValue(getMetricValue(budgetName, metrics));
        alert.setThreshold(budget.getThreshold());
        
        activeAlerts.put(alertId, alert);
        
        // Broadcast alert to WebSocket clients
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/performance-alerts", alert);
        }
        
        System.out.println("🚨 Performance Alert: " + alert.getMessage());
    }
    
    /**
     * Get metric value for a specific budget
     */
    private double getMetricValue(String budgetName, PerformanceMetrics metrics) {
        switch (budgetName.toLowerCase()) {
            case "page_load_time":
                return metrics.getAveragePageLoadTime();
            case "api_response_time":
                return metrics.getAverageApiResponseTime();
            case "web_vitals_score":
                return metrics.getWebVitalsScore();
            case "memory_usage":
                return metrics.getMemoryUsage();
            default:
                return 0.0;
        }
    }
    
    /**
     * Get current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        return collectCurrentMetrics();
    }
    
    /**
     * Get historical data for a test case
     */
    public List<PerformanceMetrics> getHistoricalData(String testCaseKey) {
        return historicalData.getOrDefault(testCaseKey, new ArrayList<>());
    }
    
    /**
     * Get active alerts
     */
    public Collection<PerformanceAlert> getActiveAlerts() {
        return activeAlerts.values();
    }
    
    /**
     * Configure performance alerts
     */
    public void configureAlerts(Map<String, Object> alertConfig) {
        // Implementation for alert configuration
        System.out.println("🔔 Performance alerts configured: " + alertConfig);
    }
    
    /**
     * Get performance budgets
     */
    public Map<String, Object> getPerformanceBudgets() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, PerformanceBudget> entry : performanceBudgets.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    /**
     * Update performance budgets
     */
    public void updatePerformanceBudgets(Map<String, Object> budgets) {
        for (Map.Entry<String, Object> entry : budgets.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> budgetData = (Map<String, Object>) entry.getValue();
                PerformanceBudget budget = new PerformanceBudget();
                budget.setName(entry.getKey());
                budget.setThreshold(Double.parseDouble(budgetData.get("threshold").toString()));
                budget.setDescription(budgetData.getOrDefault("description", "").toString());
                performanceBudgets.put(entry.getKey(), budget);
            }
        }
        System.out.println("📊 Performance budgets updated: " + budgets.keySet());
    }
    
    /**
     * Get system health status
     */
    public Map<String, Object> getSystemHealth() {
        return Map.of(
            "status", "HEALTHY",
            "uptime", System.currentTimeMillis(),
            "memoryUsage", getMemoryUsage(),
            "cpuUsage", getCpuUsage(),
            "activeAlerts", activeAlerts.size(),
            "totalTests", totalTests.get(),
            "successRate", calculateSuccessRate()
        );
    }
    
    /**
     * Get active tests count
     */
    public int getActiveTests() {
        return getActiveTestCount();
    }
    
    /**
     * Get completed tests count
     */
    public int getCompletedTests() {
        return (int) completedTests.get();
    }
    
    /**
     * Get failed tests count
     */
    public int getFailedTests() {
        return (int) failedTests.get();
    }
    
    /**
     * Increment test counters
     */
    public void incrementTestCounters(boolean success) {
        totalTests.incrementAndGet();
        if (success) {
            completedTests.incrementAndGet();
        } else {
            failedTests.incrementAndGet();
        }
    }
    
    // Helper methods for metric collection (simulated)
    private int getActiveTestCount() {
        return Math.max(0, (int) (totalTests.get() - completedTests.get() - failedTests.get()));
    }
    
    private double getAveragePageLoadTime() {
        return 1500 + (Math.random() * 1000); // Simulated: 1500-2500ms
    }
    
    private double getAverageApiResponseTime() {
        return 800 + (Math.random() * 600); // Simulated: 800-1400ms
    }
    
    private double getWebVitalsScore() {
        return 75 + (Math.random() * 20); // Simulated: 75-95
    }
    
    private double getMemoryUsage() {
        return 50 + (Math.random() * 30); // Simulated: 50-80%
    }
    
    private double getCpuUsage() {
        return 20 + (Math.random() * 40); // Simulated: 20-60%
    }
    
    private double getNetworkLatency() {
        return 100 + (Math.random() * 200); // Simulated: 100-300ms
    }
    
    private double calculateBudgetCompliance() {
        // Calculate percentage of budgets being met
        if (performanceBudgets.isEmpty()) return 100.0;
        
        int metBudgets = 0;
        PerformanceMetrics current = getCurrentMetrics();
        
        for (Map.Entry<String, PerformanceBudget> entry : performanceBudgets.entrySet()) {
            if (!isBudgetViolated(entry.getKey(), current, entry.getValue())) {
                metBudgets++;
            }
        }
        
        return (double) metBudgets / performanceBudgets.size() * 100;
    }
    
    private double calculateSuccessRate() {
        long total = totalTests.get();
        if (total == 0) return 100.0;
        return (double) completedTests.get() / total * 100;
    }
    
    private void cleanupHistoricalData() {
        for (Map.Entry<String, List<PerformanceMetrics>> entry : historicalData.entrySet()) {
            List<PerformanceMetrics> data = entry.getValue();
            if (data.size() > 1000) {
                // Keep only the last 1000 entries
                data.subList(0, data.size() - 1000).clear();
            }
        }
    }
}
