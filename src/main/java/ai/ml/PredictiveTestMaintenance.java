package ai.ml;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;

import java.util.*;

/**
 * Predictive test maintenance using ML-like algorithms
 * Predicts which tests will fail and suggests updates
 */
public class PredictiveTestMaintenance {
    
    private final FlakyTestDetector flakyDetector;
    private final TestStabilityScorer stabilityScorer;
    private final Map<String, TestPrediction> predictions;
    
    public PredictiveTestMaintenance(FlakyTestDetector flakyDetector, 
                                    TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.stabilityScorer = stabilityScorer;
        this.predictions = new HashMap<>();
    }
    
    /**
     * Predict which tests will fail
     */
    public List<TestPrediction> predictFailures() {
        List<TestPrediction> failurePredictions = new ArrayList<>();
        
        // Get all test keys
        Set<String> testKeys = flakyDetector.getAllTestKeys();
        
        for (String testKey : testKeys) {
            TestPrediction prediction = predictTestFailure(testKey);
            if (prediction.getFailureProbability() > 0.5) {
                failurePredictions.add(prediction);
            }
            predictions.put(testKey, prediction);
        }
        
        // Sort by failure probability
        failurePredictions.sort((a, b) -> 
            Double.compare(b.getFailureProbability(), a.getFailureProbability()));
        
        return failurePredictions;
    }
    
    /**
     * Predict failure for a specific test
     */
    public TestPrediction predictTestFailure(String testKey) {
        double failureProbability = 0.0;
        List<String> reasons = new ArrayList<>();
        
        // Check if test is flaky
        if (flakyDetector.isFlaky(testKey)) {
            failureProbability += 0.4;
            reasons.add("Test is flaky");
        }
        
        // Check stability score
        TestStabilityScorer.StabilityScore score = stabilityScorer.getStabilityScore(testKey);
        if (score.getLevel() == TestStabilityScorer.StabilityLevel.FLAKY) {
            failureProbability += 0.3;
            reasons.add("Low stability score: " + String.format("%.1f", score.getScore()));
        } else if (score.getLevel() == TestStabilityScorer.StabilityLevel.UNSTABLE) {
            failureProbability += 0.2;
            reasons.add("Unstable test");
        }
        
        // Check for degrading tests
        List<TestStabilityScorer.DegradingTest> degrading = stabilityScorer.getDegradingTests();
        for (TestStabilityScorer.DegradingTest degradingTest : degrading) {
            if (degradingTest.getTestKey().equals(testKey)) {
                failureProbability += 0.2;
                reasons.add("Test is degrading: " + 
                    String.format("%.2f", degradingTest.getDegradationRate()));
            }
        }
        
        // Check failure history
        FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
        if (history != null) {
            double failureRate = history.getFailureRate();
            if (failureRate > 0.3) {
                failureProbability += failureRate * 0.3;
                reasons.add("High failure rate: " + String.format("%.1f%%", failureRate * 100));
            }
        }
        
        // Clamp to 0-1
        failureProbability = Math.min(1.0, failureProbability);
        
        return new TestPrediction(testKey, failureProbability, reasons);
    }
    
    /**
     * Suggest test updates before failures
     */
    public List<TestSuggestion> suggestUpdates() {
        List<TestSuggestion> suggestions = new ArrayList<>();
        
        List<TestPrediction> predictions = predictFailures();
        
        for (TestPrediction prediction : predictions) {
            if (prediction.getFailureProbability() > 0.6) {
                suggestions.add(new TestSuggestion(
                    prediction.getTestKey(),
                    "High failure probability: " + 
                        String.format("%.1f%%", prediction.getFailureProbability() * 100),
                    generateSuggestion(prediction)
                ));
            }
        }
        
        return suggestions;
    }
    
    /**
     * Identify obsolete tests
     */
    public List<String> identifyObsoleteTests() {
        List<String> obsoleteTests = new ArrayList<>();
        
        Set<String> testKeys = flakyDetector.getAllTestKeys();
        
        for (String testKey : testKeys) {
            FlakyTestDetector.TestExecutionHistory history = flakyDetector.getHistory(testKey);
            if (history != null) {
                // Test is obsolete if:
                // 1. Hasn't been executed recently (last 30 days)
                // 2. Has very low success rate (<20%)
                // 3. Has been consistently failing
                
                List<FlakyTestDetector.ExecutionRecord> executions = history.getAllExecutions();
                if (executions.isEmpty()) {
                    continue;
                }
                
                long lastExecution = executions.get(executions.size() - 1).getTimestamp();
                long daysSinceLastExecution = (System.currentTimeMillis() - lastExecution) / (1000 * 60 * 60 * 24);
                
                double successRate = history.getSuccessRate();
                
                if (daysSinceLastExecution > 30 && successRate < 0.2) {
                    obsoleteTests.add(testKey);
                } else if (successRate < 0.1) {
                    obsoleteTests.add(testKey);
                }
            }
        }
        
        return obsoleteTests;
    }
    
    /**
     * Generate suggestion for test
     */
    private String generateSuggestion(TestPrediction prediction) {
        StringBuilder suggestion = new StringBuilder();
        
        suggestion.append("Consider updating test: ").append(prediction.getTestKey()).append("\n");
        suggestion.append("Failure probability: ").append(String.format("%.1f%%", 
            prediction.getFailureProbability() * 100)).append("\n");
        suggestion.append("Reasons:\n");
        
        for (String reason : prediction.getReasons()) {
            suggestion.append("  - ").append(reason).append("\n");
        }
        
        if (prediction.getFailureProbability() > 0.7) {
            suggestion.append("\nRecommended actions:\n");
            suggestion.append("  - Review and fix test logic\n");
            suggestion.append("  - Update locators if needed\n");
            suggestion.append("  - Add proper waits\n");
            suggestion.append("  - Consider removing if obsolete\n");
        }
        
        return suggestion.toString();
    }
    
    /**
     * Test prediction
     */
    public static class TestPrediction {
        private final String testKey;
        private final double failureProbability;
        private final List<String> reasons;
        
        public TestPrediction(String testKey, double failureProbability, List<String> reasons) {
            this.testKey = testKey;
            this.failureProbability = failureProbability;
            this.reasons = new ArrayList<>(reasons);
        }
        
        public String getTestKey() { return testKey; }
        public double getFailureProbability() { return failureProbability; }
        public List<String> getReasons() { return new ArrayList<>(reasons); }
    }
    
    /**
     * Test suggestion
     */
    public static class TestSuggestion {
        private final String testKey;
        private final String issue;
        private final String suggestion;
        
        public TestSuggestion(String testKey, String issue, String suggestion) {
            this.testKey = testKey;
            this.issue = issue;
            this.suggestion = suggestion;
        }
        
        public String getTestKey() { return testKey; }
        public String getIssue() { return issue; }
        public String getSuggestion() { return suggestion; }
    }
}

