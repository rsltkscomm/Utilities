package ai.ml;

import java.util.*;

/**
 * Detects anomalies in test behavior and performance
 */
public class AnomalyDetector {
    
    private final Map<String, List<Double>> historicalData;
    private final double anomalyThreshold;
    
    public AnomalyDetector() {
        this(2.0); // 2 standard deviations
    }
    
    public AnomalyDetector(double anomalyThreshold) {
        this.historicalData = new HashMap<>();
        this.anomalyThreshold = anomalyThreshold;
    }
    
    /**
     * Detect anomalies in test execution times
     */
    public List<Anomaly> detectExecutionTimeAnomalies(String testKey, List<Double> executionTimes) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        if (executionTimes.size() < 3) {
            return anomalies; // Need at least 3 data points
        }
        
        // Calculate statistics
        double mean = calculateMean(executionTimes);
        double stdDev = calculateStandardDeviation(executionTimes, mean);
        
        // Detect outliers
        for (int i = 0; i < executionTimes.size(); i++) {
            double value = executionTimes.get(i);
            double zScore = Math.abs((value - mean) / stdDev);
            
            if (zScore > anomalyThreshold) {
                anomalies.add(new Anomaly(
                    testKey,
                    AnomalyType.EXECUTION_TIME,
                    "Execution time anomaly",
                    String.format("Value: %.2f, Mean: %.2f, Z-Score: %.2f", 
                        value, mean, zScore),
                    i
                ));
            }
        }
        
        // Store historical data
        historicalData.put(testKey, new ArrayList<>(executionTimes));
        
        return anomalies;
    }
    
    /**
     * Detect anomalies in failure patterns
     */
    public List<Anomaly> detectFailurePatternAnomalies(String testKey, 
                                                      List<Boolean> executionResults) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        if (executionResults.size() < 5) {
            return anomalies;
        }
        
        // Calculate failure rate
        long failures = executionResults.stream().filter(r -> !r).count();
        double failureRate = (double) failures / executionResults.size();
        
        // Check for sudden change in failure pattern
        int recentWindow = Math.min(5, executionResults.size());
        List<Boolean> recent = executionResults.subList(
            executionResults.size() - recentWindow, executionResults.size());
        
        long recentFailures = recent.stream().filter(r -> !r).count();
        double recentFailureRate = (double) recentFailures / recent.size();
        
        // If recent failure rate is significantly different
        if (Math.abs(recentFailureRate - failureRate) > 0.3) {
            anomalies.add(new Anomaly(
                testKey,
                AnomalyType.FAILURE_PATTERN,
                "Failure pattern anomaly",
                String.format("Recent failure rate: %.1f%%, Historical: %.1f%%", 
                    recentFailureRate * 100, failureRate * 100),
                executionResults.size() - 1
            ));
        }
        
        return anomalies;
    }
    
    /**
     * Detect performance anomalies
     */
    public List<Anomaly> detectPerformanceAnomalies(String testKey, 
                                                   Map<String, Double> metrics) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            Double value = entry.getValue();
            
            // Get historical data for this metric
            String key = testKey + "_" + metricName;
            List<Double> history = historicalData.get(key);
            
            if (history != null && history.size() >= 3) {
                double mean = calculateMean(history);
                double stdDev = calculateStandardDeviation(history, mean);
                
                if (stdDev > 0) {
                    double zScore = Math.abs((value - mean) / stdDev);
                    
                    if (zScore > anomalyThreshold) {
                        anomalies.add(new Anomaly(
                            testKey,
                            AnomalyType.PERFORMANCE,
                            "Performance anomaly: " + metricName,
                            String.format("Value: %.2f, Mean: %.2f, Z-Score: %.2f", 
                                value, mean, zScore),
                            -1
                        ));
                    }
                }
            }
            
            // Update history
            if (history == null) {
                history = new ArrayList<>();
                historicalData.put(key, history);
            }
            history.add(value);
            if (history.size() > 50) {
                history.remove(0); // Keep only last 50 values
            }
        }
        
        return anomalies;
    }
    
    /**
     * Calculate mean
     */
    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    /**
     * Calculate standard deviation
     */
    private double calculateStandardDeviation(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    /**
     * Anomaly
     */
    public static class Anomaly {
        private final String testKey;
        private final AnomalyType type;
        private final String description;
        private final String details;
        private final int index;
        
        public Anomaly(String testKey, AnomalyType type, String description, 
                      String details, int index) {
            this.testKey = testKey;
            this.type = type;
            this.description = description;
            this.details = details;
            this.index = index;
        }
        
        public String getTestKey() { return testKey; }
        public AnomalyType getType() { return type; }
        public String getDescription() { return description; }
        public String getDetails() { return details; }
        public int getIndex() { return index; }
        
        @Override
        public String toString() {
            return String.format("Anomaly[%s: %s - %s]", testKey, type, description);
        }
    }
    
    /**
     * Anomaly type
     */
    public enum AnomalyType {
        EXECUTION_TIME,
        FAILURE_PATTERN,
        PERFORMANCE
    }
}

