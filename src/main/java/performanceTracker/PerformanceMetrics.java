package performanceTracker;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Performance Metrics Data Model
 * 
 * Represents comprehensive performance metrics for real-time monitoring
 */
public class PerformanceMetrics {
    
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private long timestamp;
    
    @JsonProperty("testCaseKey")
    private String testCaseKey;
    
    @JsonProperty("testName")
    private String testName;
    
    // Core Performance Metrics
    @JsonProperty("pageLoadTime")
    private double averagePageLoadTime;
    
    @JsonProperty("apiResponseTime")
    private double averageApiResponseTime;
    
    @JsonProperty("webVitalsScore")
    private double webVitalsScore;
    
    @JsonProperty("memoryUsage")
    private double memoryUsage;
    
    @JsonProperty("cpuUsage")
    private double cpuUsage;
    
    @JsonProperty("networkLatency")
    private double networkLatency;
    
    // Web Vitals Details
    @JsonProperty("lcp")
    private double lcp; // Largest Contentful Paint
    
    @JsonProperty("cls")
    private double cls; // Cumulative Layout Shift
    
    @JsonProperty("fcp")
    private double fcp; // First Contentful Paint
    
    @JsonProperty("ttfb")
    private double ttfb; // Time to First Byte
    
    @JsonProperty("fid")
    private double fid; // First Input Delay
    
    @JsonProperty("inp")
    private double inp; // Interaction to Next Paint
    
    // Resource Metrics
    @JsonProperty("domElements")
    private int domElements;
    
    @JsonProperty("domDepth")
    private int domDepth;
    
    @JsonProperty("resourceCount")
    private int resourceCount;
    
    @JsonProperty("javascriptSize")
    private long javascriptSize;
    
    @JsonProperty("cssSize")
    private long cssSize;
    
    @JsonProperty("imageSize")
    private long imageSize;
    
    // API Metrics
    @JsonProperty("apiCalls")
    private int apiCalls;
    
    @JsonProperty("slowApiCalls")
    private int slowApiCalls;
    
    @JsonProperty("failedApiCalls")
    private int failedApiCalls;
    
    @JsonProperty("apiPercentiles")
    private Map<String, Double> apiPercentiles;
    
    // Test Execution Metrics
    @JsonProperty("activeTests")
    private int activeTests;
    
    @JsonProperty("completedTests")
    private int completedTests;
    
    @JsonProperty("failedTests")
    private int failedTests;
    
    @JsonProperty("testDuration")
    private long testDuration;
    
    // Performance Budget Compliance
    @JsonProperty("budgetCompliance")
    private double budgetCompliance;
    
    @JsonProperty("budgetViolations")
    private List<String> budgetViolations;
    
    // Third-Party Performance
    @JsonProperty("thirdPartyImpact")
    private double thirdPartyImpact;
    
    @JsonProperty("thirdPartyResources")
    private List<ThirdPartyResource> thirdPartyResources;
    
    // Network Conditions
    @JsonProperty("networkCondition")
    private String networkCondition;
    
    @JsonProperty("bandwidth")
    private double bandwidth;
    
    @JsonProperty("packetLoss")
    private double packetLoss;
    
    // Database Performance
    @JsonProperty("databaseQueries")
    private int databaseQueries;
    
    @JsonProperty("slowQueries")
    private int slowQueries;
    
    @JsonProperty("databaseResponseTime")
    private double databaseResponseTime;
    
    // AI-Generated Insights
    @JsonProperty("insights")
    private List<String> insights;
    
    @JsonProperty("anomalies")
    private List<String> anomalies;
    
    @JsonProperty("recommendations")
    private List<String> recommendations;
    
    // Constructors
    public PerformanceMetrics() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public PerformanceMetrics(String testCaseKey, String testName) {
        this();
        this.testCaseKey = testCaseKey;
        this.testName = testName;
    }
    
    // Getters and Setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getTestCaseKey() { return testCaseKey; }
    public void setTestCaseKey(String testCaseKey) { this.testCaseKey = testCaseKey; }
    
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    
    public double getAveragePageLoadTime() { return averagePageLoadTime; }
    public void setAveragePageLoadTime(double averagePageLoadTime) { this.averagePageLoadTime = averagePageLoadTime; }
    
    public double getAverageApiResponseTime() { return averageApiResponseTime; }
    public void setAverageApiResponseTime(double averageApiResponseTime) { this.averageApiResponseTime = averageApiResponseTime; }
    
    public double getWebVitalsScore() { return webVitalsScore; }
    public void setWebVitalsScore(double webVitalsScore) { this.webVitalsScore = webVitalsScore; }
    
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public double getNetworkLatency() { return networkLatency; }
    public void setNetworkLatency(double networkLatency) { this.networkLatency = networkLatency; }
    
    public double getLcp() { return lcp; }
    public void setLcp(double lcp) { this.lcp = lcp; }
    
    public double getCls() { return cls; }
    public void setCls(double cls) { this.cls = cls; }
    
