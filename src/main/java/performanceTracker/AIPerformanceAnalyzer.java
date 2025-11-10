package performanceTracker;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI-Powered Performance Analyzer
 * 
 * Provides intelligent analysis of performance data including:
 * - Anomaly detection using statistical methods
 * - Performance trend analysis and predictions
 * - Root cause analysis for performance issues
 * - Automated optimization recommendations
 * - Pattern recognition in performance data
 */
@Component
public class AIPerformanceAnalyzer {
    
    private final Map<String, List<PerformanceMetrics>> historicalData = new ConcurrentHashMap<>();
    private final Map<String, PerformanceInsights> cachedInsights = new ConcurrentHashMap<>();
    
    /**
     * Analyze performance metrics and generate AI insights
     */
    public Map<String, Object> generateInsights() {
        Map<String, Object> insights = new HashMap<>();
        
        try {
            // Anomaly Detection
            List<String> anomalies = detectAnomalies();
            insights.put("anomalies", anomalies);
            
            // Performance Predictions
            Map<String, Object> predictions = generatePredictions();
            insights.put("predictions", predictions);
            
            // Root Cause Analysis
            List<String> rootCauses = analyzeRootCauses();
            insights.put("rootCauses", rootCauses);
            
            // Optimization Recommendations
            List<String> recommendations = generateRecommendations();
            insights.put("recommendations", recommendations);
            
            // Performance Trends
            Map<String, Object> trends = analyzeTrends();
            insights.put("trends", trends);
            
            // Performance Scoring
            double overallScore = calculatePerformanceScore();
            insights.put("overallScore", overallScore);
            
            // Risk Assessment
            Map<String, Object> riskAssessment = assessPerformanceRisks();
            insights.put("riskAssessment", riskAssessment);
            
            insights.put("timestamp", System.currentTimeMillis());
            insights.put("status", "success");
            
        } catch (Exception e) {
            insights.put("error", e.getMessage());
            insights.put("status", "error");
        }
        
        return insights;
    }
    
    /**
     * Detect performance anomalies using statistical methods
     */
    public List<String> detectAnomalies() {
        List<String> anomalies = new ArrayList<>();
        
        try {
            // Collect all performance metrics
            List<PerformanceMetrics> allMetrics = new ArrayList<>();
            for (List<PerformanceMetrics> metrics : historicalData.values()) {
                allMetrics.addAll(metrics);
            }
            
            if (allMetrics.size() < 10) {
                return anomalies; // Need at least 10 data points for anomaly detection
            }
            
            // Detect anomalies in page load time
            anomalies.addAll(detectAnomaliesInMetric(allMetrics, "pageLoadTime", 2000.0));
            
            // Detect anomalies in API response time
            anomalies.addAll(detectAnomaliesInMetric(allMetrics, "apiResponseTime", 1000.0));
            
            // Detect anomalies in Web Vitals score
            anomalies.addAll(detectAnomaliesInMetric(allMetrics, "webVitalsScore", 75.0));
            
            // Detect anomalies in memory usage
            anomalies.addAll(detectAnomaliesInMetric(allMetrics, "memoryUsage", 80.0));
            
            // Detect budget violations
            anomalies.addAll(detectBudgetViolations(allMetrics));
            
        } catch (Exception e) {
            anomalies.add("Error in anomaly detection: " + e.getMessage());
        }
        
        return anomalies;
    }
    
    /**
     * Detect anomalies in a specific metric using Z-score analysis
     */
    private List<String> detectAnomaliesInMetric(List<PerformanceMetrics> metrics, String metricName, double threshold) {
        List<String> anomalies = new ArrayList<>();
        
        try {
            List<Double> values = extractMetricValues(metrics, metricName);
            if (values.size() < 5) return anomalies;
            
            // Calculate mean and standard deviation
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = values.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
            double stdDev = Math.sqrt(variance);
            
            if (stdDev == 0) return anomalies; // No variation
            
            // Detect outliers using Z-score (threshold: 2.5)
            for (int i = 0; i < values.size(); i++) {
                double zScore = Math.abs((values.get(i) - mean) / stdDev);
                if (zScore > 2.5) {
                    String anomaly = String.format("Anomaly detected in %s: %.2f (Z-score: %.2f, threshold exceeded)", 
                        metricName, values.get(i), zScore);
                    anomalies.add(anomaly);
                }
            }
            
            // Check against absolute thresholds
            for (int i = 0; i < values.size(); i++) {
                double value = values.get(i);
                if (value > threshold) {
                    String anomaly = String.format("Performance degradation in %s: %.2f exceeds threshold %.2f", 
                        metricName, value, threshold);
                    anomalies.add(anomaly);
                }
            }
            
        } catch (Exception e) {
            anomalies.add("Error analyzing " + metricName + ": " + e.getMessage());
        }
        
        return anomalies;
    }
    
