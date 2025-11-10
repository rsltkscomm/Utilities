package performanceTracker;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.network.model.ConnectionType;

import base.DriverManager;
import config.ConfigurationManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network Conditions Simulator
 * 
 * Simulates various network conditions for comprehensive performance testing:
 * - 3G/4G/5G network conditions
 * - Custom bandwidth and latency settings
 * - Network throttling and packet loss simulation
 * - Offline/online simulation
 * - Network quality degradation testing
 */
public class NetworkConditionsSimulator {
    
    private final WebDriver driver;
    private final ConfigurationManager config;
    private DevTools devTools;
    private final Map<String, NetworkCondition> predefinedConditions;
    private String currentCondition = "none";
    
    public NetworkConditionsSimulator() {
        this.driver = DriverManager.getDriver();
        this.config = ConfigurationManager.getInstance();
        this.predefinedConditions = initializePredefinedConditions();
        
        // Initialize DevTools if available
        if (driver instanceof HasDevTools) {
            try {
                this.devTools = ((HasDevTools) driver).getDevTools();
                this.devTools.createSession();
                this.devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
            } catch (Exception e) {
                System.err.println("DevTools not available for network simulation: " + e.getMessage());
            }
        }
    }
    
    /**
     * Initialize predefined network conditions
     */
    private Map<String, NetworkCondition> initializePredefinedConditions() {
        Map<String, NetworkCondition> conditions = new ConcurrentHashMap<>();
        
        // 3G Network Conditions
        conditions.put("3g-slow", new NetworkCondition(
            "3G Slow", 500, 500, 500, 0.05, ConnectionType.OTHER, "Slow 3G connection"
        ));
        
        conditions.put("3g-fast", new NetworkCondition(
            "3G Fast", 1600, 768, 768, 0.02, ConnectionType.OTHER, "Fast 3G connection"
        ));
        
        // 4G Network Conditions
        conditions.put("4g-slow", new NetworkCondition(
            "4G Slow", 1500, 700, 700, 0.03, ConnectionType.OTHER, "Slow 4G connection"
        ));
        
        conditions.put("4g-fast", new NetworkCondition(
            "4G Fast", 9000, 9000, 9000, 0.01, ConnectionType.OTHER, "Fast 4G connection"
        ));
        
        // 5G Network Conditions
        conditions.put("5g-slow", new NetworkCondition(
            "5G Slow", 4000, 4000, 4000, 0.01, ConnectionType.OTHER, "Slow 5G connection"
        ));
        
        conditions.put("5g-fast", new NetworkCondition(
            "5G Fast", 20000, 20000, 20000, 0.005, ConnectionType.OTHER, "Fast 5G connection"
        ));
        
        // WiFi Conditions
        conditions.put("wifi-slow", new NetworkCondition(
            "WiFi Slow", 2000, 2000, 2000, 0.01, ConnectionType.WIFI, "Slow WiFi connection"
        ));
        
        conditions.put("wifi-fast", new NetworkCondition(
            "WiFi Fast", 10000, 10000, 10000, 0.001, ConnectionType.WIFI, "Fast WiFi connection"
        ));
        
        // DSL Conditions
        conditions.put("dsl", new NetworkCondition(
            "DSL", 2000, 768, 768, 0.02, ConnectionType.ETHERNET, "DSL connection"
        ));
        
        // Cable Conditions
        conditions.put("cable", new NetworkCondition(
            "Cable", 5000, 1000, 1000, 0.01, ConnectionType.ETHERNET, "Cable connection"
        ));
        
        // Extreme Conditions for Stress Testing
        conditions.put("dial-up", new NetworkCondition(
            "Dial-up", 50, 50, 50, 0.1, ConnectionType.NONE, "Dial-up connection (stress test)"
        ));
        
        conditions.put("satellite", new NetworkCondition(
            "Satellite", 2000, 2000, 2000, 0.15, ConnectionType.OTHER, "Satellite connection (high latency)"
        ));
        
        // Custom Conditions
        conditions.put("offline", new NetworkCondition(
            "Offline", 0, 0, 0, 1.0, ConnectionType.NONE, "Offline mode"
        ));
        
        return conditions;
    }
    
