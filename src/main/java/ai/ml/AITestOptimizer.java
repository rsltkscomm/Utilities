package ai.ml;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;
import reporting.TestLogManager;

import java.util.*;

/**
 * AI-powered test optimization
 */
public class AITestOptimizer {
    
    private final FlakyTestDetector flakyDetector;
    private final TestStabilityScorer stabilityScorer;
    private final PredictiveTestMaintenance predictiveMaintenance;
    private final IntelligentTestPrioritizer prioritizer;
    
    public AITestOptimizer(FlakyTestDetector flakyDetector,
                          TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.stabilityScorer = stabilityScorer;
        this.predictiveMaintenance = new PredictiveTestMaintenance(flakyDetector, stabilityScorer);
        this.prioritizer = new IntelligentTestPrioritizer(flakyDetector, stabilityScorer);
    }
    
    /**
     * Analyze test suite and suggest improvements
     */
    public OptimizationReport analyzeTestSuite(Set<String> testKeys) {
        TestLogManager.info("Analyzing test suite with " + testKeys.size() + " tests");
        
        OptimizationReport report = new OptimizationReport();
        
        // Identify redundant tests
        report.setRedundantTests(identifyRedundantTests(testKeys));
        
        // Get test improvements
        report.setImprovements(suggestTestImprovements(testKeys));
        
        // Optimize execution order
        report.setOptimizedOrder(prioritizer.optimizeExecutionOrder(testKeys));
        
        // Get obsolete tests
        report.setObsoleteTests(predictiveMaintenance.identifyObsoleteTests());
        
        // Calculate optimization score
        report.calculateOptimizationScore();
        
        return report;
    }
    
    /**
     * Identify redundant tests
     */
    public List<String> identifyRedundantTests(Set<String> testKeys) {
        List<String> redundant = new ArrayList<>();
        
        // Group tests by functionality (simplified - in production use test metadata)
        Map<String, List<String>> testGroups = new HashMap<>();
        
        for (String testKey : testKeys) {
            // Extract functionality from test name (simplified)
            String functionality = extractFunctionality(testKey);
            testGroups.computeIfAbsent(functionality, k -> new ArrayList<>()).add(testKey);
        }
        
        // Find groups with multiple similar tests
        for (Map.Entry<String, List<String>> entry : testGroups.entrySet()) {
            List<String> tests = entry.getValue();
            if (tests.size() > 1) {
                // Check if tests are similar (same functionality, similar execution)
                for (int i = 0; i < tests.size(); i++) {
                    for (int j = i + 1; j < tests.size(); j++) {
                        if (areTestsSimilar(tests.get(i), tests.get(j))) {
                            // Mark one as redundant (prefer more stable one)
                            String redundantTest = selectRedundantTest(tests.get(i), tests.get(j));
                            if (!redundant.contains(redundantTest)) {
                                redundant.add(redundantTest);
                            }
                        }
                    }
                }
            }
        }
        
        return redundant;
    }
    
    /**
     * Suggest test improvements
     */
    public List<TestImprovement> suggestTestImprovements(Set<String> testKeys) {
        List<TestImprovement> improvements = new ArrayList<>();
        
        for (String testKey : testKeys) {
            List<String> suggestions = new ArrayList<>();
            
            // Check stability
            TestStabilityScorer.StabilityScore score = stabilityScorer.getStabilityScore(testKey);
            if (score.getLevel() == TestStabilityScorer.StabilityLevel.FLAKY) {
                suggestions.add("Improve test stability - add proper waits");
                suggestions.add("Review and update locators");
            }
            
            // Check if flaky
            if (flakyDetector.isFlaky(testKey)) {
                suggestions.add("Fix flaky test - review test logic");
                suggestions.add("Add retry logic if appropriate");
            }
            
            // Check for degrading
            for (TestStabilityScorer.DegradingTest degrading : stabilityScorer.getDegradingTests()) {
                if (degrading.getTestKey().equals(testKey)) {
                    suggestions.add("Test is degrading - immediate attention needed");
                    break;
                }
            }
            
            if (!suggestions.isEmpty()) {
                improvements.add(new TestImprovement(testKey, suggestions));
            }
        }
        
        return improvements;
    }
    
