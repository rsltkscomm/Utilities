package performanceTracker;

import org.springframework.stereotype.Component;

import config.ConfigurationManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Comparative Performance Analyzer
 * 
 * Provides comprehensive comparison analysis between:
 * - Different releases and versions
 * - Previous and current test runs
 * - Baseline and current performance
 * - A/B test results
 * - Performance trends over time
 */
@Component
public class ComparativeAnalyzer {
    
    private final Map<String, List<PerformanceMetrics>> historicalData = new ConcurrentHashMap<>();
    private final Map<String, ReleaseComparison> releaseComparisons = new ConcurrentHashMap<>();
    private final Map<String, TrendAnalysis> trendAnalyses = new ConcurrentHashMap<>();
    private final ConfigurationManager config;
    
    public ComparativeAnalyzer() {
        this.config = ConfigurationManager.getInstance();
    }
    
    /**
     * Compare performance between two releases
     */
    public ReleaseComparison compareReleases(String testCaseKey) {
        ReleaseComparison comparison = new ReleaseComparison();
        comparison.setTestCaseKey(testCaseKey);
        comparison.setTimestamp(System.currentTimeMillis());
        
        try {
            List<PerformanceMetrics> currentData = historicalData.get(testCaseKey + "_current");
            List<PerformanceMetrics> previousData = historicalData.get(testCaseKey + "_previous");
            
            if (currentData == null || previousData == null || currentData.isEmpty() || previousData.isEmpty()) {
                comparison.setError("Insufficient data for comparison");
                return comparison;
            }
            
            // Calculate comparison metrics
            calculateReleaseComparisonMetrics(comparison, currentData, previousData);
            
            // Identify significant changes
            identifySignificantChanges(comparison, currentData, previousData);
            
            // Generate insights
            generateComparisonInsights(comparison);
            
            // Calculate performance score
            comparison.setOverallPerformanceScore(calculateOverallPerformanceScore(comparison));
            
        } catch (Exception e) {
            comparison.setError("Error in release comparison: " + e.getMessage());
        }
        
        return comparison;
    }
    
    /**
     * Compare current run with previous run
     */
    public RunComparison compareRuns(String testCaseKey, String currentRunId, String previousRunId) {
        RunComparison comparison = new RunComparison();
        comparison.setTestCaseKey(testCaseKey);
        comparison.setCurrentRunId(currentRunId);
        comparison.setPreviousRunId(previousRunId);
        comparison.setTimestamp(System.currentTimeMillis());
        
        try {
            List<PerformanceMetrics> currentData = historicalData.get(testCaseKey + "_" + currentRunId);
            List<PerformanceMetrics> previousData = historicalData.get(testCaseKey + "_" + previousRunId);
            
            if (currentData == null || previousData == null || currentData.isEmpty() || previousData.isEmpty()) {
                comparison.setError("Insufficient data for run comparison");
                return comparison;
            }
            
            // Calculate run comparison metrics
            calculateRunComparisonMetrics(comparison, currentData, previousData);
            
            // Analyze performance changes
            analyzePerformanceChanges(comparison, currentData, previousData);
            
            // Generate recommendations
            generateRunComparisonRecommendations(comparison);
            
        } catch (Exception e) {
            comparison.setError("Error in run comparison: " + e.getMessage());
        }
        
        return comparison;
    }
    
    /**
     * Analyze performance trends over time
     */
    public TrendAnalysis analyzeTrends(String testCaseKey, int daysBack) {
        TrendAnalysis trendAnalysis = new TrendAnalysis();
        trendAnalysis.setTestCaseKey(testCaseKey);
        trendAnalysis.setDaysBack(daysBack);
        trendAnalysis.setTimestamp(System.currentTimeMillis());
        
        try {
            List<PerformanceMetrics> allData = historicalData.get(testCaseKey);
            
            if (allData == null || allData.isEmpty()) {
                trendAnalysis.setError("No historical data available for trend analysis");
                return trendAnalysis;
            }
            
            // Filter data by time range
            long cutoffTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L);
            List<PerformanceMetrics> recentData = allData.stream()
                .filter(metric -> metric.getTimestamp() >= cutoffTime)
                .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                .collect(Collectors.toList());
            
            if (recentData.isEmpty()) {
                trendAnalysis.setError("No data in the specified time range");
                return trendAnalysis;
            }
            
            // Calculate trend metrics
            calculateTrendMetrics(trendAnalysis, recentData);
            
            // Identify trend patterns
            identifyTrendPatterns(trendAnalysis, recentData);
            
            // Generate trend predictions
            generateTrendPredictions(trendAnalysis, recentData);
            
            // Calculate trend score
            trendAnalysis.setTrendScore(calculateTrendScore(trendAnalysis));
            
        } catch (Exception e) {
            trendAnalysis.setError("Error in trend analysis: " + e.getMessage());
        }
        
