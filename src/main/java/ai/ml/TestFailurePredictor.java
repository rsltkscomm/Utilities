package ai.ml;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;

import java.util.*;

/**
 * ML-based test failure prediction
 */
public class TestFailurePredictor {
    
    private final FlakyTestDetector flakyDetector;
    private final TestStabilityScorer stabilityScorer;
    private final Map<String, FailureRisk> riskCache;
    
    public TestFailurePredictor(FlakyTestDetector flakyDetector,
                               TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.stabilityScorer = stabilityScorer;
        this.riskCache = new HashMap<>();
    }
    
    /**
     * Predict failure risk for a test
     */
    public FailureRisk predictFailureRisk(String testKey) {
        if (riskCache.containsKey(testKey)) {
            return riskCache.get(testKey);
        }
        
        double riskScore = calculateRiskScore(testKey);
        RiskLevel level = determineRiskLevel(riskScore);
        List<String> factors = identifyRiskFactors(testKey);
        
        FailureRisk risk = new FailureRisk(testKey, riskScore, level, factors);
        riskCache.put(testKey, risk);
        
        return risk;
    }
    
    /**
     * Predict failure risks for multiple tests
     */
    public List<FailureRisk> predictFailureRisks(Set<String> testKeys) {
        List<FailureRisk> risks = new ArrayList<>();
        
        for (String testKey : testKeys) {
            risks.add(predictFailureRisk(testKey));
        }
        
        // Sort by risk score (highest first)
        risks.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
        
        return risks;
    }
    
    /**
     * Calculate risk score (0-1)
     */
    private double calculateRiskScore(String testKey) {
        double risk = 0.0;
        
        // Factor 1: Flakiness (40% weight)
        if (flakyDetector.isFlaky(testKey)) {
            risk += 0.4;
        }
        
        // Factor 2: Stability score (30% weight)
        TestStabilityScorer.StabilityScore score = stabilityScorer.getStabilityScore(testKey);
        if (score.getLevel() == TestStabilityScorer.StabilityLevel.FLAKY) {
            risk += 0.3;
        } else if (score.getLevel() == TestStabilityScorer.StabilityLevel.UNSTABLE) {
            risk += 0.15;
        }
        
        // Factor 3: Failure rate (20% weight)
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        if (history != null) {
            double failureRate = history.getFailureRate();
            risk += failureRate * 0.2;
        }
        
        // Factor 4: Degrading trend (10% weight)
        List<TestStabilityScorer.DegradingTest> degrading = stabilityScorer.getDegradingTests();
        for (TestStabilityScorer.DegradingTest degradingTest : degrading) {
            if (degradingTest.getTestKey().equals(testKey)) {
                risk += 0.1;
                break;
            }
        }
        
        return Math.min(1.0, risk);
    }
    
    /**
     * Determine risk level
     */
    private RiskLevel determineRiskLevel(double riskScore) {
        if (riskScore >= 0.7) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 0.4) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
    
    /**
     * Identify risk factors
     */
    private List<String> identifyRiskFactors(String testKey) {
        List<String> factors = new ArrayList<>();
        
        if (flakyDetector.isFlaky(testKey)) {
            factors.add("Test is flaky");
        }
        
        TestStabilityScorer.StabilityScore score = stabilityScorer.getStabilityScore(testKey);
        if (score.getLevel() == TestStabilityScorer.StabilityLevel.FLAKY) {
            factors.add("Low stability score: " + String.format("%.1f", score.getScore()));
        }
        
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        if (history != null) {
            double failureRate = history.getFailureRate();
            if (failureRate > 0.3) {
                factors.add("High failure rate: " + String.format("%.1f%%", failureRate * 100));
            }
        }
        
        // Check for degrading
        for (TestStabilityScorer.DegradingTest degradingTest : stabilityScorer.getDegradingTests()) {
            if (degradingTest.getTestKey().equals(testKey)) {
                factors.add("Test is degrading");
                break;
            }
        }
        
        return factors;
    }
    
    /**
     * Get preventive actions
     */
    public List<String> getPreventiveActions(String testKey) {
        List<String> actions = new ArrayList<>();
        FailureRisk risk = predictFailureRisk(testKey);
        
        if (risk.getLevel() == RiskLevel.HIGH) {
            actions.add("Review and fix test immediately");
            actions.add("Update locators if needed");
            actions.add("Add proper waits and synchronization");
            actions.add("Consider test refactoring");
        } else if (risk.getLevel() == RiskLevel.MEDIUM) {
            actions.add("Monitor test closely");
            actions.add("Review test stability");
            actions.add("Consider preventive fixes");
        } else {
            actions.add("Continue monitoring");
        }
        
        return actions;
    }
    
    /**
     * Failure risk
     */
    public static class FailureRisk {
        private final String testKey;
        private final double riskScore;
        private final RiskLevel level;
        private final List<String> factors;
        
        public FailureRisk(String testKey, double riskScore, RiskLevel level, 
                          List<String> factors) {
            this.testKey = testKey;
            this.riskScore = riskScore;
            this.level = level;
            this.factors = new ArrayList<>(factors);
        }
        
        public String getTestKey() { return testKey; }
        public double getRiskScore() { return riskScore; }
        public RiskLevel getLevel() { return level; }
        public List<String> getFactors() { return new ArrayList<>(factors); }
        
        @Override
        public String toString() {
            return String.format("FailureRisk[%s: %.1f%% (%s)]", 
                testKey, riskScore * 100, level);
        }
    }
    
    /**
     * Risk level
     */
    public enum RiskLevel {
        HIGH, MEDIUM, LOW
    }
}

