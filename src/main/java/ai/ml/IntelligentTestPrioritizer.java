package ai.ml;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent test prioritization based on risk and impact
 */
public class IntelligentTestPrioritizer {
    
    private final FlakyTestDetector flakyDetector;
    private final TestStabilityScorer stabilityScorer;
    
    public IntelligentTestPrioritizer(FlakyTestDetector flakyDetector,
                                     TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.stabilityScorer = stabilityScorer;
    }
    
    /**
     * Prioritize tests based on risk
     */
    public List<TestPriority> prioritizeTests(Set<String> testKeys) {
        List<TestPriority> priorities = new ArrayList<>();
        
        for (String testKey : testKeys) {
            double riskScore = calculateRiskScore(testKey);
            double impactScore = calculateImpactScore(testKey);
            double priorityScore = (riskScore * 0.6) + (impactScore * 0.4);
            
            PriorityLevel level = determinePriorityLevel(priorityScore);
            
            priorities.add(new TestPriority(testKey, priorityScore, riskScore, 
                impactScore, level));
        }
        
        // Sort by priority score (highest first)
        priorities.sort((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()));
        
        return priorities;
    }
    
    /**
     * Calculate risk score (0-1)
     */
    private double calculateRiskScore(String testKey) {
        double risk = 0.0;
        
        // Check if flaky
        if (flakyDetector.isFlaky(testKey)) {
            risk += 0.3;
        }
        
        // Check stability
        TestStabilityScorer.StabilityScore score = stabilityScorer.getStabilityScore(testKey);
        if (score.getLevel() == TestStabilityScorer.StabilityLevel.FLAKY) {
            risk += 0.4;
        } else if (score.getLevel() == TestStabilityScorer.StabilityLevel.UNSTABLE) {
            risk += 0.2;
        }
        
        // Check failure rate
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        if (history != null) {
            double failureRate = history.getFailureRate();
            risk += failureRate * 0.3;
        }
        
        return Math.min(1.0, risk);
    }
    
    /**
     * Calculate impact score (0-1)
     */
    private double calculateImpactScore(String testKey) {
        double impact = 0.5; // Default impact
        
        // Check if test covers critical functionality
        // (In production, this would be based on test metadata, tags, etc.)
        if (testKey.contains("login") || testKey.contains("payment") || 
            testKey.contains("checkout")) {
            impact = 1.0; // Critical functionality
        } else if (testKey.contains("search") || testKey.contains("navigation")) {
            impact = 0.7; // Important functionality
        }
        
        // Check execution frequency
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        if (history != null) {
            int totalExecutions = history.getTotalExecutions();
            if (totalExecutions > 50) {
                impact += 0.2; // Frequently executed
            }
        }
        
        return Math.min(1.0, impact);
    }
    
    /**
     * Determine priority level
     */
    private PriorityLevel determinePriorityLevel(double priorityScore) {
        if (priorityScore >= 0.7) {
            return PriorityLevel.HIGH;
        } else if (priorityScore >= 0.4) {
            return PriorityLevel.MEDIUM;
        } else {
            return PriorityLevel.LOW;
        }
    }
    
    /**
     * Optimize test execution order
     */
    public List<String> optimizeExecutionOrder(Set<String> testKeys) {
        List<TestPriority> priorities = prioritizeTests(testKeys);
        
        // Return test keys in priority order
        return priorities.stream()
            .map(TestPriority::getTestKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Get high-impact tests
     */
    public List<String> getHighImpactTests(Set<String> testKeys) {
        return prioritizeTests(testKeys).stream()
            .filter(p -> p.getLevel() == PriorityLevel.HIGH)
            .map(TestPriority::getTestKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Test priority
     */
    public static class TestPriority {
        private final String testKey;
        private final double priorityScore;
        private final double riskScore;
        private final double impactScore;
        private final PriorityLevel level;
        
        public TestPriority(String testKey, double priorityScore, 
                          double riskScore, double impactScore, PriorityLevel level) {
            this.testKey = testKey;
            this.priorityScore = priorityScore;
            this.riskScore = riskScore;
            this.impactScore = impactScore;
            this.level = level;
        }
        
        public String getTestKey() { return testKey; }
        public double getPriorityScore() { return priorityScore; }
        public double getRiskScore() { return riskScore; }
        public double getImpactScore() { return impactScore; }
        public PriorityLevel getLevel() { return level; }
    }
    
    /**
     * Priority level
     */
    public enum PriorityLevel {
        HIGH, MEDIUM, LOW
    }
}