    /**
     * Extract values for a specific metric
     */
    private List<Double> extractMetricValues(List<PerformanceMetrics> metrics, String metricName) {
        return metrics.stream().map(metric -> {
            switch (metricName) {
                case "pageLoadTime": return metric.getAveragePageLoadTime();
                case "apiResponseTime": return metric.getAverageApiResponseTime();
                case "webVitalsScore": return metric.getWebVitalsScore();
                case "memoryUsage": return metric.getMemoryUsage();
                default: return 0.0;
            }
        }).collect(Collectors.toList());
    }
    
    /**
     * Detect budget violations
     */
    private List<String> detectBudgetViolations(List<PerformanceMetrics> metrics) {
        List<String> violations = new ArrayList<>();
        
        for (PerformanceMetrics metric : metrics) {
            if (metric.getBudgetViolations() != null && !metric.getBudgetViolations().isEmpty()) {
                for (String violation : metric.getBudgetViolations()) {
                    violations.add("Budget violation: " + violation + " at " + new Date(metric.getTimestamp()));
                }
            }
        }
        
        return violations;
    }
    
    /**
     * Generate performance predictions using trend analysis
     */
    public Map<String, Object> generatePredictions() {
        Map<String, Object> predictions = new HashMap<>();
        
        try {
            // Collect recent metrics (last 30 data points)
            List<PerformanceMetrics> recentMetrics = getRecentMetrics(30);
            
            if (recentMetrics.size() < 5) {
                predictions.put("error", "Insufficient data for predictions");
                return predictions;
            }
            
            // Predict page load time
            double predictedPageLoadTime = predictMetric(recentMetrics, "pageLoadTime");
            predictions.put("predictedPageLoadTime", predictedPageLoadTime);
            
            // Predict API response time
            double predictedApiResponseTime = predictMetric(recentMetrics, "apiResponseTime");
            predictions.put("predictedApiResponseTime", predictedApiResponseTime);
            
            // Predict Web Vitals score
            double predictedWebVitalsScore = predictMetric(recentMetrics, "webVitalsScore");
            predictions.put("predictedWebVitalsScore", predictedWebVitalsScore);
            
            // Predict memory usage
            double predictedMemoryUsage = predictMetric(recentMetrics, "memoryUsage");
            predictions.put("predictedMemoryUsage", predictedMemoryUsage);
            
            // Confidence levels
            predictions.put("confidence", calculatePredictionConfidence(recentMetrics));
            
            // Time horizon
            predictions.put("timeHorizon", "24 hours");
            
        } catch (Exception e) {
            predictions.put("error", e.getMessage());
        }
        
        return predictions;
    }
    
    /**
     * Predict a specific metric using linear regression
     */
    private double predictMetric(List<PerformanceMetrics> metrics, String metricName) {
        List<Double> values = extractMetricValues(metrics, metricName);
        if (values.size() < 2) return 0.0;
        
        // Simple linear regression
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        int n = values.size();
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        // Predict next value
        return slope * n + intercept;
    }
    
