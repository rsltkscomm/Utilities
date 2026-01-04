package listeners.retry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates stability scores for tests and tracks trends
 */
public class TestStabilityScorer {
    
    private final FlakyTestDetector detector;
    private final Map<String, StabilityScore> stabilityScores;
    private final Map<String, List<StabilityTrend>> stabilityTrends;
    
    public TestStabilityScorer(FlakyTestDetector detector) {
        this.detector = detector;
        this.stabilityScores = new HashMap<>();
        this.stabilityTrends = new HashMap<>();
    }
    
    /**
     * Calculate stability score for a test
     */
    public StabilityScore calculateStabilityScore(String testKey) {
        FlakyTestDetector.TestExecutionHistory history = detector.getHistory(testKey);
        if (history == null) {
            return new StabilityScore(testKey, 0.0, StabilityLevel.UNKNOWN, 0, 0, 0);
        }
        
        int totalExecutions = history.getTotalExecutions();
        int passedCount = history.getPassedCount();
        int failedCount = history.getFailedCount();
        double successRate = history.getSuccessRate();
        double failureRate = history.getFailureRate();
        
        // Calculate stability score (0-100)
        double stabilityScore = successRate * 100;
        
        // Adjust score based on consistency
        double consistencyBonus = calculateConsistencyBonus(history);
        stabilityScore += consistencyBonus;
        
        // Clamp to 0-100
        stabilityScore = Math.max(0, Math.min(100, stabilityScore));
        
        // Determine stability level
        StabilityLevel level = determineStabilityLevel(stabilityScore, failureRate);
        
        StabilityScore score = new StabilityScore(
            testKey, stabilityScore, level, totalExecutions, passedCount, failedCount
        );
        
        stabilityScores.put(testKey, score);
        
        // Track trend
        recordTrend(testKey, stabilityScore);
        
        return score;
    }
    
    /**
     * Calculate consistency bonus based on recent execution pattern
     */
    private double calculateConsistencyBonus(FlakyTestDetector.TestExecutionHistory history) {
        List<FlakyTestDetector.ExecutionRecord> recent = history.getRecentExecutions(10);
        if (recent.size() < 3) {
            return 0;
        }
        
        // Check if recent executions are consistent
        boolean allPassed = recent.stream().allMatch(FlakyTestDetector.ExecutionRecord::isPassed);
        boolean allFailed = recent.stream().noneMatch(FlakyTestDetector.ExecutionRecord::isPassed);
        
        if (allPassed) {
            return 5.0; // Bonus for consistent passes
        } else if (allFailed) {
            return -10.0; // Penalty for consistent failures
        }
        
        // Mixed results - check pattern
        int alternations = countAlternations(recent);
        if (alternations > recent.size() / 2) {
            return -5.0; // Penalty for high alternation (flaky)
        }
        
        return 0;
    }
    
    /**
     * Count alternations between pass/fail
     */
    private int countAlternations(List<FlakyTestDetector.ExecutionRecord> records) {
        if (records.size() < 2) {
            return 0;
        }
        
        int alternations = 0;
        boolean previous = records.get(0).isPassed();
        
        for (int i = 1; i < records.size(); i++) {
            boolean current = records.get(i).isPassed();
            if (current != previous) {
                alternations++;
            }
            previous = current;
        }
        
        return alternations;
    }
    
    /**
     * Determine stability level
     */
    private StabilityLevel determineStabilityLevel(double score, double failureRate) {
        if (score >= 95 && failureRate < 0.05) {
            return StabilityLevel.STABLE;
        } else if (score >= 80 && failureRate < 0.20) {
            return StabilityLevel.MOSTLY_STABLE;
        } else if (score >= 60 && failureRate < 0.40) {
            return StabilityLevel.UNSTABLE;
        } else if (failureRate >= 0.40) {
            return StabilityLevel.FLAKY;
        } else {
            return StabilityLevel.UNKNOWN;
        }
    }
    
    /**
     * Record stability trend
     */
    private void recordTrend(String testKey, double score) {
        stabilityTrends.computeIfAbsent(testKey, k -> new ArrayList<>())
            .add(new StabilityTrend(System.currentTimeMillis(), score));
        
        // Keep only last 50 trend points
        List<StabilityTrend> trends = stabilityTrends.get(testKey);
        if (trends.size() > 50) {
            trends.remove(0);
        }
    }
    