        return trendAnalysis;
    }
    
    /**
     * Perform A/B test comparison
     */
    public ABTestComparison compareABTest(String testCaseKey, String variantA, String variantB) {
        ABTestComparison comparison = new ABTestComparison();
        comparison.setTestCaseKey(testCaseKey);
        comparison.setVariantA(variantA);
        comparison.setVariantB(variantB);
        comparison.setTimestamp(System.currentTimeMillis());
        
        try {
            List<PerformanceMetrics> variantAData = historicalData.get(testCaseKey + "_" + variantA);
            List<PerformanceMetrics> variantBData = historicalData.get(testCaseKey + "_" + variantB);
            
            if (variantAData == null || variantBData == null || variantAData.isEmpty() || variantBData.isEmpty()) {
                comparison.setError("Insufficient data for A/B test comparison");
                return comparison;
            }
            
            // Calculate A/B test metrics
            calculateABTestMetrics(comparison, variantAData, variantBData);
            
            // Perform statistical significance test
            performStatisticalSignificanceTest(comparison, variantAData, variantBData);
            
            // Determine winning variant
            determineWinningVariant(comparison);
            
            // Generate A/B test insights
            generateABTestInsights(comparison);
            
        } catch (Exception e) {
            comparison.setError("Error in A/B test comparison: " + e.getMessage());
        }
        
        return comparison;
    }
    
    /**
     * Compare against baseline performance
     */
    public BaselineComparison compareAgainstBaseline(String testCaseKey) {
        BaselineComparison comparison = new BaselineComparison();
        comparison.setTestCaseKey(testCaseKey);
        comparison.setTimestamp(System.currentTimeMillis());
        
        try {
            List<PerformanceMetrics> currentData = historicalData.get(testCaseKey + "_current");
            List<PerformanceMetrics> baselineData = historicalData.get(testCaseKey + "_baseline");
            
            if (currentData == null || baselineData == null || currentData.isEmpty() || baselineData.isEmpty()) {
                comparison.setError("Insufficient data for baseline comparison");
                return comparison;
            }
            
            // Calculate baseline comparison metrics
            calculateBaselineComparisonMetrics(comparison, currentData, baselineData);
            
            // Identify performance regressions
            identifyPerformanceRegressions(comparison, currentData, baselineData);
            
            // Generate baseline insights
            generateBaselineInsights(comparison);
            
            // Calculate regression score
            comparison.setRegressionScore(calculateRegressionScore(comparison));
            
        } catch (Exception e) {
            comparison.setError("Error in baseline comparison: " + e.getMessage());
        }
        
        return comparison;
    }
    
    /**
     * Calculate release comparison metrics
     */
    private void calculateReleaseComparisonMetrics(ReleaseComparison comparison, 
                                                 List<PerformanceMetrics> currentData, 
                                                 List<PerformanceMetrics> previousData) {
        
        // Calculate averages for current release
        PerformanceMetrics currentAvg = calculateAverageMetrics(currentData);
        PerformanceMetrics previousAvg = calculateAverageMetrics(previousData);
        
        // Calculate percentage changes
        comparison.setPageLoadTimeChange(calculatePercentageChange(previousAvg.getAveragePageLoadTime(), currentAvg.getAveragePageLoadTime()));
        comparison.setApiResponseTimeChange(calculatePercentageChange(previousAvg.getAverageApiResponseTime(), currentAvg.getAverageApiResponseTime()));
        comparison.setWebVitalsScoreChange(calculatePercentageChange(previousAvg.getWebVitalsScore(), currentAvg.getWebVitalsScore()));
        comparison.setMemoryUsageChange(calculatePercentageChange(previousAvg.getMemoryUsage(), currentAvg.getMemoryUsage()));
        comparison.setCpuUsageChange(calculatePercentageChange(previousAvg.getCpuUsage(), currentAvg.getCpuUsage()));
        
        // Calculate Web Vitals changes
        comparison.setLcpChange(calculatePercentageChange(previousAvg.getLcp(), currentAvg.getLcp()));
        comparison.setClsChange(calculatePercentageChange(previousAvg.getCls(), currentAvg.getCls()));
        comparison.setFcpChange(calculatePercentageChange(previousAvg.getFcp(), currentAvg.getFcp()));
        comparison.setTtfbChange(calculatePercentageChange(previousAvg.getTtfb(), currentAvg.getTtfb()));
        comparison.setInpChange(calculatePercentageChange(previousAvg.getInp(), currentAvg.getInp()));
        comparison.setFidChange(calculatePercentageChange(previousAvg.getFid(), currentAvg.getFid()));
        
        // Calculate resource changes
        comparison.setResourceCountChange(calculatePercentageChange(previousAvg.getResourceCount(), currentAvg.getResourceCount()));
        comparison.setJavascriptSizeChange(calculatePercentageChange(previousAvg.getJavascriptSize(), currentAvg.getJavascriptSize()));
        comparison.setCssSizeChange(calculatePercentageChange(previousAvg.getCssSize(), currentAvg.getCssSize()));
        comparison.setImageSizeChange(calculatePercentageChange(previousAvg.getImageSize(), currentAvg.getImageSize()));
        
        // Calculate API changes
        comparison.setApiCallsChange(calculatePercentageChange(previousAvg.getApiCalls(), currentAvg.getApiCalls()));
        comparison.setSlowApiCallsChange(calculatePercentageChange(previousAvg.getSlowApiCalls(), currentAvg.getSlowApiCalls()));
        comparison.setFailedApiCallsChange(calculatePercentageChange(previousAvg.getFailedApiCalls(), currentAvg.getFailedApiCalls()));
        
        // Store current and previous averages
        comparison.setCurrentAverage(currentAvg);
        comparison.setPreviousAverage(previousAvg);
    }
    
    /**
     * Calculate run comparison metrics
     */
    private void calculateRunComparisonMetrics(RunComparison comparison, 
                                             List<PerformanceMetrics> currentData, 
                                             List<PerformanceMetrics> previousData) {
        
        // Calculate averages
        PerformanceMetrics currentAvg = calculateAverageMetrics(currentData);
        PerformanceMetrics previousAvg = calculateAverageMetrics(previousData);
        
        // Calculate changes
        comparison.setPageLoadTimeChange(calculatePercentageChange(previousAvg.getAveragePageLoadTime(), currentAvg.getAveragePageLoadTime()));
        comparison.setApiResponseTimeChange(calculatePercentageChange(previousAvg.getAverageApiResponseTime(), currentAvg.getAverageApiResponseTime()));
        comparison.setWebVitalsScoreChange(calculatePercentageChange(previousAvg.getWebVitalsScore(), currentAvg.getWebVitalsScore()));
        comparison.setMemoryUsageChange(calculatePercentageChange(previousAvg.getMemoryUsage(), currentAvg.getMemoryUsage()));
        
        // Calculate test execution changes
        comparison.setTestDurationChange(calculatePercentageChange(previousAvg.getTestDuration(), currentAvg.getTestDuration()));
        comparison.setActiveTestsChange(calculatePercentageChange(previousAvg.getActiveTests(), currentAvg.getActiveTests()));
        comparison.setCompletedTestsChange(calculatePercentageChange(previousAvg.getCompletedTests(), currentAvg.getCompletedTests()));
        comparison.setFailedTestsChange(calculatePercentageChange(previousAvg.getFailedTests(), currentAvg.getFailedTests()));
        
        // Store averages
        comparison.setCurrentAverage(currentAvg);
        comparison.setPreviousAverage(previousAvg);
    }
    
    /**
     * Calculate trend metrics
     */
    private void calculateTrendMetrics(TrendAnalysis trendAnalysis, List<PerformanceMetrics> data) {
        if (data.size() < 2) {
            return;
        }
        
        // Calculate trend for each metric
        trendAnalysis.setPageLoadTimeTrend(calculateTrend(data, m -> m.getAveragePageLoadTime()));
        trendAnalysis.setApiResponseTimeTrend(calculateTrend(data, m -> m.getAverageApiResponseTime()));
        trendAnalysis.setWebVitalsScoreTrend(calculateTrend(data, m -> m.getWebVitalsScore()));
        trendAnalysis.setMemoryUsageTrend(calculateTrend(data, m -> m.getMemoryUsage()));
        trendAnalysis.setCpuUsageTrend(calculateTrend(data, m -> m.getCpuUsage()));
        
        // Calculate Web Vitals trends
        trendAnalysis.setLcpTrend(calculateTrend(data, m -> m.getLcp()));
        trendAnalysis.setClsTrend(calculateTrend(data, m -> m.getCls()));
        trendAnalysis.setFcpTrend(calculateTrend(data, m -> m.getFcp()));
        trendAnalysis.setTtfbTrend(calculateTrend(data, m -> m.getTtfb()));
        trendAnalysis.setInpTrend(calculateTrend(data, m -> m.getInp()));
        trendAnalysis.setFidTrend(calculateTrend(data, m -> m.getFid()));
        
        // Calculate volatility (standard deviation)
        trendAnalysis.setPageLoadTimeVolatility(calculateVolatility(data, m -> m.getAveragePageLoadTime()));
        trendAnalysis.setApiResponseTimeVolatility(calculateVolatility(data, m -> m.getAverageApiResponseTime()));
        trendAnalysis.setWebVitalsScoreVolatility(calculateVolatility(data, m -> m.getWebVitalsScore()));
        
        // Calculate correlation with time
        trendAnalysis.setTimeCorrelation(calculateTimeCorrelation(data));
    }
    
    /**
     * Calculate A/B test metrics
     */
    private void calculateABTestMetrics(ABTestComparison comparison, 
                                      List<PerformanceMetrics> variantAData, 
                                      List<PerformanceMetrics> variantBData) {
        
        PerformanceMetrics variantAAvg = calculateAverageMetrics(variantAData);
        PerformanceMetrics variantBAvg = calculateAverageMetrics(variantBData);
        
        // Calculate differences
        comparison.setPageLoadTimeDifference(variantBAvg.getAveragePageLoadTime() - variantAAvg.getAveragePageLoadTime());
        comparison.setApiResponseTimeDifference(variantBAvg.getAverageApiResponseTime() - variantAAvg.getAverageApiResponseTime());
        comparison.setWebVitalsScoreDifference(variantBAvg.getWebVitalsScore() - variantAAvg.getWebVitalsScore());
        comparison.setMemoryUsageDifference(variantBAvg.getMemoryUsage() - variantAAvg.getMemoryUsage());
        
        // Calculate percentage improvements
        comparison.setPageLoadTimeImprovement(calculatePercentageChange(variantAAvg.getAveragePageLoadTime(), variantBAvg.getAveragePageLoadTime()));
        comparison.setApiResponseTimeImprovement(calculatePercentageChange(variantAAvg.getAverageApiResponseTime(), variantBAvg.getAverageApiResponseTime()));
        comparison.setWebVitalsScoreImprovement(calculatePercentageChange(variantAAvg.getWebVitalsScore(), variantBAvg.getWebVitalsScore()));
        
        // Store averages
        comparison.setVariantAAverage(variantAAvg);
        comparison.setVariantBAverage(variantBAvg);
    }
    
    /**
     * Calculate baseline comparison metrics
     */
    private void calculateBaselineComparisonMetrics(BaselineComparison comparison, 
                                                  List<PerformanceMetrics> currentData, 
                                                  List<PerformanceMetrics> baselineData) {
        
        PerformanceMetrics currentAvg = calculateAverageMetrics(currentData);
        PerformanceMetrics baselineAvg = calculateAverageMetrics(baselineData);
        
        // Calculate deviations from baseline
        comparison.setPageLoadTimeDeviation(calculatePercentageChange(baselineAvg.getAveragePageLoadTime(), currentAvg.getAveragePageLoadTime()));
        comparison.setApiResponseTimeDeviation(calculatePercentageChange(baselineAvg.getAverageApiResponseTime(), currentAvg.getAverageApiResponseTime()));
        comparison.setWebVitalsScoreDeviation(calculatePercentageChange(baselineAvg.getWebVitalsScore(), currentAvg.getWebVitalsScore()));
        comparison.setMemoryUsageDeviation(calculatePercentageChange(baselineAvg.getMemoryUsage(), currentAvg.getMemoryUsage()));
        
        // Calculate regression indicators
        comparison.setPageLoadTimeRegression(currentAvg.getAveragePageLoadTime() > baselineAvg.getAveragePageLoadTime() * 1.1); // 10% regression
        comparison.setApiResponseTimeRegression(currentAvg.getAverageApiResponseTime() > baselineAvg.getAverageApiResponseTime() * 1.1);
        comparison.setWebVitalsScoreRegression(currentAvg.getWebVitalsScore() < baselineAvg.getWebVitalsScore() * 0.9); // 10% regression
        
        // Store averages
        comparison.setCurrentAverage(currentAvg);
        comparison.setBaselineAverage(baselineAvg);
    }
    
    /**
     * Identify significant changes
     */
    private void identifySignificantChanges(ReleaseComparison comparison, 
                                          List<PerformanceMetrics> currentData, 
                                          List<PerformanceMetrics> previousData) {
        
        List<String> significantChanges = new ArrayList<>();
        
        // Define significance thresholds
        double significantThreshold = 10.0; // 10% change is significant
        
        if (Math.abs(comparison.getPageLoadTimeChange()) > significantThreshold) {
            String change = String.format("Page Load Time %s by %.1f%%", 
                comparison.getPageLoadTimeChange() > 0 ? "increased" : "decreased", 
                Math.abs(comparison.getPageLoadTimeChange()));
            significantChanges.add(change);
        }
        
        if (Math.abs(comparison.getApiResponseTimeChange()) > significantThreshold) {
            String change = String.format("API Response Time %s by %.1f%%", 
                comparison.getApiResponseTimeChange() > 0 ? "increased" : "decreased", 
                Math.abs(comparison.getApiResponseTimeChange()));
            significantChanges.add(change);
        }
        
        if (Math.abs(comparison.getWebVitalsScoreChange()) > significantThreshold) {
            String change = String.format("Web Vitals Score %s by %.1f%%", 
                comparison.getWebVitalsScoreChange() > 0 ? "improved" : "degraded", 
                Math.abs(comparison.getWebVitalsScoreChange()));
            significantChanges.add(change);
        }
        
        if (Math.abs(comparison.getLcpChange()) > significantThreshold) {
            String change = String.format("LCP %s by %.1f%%", 
                comparison.getLcpChange() > 0 ? "increased" : "decreased", 
                Math.abs(comparison.getLcpChange()));
            significantChanges.add(change);
        }
        
        comparison.setSignificantChanges(significantChanges);
    }
    
    /**
     * Analyze performance changes
     */
    private void analyzePerformanceChanges(RunComparison comparison, 
                                         List<PerformanceMetrics> currentData, 
                                         List<PerformanceMetrics> previousData) {
        
        List<String> performanceChanges = new ArrayList<>();
        
        // Analyze page load time changes
        if (comparison.getPageLoadTimeChange() > 5) {
            performanceChanges.add("Page load time increased - investigate potential performance regression");
        } else if (comparison.getPageLoadTimeChange() < -5) {
            performanceChanges.add("Page load time improved - performance optimization successful");
        }
        
        // Analyze API response time changes
        if (comparison.getApiResponseTimeChange() > 10) {
            performanceChanges.add("API response time increased - check backend performance");
        } else if (comparison.getApiResponseTimeChange() < -10) {
            performanceChanges.add("API response time improved - backend optimization effective");
        }
        
        // Analyze test execution changes
        if (comparison.getFailedTestsChange() > 0) {
            performanceChanges.add("Test failure rate increased - investigate test stability");
        }
        
        if (comparison.getTestDurationChange() > 20) {
            performanceChanges.add("Test execution time increased significantly - check test efficiency");
        }
        
        comparison.setPerformanceChanges(performanceChanges);
    }
    
    /**
     * Identify trend patterns
     */
    private void identifyTrendPatterns(TrendAnalysis trendAnalysis, List<PerformanceMetrics> data) {
        List<String> patterns = new ArrayList<>();
        
        // Check for improving trends
        if (trendAnalysis.getPageLoadTimeTrend() < -5) {
            patterns.add("Page load time showing improving trend");
        } else if (trendAnalysis.getPageLoadTimeTrend() > 5) {
            patterns.add("Page load time showing degrading trend");
        }
        
        if (trendAnalysis.getWebVitalsScoreTrend() > 5) {
            patterns.add("Web Vitals score showing improving trend");
        } else if (trendAnalysis.getWebVitalsScoreTrend() < -5) {
            patterns.add("Web Vitals score showing degrading trend");
        }
        
        // Check for high volatility
        if (trendAnalysis.getPageLoadTimeVolatility() > 500) {
            patterns.add("High volatility in page load time - inconsistent performance");
        }
        
        if (trendAnalysis.getWebVitalsScoreVolatility() > 20) {
            patterns.add("High volatility in Web Vitals score - performance inconsistency");
        }
        
        trendAnalysis.setPatterns(patterns);
    }
    
    /**
     * Perform statistical significance test
     */
    private void performStatisticalSignificanceTest(ABTestComparison comparison, 
                                                   List<PerformanceMetrics> variantAData, 
                                                   List<PerformanceMetrics> variantBData) {
        
        // Simple t-test simulation (in real implementation, use proper statistical library)
        double variantAMean = variantAData.stream().mapToDouble(PerformanceMetrics::getAveragePageLoadTime).average().orElse(0.0);
        double variantBMean = variantBData.stream().mapToDouble(PerformanceMetrics::getAveragePageLoadTime).average().orElse(0.0);
        
        double variantAVariance = calculateVariance(variantAData, m -> m.getAveragePageLoadTime());
        double variantBVariance = calculateVariance(variantBData, m -> m.getAveragePageLoadTime());
        
        // Simplified significance calculation
        double standardError = Math.sqrt((variantAVariance / variantAData.size()) + (variantBVariance / variantBData.size()));
        double tStatistic = Math.abs(variantBMean - variantAMean) / standardError;
        
        // Assume degrees of freedom = n1 + n2 - 2
        int degreesOfFreedom = variantAData.size() + variantBData.size() - 2;
        
        // Simplified p-value calculation (in real implementation, use proper statistical library)
        double pValue = Math.exp(-tStatistic * tStatistic / 2);
        
        comparison.setStatisticalSignificance(pValue < 0.05);
        comparison.setPValue(pValue);
        comparison.setTStatistic(tStatistic);
        comparison.setDegreesOfFreedom(degreesOfFreedom);
    }
    
    /**
     * Determine winning variant
     */
    private void determineWinningVariant(ABTestComparison comparison) {
        if (!comparison.isStatisticalSignificance()) {
            comparison.setWinningVariant("Inconclusive");
            comparison.setConfidenceLevel("Low");
            return;
        }
        
        // Determine winner based on page load time (lower is better)
        if (comparison.getPageLoadTimeImprovement() > 0) {
            comparison.setWinningVariant("Variant B");
            comparison.setConfidenceLevel("High");
        } else {
            comparison.setWinningVariant("Variant A");
            comparison.setConfidenceLevel("High");
        }
    }
    
    /**
     * Identify performance regressions
     */
    private void identifyPerformanceRegressions(BaselineComparison comparison, 
                                               List<PerformanceMetrics> currentData, 
                                               List<PerformanceMetrics> baselineData) {
        
        List<String> regressions = new ArrayList<>();
        
        if (comparison.isPageLoadTimeRegression()) {
            regressions.add(String.format("Page Load Time regression: %.1f%% slower than baseline", 
                comparison.getPageLoadTimeDeviation()));
        }
        
        if (comparison.isApiResponseTimeRegression()) {
            regressions.add(String.format("API Response Time regression: %.1f%% slower than baseline", 
                comparison.getApiResponseTimeDeviation()));
        }
        
        if (comparison.isWebVitalsScoreRegression()) {
            regressions.add(String.format("Web Vitals Score regression: %.1f%% lower than baseline", 
                Math.abs(comparison.getWebVitalsScoreDeviation())));
        }
        
        comparison.setRegressions(regressions);
    }
    
    /**
     * Generate comparison insights
     */
    private void generateComparisonInsights(ReleaseComparison comparison) {
        List<String> insights = new ArrayList<>();
        
        if (comparison.getPageLoadTimeChange() < -10) {
            insights.add("Significant improvement in page load time - great optimization work!");
        } else if (comparison.getPageLoadTimeChange() > 10) {
            insights.add("Page load time regression detected - investigate recent changes");
        }
        
        if (comparison.getWebVitalsScoreChange() > 10) {
            insights.add("Web Vitals score improved significantly - better user experience");
        } else if (comparison.getWebVitalsScoreChange() < -10) {
            insights.add("Web Vitals score declined - review Core Web Vitals optimization");
        }
        
        if (comparison.getApiResponseTimeChange() > 15) {
            insights.add("API response time increased - check backend performance and database queries");
        }
        
        comparison.setInsights(insights);
    }
    
    /**
     * Generate run comparison recommendations
     */
    private void generateRunComparisonRecommendations(RunComparison comparison) {
        List<String> recommendations = new ArrayList<>();
        
        if (comparison.getPageLoadTimeChange() > 5) {
            recommendations.add("Investigate page load time increase - check for new blocking resources");
        }
        
        if (comparison.getFailedTestsChange() > 0) {
            recommendations.add("Address test failures to improve reliability");
        }
        
        if (comparison.getTestDurationChange() > 20) {
            recommendations.add("Optimize test execution time - consider parallel execution");
        }
        
        comparison.setRecommendations(recommendations);
    }
    
    /**
     * Generate trend predictions
     */
    private void generateTrendPredictions(TrendAnalysis trendAnalysis, List<PerformanceMetrics> data) {
        List<String> predictions = new ArrayList<>();
        
        // Simple linear trend prediction
        if (trendAnalysis.getPageLoadTimeTrend() < -5) {
            predictions.add("Page load time expected to continue improving");
        } else if (trendAnalysis.getPageLoadTimeTrend() > 5) {
            predictions.add("Page load time expected to continue degrading - take action");
        }
        
        if (trendAnalysis.getWebVitalsScoreTrend() > 5) {
            predictions.add("Web Vitals score expected to continue improving");
        } else if (trendAnalysis.getWebVitalsScoreTrend() < -5) {
            predictions.add("Web Vitals score expected to continue declining - optimize Core Web Vitals");
        }
        
        trendAnalysis.setPredictions(predictions);
    }
    
    /**
     * Generate A/B test insights
     */
    private void generateABTestInsights(ABTestComparison comparison) {
        List<String> insights = new ArrayList<>();
        
        if (comparison.isStatisticalSignificance()) {
            insights.add(String.format("Statistically significant difference found (p-value: %.4f)", comparison.getPValue()));
            
            if (comparison.getPageLoadTimeImprovement() > 0) {
                insights.add(String.format("Variant B shows %.1f%% improvement in page load time", comparison.getPageLoadTimeImprovement()));
            } else {
                insights.add(String.format("Variant A shows %.1f%% better page load time", Math.abs(comparison.getPageLoadTimeImprovement())));
            }
        } else {
            insights.add("No statistically significant difference found - need more data");
        }
        
        comparison.setInsights(insights);
    }
    
    /**
     * Generate baseline insights
     */
    private void generateBaselineInsights(BaselineComparison comparison) {
        List<String> insights = new ArrayList<>();
        
        if (comparison.getRegressions().isEmpty()) {
            insights.add("No significant performance regressions detected - good baseline compliance");
        } else {
            insights.add("Performance regressions detected - review recent changes");
        }
        
        if (comparison.getPageLoadTimeDeviation() < -10) {
            insights.add("Significant improvement over baseline - excellent optimization");
        }
        
        if (comparison.getWebVitalsScoreDeviation() > 10) {
            insights.add("Web Vitals score significantly improved over baseline");
        }
        
        comparison.setInsights(insights);
    }
    
    /**
     * Calculate average metrics from a list
     */
    private PerformanceMetrics calculateAverageMetrics(List<PerformanceMetrics> data) {
        if (data.isEmpty()) {
            return new PerformanceMetrics();
        }
        
        PerformanceMetrics avg = new PerformanceMetrics();
        
        avg.setAveragePageLoadTime(data.stream().mapToDouble(PerformanceMetrics::getAveragePageLoadTime).average().orElse(0.0));
        avg.setAverageApiResponseTime(data.stream().mapToDouble(PerformanceMetrics::getAverageApiResponseTime).average().orElse(0.0));
        avg.setWebVitalsScore(data.stream().mapToDouble(PerformanceMetrics::getWebVitalsScore).average().orElse(0.0));
        avg.setMemoryUsage(data.stream().mapToDouble(PerformanceMetrics::getMemoryUsage).average().orElse(0.0));
        avg.setCpuUsage(data.stream().mapToDouble(PerformanceMetrics::getCpuUsage).average().orElse(0.0));
        
        avg.setLcp(data.stream().mapToDouble(PerformanceMetrics::getLcp).average().orElse(0.0));
        avg.setCls(data.stream().mapToDouble(PerformanceMetrics::getCls).average().orElse(0.0));
        avg.setFcp(data.stream().mapToDouble(PerformanceMetrics::getFcp).average().orElse(0.0));
        avg.setTtfb(data.stream().mapToDouble(PerformanceMetrics::getTtfb).average().orElse(0.0));
        avg.setInp(data.stream().mapToDouble(PerformanceMetrics::getInp).average().orElse(0.0));
        avg.setFid(data.stream().mapToDouble(PerformanceMetrics::getFid).average().orElse(0.0));
        
        avg.setResourceCount((int) data.stream().mapToInt(PerformanceMetrics::getResourceCount).average().orElse(0.0));
        avg.setJavascriptSize((long) data.stream().mapToLong(PerformanceMetrics::getJavascriptSize).average().orElse(0.0));
        avg.setCssSize((long) data.stream().mapToLong(PerformanceMetrics::getCssSize).average().orElse(0.0));
        avg.setImageSize((long) data.stream().mapToLong(PerformanceMetrics::getImageSize).average().orElse(0.0));
        
        avg.setApiCalls((int) data.stream().mapToInt(PerformanceMetrics::getApiCalls).average().orElse(0.0));
        avg.setSlowApiCalls((int) data.stream().mapToInt(PerformanceMetrics::getSlowApiCalls).average().orElse(0.0));
        avg.setFailedApiCalls((int) data.stream().mapToInt(PerformanceMetrics::getFailedApiCalls).average().orElse(0.0));
        
        avg.setTestDuration((long) data.stream().mapToLong(PerformanceMetrics::getTestDuration).average().orElse(0.0));
        avg.setActiveTests((int) data.stream().mapToInt(PerformanceMetrics::getActiveTests).average().orElse(0.0));
        avg.setCompletedTests((int) data.stream().mapToInt(PerformanceMetrics::getCompletedTests).average().orElse(0.0));
        avg.setFailedTests((int) data.stream().mapToInt(PerformanceMetrics::getFailedTests).average().orElse(0.0));
        
        return avg;
    }
    
    /**
     * Calculate percentage change
     */
    private double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) {
            return newValue == 0 ? 0 : 100.0;
        }
        return ((newValue - oldValue) / oldValue) * 100.0;
    }
    
    /**
     * Calculate trend using linear regression
     */
    private double calculateTrend(List<PerformanceMetrics> data, java.util.function.Function<PerformanceMetrics, Double> metricExtractor) {
        if (data.size() < 2) return 0.0;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        int n = data.size();
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = metricExtractor.apply(data.get(i));
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        return slope;
    }
    
    /**
     * Calculate volatility (standard deviation)
     */
    private double calculateVolatility(List<PerformanceMetrics> data, java.util.function.Function<PerformanceMetrics, Double> metricExtractor) {
        if (data.size() < 2) return 0.0;
        
        double mean = data.stream().mapToDouble(metricExtractor::apply).average().orElse(0.0);
        double variance = data.stream().mapToDouble(x -> Math.pow(metricExtractor.apply(x) - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate variance
     */
    private double calculateVariance(List<PerformanceMetrics> data, java.util.function.Function<PerformanceMetrics, Double> metricExtractor) {
        if (data.size() < 2) return 0.0;
        
        double mean = data.stream().mapToDouble(metricExtractor::apply).average().orElse(0.0);
        return data.stream().mapToDouble(x -> Math.pow(metricExtractor.apply(x) - mean, 2)).average().orElse(0.0);
    }
    
    /**
     * Calculate correlation with time
     */
    private double calculateTimeCorrelation(List<PerformanceMetrics> data) {
        if (data.size() < 2) return 0.0;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, sumYY = 0;
        int n = data.size();
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = data.get(i).getAveragePageLoadTime();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
            sumYY += y * y;
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumXX - sumX * sumX) * (n * sumYY - sumY * sumY));
        
        return denominator == 0 ? 0 : numerator / denominator;
    }
    
    /**
     * Calculate overall performance score
     */
    private double calculateOverallPerformanceScore(ReleaseComparison comparison) {
        double score = 100.0; // Start with perfect score
        
        // Deduct points for regressions
        if (comparison.getPageLoadTimeChange() > 0) {
            score -= Math.min(30, comparison.getPageLoadTimeChange());
        }
        
        if (comparison.getApiResponseTimeChange() > 0) {
            score -= Math.min(25, comparison.getApiResponseTimeChange());
        }
        
        if (comparison.getWebVitalsScoreChange() < 0) {
            score -= Math.min(20, Math.abs(comparison.getWebVitalsScoreChange()));
        }
        
        // Add points for improvements
        if (comparison.getPageLoadTimeChange() < 0) {
            score += Math.min(20, Math.abs(comparison.getPageLoadTimeChange()));
        }
        
        if (comparison.getWebVitalsScoreChange() > 0) {
            score += Math.min(15, comparison.getWebVitalsScoreChange());
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Calculate trend score
     */
    private double calculateTrendScore(TrendAnalysis trendAnalysis) {
        double score = 50.0; // Start with neutral score
        
        // Adjust based on trends
        if (trendAnalysis.getPageLoadTimeTrend() < -5) {
            score += 20; // Improving trend
        } else if (trendAnalysis.getPageLoadTimeTrend() > 5) {
            score -= 20; // Degrading trend
        }
        
        if (trendAnalysis.getWebVitalsScoreTrend() > 5) {
            score += 15; // Improving Web Vitals
        } else if (trendAnalysis.getWebVitalsScoreTrend() < -5) {
            score -= 15; // Degrading Web Vitals
        }
        
        // Adjust for volatility
        if (trendAnalysis.getPageLoadTimeVolatility() > 500) {
            score -= 10; // High volatility
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Calculate regression score
     */
    private double calculateRegressionScore(BaselineComparison comparison) {
        double score = 100.0; // Start with perfect score
        
        // Deduct for regressions
        if (comparison.isPageLoadTimeRegression()) {
            score -= 30;
        }
        
        if (comparison.isApiResponseTimeRegression()) {
            score -= 25;
        }
        
        if (comparison.isWebVitalsScoreRegression()) {
            score -= 20;
        }
        
        // Add for improvements
        if (comparison.getPageLoadTimeDeviation() < -10) {
            score += 15;
        }
        
        if (comparison.getWebVitalsScoreDeviation() > 10) {
            score += 10;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Add metrics to historical data
     */
    public void addMetrics(String testCaseKey, PerformanceMetrics metrics) {
        historicalData.computeIfAbsent(testCaseKey, k -> new ArrayList<>()).add(metrics);
        
        // Keep only last 1000 entries per test case
        List<PerformanceMetrics> testMetrics = historicalData.get(testCaseKey);
        if (testMetrics.size() > 1000) {
            testMetrics.subList(0, testMetrics.size() - 1000).clear();
        }
    }
    
    /**
     * Get latest comparisons
     */
    public List<ReleaseComparison> getLatestComparisons() {
        return new ArrayList<>(releaseComparisons.values());
    }
    
    /**
     * Get trend analysis for a test case
     */
    public TrendAnalysis getTrendAnalysis(String testCaseKey) {
        return trendAnalyses.get(testCaseKey);
    }
    
    // Data Models
    
    public static class ReleaseComparison {
        private String testCaseKey;
        private long timestamp;
        private double pageLoadTimeChange;
        private double apiResponseTimeChange;
        private double webVitalsScoreChange;
        private double memoryUsageChange;
        private double cpuUsageChange;
        private double lcpChange;
        private double clsChange;
        private double fcpChange;
        private double ttfbChange;
        private double inpChange;
        private double fidChange;
        private double resourceCountChange;
        private double javascriptSizeChange;
        private double cssSizeChange;
        private double imageSizeChange;
        private double apiCallsChange;
        private double slowApiCallsChange;
        private double failedApiCallsChange;
        private PerformanceMetrics currentAverage;
        private PerformanceMetrics previousAverage;
        private List<String> significantChanges;
        private List<String> insights;
        private double overallPerformanceScore;
        private String error;
        
        // Getters and setters
        public String getTestCaseKey() { return testCaseKey; }
        public void setTestCaseKey(String testCaseKey) { this.testCaseKey = testCaseKey; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public double getPageLoadTimeChange() { return pageLoadTimeChange; }
        public void setPageLoadTimeChange(double pageLoadTimeChange) { this.pageLoadTimeChange = pageLoadTimeChange; }
        
        public double getApiResponseTimeChange() { return apiResponseTimeChange; }
        public void setApiResponseTimeChange(double apiResponseTimeChange) { this.apiResponseTimeChange = apiResponseTimeChange; }
        
        public double getWebVitalsScoreChange() { return webVitalsScoreChange; }
        public void setWebVitalsScoreChange(double webVitalsScoreChange) { this.webVitalsScoreChange = webVitalsScoreChange; }
        
        public double getMemoryUsageChange() { return memoryUsageChange; }
        public void setMemoryUsageChange(double memoryUsageChange) { this.memoryUsageChange = memoryUsageChange; }
        
        public double getCpuUsageChange() { return cpuUsageChange; }
        public void setCpuUsageChange(double cpuUsageChange) { this.cpuUsageChange = cpuUsageChange; }
        
        public double getLcpChange() { return lcpChange; }
        public void setLcpChange(double lcpChange) { this.lcpChange = lcpChange; }
        
        public double getClsChange() { return clsChange; }
        public void setClsChange(double clsChange) { this.clsChange = clsChange; }
        
        public double getFcpChange() { return fcpChange; }
        public void setFcpChange(double fcpChange) { this.fcpChange = fcpChange; }
        
        public double getTtfbChange() { return ttfbChange; }
        public void setTtfbChange(double ttfbChange) { this.ttfbChange = ttfbChange; }
        
        public double getInpChange() { return inpChange; }
        public void setInpChange(double inpChange) { this.inpChange = inpChange; }
        
        public double getFidChange() { return fidChange; }
        public void setFidChange(double fidChange) { this.fidChange = fidChange; }
        
        public double getResourceCountChange() { return resourceCountChange; }
        public void setResourceCountChange(double resourceCountChange) { this.resourceCountChange = resourceCountChange; }
        
        public double getJavascriptSizeChange() { return javascriptSizeChange; }
        public void setJavascriptSizeChange(double javascriptSizeChange) { this.javascriptSizeChange = javascriptSizeChange; }
        
        public double getCssSizeChange() { return cssSizeChange; }
        public void setCssSizeChange(double cssSizeChange) { this.cssSizeChange = cssSizeChange; }
        
        public double getImageSizeChange() { return imageSizeChange; }
        public void setImageSizeChange(double imageSizeChange) { this.imageSizeChange = imageSizeChange; }
        
        public double getApiCallsChange() { return apiCallsChange; }
        public void setApiCallsChange(double apiCallsChange) { this.apiCallsChange = apiCallsChange; }
        
        public double getSlowApiCallsChange() { return slowApiCallsChange; }
        public void setSlowApiCallsChange(double slowApiCallsChange) { this.slowApiCallsChange = slowApiCallsChange; }
        
        public double getFailedApiCallsChange() { return failedApiCallsChange; }
        public void setFailedApiCallsChange(double failedApiCallsChange) { this.failedApiCallsChange = failedApiCallsChange; }
        
        public PerformanceMetrics getCurrentAverage() { return currentAverage; }
        public void setCurrentAverage(PerformanceMetrics currentAverage) { this.currentAverage = currentAverage; }
        
        public PerformanceMetrics getPreviousAverage() { return previousAverage; }
        public void setPreviousAverage(PerformanceMetrics previousAverage) { this.previousAverage = previousAverage; }
        
        public List<String> getSignificantChanges() { return significantChanges; }
        public void setSignificantChanges(List<String> significantChanges) { this.significantChanges = significantChanges; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
        
        public double getOverallPerformanceScore() { return overallPerformanceScore; }
        public void setOverallPerformanceScore(double overallPerformanceScore) { this.overallPerformanceScore = overallPerformanceScore; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("testCaseKey", testCaseKey);
            map.put("timestamp", timestamp);
            map.put("pageLoadTimeChange", pageLoadTimeChange);
            map.put("apiResponseTimeChange", apiResponseTimeChange);
            map.put("webVitalsScoreChange", webVitalsScoreChange);
            map.put("memoryUsageChange", memoryUsageChange);
            map.put("overallPerformanceScore", overallPerformanceScore);
            map.put("significantChanges", significantChanges);
            map.put("insights", insights);
            map.put("error", error);
            return map;
        }
    }
    
    public static class RunComparison {
        private String testCaseKey;
        private String currentRunId;
        private String previousRunId;
        private long timestamp;
        private double pageLoadTimeChange;
        private double apiResponseTimeChange;
        private double webVitalsScoreChange;
        private double memoryUsageChange;
        private double testDurationChange;
        private double activeTestsChange;
        private double completedTestsChange;
        private double failedTestsChange;
        private PerformanceMetrics currentAverage;
        private PerformanceMetrics previousAverage;
        private List<String> performanceChanges;
        private List<String> recommendations;
        private String error;
        
        // Getters and setters
        public String getTestCaseKey() { return testCaseKey; }
        public void setTestCaseKey(String testCaseKey) { this.testCaseKey = testCaseKey; }
        
        public String getCurrentRunId() { return currentRunId; }
        public void setCurrentRunId(String currentRunId) { this.currentRunId = currentRunId; }
        
        public String getPreviousRunId() { return previousRunId; }
        public void setPreviousRunId(String previousRunId) { this.previousRunId = previousRunId; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public double getPageLoadTimeChange() { return pageLoadTimeChange; }
        public void setPageLoadTimeChange(double pageLoadTimeChange) { this.pageLoadTimeChange = pageLoadTimeChange; }
        
        public double getApiResponseTimeChange() { return apiResponseTimeChange; }
        public void setApiResponseTimeChange(double apiResponseTimeChange) { this.apiResponseTimeChange = apiResponseTimeChange; }
        
        public double getWebVitalsScoreChange() { return webVitalsScoreChange; }
        public void setWebVitalsScoreChange(double webVitalsScoreChange) { this.webVitalsScoreChange = webVitalsScoreChange; }
        
        public double getMemoryUsageChange() { return memoryUsageChange; }
        public void setMemoryUsageChange(double memoryUsageChange) { this.memoryUsageChange = memoryUsageChange; }
        
        public double getTestDurationChange() { return testDurationChange; }
        public void setTestDurationChange(double testDurationChange) { this.testDurationChange = testDurationChange; }
        
        public double getActiveTestsChange() { return activeTestsChange; }
        public void setActiveTestsChange(double activeTestsChange) { this.activeTestsChange = activeTestsChange; }
        
        public double getCompletedTestsChange() { return completedTestsChange; }
        public void setCompletedTestsChange(double completedTestsChange) { this.completedTestsChange = completedTestsChange; }
        
        public double getFailedTestsChange() { return failedTestsChange; }
        public void setFailedTestsChange(double failedTestsChange) { this.failedTestsChange = failedTestsChange; }
        
        public PerformanceMetrics getCurrentAverage() { return currentAverage; }
        public void setCurrentAverage(PerformanceMetrics currentAverage) { this.currentAverage = currentAverage; }
        
        public PerformanceMetrics getPreviousAverage() { return previousAverage; }
        public void setPreviousAverage(PerformanceMetrics previousAverage) { this.previousAverage = previousAverage; }
        
        public List<String> getPerformanceChanges() { return performanceChanges; }
        public void setPerformanceChanges(List<String> performanceChanges) { this.performanceChanges = performanceChanges; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class TrendAnalysis {
        private String testCaseKey;
        private int daysBack;
        private long timestamp;
        private double pageLoadTimeTrend;
        private double apiResponseTimeTrend;
        private double webVitalsScoreTrend;
        private double memoryUsageTrend;
        private double cpuUsageTrend;
        private double lcpTrend;
        private double clsTrend;
        private double fcpTrend;
        private double ttfbTrend;
        private double inpTrend;
        private double fidTrend;
        private double pageLoadTimeVolatility;
        private double apiResponseTimeVolatility;
        private double webVitalsScoreVolatility;
        private double timeCorrelation;
        private List<String> patterns;
        private List<String> predictions;
        private double trendScore;
        private String error;
        
        // Getters and setters
        public String getTestCaseKey() { return testCaseKey; }
        public void setTestCaseKey(String testCaseKey) { this.testCaseKey = testCaseKey; }
        
        public int getDaysBack() { return daysBack; }
        public void setDaysBack(int daysBack) { this.daysBack = daysBack; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public double getPageLoadTimeTrend() { return pageLoadTimeTrend; }
        public void setPageLoadTimeTrend(double pageLoadTimeTrend) { this.pageLoadTimeTrend = pageLoadTimeTrend; }
        
        public double getApiResponseTimeTrend() { return apiResponseTimeTrend; }
        public void setApiResponseTimeTrend(double apiResponseTimeTrend) { this.apiResponseTimeTrend = apiResponseTimeTrend; }
        
        public double getWebVitalsScoreTrend() { return webVitalsScoreTrend; }
        public void setWebVitalsScoreTrend(double webVitalsScoreTrend) { this.webVitalsScoreTrend = webVitalsScoreTrend; }
        
        public double getMemoryUsageTrend() { return memoryUsageTrend; }
        public void setMemoryUsageTrend(double memoryUsageTrend) { this.memoryUsageTrend = memoryUsageTrend; }
        
        public double getCpuUsageTrend() { return cpuUsageTrend; }
        public void setCpuUsageTrend(double cpuUsageTrend) { this.cpuUsageTrend = cpuUsageTrend; }
        
        public double getLcpTrend() { return lcpTrend; }
        public void setLcpTrend(double lcpTrend) { this.lcpTrend = lcpTrend; }
        
        public double getClsTrend() { return clsTrend; }
        public void setClsTrend(double clsTrend) { this.clsTrend = clsTrend; }
        
        public double getFcpTrend() { return fcpTrend; }
        public void setFcpTrend(double fcpTrend) { this.fcpTrend = fcpTrend; }
        
        public double getTtfbTrend() { return ttfbTrend; }
        public void setTtfbTrend(double ttfbTrend) { this.ttfbTrend = ttfbTrend; }
        
        public double getInpTrend() { return inpTrend; }
        public void setInpTrend(double inpTrend) { this.inpTrend = inpTrend; }
        
        public double getFidTrend() { return fidTrend; }
        public void setFidTrend(double fidTrend) { this.fidTrend = fidTrend; }
        
        public double getPageLoadTimeVolatility() { return pageLoadTimeVolatility; }
        public void setPageLoadTimeVolatility(double pageLoadTimeVolatility) { this.pageLoadTimeVolatility = pageLoadTimeVolatility; }
        
        public double getApiResponseTimeVolatility() { return apiResponseTimeVolatility; }
        public void setApiResponseTimeVolatility(double apiResponseTimeVolatility) { this.apiResponseTimeVolatility = apiResponseTimeVolatility; }
        
        public double getWebVitalsScoreVolatility() { return webVitalsScoreVolatility; }
        public void setWebVitalsScoreVolatility(double webVitalsScoreVolatility) { this.webVitalsScoreVolatility = webVitalsScoreVolatility; }
        
        public double getTimeCorrelation() { return timeCorrelation; }
        public void setTimeCorrelation(double timeCorrelation) { this.timeCorrelation = timeCorrelation; }
        
        public List<String> getPatterns() { return patterns; }
        public void setPatterns(List<String> patterns) { this.patterns = patterns; }
        
        public List<String> getPredictions() { return predictions; }
        public void setPredictions(List<String> predictions) { this.predictions = predictions; }
        
        public double getTrendScore() { return trendScore; }
        public void setTrendScore(double trendScore) { this.trendScore = trendScore; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class ABTestComparison {
        private String testCaseKey;
        private String variantA;
        private String variantB;
        private long timestamp;
        private double pageLoadTimeDifference;
        private double apiResponseTimeDifference;
        private double webVitalsScoreDifference;
        private double memoryUsageDifference;
        private double pageLoadTimeImprovement;
        private double apiResponseTimeImprovement;
        private double webVitalsScoreImprovement;
        private PerformanceMetrics variantAAverage;
        private PerformanceMetrics variantBAverage;
        private boolean statisticalSignificance;
        private double pValue;
        private double tStatistic;
        private int degreesOfFreedom;
        private String winningVariant;
        private String confidenceLevel;
        private List<String> insights;
        private String error;
        
        // Getters and setters
        public String getTestCaseKey() { return testCaseKey; }
        public void setTestCaseKey(String testCaseKey) { this.testCaseKey = testCaseKey; }
        
        public String getVariantA() { return variantA; }
        public void setVariantA(String variantA) { this.variantA = variantA; }
        
        public String getVariantB() { return variantB; }
        public void setVariantB(String variantB) { this.variantB = variantB; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public double getPageLoadTimeDifference() { return pageLoadTimeDifference; }
        public void setPageLoadTimeDifference(double pageLoadTimeDifference) { this.pageLoadTimeDifference = pageLoadTimeDifference; }
        
        public double getApiResponseTimeDifference() { return apiResponseTimeDifference; }
        public void setApiResponseTimeDifference(double apiResponseTimeDifference) { this.apiResponseTimeDifference = apiResponseTimeDifference; }
        
        public double getWebVitalsScoreDifference() { return webVitalsScoreDifference; }
        public void setWebVitalsScoreDifference(double webVitalsScoreDifference) { this.webVitalsScoreDifference = webVitalsScoreDifference; }
        
        public double getMemoryUsageDifference() { return memoryUsageDifference; }
        public void setMemoryUsageDifference(double memoryUsageDifference) { this.memoryUsageDifference = memoryUsageDifference; }
        
        public double getPageLoadTimeImprovement() { return pageLoadTimeImprovement; }
        public void setPageLoadTimeImprovement(double pageLoadTimeImprovement) { this.pageLoadTimeImprovement = pageLoadTimeImprovement; }
        
        public double getApiResponseTimeImprovement() { return apiResponseTimeImprovement; }
        public void setApiResponseTimeImprovement(double apiResponseTimeImprovement) { this.apiResponseTimeImprovement = apiResponseTimeImprovement; }
        
        public double getWebVitalsScoreImprovement() { return webVitalsScoreImprovement; }
        public void setWebVitalsScoreImprovement(double webVitalsScoreImprovement) { this.webVitalsScoreImprovement = webVitalsScoreImprovement; }
        
        public PerformanceMetrics getVariantAAverage() { return variantAAverage; }
        public void setVariantAAverage(PerformanceMetrics variantAAverage) { this.variantAAverage = variantAAverage; }
        
        public PerformanceMetrics getVariantBAverage() { return variantBAverage; }
        public void setVariantBAverage(PerformanceMetrics variantBAverage) { this.variantBAverage = variantBAverage; }
        
        public boolean isStatisticalSignificance() { return statisticalSignificance; }
        public void setStatisticalSignificance(boolean statisticalSignificance) { this.statisticalSignificance = statisticalSignificance; }
        
        public double getPValue() { return pValue; }
        public void setPValue(double pValue) { this.pValue = pValue; }
        
        public double getTStatistic() { return tStatistic; }
        public void setTStatistic(double tStatistic) { this.tStatistic = tStatistic; }
        
        public int getDegreesOfFreedom() { return degreesOfFreedom; }
        public void setDegreesOfFreedom(int degreesOfFreedom) { this.degreesOfFreedom = degreesOfFreedom; }
        
        public String getWinningVariant() { return winningVariant; }
        public void setWinningVariant(String winningVariant) { this.winningVariant = winningVariant; }
        
        public String getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class BaselineComparison {
        private String testCaseKey;
        private long timestamp;
        private double pageLoadTimeDeviation;
        private double apiResponseTimeDeviation;
        private double webVitalsScoreDeviation;
        private double memoryUsageDeviation;
        private boolean pageLoadTimeRegression;
        private boolean apiResponseTimeRegression;
        private boolean webVitalsScoreRegression;
        private PerformanceMetrics currentAverage;
        private PerformanceMetrics baselineAverage;
        private List<String> regressions;
        private List<String> insights;
        private double regressionScore;
        private String error;
        
        // Getters and setters
        public String getTestCaseKey() { return testCaseKey; }
        public void setTestCaseKey(String testCaseKey) { this.testCaseKey = testCaseKey; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public double getPageLoadTimeDeviation() { return pageLoadTimeDeviation; }
        public void setPageLoadTimeDeviation(double pageLoadTimeDeviation) { this.pageLoadTimeDeviation = pageLoadTimeDeviation; }
        
        public double getApiResponseTimeDeviation() { return apiResponseTimeDeviation; }
        public void setApiResponseTimeDeviation(double apiResponseTimeDeviation) { this.apiResponseTimeDeviation = apiResponseTimeDeviation; }
        
        public double getWebVitalsScoreDeviation() { return webVitalsScoreDeviation; }
        public void setWebVitalsScoreDeviation(double webVitalsScoreDeviation) { this.webVitalsScoreDeviation = webVitalsScoreDeviation; }
        
        public double getMemoryUsageDeviation() { return memoryUsageDeviation; }
        public void setMemoryUsageDeviation(double memoryUsageDeviation) { this.memoryUsageDeviation = memoryUsageDeviation; }
        
        public boolean isPageLoadTimeRegression() { return pageLoadTimeRegression; }
        public void setPageLoadTimeRegression(boolean pageLoadTimeRegression) { this.pageLoadTimeRegression = pageLoadTimeRegression; }
        
        public boolean isApiResponseTimeRegression() { return apiResponseTimeRegression; }
        public void setApiResponseTimeRegression(boolean apiResponseTimeRegression) { this.apiResponseTimeRegression = apiResponseTimeRegression; }
        
        public boolean isWebVitalsScoreRegression() { return webVitalsScoreRegression; }
        public void setWebVitalsScoreRegression(boolean webVitalsScoreRegression) { this.webVitalsScoreRegression = webVitalsScoreRegression; }
        
        public PerformanceMetrics getCurrentAverage() { return currentAverage; }
        public void setCurrentAverage(PerformanceMetrics currentAverage) { this.currentAverage = currentAverage; }
        
        public PerformanceMetrics getBaselineAverage() { return baselineAverage; }
        public void setBaselineAverage(PerformanceMetrics baselineAverage) { this.baselineAverage = baselineAverage; }
        
        public List<String> getRegressions() { return regressions; }
        public void setRegressions(List<String> regressions) { this.regressions = regressions; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
        
        public double getRegressionScore() { return regressionScore; }
        public void setRegressionScore(double regressionScore) { this.regressionScore = regressionScore; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