    /**
     * Analyze root causes of performance issues
     */
    public List<String> analyzeRootCauses() {
        List<String> rootCauses = new ArrayList<>();
        
        try {
            List<PerformanceMetrics> recentMetrics = getRecentMetrics(20);
            
            if (recentMetrics.isEmpty()) {
                return rootCauses;
            }
            
            PerformanceMetrics latest = recentMetrics.get(recentMetrics.size() - 1);
            
            // Analyze page load time issues
            if (latest.getAveragePageLoadTime() > 3000) {
                if (latest.getResourceCount() > 100) {
                    rootCauses.add("High resource count (" + latest.getResourceCount() + ") causing slow page load");
                }
                if (latest.getJavascriptSize() > 1000000) { // 1MB
                    rootCauses.add("Large JavaScript bundle (" + (latest.getJavascriptSize() / 1024) + "KB) impacting load time");
                }
                if (latest.getThirdPartyImpact() > 30) {
                    rootCauses.add("Third-party scripts causing " + latest.getThirdPartyImpact() + "% performance impact");
                }
            }
            
            // Analyze API response time issues
            if (latest.getAverageApiResponseTime() > 2000) {
                rootCauses.add("Slow API responses (" + latest.getAverageApiResponseTime() + "ms average)");
                if (latest.getFailedApiCalls() > 0) {
                    rootCauses.add("API failures detected (" + latest.getFailedApiCalls() + " failed calls)");
                }
            }
            
            // Analyze Web Vitals issues
            if (latest.getWebVitalsScore() < 80) {
                if (latest.getLcp() > 2500) {
                    rootCauses.add("Poor LCP score (" + latest.getLcp() + "ms) - largest contentful paint too slow");
                }
                if (latest.getCls() > 0.1) {
                    rootCauses.add("High CLS score (" + latest.getCls() + ") - layout shifts causing poor UX");
                }
                if (latest.getFcp() > 1800) {
                    rootCauses.add("Slow FCP (" + latest.getFcp() + "ms) - first contentful paint delayed");
                }
            }
            
            // Analyze memory issues
            if (latest.getMemoryUsage() > 85) {
                rootCauses.add("High memory usage (" + latest.getMemoryUsage() + "%) - potential memory leak");
            }
            
            // Analyze database issues
            if (latest.getDatabaseResponseTime() > 500) {
                rootCauses.add("Slow database queries (" + latest.getDatabaseResponseTime() + "ms average)");
                if (latest.getSlowQueries() > 0) {
                    rootCauses.add("Slow queries detected (" + latest.getSlowQueries() + " queries > 1000ms)");
                }
            }
            
        } catch (Exception e) {
            rootCauses.add("Error in root cause analysis: " + e.getMessage());
        }
        
        return rootCauses;
    }
    