    public double getFcp() { return fcp; }
    public void setFcp(double fcp) { this.fcp = fcp; }
    
    public double getTtfb() { return ttfb; }
    public void setTtfb(double ttfb) { this.ttfb = ttfb; }
    
    public double getFid() { return fid; }
    public void setFid(double fid) { this.fid = fid; }
    
    public double getInp() { return inp; }
    public void setInp(double inp) { this.inp = inp; }
    
    public int getDomElements() { return domElements; }
    public void setDomElements(int domElements) { this.domElements = domElements; }
    
    public int getDomDepth() { return domDepth; }
    public void setDomDepth(int domDepth) { this.domDepth = domDepth; }
    
    public int getResourceCount() { return resourceCount; }
    public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
    
    public long getJavascriptSize() { return javascriptSize; }
    public void setJavascriptSize(long javascriptSize) { this.javascriptSize = javascriptSize; }
    
    public long getCssSize() { return cssSize; }
    public void setCssSize(long cssSize) { this.cssSize = cssSize; }
    
    public long getImageSize() { return imageSize; }
    public void setImageSize(long imageSize) { this.imageSize = imageSize; }
    
    public int getApiCalls() { return apiCalls; }
    public void setApiCalls(int apiCalls) { this.apiCalls = apiCalls; }
    
    public int getSlowApiCalls() { return slowApiCalls; }
    public void setSlowApiCalls(int slowApiCalls) { this.slowApiCalls = slowApiCalls; }
    
    public int getFailedApiCalls() { return failedApiCalls; }
    public void setFailedApiCalls(int failedApiCalls) { this.failedApiCalls = failedApiCalls; }
    
    public Map<String, Double> getApiPercentiles() { return apiPercentiles; }
    public void setApiPercentiles(Map<String, Double> apiPercentiles) { this.apiPercentiles = apiPercentiles; }
    
    public int getActiveTests() { return activeTests; }
    public void setActiveTests(int activeTests) { this.activeTests = activeTests; }
    
    public int getCompletedTests() { return completedTests; }
    public void setCompletedTests(int completedTests) { this.completedTests = completedTests; }
    
    public int getFailedTests() { return failedTests; }
    public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
    
    public long getTestDuration() { return testDuration; }
    public void setTestDuration(long testDuration) { this.testDuration = testDuration; }
    
    public double getBudgetCompliance() { return budgetCompliance; }
    public void setBudgetCompliance(double budgetCompliance) { this.budgetCompliance = budgetCompliance; }
    
    public List<String> getBudgetViolations() { return budgetViolations; }
    public void setBudgetViolations(List<String> budgetViolations) { this.budgetViolations = budgetViolations; }
    
    public double getThirdPartyImpact() { return thirdPartyImpact; }
    public void setThirdPartyImpact(double thirdPartyImpact) { this.thirdPartyImpact = thirdPartyImpact; }
    
    public List<ThirdPartyResource> getThirdPartyResources() { return thirdPartyResources; }
    public void setThirdPartyResources(List<ThirdPartyResource> thirdPartyResources) { this.thirdPartyResources = thirdPartyResources; }
    
    public String getNetworkCondition() { return networkCondition; }
    public void setNetworkCondition(String networkCondition) { this.networkCondition = networkCondition; }
    
    public double getBandwidth() { return bandwidth; }
    public void setBandwidth(double bandwidth) { this.bandwidth = bandwidth; }
    
    public double getPacketLoss() { return packetLoss; }
    public void setPacketLoss(double packetLoss) { this.packetLoss = packetLoss; }
    
    public int getDatabaseQueries() { return databaseQueries; }
    public void setDatabaseQueries(int databaseQueries) { this.databaseQueries = databaseQueries; }
    
    public int getSlowQueries() { return slowQueries; }
    public void setSlowQueries(int slowQueries) { this.slowQueries = slowQueries; }
    
    public double getDatabaseResponseTime() { return databaseResponseTime; }
    public void setDatabaseResponseTime(double databaseResponseTime) { this.databaseResponseTime = databaseResponseTime; }
    
    public List<String> getInsights() { return insights; }
    public void setInsights(List<String> insights) { this.insights = insights; }
    
    public List<String> getAnomalies() { return anomalies; }
    public void setAnomalies(List<String> anomalies) { this.anomalies = anomalies; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    
    /**
     * Third-Party Resource information
     */
    public static class ThirdPartyResource {
        private String name;
        private String domain;
        private long size;
        private double loadTime;
        private double impact;
        
        // Constructors
        public ThirdPartyResource() {}
        
        public ThirdPartyResource(String name, String domain, long size, double loadTime, double impact) {
            this.name = name;
            this.domain = domain;
            this.size = size;
            this.loadTime = loadTime;
            this.impact = impact;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public double getLoadTime() { return loadTime; }
        public void setLoadTime(double loadTime) { this.loadTime = loadTime; }
        
        public double getImpact() { return impact; }
        public void setImpact(double impact) { this.impact = impact; }
    }
}