    /**
     * Get stability score
     */
    public StabilityScore getStabilityScore(String testKey) {
        return stabilityScores.getOrDefault(testKey, calculateStabilityScore(testKey));
    }
    
    /**
     * Get all stability scores
     */
    public Map<String, StabilityScore> getAllStabilityScores() {
        // Recalculate all scores
        for (String testKey : detector.getAllTestKeys()) {
            calculateStabilityScore(testKey);
        }
        return new HashMap<>(stabilityScores);
    }
    
    /**
     * Get tests with degrading stability
     */
    public List<DegradingTest> getDegradingTests() {
        List<DegradingTest> degradingTests = new ArrayList<>();
        
        for (Map.Entry<String, List<StabilityTrend>> entry : stabilityTrends.entrySet()) {
            List<StabilityTrend> trends = entry.getValue();
            if (trends.size() < 5) {
                continue; // Need at least 5 data points
            }
            
            // Calculate trend (simple linear regression slope)
            double slope = calculateTrendSlope(trends);
            
            if (slope < -2.0) { // Degrading by more than 2 points per data point
                StabilityScore current = getStabilityScore(entry.getKey());
                degradingTests.add(new DegradingTest(
                    entry.getKey(),
                    current,
                    slope,
                    trends.get(0).getScore(), // First score
                    trends.get(trends.size() - 1).getScore() // Latest score
                ));
            }
        }
        
        return degradingTests.stream()
            .sorted(Comparator.comparing(DegradingTest::getDegradationRate))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate trend slope
     */
    private double calculateTrendSlope(List<StabilityTrend> trends) {
        if (trends.size() < 2) {
            return 0;
        }
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = trends.size();
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = trends.get(i).getScore();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * Get stability trend for a test
     */
    public List<StabilityTrend> getStabilityTrend(String testKey) {
        return new ArrayList<>(stabilityTrends.getOrDefault(testKey, new ArrayList<>()));
    }
    
    /**
     * Stability score
     */
    public static class StabilityScore {
        private final String testKey;
        private final double score;
        private final StabilityLevel level;
        private final int totalExecutions;
        private final int passedCount;
        private final int failedCount;
        
        public StabilityScore(String testKey, double score, StabilityLevel level,
                             int totalExecutions, int passedCount, int failedCount) {
            this.testKey = testKey;
            this.score = score;
            this.level = level;
            this.totalExecutions = totalExecutions;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
        }
        
        public String getTestKey() { return testKey; }
        public double getScore() { return score; }
        public StabilityLevel getLevel() { return level; }
        public int getTotalExecutions() { return totalExecutions; }
        public int getPassedCount() { return passedCount; }
        public int getFailedCount() { return failedCount; }
        
        @Override
        public String toString() {
            return String.format("StabilityScore[%s: %.1f%% (%s)]", 
                testKey, score, level);
        }
    }
    
    /**
     * Stability level
     */
    public enum StabilityLevel {
        STABLE,           // 95-100% success rate
        MOSTLY_STABLE,    // 80-94% success rate
        UNSTABLE,         // 60-79% success rate
        FLAKY,            // <60% success rate
        UNKNOWN           // Insufficient data
    }
    
    /**
     * Stability trend point
     */
    public static class StabilityTrend {
        private final long timestamp;
        private final double score;
        
        public StabilityTrend(long timestamp, double score) {
            this.timestamp = timestamp;
            this.score = score;
        }
        
        public long getTimestamp() { return timestamp; }
        public double getScore() { return score; }
    }
    
    /**
     * Degrading test information
     */
    public static class DegradingTest {
        private final String testKey;
        private final StabilityScore currentScore;
        private final double degradationRate;
        private final double initialScore;
        private final double latestScore;
        
        public DegradingTest(String testKey, StabilityScore currentScore, 
                            double degradationRate, double initialScore, double latestScore) {
            this.testKey = testKey;
            this.currentScore = currentScore;
            this.degradationRate = degradationRate;
            this.initialScore = initialScore;
            this.latestScore = latestScore;
        }
        
        public String getTestKey() { return testKey; }
        public StabilityScore getCurrentScore() { return currentScore; }
        public double getDegradationRate() { return degradationRate; }
        public double getInitialScore() { return initialScore; }
        public double getLatestScore() { return latestScore; }
        
        @Override
        public String toString() {
            return String.format("DegradingTest[%s: %.1f%% -> %.1f%% (%.2f/point)]", 
                testKey, initialScore, latestScore, degradationRate);
        }
    }
}

