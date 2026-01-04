package ai.ml.integration;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;

import java.util.*;

/**
 * Feature extractor for ML models
 * Extracts features from test execution data
 */
public class FeatureExtractor {
    
    private final FlakyTestDetector flakyDetector;
    private final TestStabilityScorer stabilityScorer;
    
    public FeatureExtractor(FlakyTestDetector flakyDetector,
                            TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.stabilityScorer = stabilityScorer;
    }
    
    /**
     * Extract features for test failure prediction
     */
    public double[] extractFailurePredictionFeatures(String testKey) {
        List<Double> features = new ArrayList<>();
        
        // Feature 1: Is flaky (0 or 1)
        features.add(flakyDetector.isFlaky(testKey) ? 1.0 : 0.0);
        
        // Feature 2: Stability score (0-1)
        TestStabilityScorer.StabilityScore score = stabilityScorer.getStabilityScore(testKey);
        features.add(score.getScore());
        
        // Feature 3: Failure rate (0-1)
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        double failureRate = history != null ? history.getFailureRate() : 0.0;
        features.add(failureRate);
        
        // Feature 4: Success rate (0-1)
        double successRate = history != null ? history.getSuccessRate() : 1.0;
        features.add(successRate);
        
        // Feature 5: Total executions (normalized)
        int totalExecutions = history != null ? history.getTotalExecutions() : 0;
        features.add(normalize(totalExecutions, 0, 100));
        
        // Feature 6: Recent failure rate (0-1)
        double recentFailureRate = calculateRecentFailureRate(history);
        features.add(recentFailureRate);
        
        // Feature 7: Is degrading (0 or 1)
        boolean isDegrading = isTestDegrading(testKey);
        features.add(isDegrading ? 1.0 : 0.0);
        
        // Feature 8: Average execution time (normalized)
        double avgExecutionTime = calculateAverageExecutionTime(history);
        features.add(normalize(avgExecutionTime, 0, 60)); // Normalize to 0-60 seconds
        
        return features.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Extract features for locator success prediction
     */
    public double[] extractLocatorFeatures(String locator, String locatorType, 
                                          String elementDescription) {
        List<Double> features = new ArrayList<>();
        
        // Feature 1: Locator type encoding
        features.add(encodeLocatorType(locatorType));
        
        // Feature 2: Locator length (normalized)
        features.add(normalize(locator.length(), 0, 200));
        
        // Feature 3: Contains ID (0 or 1)
        features.add(locator.contains("#") ? 1.0 : 0.0);
        
        // Feature 4: Contains class (0 or 1)
        features.add(locator.contains(".") ? 1.0 : 0.0);
        
        // Feature 5: Contains text (0 or 1)
        features.add(locator.contains("text=") ? 1.0 : 0.0);
        
        // Feature 6: Element description length (normalized)
        features.add(normalize(elementDescription.length(), 0, 100));
        
        // Feature 7: Complexity score (number of selectors)
        int complexity = locator.split(" ").length;
        features.add(normalize(complexity, 1, 10));
        
        return features.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Extract features for execution time prediction
     */
    public double[] extractExecutionTimeFeatures(String testKey, 
                                                Map<String, Object> context) {
        List<Double> features = new ArrayList<>();
        
        // Feature 1: Historical average execution time
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        double avgTime = calculateAverageExecutionTime(history);
        features.add(normalize(avgTime, 0, 60));
        
        // Feature 2: Test complexity (from context)
        int complexity = (Integer) context.getOrDefault("complexity", 1);
        features.add(normalize(complexity, 1, 10));
        
        // Feature 3: Number of steps (from context)
        int steps = (Integer) context.getOrDefault("steps", 1);
        features.add(normalize(steps, 1, 50));
        
        // Feature 4: Has database operations (0 or 1)
        boolean hasDbOps = (Boolean) context.getOrDefault("hasDbOps", false);
        features.add(hasDbOps ? 1.0 : 0.0);
        
        // Feature 5: Has API calls (0 or 1)
        boolean hasApiCalls = (Boolean) context.getOrDefault("hasApiCalls", false);
        features.add(hasApiCalls ? 1.0 : 0.0);
        
        return features.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Calculate recent failure rate
     */
    private double calculateRecentFailureRate(FlakyTestDetector.TestExecutionHistory history) {
        if (history == null) {
            return 0.0;
        }
        
        List<FlakyTestDetector.ExecutionRecord> executions = history.getAllExecutions();
        if (executions.isEmpty()) {
            return 0.0;
        }
        
        // Get last 5 executions
        int recentCount = Math.min(5, executions.size());
        int recentFailures = 0;
        
        for (int i = executions.size() - recentCount; i < executions.size(); i++) {
            FlakyTestDetector.ExecutionRecord record = executions.get(i);
            if (!record.isPassed()) {
                recentFailures++;
            }
        }
        
        return recentCount > 0 ? (double) recentFailures / recentCount : 0.0;
    }
    
    /**
     * Check if test is degrading
     */
    private boolean isTestDegrading(String testKey) {
        List<TestStabilityScorer.DegradingTest> degrading = 
            stabilityScorer.getDegradingTests();
        
        for (TestStabilityScorer.DegradingTest degradingTest : degrading) {
            if (degradingTest.getTestKey().equals(testKey)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Calculate average execution time
     */
    private double calculateAverageExecutionTime(FlakyTestDetector.TestExecutionHistory history) {
        if (history == null) {
            return 0.0;
        }
        
        // Simplified - in production, track actual execution times
        return 5.0; // Default 5 seconds
    }
    
    /**
     * Encode locator type
     */
    private double encodeLocatorType(String locatorType) {
        switch (locatorType.toLowerCase()) {
            case "id": return 1.0;
            case "class": return 2.0;
            case "xpath": return 3.0;
            case "css": return 4.0;
            case "text": return 5.0;
            default: return 0.0;
        }
    }
    
    /**
     * Normalize value to 0-1 range
     */
    private double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.0;
        }
        double normalized = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, normalized));
    }
}

