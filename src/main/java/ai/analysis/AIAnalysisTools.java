package ai.analysis;

import ai.HealingTracker;
import ai.HealingStatistics;
import performanceTracker.AIPerformanceAnalyzer;
import reporting.TestLogManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom AI Analysis Tools
 * 
 * Provides various analysis tools for test automation
 */
public class AIAnalysisTools {
    
    /**
     * Analyze test stability
     */
    public TestStabilityAnalysis analyzeTestStability(String testName, List<TestExecution> executions) {
        if (executions == null || executions.isEmpty()) {
            return new TestStabilityAnalysis(testName, 0.0, 0, 0, new ArrayList<>());
        }
        
        int total = executions.size();
        int passed = (int) executions.stream().filter(e -> e.getStatus().equals("PASSED")).count();
        int failed = total - passed;
        double stability = (double) passed / total;
        
        List<String> issues = new ArrayList<>();
        if (stability < 0.8) {
            issues.add("Test stability is below 80%");
        }
        
        // Analyze failure patterns
        Map<String, Long> failureReasons = executions.stream()
            .filter(e -> e.getStatus().equals("FAILED"))
            .collect(Collectors.groupingBy(
                TestExecution::getFailureReason,
                Collectors.counting()
            ));
        
        if (!failureReasons.isEmpty()) {
            String mostCommonReason = failureReasons.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
            issues.add("Most common failure: " + mostCommonReason);
        }
        
        return new TestStabilityAnalysis(testName, stability, passed, failed, issues);
    }
    
    /**
     * Analyze locator reliability
     */
    public LocatorReliabilityAnalysis analyzeLocatorReliability() {
        HealingTracker tracker = HealingTracker.getInstance();
        List<String> elementsNeedingUpdate = tracker.getElementsNeedingUpdate();
        
        Map<String, HealingStatistics> allStats = new HashMap<>();
        for (String elementName : elementsNeedingUpdate) {
            HealingStatistics stats = tracker.getStatistics(elementName);
            if (stats != null) {
                allStats.put(elementName, stats);
            }
        }
        
        return new LocatorReliabilityAnalysis(allStats, elementsNeedingUpdate);
    }
    
    /**
     * Analyze performance trends
     */
    public PerformanceTrendAnalysis analyzePerformanceTrends() {
        AIPerformanceAnalyzer analyzer = new AIPerformanceAnalyzer();
        Map<String, Object> insights = analyzer.generateInsights();
        
        Map<String, Object> trends = (Map<String, Object>) insights.getOrDefault("trends", new HashMap<>());
        double overallScore = (Double) insights.getOrDefault("overallScore", 0.0);
        List<String> recommendations = (List<String>) insights.getOrDefault("recommendations", new ArrayList<>());
        
        return new PerformanceTrendAnalysis(trends, overallScore, recommendations);
    }
    
    /**
     * Generate test health report
     */
    public TestHealthReport generateTestHealthReport(List<TestExecution> executions) {
        Map<String, TestStabilityAnalysis> stabilityMap = new HashMap<>();
        
        // Group by test name
        Map<String, List<TestExecution>> byTest = executions.stream()
            .collect(Collectors.groupingBy(TestExecution::getTestName));
        
        for (Map.Entry<String, List<TestExecution>> entry : byTest.entrySet()) {
            stabilityMap.put(entry.getKey(), analyzeTestStability(entry.getKey(), entry.getValue()));
        }
        
        // Overall statistics
        int totalTests = stabilityMap.size();
        long healthyTests = stabilityMap.values().stream()
            .filter(a -> a.getStability() >= 0.8)
            .count();
        
        double overallHealth = totalTests > 0 ? (double) healthyTests / totalTests : 0.0;
        
        return new TestHealthReport(stabilityMap, overallHealth, totalTests, (int) healthyTests);
    }
    
    /**
     * Test Stability Analysis
     */
    public static class TestStabilityAnalysis {
        private final String testName;
        private final double stability;
        private final int passed;
        private final int failed;
        private final List<String> issues;
        
        public TestStabilityAnalysis(String testName, double stability, int passed, int failed, List<String> issues) {
            this.testName = testName;
            this.stability = stability;
            this.passed = passed;
            this.failed = failed;
            this.issues = issues;
        }
        
        public String getTestName() { return testName; }
        public double getStability() { return stability; }
        public int getPassed() { return passed; }
        public int getFailed() { return failed; }
        public List<String> getIssues() { return issues; }
    }
    
    /**
     * Locator Reliability Analysis
     */
    public static class LocatorReliabilityAnalysis {
        private final Map<String, HealingStatistics> statistics;
        private final List<String> elementsNeedingUpdate;
        
        public LocatorReliabilityAnalysis(Map<String, HealingStatistics> statistics, List<String> elementsNeedingUpdate) {
            this.statistics = statistics;
            this.elementsNeedingUpdate = elementsNeedingUpdate;
        }
        
        public Map<String, HealingStatistics> getStatistics() { return statistics; }
        public List<String> getElementsNeedingUpdate() { return elementsNeedingUpdate; }
    }
    
    /**
     * Performance Trend Analysis
     */
    public static class PerformanceTrendAnalysis {
        private final Map<String, Object> trends;
        private final double overallScore;
        private final List<String> recommendations;
        
        public PerformanceTrendAnalysis(Map<String, Object> trends, double overallScore, List<String> recommendations) {
            this.trends = trends;
            this.overallScore = overallScore;
            this.recommendations = recommendations;
        }
        
        public Map<String, Object> getTrends() { return trends; }
        public double getOverallScore() { return overallScore; }
        public List<String> getRecommendations() { return recommendations; }
    }
    
    /**
     * Test Health Report
     */
    public static class TestHealthReport {
        private final Map<String, TestStabilityAnalysis> stabilityMap;
        private final double overallHealth;
        private final int totalTests;
        private final int healthyTests;
        
        public TestHealthReport(Map<String, TestStabilityAnalysis> stabilityMap, double overallHealth, 
                               int totalTests, int healthyTests) {
            this.stabilityMap = stabilityMap;
            this.overallHealth = overallHealth;
            this.totalTests = totalTests;
            this.healthyTests = healthyTests;
        }
        
        public Map<String, TestStabilityAnalysis> getStabilityMap() { return stabilityMap; }
        public double getOverallHealth() { return overallHealth; }
        public int getTotalTests() { return totalTests; }
        public int getHealthyTests() { return healthyTests; }
    }
    
    /**
     * Test Execution model
     */
    public static class TestExecution {
        private final String testName;
        private final String status;
        private final String failureReason;
        private final long executionTime;
        
        public TestExecution(String testName, String status, String failureReason, long executionTime) {
            this.testName = testName;
            this.status = status;
            this.failureReason = failureReason;
            this.executionTime = executionTime;
        }
        
        public String getTestName() { return testName; }
        public String getStatus() { return status; }
        public String getFailureReason() { return failureReason; }
        public long getExecutionTime() { return executionTime; }
    }
}