    /**
     * Generate optimization recommendations
     */
    public List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        try {
            List<PerformanceMetrics> recentMetrics = getRecentMetrics(10);
            
            if (recentMetrics.isEmpty()) {
                return recommendations;
            }
            
            PerformanceMetrics latest = recentMetrics.get(recentMetrics.size() - 1);
            
            // JavaScript optimization recommendations
            if (latest.getJavascriptSize() > 500000) { // 500KB
                recommendations.add("Consider code splitting to reduce JavaScript bundle size (" + 
                    (latest.getJavascriptSize() / 1024) + "KB)");
            }
            
            // CSS optimization recommendations
            if (latest.getCssSize() > 100000) { // 100KB
                recommendations.add("Optimize CSS delivery and remove unused styles (" + 
                    (latest.getCssSize() / 1024) + "KB)");
            }
            
            // Image optimization recommendations
            if (latest.getImageSize() > 2000000) { // 2MB
                recommendations.add("Optimize images and consider WebP format (" + 
                    (latest.getImageSize() / 1024 / 1024) + "MB)");
            }
            
            // Third-party optimization recommendations
            if (latest.getThirdPartyImpact() > 20) {
                recommendations.add("Review third-party scripts and implement lazy loading (" + 
                    latest.getThirdPartyImpact() + "% impact)");
            }
            
            // API optimization recommendations
            if (latest.getAverageApiResponseTime() > 1000) {
                recommendations.add("Implement API caching and optimize backend queries (" + 
                    latest.getAverageApiResponseTime() + "ms average)");
            }
            
            // Database optimization recommendations
            if (latest.getDatabaseResponseTime() > 300) {
                recommendations.add("Optimize database queries and add indexes (" + 
                    latest.getDatabaseResponseTime() + "ms average)");
            }
            
            // DOM optimization recommendations
            if (latest.getDomElements() > 1000) {
                recommendations.add("Reduce DOM complexity (" + latest.getDomElements() + " elements)");
            }
            
            // Network optimization recommendations
            if (latest.getNetworkLatency() > 200) {
                recommendations.add("Consider CDN implementation and network optimization (" + 
                    latest.getNetworkLatency() + "ms latency)");
            }
            
            // Memory optimization recommendations
            if (latest.getMemoryUsage() > 75) {
                recommendations.add("Implement memory optimization and garbage collection tuning (" + 
                    latest.getMemoryUsage() + "% usage)");
            }
            
        } catch (Exception e) {
            recommendations.add("Error generating recommendations: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Analyze performance trends
     */
    public Map<String, Object> analyzeTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        try {
            List<PerformanceMetrics> recentMetrics = getRecentMetrics(20);
            
            if (recentMetrics.size() < 5) {
                trends.put("error", "Insufficient data for trend analysis");
                return trends;
            }
            
            // Calculate trends for key metrics
            trends.put("pageLoadTimeTrend", calculateTrend(recentMetrics, "pageLoadTime"));
            trends.put("apiResponseTimeTrend", calculateTrend(recentMetrics, "apiResponseTime"));
            trends.put("webVitalsScoreTrend", calculateTrend(recentMetrics, "webVitalsScore"));
            trends.put("memoryUsageTrend", calculateTrend(recentMetrics, "memoryUsage"));
            
            // Overall trend assessment
            double overallTrend = calculateOverallTrend(recentMetrics);
            trends.put("overallTrend", overallTrend);
            trends.put("trendDescription", getTrendDescription(overallTrend));
            
        } catch (Exception e) {
            trends.put("error", e.getMessage());
        }
        
        return trends;
    }
    
    /**
     * Calculate trend for a specific metric
     */
    private double calculateTrend(List<PerformanceMetrics> metrics, String metricName) {
        List<Double> values = extractMetricValues(metrics, metricName);
        if (values.size() < 2) return 0.0;
        
        double first = values.get(0);
        double last = values.get(values.size() - 1);
        
        return ((last - first) / first) * 100; // Percentage change
    }
    
    /**
     * Calculate overall performance trend
     */
    private double calculateOverallTrend(List<PerformanceMetrics> metrics) {
        double pageLoadTrend = calculateTrend(metrics, "pageLoadTime");
        double apiTrend = calculateTrend(metrics, "apiResponseTime");
        double webVitalsTrend = calculateTrend(metrics, "webVitalsScore");
        double memoryTrend = calculateTrend(metrics, "memoryUsage");
        
        // Weight the trends (negative for metrics where lower is better)
        return (-pageLoadTrend + -apiTrend + webVitalsTrend + -memoryTrend) / 4;
    }
    
    /**
     * Get trend description
     */
    private String getTrendDescription(double trend) {
        if (trend > 10) return "Significantly Improving";
        if (trend > 5) return "Improving";
        if (trend > -5) return "Stable";
        if (trend > -10) return "Declining";
        return "Significantly Declining";
    }
    
    /**
     * Calculate overall performance score
     */
    public double calculatePerformanceScore() {
        try {
            List<PerformanceMetrics> recentMetrics = getRecentMetrics(10);
            
            if (recentMetrics.isEmpty()) {
                return 0.0;
            }
            
            PerformanceMetrics latest = recentMetrics.get(recentMetrics.size() - 1);
            
            // Calculate score based on key metrics (0-100 scale)
            double pageLoadScore = Math.max(0, 100 - (latest.getAveragePageLoadTime() / 50));
            double apiScore = Math.max(0, 100 - (latest.getAverageApiResponseTime() / 25));
            double webVitalsScore = latest.getWebVitalsScore();
            double memoryScore = Math.max(0, 100 - latest.getMemoryUsage());
            
            return (pageLoadScore + apiScore + webVitalsScore + memoryScore) / 4;
            
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Assess performance risks
     */
    public Map<String, Object> assessPerformanceRisks() {
        Map<String, Object> risks = new HashMap<>();
        
        try {
            List<PerformanceMetrics> recentMetrics = getRecentMetrics(10);
            
            if (recentMetrics.isEmpty()) {
                risks.put("error", "No data available for risk assessment");
                return risks;
            }
            
            PerformanceMetrics latest = recentMetrics.get(recentMetrics.size() - 1);
            List<String> riskFactors = new ArrayList<>();
            int riskScore = 0;
            
            // High page load time risk
            if (latest.getAveragePageLoadTime() > 3000) {
                riskFactors.add("High page load time (>3s)");
                riskScore += 30;
            }
            
            // High API response time risk
            if (latest.getAverageApiResponseTime() > 2000) {
                riskFactors.add("Slow API responses (>2s)");
                riskScore += 25;
            }
            
            // Low Web Vitals score risk
            if (latest.getWebVitalsScore() < 75) {
                riskFactors.add("Poor Web Vitals score (<75)");
                riskScore += 20;
            }
            
            // High memory usage risk
            if (latest.getMemoryUsage() > 85) {
                riskFactors.add("High memory usage (>85%)");
                riskScore += 15;
            }
            
            // Database performance risk
            if (latest.getDatabaseResponseTime() > 500) {
                riskFactors.add("Slow database queries (>500ms)");
                riskScore += 10;
            }
            
            risks.put("riskScore", riskScore);
            risks.put("riskFactors", riskFactors);
            risks.put("riskLevel", getRiskLevel(riskScore));
            
        } catch (Exception e) {
            risks.put("error", e.getMessage());
        }
        
        return risks;
    }
    
    /**
     * Get risk level based on score
     */
    private String getRiskLevel(int riskScore) {
        if (riskScore >= 80) return "CRITICAL";
        if (riskScore >= 60) return "HIGH";
        if (riskScore >= 40) return "MEDIUM";
        if (riskScore >= 20) return "LOW";
        return "MINIMAL";
    }
    
    /**
     * Get performance predictions for a specific test case
     */
    public Map<String, Object> getPerformancePredictions(String testCaseKey) {
        Map<String, Object> predictions = new HashMap<>();
        
        try {
            List<PerformanceMetrics> testMetrics = historicalData.getOrDefault(testCaseKey, new ArrayList<>());
            
            if (testMetrics.size() < 5) {
                predictions.put("error", "Insufficient historical data for predictions");
                return predictions;
            }
            
            // Generate predictions for this specific test case
            predictions.put("testCaseKey", testCaseKey);
            predictions.put("dataPoints", testMetrics.size());
            predictions.put("predictions", predictMetric(testMetrics, "pageLoadTime"));
            
        } catch (Exception e) {
            predictions.put("error", e.getMessage());
        }
        
        return predictions;
    }
    
    /**
     * Get latest insights
     */
    public Map<String, Object> getLatestInsights() {
        return cachedInsights.computeIfAbsent("latest", k -> new PerformanceInsights()).toMap();
    }
    
    /**
     * Get recent metrics for analysis
     */
    private List<PerformanceMetrics> getRecentMetrics(int count) {
        List<PerformanceMetrics> allMetrics = new ArrayList<>();
        for (List<PerformanceMetrics> metrics : historicalData.values()) {
            allMetrics.addAll(metrics);
        }
        
        // Sort by timestamp and get recent ones
        allMetrics.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        
        int startIndex = Math.max(0, allMetrics.size() - count);
        return allMetrics.subList(startIndex, allMetrics.size());
    }
    
    /**
     * Calculate prediction confidence
     */
    private double calculatePredictionConfidence(List<PerformanceMetrics> metrics) {
        // Simple confidence calculation based on data consistency
        if (metrics.size() < 5) return 0.5;
        
        List<Double> values = extractMetricValues(metrics, "pageLoadTime");
        double variance = calculateVariance(values);
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Lower variance = higher confidence
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        return Math.max(0.3, Math.min(0.95, 1.0 - coefficientOfVariation));
    }
    
    /**
     * Calculate variance
     */
    private double calculateVariance(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return values.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
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
     * Performance Insights data model
     */
    public static class PerformanceInsights {
        private List<String> anomalies = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private Map<String, Object> predictions = new HashMap<>();
        private double overallScore = 0.0;
        private long timestamp = System.currentTimeMillis();
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("anomalies", anomalies);
            map.put("recommendations", recommendations);
            map.put("predictions", predictions);
            map.put("overallScore", overallScore);
            map.put("timestamp", timestamp);
            return map;
        }
        
        // Getters and setters
        public List<String> getAnomalies() { return anomalies; }
        public void setAnomalies(List<String> anomalies) { this.anomalies = anomalies; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public Map<String, Object> getPredictions() { return predictions; }
        public void setPredictions(Map<String, Object> predictions) { this.predictions = predictions; }
        
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