    /**
     * Apply a predefined network condition
     */
    public boolean applyNetworkCondition(String conditionName) {
        NetworkCondition condition = predefinedConditions.get(conditionName);
        if (condition == null) {
            System.err.println("Unknown network condition: " + conditionName);
            return false;
        }
        
        return applyNetworkCondition(condition);
    }
    
    /**
     * Apply a network condition
     */
    public boolean applyNetworkCondition(NetworkCondition condition) {
        if (devTools == null) {
            System.err.println("DevTools not available for network simulation");
            return false;
        }
        
        try {
            // Apply network conditions using Chrome DevTools Protocol
            devTools.send(Network.emulateNetworkConditions(
                false, // offline
                condition.getLatency(),
                condition.getDownloadBandwidth(),
                condition.getUploadBandwidth(),
                Optional.empty() // packet loss rate
            ));
            
            currentCondition = condition.getName();
            System.out.println("🌐 Network condition applied: " + condition.getName());
            System.out.println("   📊 Latency: " + condition.getLatency() + "ms");
            System.out.println("   ⬇️ Download: " + condition.getDownloadBandwidth() + " bps");
            System.out.println("   ⬆️ Upload: " + condition.getUploadBandwidth() + " bps");
            System.out.println("   📦 Packet Loss: " + (condition.getPacketLossRate() * 100) + "%");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error applying network condition: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Apply custom network condition
     */
    public boolean applyCustomNetworkCondition(String name, int latency, int downloadBandwidth, 
                                             int uploadBandwidth, double packetLossRate, 
                                             String description) {
        NetworkCondition customCondition = new NetworkCondition(
            name, latency, downloadBandwidth, uploadBandwidth, packetLossRate, 
            ConnectionType.OTHER, description
        );
        
        return applyNetworkCondition(customCondition);
    }
    
    /**
     * Simulate network degradation over time
     */
    public void simulateNetworkDegradation(int durationSeconds, String startCondition, String endCondition) {
        NetworkCondition start = predefinedConditions.get(startCondition);
        NetworkCondition end = predefinedConditions.get(endCondition);
        
        if (start == null || end == null) {
            System.err.println("Invalid network conditions for degradation simulation");
            return;
        }
        
        System.out.println("🌐 Starting network degradation simulation:");
        System.out.println("   From: " + start.getName() + " to " + end.getName());
        System.out.println("   Duration: " + durationSeconds + " seconds");
        
        // Apply initial condition
        applyNetworkCondition(start);
        
        // Gradually degrade network over time
        int steps = 10;
        int stepDuration = durationSeconds / steps;
        
        for (int i = 1; i <= steps; i++) {
            try {
                Thread.sleep(stepDuration * 1000);
                
                // Interpolate between start and end conditions
                double progress = (double) i / steps;
                
                int currentLatency = interpolate(start.getLatency(), end.getLatency(), progress);
                int currentDownload = interpolate(start.getDownloadBandwidth(), end.getDownloadBandwidth(), progress);
                int currentUpload = interpolate(start.getUploadBandwidth(), end.getUploadBandwidth(), progress);
                double currentPacketLoss = interpolateDouble(start.getPacketLossRate(), end.getPacketLossRate(), progress);
                
                NetworkCondition intermediateCondition = new NetworkCondition(
                    "Degraded " + i, currentLatency, currentDownload, currentUpload, 
                    currentPacketLoss, ConnectionType.OTHER, "Degradation step " + i
                );
                
                applyNetworkCondition(intermediateCondition);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Apply final condition
        applyNetworkCondition(end);
        System.out.println("🌐 Network degradation simulation completed");
    }
    
    /**
     * Simulate network recovery
     */
    public void simulateNetworkRecovery(int durationSeconds, String startCondition, String endCondition) {
        NetworkCondition start = predefinedConditions.get(startCondition);
        NetworkCondition end = predefinedConditions.get(endCondition);
        
        if (start == null || end == null) {
            System.err.println("Invalid network conditions for recovery simulation");
            return;
        }
        
        System.out.println("🌐 Starting network recovery simulation:");
        System.out.println("   From: " + start.getName() + " to " + end.getName());
        System.out.println("   Duration: " + durationSeconds + " seconds");
        
        // Apply initial condition
        applyNetworkCondition(start);
        
        // Gradually improve network over time
        int steps = 10;
        int stepDuration = durationSeconds / steps;
        
        for (int i = 1; i <= steps; i++) {
            try {
                Thread.sleep(stepDuration * 1000);
                
                // Interpolate between start and end conditions
                double progress = (double) i / steps;
                
                int currentLatency = interpolate(start.getLatency(), end.getLatency(), progress);
                int currentDownload = interpolate(start.getDownloadBandwidth(), end.getDownloadBandwidth(), progress);
                int currentUpload = interpolate(start.getUploadBandwidth(), end.getUploadBandwidth(), progress);
                double currentPacketLoss = interpolateDouble(start.getPacketLossRate(), end.getPacketLossRate(), progress);
                
                NetworkCondition intermediateCondition = new NetworkCondition(
                    "Recovering " + i, currentLatency, currentDownload, currentUpload, 
                    currentPacketLoss, ConnectionType.OTHER, "Recovery step " + i
                );
                
                applyNetworkCondition(intermediateCondition);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Apply final condition
        applyNetworkCondition(end);
        System.out.println("🌐 Network recovery simulation completed");
    }
    
    /**
     * Simulate intermittent connectivity
     */
    public void simulateIntermittentConnectivity(int durationSeconds, int offlineIntervalSeconds, 
                                               int onlineIntervalSeconds) {
        System.out.println("🌐 Starting intermittent connectivity simulation:");
        System.out.println("   Duration: " + durationSeconds + " seconds");
        System.out.println("   Offline interval: " + offlineIntervalSeconds + " seconds");
        System.out.println("   Online interval: " + onlineIntervalSeconds + " seconds");
        
        long startTime = System.currentTimeMillis();
        boolean isOnline = true;
        
        while (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
            try {
                if (isOnline) {
                    // Go offline
                    applyNetworkCondition("offline");
                    System.out.println("📴 Network went offline");
                    Thread.sleep(offlineIntervalSeconds * 1000);
                } else {
                    // Go online
                    applyNetworkCondition("4g-fast");
                    System.out.println("📶 Network came online");
                    Thread.sleep(onlineIntervalSeconds * 1000);
                }
                
                isOnline = !isOnline;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("🌐 Intermittent connectivity simulation completed");
    }
    
    /**
     * Reset network to normal conditions
     */
    public boolean resetToNormalNetwork() {
        if (devTools == null) {
            System.err.println("DevTools not available for network reset");
            return false;
        }
        
        try {
            // Reset to normal network conditions
            devTools.send(Network.emulateNetworkConditions(
                false, // offline
                0, // latency
                -1, // download bandwidth (unlimited)
                -1, // upload bandwidth (unlimited)
                Optional.empty() // packet loss
            ));
            
            currentCondition = "normal";
            System.out.println("🌐 Network reset to normal conditions");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error resetting network: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current network condition
     */
    public String getCurrentCondition() {
        return currentCondition;
    }
    
    /**
     * Get all available network conditions
     */
    public Map<String, NetworkCondition> getAvailableConditions() {
        return new HashMap<>(predefinedConditions);
    }
    
    /**
     * Get network condition by name
     */
    public NetworkCondition getNetworkCondition(String name) {
        return predefinedConditions.get(name);
    }
    
    /**
     * Test network condition impact on performance
     */
    public NetworkPerformanceTestResult testNetworkConditionImpact(String conditionName, 
                                                                 Runnable testAction) {
        NetworkCondition condition = predefinedConditions.get(conditionName);
        if (condition == null) {
            return new NetworkPerformanceTestResult(conditionName, false, "Unknown condition", 0, 0, 0);
        }
        
        System.out.println("🧪 Testing network condition impact: " + conditionName);
        
        // Apply network condition
        boolean applied = applyNetworkCondition(condition);
        if (!applied) {
            return new NetworkPerformanceTestResult(conditionName, false, "Failed to apply condition", 0, 0, 0);
        }
        
        // Measure performance
        long startTime = System.currentTimeMillis();
        long startMemory = getMemoryUsage();
        
        try {
            testAction.run();
        } catch (Exception e) {
            return new NetworkPerformanceTestResult(conditionName, false, e.getMessage(), 0, 0, 0);
        }
        
        long endTime = System.currentTimeMillis();
        long endMemory = getMemoryUsage();
        
        long duration = endTime - startTime;
        long memoryUsed = endMemory - startMemory;
        
        System.out.println("✅ Network condition test completed:");
        System.out.println("   Duration: " + duration + "ms");
        System.out.println("   Memory used: " + memoryUsed + " bytes");
        
        return new NetworkPerformanceTestResult(conditionName, true, "Success", duration, memoryUsed, 0);
    }
    
    /**
     * Compare performance across multiple network conditions
     */
    public Map<String, NetworkPerformanceTestResult> compareNetworkConditions(
            String[] conditionNames, Runnable testAction) {
        
        Map<String, NetworkPerformanceTestResult> results = new HashMap<>();
        
        System.out.println("🧪 Comparing performance across network conditions:");
        
        for (String conditionName : conditionNames) {
            System.out.println("   Testing: " + conditionName);
            
            NetworkPerformanceTestResult result = testNetworkConditionImpact(conditionName, testAction);
            results.put(conditionName, result);
            
            // Wait between tests
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Reset to normal
        resetToNormalNetwork();
        
        return results;
    }
    
    /**
     * Interpolate between two values
     */
    private int interpolate(int start, int end, double progress) {
        return (int) (start + (end - start) * progress);
    }
    
    private double interpolateDouble(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
    
    /**
     * Get current memory usage
     */
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Network Condition data model
     */
    public static class NetworkCondition {
        private final String name;
        private final int latency; // milliseconds
        private final int downloadBandwidth; // bytes per second
        private final int uploadBandwidth; // bytes per second
        private final double packetLossRate; // 0.0 to 1.0
        private final ConnectionType connectionType;
        private final String description;
        
        public NetworkCondition(String name, int latency, int downloadBandwidth, 
                              int uploadBandwidth, double packetLossRate, 
                              ConnectionType connectionType, String description) {
            this.name = name;
            this.latency = latency;
            this.downloadBandwidth = downloadBandwidth;
            this.uploadBandwidth = uploadBandwidth;
            this.packetLossRate = Math.max(0.0, Math.min(1.0, packetLossRate));
            this.connectionType = connectionType;
            this.description = description;
        }
        
        // Getters
        public String getName() { return name; }
        public int getLatency() { return latency; }
        public int getDownloadBandwidth() { return downloadBandwidth; }
        public int getUploadBandwidth() { return uploadBandwidth; }
        public double getPacketLossRate() { return packetLossRate; }
        public ConnectionType getConnectionType() { return connectionType; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("%s (Latency: %dms, Download: %d bps, Upload: %d bps, Loss: %.2f%%)",
                name, latency, downloadBandwidth, uploadBandwidth, packetLossRate * 100);
        }
    }
    
    /**
     * Network Performance Test Result
     */
    public static class NetworkPerformanceTestResult {
        private final String conditionName;
        private final boolean success;
        private final String message;
        private final long duration;
        private final long memoryUsed;
        private final double performanceScore;
        
        public NetworkPerformanceTestResult(String conditionName, boolean success, String message, 
                                          long duration, long memoryUsed, double performanceScore) {
            this.conditionName = conditionName;
            this.success = success;
            this.message = message;
            this.duration = duration;
            this.memoryUsed = memoryUsed;
            this.performanceScore = performanceScore;
        }
        
        // Getters
        public String getConditionName() { return conditionName; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getDuration() { return duration; }
        public long getMemoryUsed() { return memoryUsed; }
        public double getPerformanceScore() { return performanceScore; }
        
        @Override
        public String toString() {
            return String.format("NetworkTest[%s: %s, Duration: %dms, Memory: %d bytes, Score: %.2f]",
                conditionName, success ? "SUCCESS" : "FAILED", duration, memoryUsed, performanceScore);
        }
    }
}