    /**
     * Optimize execution order
     */
    public List<String> optimizeExecutionOrder(Set<String> testKeys) {
        return prioritizer.optimizeExecutionOrder(testKeys);
    }
    
    /**
     * Generate optimization report
     */
    public void generateReport(OptimizationReport report) {
        TestLogManager.info("=== Test Optimization Report ===");
        TestLogManager.info("Optimization Score: " + String.format("%.1f", report.getOptimizationScore()));
        TestLogManager.info("Redundant Tests: " + report.getRedundantTests().size());
        TestLogManager.info("Obsolete Tests: " + report.getObsoleteTests().size());
        TestLogManager.info("Improvements Suggested: " + report.getImprovements().size());
    }
    
    /**
     * Extract functionality from test name
     */
    private String extractFunctionality(String testKey) {
        // Simplified - extract key words
        String lower = testKey.toLowerCase();
        if (lower.contains("login")) return "login";
        if (lower.contains("search")) return "search";
        if (lower.contains("checkout")) return "checkout";
        if (lower.contains("payment")) return "payment";
        return "other";
    }
    
    /**
     * Check if tests are similar
     */
    private boolean areTestsSimilar(String test1, String test2) {
        // Simplified similarity check
        // In production, use more sophisticated comparison
        String func1 = extractFunctionality(test1);
        String func2 = extractFunctionality(test2);
        
        return func1.equals(func2) && !func1.equals("other");
    }
    
    /**
     * Select which test is redundant
     */
    private String selectRedundantTest(String test1, String test2) {
        // Prefer keeping the more stable test
        TestStabilityScorer.StabilityScore score1 = stabilityScorer.getStabilityScore(test1);
        TestStabilityScorer.StabilityScore score2 = stabilityScorer.getStabilityScore(test2);
        
        return score1.getScore() < score2.getScore() ? test1 : test2;
    }
    
    /**
     * Optimization report
     */
    public static class OptimizationReport {
        private List<String> redundantTests;
        private List<TestImprovement> improvements;
        private List<String> optimizedOrder;
        private List<String> obsoleteTests;
        private double optimizationScore;
        
        public void calculateOptimizationScore() {
            // Score based on optimization opportunities
            double score = 100.0;
            
            // Deduct for redundant tests
            score -= redundantTests.size() * 2.0;
            
            // Deduct for obsolete tests
            score -= obsoleteTests.size() * 3.0;
            
            // Deduct for improvements needed
            score -= improvements.size() * 1.0;
            
            optimizationScore = Math.max(0.0, score);
        }
        
        public List<String> getRedundantTests() { return redundantTests; }
        public void setRedundantTests(List<String> redundantTests) { 
            this.redundantTests = redundantTests; 
        }
        
        public List<TestImprovement> getImprovements() { return improvements; }
        public void setImprovements(List<TestImprovement> improvements) { 
            this.improvements = improvements; 
        }
        
        public List<String> getOptimizedOrder() { return optimizedOrder; }
        public void setOptimizedOrder(List<String> optimizedOrder) { 
            this.optimizedOrder = optimizedOrder; 
        }
        
        public List<String> getObsoleteTests() { return obsoleteTests; }
        public void setObsoleteTests(List<String> obsoleteTests) { 
            this.obsoleteTests = obsoleteTests; 
        }
        
        public double getOptimizationScore() { return optimizationScore; }
    }
    
    /**
     * Test improvement
     */
    public static class TestImprovement {
        private final String testKey;
        private final List<String> suggestions;
        
        public TestImprovement(String testKey, List<String> suggestions) {
            this.testKey = testKey;
            this.suggestions = new ArrayList<>(suggestions);
        }
        
        public String getTestKey() { return testKey; }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
    }
}

