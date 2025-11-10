package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import base.DriverManager;
import config.ConfigurationManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Database Performance Analyzer
 * 
 * Analyzes database performance impact on web applications:
 * - Database query performance monitoring
 * - Connection pool analysis
 * - Query execution time tracking
 * - Database resource usage monitoring
 * - Query optimization recommendations
 */
public class DatabasePerformanceAnalyzer {
    
    private final WebDriver driver;
    private final JavascriptExecutor jsExecutor;
    private final ConfigurationManager config;
    private final Map<String, DatabaseQuery> databaseQueries;
    private final Map<String, DatabaseConnection> databaseConnections;
    
    public DatabasePerformanceAnalyzer() {
    	WebDriver driver = DriverManager.getDriver();
        this.driver = driver;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.config = ConfigurationManager.getInstance();
        this.databaseQueries = new ConcurrentHashMap<>();
        this.databaseConnections = new ConcurrentHashMap<>();
    }
    
    /**
     * Analyze database performance impact
     */
    public DatabasePerformanceAnalysisResult analyzeDatabasePerformance() {
        DatabasePerformanceAnalysisResult result = new DatabasePerformanceAnalysisResult();
        result.setTimestamp(System.currentTimeMillis());
        
        try {
            // Monitor database queries
            List<DatabaseQuery> queries = monitorDatabaseQueries();
            result.setDatabaseQueries(queries);
            
            // Analyze connection pool performance
            DatabaseConnectionPool connectionPool = analyzeConnectionPool();
            result.setConnectionPool(connectionPool);
            
            // Monitor database resource usage
            DatabaseResourceUsage resourceUsage = monitorDatabaseResourceUsage();
            result.setResourceUsage(resourceUsage);
            
            // Calculate performance metrics
            calculateDatabasePerformanceMetrics(result, queries);
            
            // Identify performance bottlenecks
            identifyPerformanceBottlenecks(result, queries);
            
            // Generate optimization recommendations
            result.setRecommendations(generateDatabaseOptimizationRecommendations(result));
            
            // Analyze query patterns
            analyzeQueryPatterns(result, queries);
            
        } catch (Exception e) {
            result.setError("Error analyzing database performance: " + e.getMessage());
            System.err.println("Error in database performance analysis: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Monitor database queries using Performance Observer API
     */
    private List<DatabaseQuery> monitorDatabaseQueries() {
        List<DatabaseQuery> queries = new ArrayList<>();
        
        try {
            String script = """
                return new Promise((resolve) => {
                    const queries = [];
                    let queryId = 0;
                    
                    // Monitor fetch requests (API calls that might involve database queries)
                    const originalFetch = window.fetch;
                    window.fetch = function(...args) {
                        const startTime = performance.now();
                        const queryId = ++queryId;
                        
                        return originalFetch.apply(this, args)
                            .then(response => {
                                const endTime = performance.now();
                                const duration = endTime - startTime;
                                
                                queries.push({
                                    id: queryId,
                                    url: args[0],
                                    method: args[1]?.method || 'GET',
                                    duration: duration,
                                    startTime: startTime,
                                    endTime: endTime,
                                    status: response.status,
                                    size: response.headers.get('content-length') || 0,
                                    type: 'API_CALL'
                                });
                                
                                return response;
                            })
                            .catch(error => {
                                const endTime = performance.now();
                                const duration = endTime - startTime;
                                
                                queries.push({
                                    id: queryId,
                                    url: args[0],
                                    method: args[1]?.method || 'GET',
                                    duration: duration,
                                    startTime: startTime,
                                    endTime: endTime,
                                    status: 0,
                                    size: 0,
                                    type: 'API_CALL',
                                    error: error.message
                                });
                                
                                throw error;
                            });
                    };
                    
                    // Monitor XMLHttpRequest (legacy API calls)
                    const originalXHROpen = XMLHttpRequest.prototype.open;
                    const originalXHRSend = XMLHttpRequest.prototype.send;
                    
                    XMLHttpRequest.prototype.open = function(method, url, ...args) {
                        this._method = method;
                        this._url = url;
                        this._startTime = performance.now();
                        this._queryId = ++queryId;
                        
                        return originalXHROpen.apply(this, [method, url, ...args]);
                    };
                    
                    XMLHttpRequest.prototype.send = function(data) {
                        const xhr = this;
                        
                        xhr.addEventListener('load', function() {
                            const endTime = performance.now();
                            const duration = endTime - xhr._startTime;
                            
                            queries.push({
                                id: xhr._queryId,
                                url: xhr._url,
                                method: xhr._method,
                                duration: duration,
                                startTime: xhr._startTime,
                                endTime: endTime,
                                status: xhr.status,
                                size: xhr.responseText?.length || 0,
                                type: 'XHR'
                            });
                        });
                        
                        xhr.addEventListener('error', function() {
                            const endTime = performance.now();
                            const duration = endTime - xhr._startTime;
                            
                            queries.push({
                                id: xhr._queryId,
                                url: xhr._url,
                                method: xhr._method,
                                duration: duration,
                                startTime: xhr._startTime,
                                endTime: endTime,
                                status: 0,
                                size: 0,
                                type: 'XHR',
                                error: 'Network error'
                            });
                        });
                        
                        return originalXHRSend.apply(this, [data]);
                    };
                    
                    // Wait for some time to collect queries
                    setTimeout(() => {
                        resolve(queries);
                    }, 5000);
                });
                """;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> queryData = (List<Map<String, Object>>) jsExecutor.executeAsyncScript(script);
            
            for (Map<String, Object> data : queryData) {
                DatabaseQuery query = new DatabaseQuery();
                query.setId(data.get("id").toString());
                query.setUrl(data.get("url").toString());
                query.setMethod(data.get("method").toString());
                query.setDuration(((Number) data.get("duration")).doubleValue());
                query.setStartTime(((Number) data.get("startTime")).doubleValue());
                query.setEndTime(((Number) data.get("endTime")).doubleValue());
                query.setStatus(((Number) data.get("status")).intValue());
                query.setSize(((Number) data.get("size")).longValue());
                query.setType(data.get("type").toString());
                
                if (data.containsKey("error")) {
                    query.setError(data.get("error").toString());
                }
                
                // Analyze query characteristics
                analyzeQueryCharacteristics(query);
                
                queries.add(query);
            }
            
        } catch (Exception e) {
            System.err.println("Error monitoring database queries: " + e.getMessage());
        }
        
        return queries;
    }
    
    /**
     * Analyze query characteristics
     */
    private void analyzeQueryCharacteristics(DatabaseQuery query) {
        String url = query.getUrl().toLowerCase();
        
        // Determine if it's likely a database query
        if (url.contains("api/") || url.contains("/query") || url.contains("/data") || 
            url.contains("/search") || url.contains("/users") || url.contains("/products")) {
            query.setLikelyDatabaseQuery(true);
        }
        
        // Categorize query type
        if (url.contains("select") || query.getMethod().equals("GET")) {
            query.setQueryType("SELECT");
        } else if (url.contains("insert") || query.getMethod().equals("POST")) {
            query.setQueryType("INSERT");
        } else if (url.contains("update") || query.getMethod().equals("PUT")) {
            query.setQueryType("UPDATE");
        } else if (url.contains("delete") || query.getMethod().equals("DELETE")) {
            query.setQueryType("DELETE");
        } else {
            query.setQueryType("UNKNOWN");
        }
        
        // Determine query complexity based on duration and size
        if (query.getDuration() > 2000) {
            query.setComplexity("HIGH");
        } else if (query.getDuration() > 1000) {
            query.setComplexity("MEDIUM");
        } else {
            query.setComplexity("LOW");
        }
        
        // Check for potential performance issues
        if (query.getDuration() > 5000) {
            query.setPerformanceIssue("SLOW_QUERY");
        } else if (query.getStatus() >= 400) {
            query.setPerformanceIssue("FAILED_QUERY");
        } else if (query.getSize() > 1000000) { // 1MB
            query.setPerformanceIssue("LARGE_RESPONSE");
        } else {
            query.setPerformanceIssue("NONE");
        }
    }
    
    /**
     * Analyze connection pool performance
     */
    private DatabaseConnectionPool analyzeConnectionPool() {
        DatabaseConnectionPool connectionPool = new DatabaseConnectionPool();
        
        try {
            // Simulate connection pool analysis (in real implementation, this would connect to actual database)
            connectionPool.setActiveConnections(5);
            connectionPool.setIdleConnections(10);
            connectionPool.setMaxConnections(20);
            connectionPool.setConnectionWaitTime(50); // milliseconds
            connectionPool.setConnectionTimeout(30000); // milliseconds
            
            // Calculate pool utilization
            double utilization = (double) connectionPool.getActiveConnections() / connectionPool.getMaxConnections() * 100;
            connectionPool.setUtilizationPercentage(utilization);
            
            // Determine pool health
            if (utilization > 80) {
                connectionPool.setHealthStatus("CRITICAL");
            } else if (utilization > 60) {
                connectionPool.setHealthStatus("WARNING");
            } else {
                connectionPool.setHealthStatus("HEALTHY");
            }
            
        } catch (Exception e) {
            System.err.println("Error analyzing connection pool: " + e.getMessage());
        }
        
        return connectionPool;
    }
    
    /**
     * Monitor database resource usage
     */
    private DatabaseResourceUsage monitorDatabaseResourceUsage() {
        DatabaseResourceUsage resourceUsage = new DatabaseResourceUsage();
        
        try {
            // Simulate database resource monitoring (in real implementation, this would connect to actual database)
            resourceUsage.setCpuUsage(25.5); // percentage
            resourceUsage.setMemoryUsage(1024); // MB
            resourceUsage.setDiskUsage(5120); // MB
            resourceUsage.setNetworkLatency(15); // milliseconds
            resourceUsage.setActiveSessions(12);
            resourceUsage.setLockWaits(3);
            resourceUsage.setDeadlocks(0);
            resourceUsage.setBufferHitRatio(95.2); // percentage
            
            // Calculate resource health
            String healthStatus = "HEALTHY";
            if (resourceUsage.getCpuUsage() > 80 || resourceUsage.getMemoryUsage() > 4096) {
                healthStatus = "CRITICAL";
            } else if (resourceUsage.getCpuUsage() > 60 || resourceUsage.getMemoryUsage() > 2048) {
                healthStatus = "WARNING";
            }
            resourceUsage.setHealthStatus(healthStatus);
            
        } catch (Exception e) {
            System.err.println("Error monitoring database resource usage: " + e.getMessage());
        }
        
        return resourceUsage;
    }
    
    /**
     * Calculate database performance metrics
     */
    private void calculateDatabasePerformanceMetrics(DatabasePerformanceAnalysisResult result, List<DatabaseQuery> queries) {
        if (queries.isEmpty()) {
            return;
        }
        
        // Calculate basic metrics
        result.setTotalQueries(queries.size());
        result.setSuccessfulQueries((int) queries.stream().filter(q -> q.getStatus() >= 200 && q.getStatus() < 300).count());
        result.setFailedQueries((int) queries.stream().filter(q -> q.getStatus() >= 400).count());
        result.setSlowQueries((int) queries.stream().filter(q -> q.getDuration() > 1000).count());
        
        // Calculate timing metrics
        double averageResponseTime = queries.stream().mapToDouble(DatabaseQuery::getDuration).average().orElse(0.0);
        result.setAverageResponseTime(averageResponseTime);
        
        double minResponseTime = queries.stream().mapToDouble(DatabaseQuery::getDuration).min().orElse(0.0);
        result.setMinResponseTime(minResponseTime);
        
        double maxResponseTime = queries.stream().mapToDouble(DatabaseQuery::getDuration).max().orElse(0.0);
        result.setMaxResponseTime(maxResponseTime);
        
        // Calculate percentiles
        List<Double> responseTimes = queries.stream().mapToDouble(DatabaseQuery::getDuration).sorted().boxed().collect(Collectors.toList());
        result.setP50ResponseTime(calculatePercentile(responseTimes, 50));
        result.setP75ResponseTime(calculatePercentile(responseTimes, 75));
        result.setP90ResponseTime(calculatePercentile(responseTimes, 90));
        result.setP95ResponseTime(calculatePercentile(responseTimes, 95));
        result.setP99ResponseTime(calculatePercentile(responseTimes, 99));
        
        // Calculate throughput
        double totalTime = queries.stream().mapToDouble(q -> q.getEndTime() - q.getStartTime()).max().orElse(1.0);
        double throughput = queries.size() / (totalTime / 1000); // queries per second
        result.setThroughput(throughput);
        
        // Calculate error rate
        double errorRate = (double) result.getFailedQueries() / result.getTotalQueries() * 100;
        result.setErrorRate(errorRate);
        
        // Calculate success rate
        double successRate = (double) result.getSuccessfulQueries() / result.getTotalQueries() * 100;
        result.setSuccessRate(successRate);
    }
    
    /**
     * Identify performance bottlenecks
     */
    private void identifyPerformanceBottlenecks(DatabasePerformanceAnalysisResult result, List<DatabaseQuery> queries) {
        List<String> bottlenecks = new ArrayList<>();
        
        // Check for slow queries
        long slowQueryCount = queries.stream().filter(q -> q.getDuration() > 2000).count();
        if (slowQueryCount > 0) {
            bottlenecks.add(String.format("%d slow queries (>2s) detected", slowQueryCount));
        }
        
        // Check for high error rate
        if (result.getErrorRate() > 5) {
            bottlenecks.add(String.format("High error rate: %.1f%%", result.getErrorRate()));
        }
        
        // Check for connection pool issues
        if (result.getConnectionPool().getUtilizationPercentage() > 80) {
            bottlenecks.add("Connection pool utilization high: " + String.format("%.1f%%", result.getConnectionPool().getUtilizationPercentage()));
        }
        
        // Check for resource constraints
        if (result.getResourceUsage().getCpuUsage() > 80) {
            bottlenecks.add("High CPU usage: " + String.format("%.1f%%", result.getResourceUsage().getCpuUsage()));
        }
        
        if (result.getResourceUsage().getMemoryUsage() > 4096) {
            bottlenecks.add("High memory usage: " + result.getResourceUsage().getMemoryUsage() + " MB");
        }
        
        // Check for lock waits and deadlocks
        if (result.getResourceUsage().getLockWaits() > 10) {
            bottlenecks.add("High number of lock waits: " + result.getResourceUsage().getLockWaits());
        }
        
        if (result.getResourceUsage().getDeadlocks() > 0) {
            bottlenecks.add("Deadlocks detected: " + result.getResourceUsage().getDeadlocks());
        }
        
        // Check for low buffer hit ratio
        if (result.getResourceUsage().getBufferHitRatio() < 90) {
            bottlenecks.add("Low buffer hit ratio: " + String.format("%.1f%%", result.getResourceUsage().getBufferHitRatio()));
        }
        
        result.setPerformanceBottlenecks(bottlenecks);
    }
    
    /**
     * Generate database optimization recommendations
     */
    private List<String> generateDatabaseOptimizationRecommendations(DatabasePerformanceAnalysisResult result) {
        List<String> recommendations = new ArrayList<>();
        
        // Slow query recommendations
        if (result.getSlowQueries() > 0) {
            recommendations.add("Optimize slow queries - consider adding indexes or rewriting queries");
            recommendations.add("Use query execution plans to identify bottlenecks");
            recommendations.add("Consider query result caching for frequently accessed data");
        }
        
        // High error rate recommendations
        if (result.getErrorRate() > 5) {
            recommendations.add("Investigate and fix database errors - check connection issues and query syntax");
            recommendations.add("Implement proper error handling and retry mechanisms");
        }
        
        // Connection pool recommendations
        if (result.getConnectionPool().getUtilizationPercentage() > 80) {
            recommendations.add("Increase connection pool size or optimize connection usage");
            recommendations.add("Implement connection pooling best practices");
        }
        
        // Resource usage recommendations
        if (result.getResourceUsage().getCpuUsage() > 80) {
            recommendations.add("Optimize CPU-intensive queries and consider hardware upgrades");
            recommendations.add("Use database profiling to identify CPU bottlenecks");
        }
        
        if (result.getResourceUsage().getMemoryUsage() > 4096) {
            recommendations.add("Optimize memory usage - check for memory leaks and large result sets");
            recommendations.add("Consider increasing database memory allocation");
        }
        
        // Lock wait recommendations
        if (result.getResourceUsage().getLockWaits() > 10) {
            recommendations.add("Optimize transaction isolation levels and reduce lock contention");
            recommendations.add("Consider implementing optimistic locking strategies");
        }
        
        // Deadlock recommendations
        if (result.getResourceUsage().getDeadlocks() > 0) {
            recommendations.add("Analyze deadlock patterns and optimize transaction order");
            recommendations.add("Implement deadlock detection and resolution strategies");
        }
        
        // Buffer hit ratio recommendations
        if (result.getResourceUsage().getBufferHitRatio() < 90) {
            recommendations.add("Increase database buffer cache size");
            recommendations.add("Optimize query patterns to improve data locality");
        }
        
        // General recommendations
        recommendations.add("Implement database monitoring and alerting");
        recommendations.add("Regular database maintenance and index optimization");
        recommendations.add("Use database performance tuning tools");
        recommendations.add("Consider read replicas for read-heavy workloads");
        
        return recommendations;
    }
    
    /**
     * Analyze query patterns
     */
    private void analyzeQueryPatterns(DatabasePerformanceAnalysisResult result, List<DatabaseQuery> queries) {
        QueryPatternAnalysis patternAnalysis = new QueryPatternAnalysis();
        
        // Analyze by query type
        Map<String, Long> queryTypeCounts = queries.stream()
            .collect(Collectors.groupingBy(DatabaseQuery::getQueryType, Collectors.counting()));
        patternAnalysis.setQueryTypeCounts(queryTypeCounts);
        
        // Analyze by complexity
        Map<String, Long> complexityCounts = queries.stream()
            .collect(Collectors.groupingBy(DatabaseQuery::getComplexity, Collectors.counting()));
        patternAnalysis.setComplexityCounts(complexityCounts);
        
        // Analyze by performance issues
        Map<String, Long> issueCounts = queries.stream()
            .collect(Collectors.groupingBy(DatabaseQuery::getPerformanceIssue, Collectors.counting()));
        patternAnalysis.setIssueCounts(issueCounts);
        
        // Analyze by URL patterns
        Map<String, Long> urlPatterns = queries.stream()
            .collect(Collectors.groupingBy(query -> extractUrlPattern(query.getUrl()), Collectors.counting()));
        patternAnalysis.setUrlPatterns(urlPatterns);
        
        // Calculate pattern insights
        List<String> patternInsights = new ArrayList<>();
        
        if (queryTypeCounts.getOrDefault("SELECT", 0L) > queryTypeCounts.getOrDefault("INSERT", 0L) + 
            queryTypeCounts.getOrDefault("UPDATE", 0L) + queryTypeCounts.getOrDefault("DELETE", 0L)) {
            patternInsights.add("Read-heavy workload detected - consider read replicas");
        }
        
        if (complexityCounts.getOrDefault("HIGH", 0L) > queries.size() * 0.2) {
            patternInsights.add("High proportion of complex queries - consider optimization");
        }
        
        if (issueCounts.getOrDefault("SLOW_QUERY", 0L) > 0) {
            patternInsights.add("Slow queries detected - prioritize optimization");
        }
        
        patternAnalysis.setInsights(patternInsights);
        
        result.setPatternAnalysis(patternAnalysis);
    }
    
    /**
     * Extract URL pattern from query URL
     */
    private String extractUrlPattern(String url) {
        try {
            // Extract pattern from URL (e.g., /api/users -> /api/users)
            if (url.contains("/api/")) {
                String[] parts = url.split("/api/");
                if (parts.length > 1) {
                    String path = parts[1];
                    if (path.contains("?")) {
                        path = path.substring(0, path.indexOf("?"));
                    }
                    return "/api/" + path.split("/")[0];
                }
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }
    
    /**
     * Calculate percentile
     */
    private double calculatePercentile(List<Double> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }
    
    // Data Models
    
    public static class DatabaseQuery {
        private String id;
        private String url;
        private String method;
        private double duration;
        private double startTime;
        private double endTime;
        private int status;
        private long size;
        private String type;
        private String error;
        private boolean likelyDatabaseQuery;
        private String queryType;
        private String complexity;
        private String performanceIssue;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
        
        public double getStartTime() { return startTime; }
        public void setStartTime(double startTime) { this.startTime = startTime; }
        
        public double getEndTime() { return endTime; }
        public void setEndTime(double endTime) { this.endTime = endTime; }
        
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public boolean isLikelyDatabaseQuery() { return likelyDatabaseQuery; }
        public void setLikelyDatabaseQuery(boolean likelyDatabaseQuery) { this.likelyDatabaseQuery = likelyDatabaseQuery; }
        
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public String getComplexity() { return complexity; }
        public void setComplexity(String complexity) { this.complexity = complexity; }
        
        public String getPerformanceIssue() { return performanceIssue; }
        public void setPerformanceIssue(String performanceIssue) { this.performanceIssue = performanceIssue; }
    }
    
    public static class DatabaseConnectionPool {
        private int activeConnections;
        private int idleConnections;
        private int maxConnections;
        private double connectionWaitTime;
        private int connectionTimeout;
        private double utilizationPercentage;
        private String healthStatus;
        
        // Getters and setters
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        
        public int getIdleConnections() { return idleConnections; }
        public void setIdleConnections(int idleConnections) { this.idleConnections = idleConnections; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public double getConnectionWaitTime() { return connectionWaitTime; }
        public void setConnectionWaitTime(double connectionWaitTime) { this.connectionWaitTime = connectionWaitTime; }
        
        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        
        public double getUtilizationPercentage() { return utilizationPercentage; }
        public void setUtilizationPercentage(double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
        
        public String getHealthStatus() { return healthStatus; }
        public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    }
    
    public static class DatabaseResourceUsage {
        private double cpuUsage;
        private long memoryUsage;
        private long diskUsage;
        private double networkLatency;
        private int activeSessions;
        private int lockWaits;
        private int deadlocks;
        private double bufferHitRatio;
        private String healthStatus;
        
        // Getters and setters
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public long getDiskUsage() { return diskUsage; }
        public void setDiskUsage(long diskUsage) { this.diskUsage = diskUsage; }
        
        public double getNetworkLatency() { return networkLatency; }
        public void setNetworkLatency(double networkLatency) { this.networkLatency = networkLatency; }
        
        public int getActiveSessions() { return activeSessions; }
        public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
        
        public int getLockWaits() { return lockWaits; }
        public void setLockWaits(int lockWaits) { this.lockWaits = lockWaits; }
        
        public int getDeadlocks() { return deadlocks; }
        public void setDeadlocks(int deadlocks) { this.deadlocks = deadlocks; }
        
        public double getBufferHitRatio() { return bufferHitRatio; }
        public void setBufferHitRatio(double bufferHitRatio) { this.bufferHitRatio = bufferHitRatio; }
        
        public String getHealthStatus() { return healthStatus; }
        public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    }
    
    public static class QueryPatternAnalysis {
        private Map<String, Long> queryTypeCounts;
        private Map<String, Long> complexityCounts;
        private Map<String, Long> issueCounts;
        private Map<String, Long> urlPatterns;
        private List<String> insights;
        
        // Getters and setters
        public Map<String, Long> getQueryTypeCounts() { return queryTypeCounts; }
        public void setQueryTypeCounts(Map<String, Long> queryTypeCounts) { this.queryTypeCounts = queryTypeCounts; }
        
        public Map<String, Long> getComplexityCounts() { return complexityCounts; }
        public void setComplexityCounts(Map<String, Long> complexityCounts) { this.complexityCounts = complexityCounts; }
        
        public Map<String, Long> getIssueCounts() { return issueCounts; }
        public void setIssueCounts(Map<String, Long> issueCounts) { this.issueCounts = issueCounts; }
        
        public Map<String, Long> getUrlPatterns() { return urlPatterns; }
        public void setUrlPatterns(Map<String, Long> urlPatterns) { this.urlPatterns = urlPatterns; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
    }
    
    public static class DatabasePerformanceAnalysisResult {
        private long timestamp;
        private List<DatabaseQuery> databaseQueries;
        private DatabaseConnectionPool connectionPool;
        private DatabaseResourceUsage resourceUsage;
        private int totalQueries;
        private int successfulQueries;
        private int failedQueries;
        private int slowQueries;
        private double averageResponseTime;
        private double minResponseTime;
        private double maxResponseTime;
        private double p50ResponseTime;
        private double p75ResponseTime;
        private double p90ResponseTime;
        private double p95ResponseTime;
        private double p99ResponseTime;
        private double throughput;
        private double errorRate;
        private double successRate;
        private List<String> performanceBottlenecks;
        private List<String> recommendations;
        private QueryPatternAnalysis patternAnalysis;
        private String error;
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public List<DatabaseQuery> getDatabaseQueries() { return databaseQueries; }
        public void setDatabaseQueries(List<DatabaseQuery> databaseQueries) { this.databaseQueries = databaseQueries; }
        
        public DatabaseConnectionPool getConnectionPool() { return connectionPool; }
        public void setConnectionPool(DatabaseConnectionPool connectionPool) { this.connectionPool = connectionPool; }
        
        public DatabaseResourceUsage getResourceUsage() { return resourceUsage; }
        public void setResourceUsage(DatabaseResourceUsage resourceUsage) { this.resourceUsage = resourceUsage; }
        
        public int getTotalQueries() { return totalQueries; }
        public void setTotalQueries(int totalQueries) { this.totalQueries = totalQueries; }
        
        public int getSuccessfulQueries() { return successfulQueries; }
        public void setSuccessfulQueries(int successfulQueries) { this.successfulQueries = successfulQueries; }
        
        public int getFailedQueries() { return failedQueries; }
        public void setFailedQueries(int failedQueries) { this.failedQueries = failedQueries; }
        
        public int getSlowQueries() { return slowQueries; }
        public void setSlowQueries(int slowQueries) { this.slowQueries = slowQueries; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public double getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(double minResponseTime) { this.minResponseTime = minResponseTime; }
        
        public double getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(double maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public double getP50ResponseTime() { return p50ResponseTime; }
        public void setP50ResponseTime(double p50ResponseTime) { this.p50ResponseTime = p50ResponseTime; }
        
        public double getP75ResponseTime() { return p75ResponseTime; }
        public void setP75ResponseTime(double p75ResponseTime) { this.p75ResponseTime = p75ResponseTime; }
        
        public double getP90ResponseTime() { return p90ResponseTime; }
        public void setP90ResponseTime(double p90ResponseTime) { this.p90ResponseTime = p90ResponseTime; }
        
        public double getP95ResponseTime() { return p95ResponseTime; }
        public void setP95ResponseTime(double p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
        
        public double getP99ResponseTime() { return p99ResponseTime; }
        public void setP99ResponseTime(double p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
        
        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
        
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public List<String> getPerformanceBottlenecks() { return performanceBottlenecks; }
        public void setPerformanceBottlenecks(List<String> performanceBottlenecks) { this.performanceBottlenecks = performanceBottlenecks; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public QueryPatternAnalysis getPatternAnalysis() { return patternAnalysis; }
        public void setPatternAnalysis(QueryPatternAnalysis patternAnalysis) { this.patternAnalysis = patternAnalysis; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class DatabaseConnection {
        private String id;
        private String status;
        private long creationTime;
        private long lastUsedTime;
        private int queryCount;
        private double totalQueryTime;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getCreationTime() { return creationTime; }
        public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
        
        public long getLastUsedTime() { return lastUsedTime; }
        public void setLastUsedTime(long lastUsedTime) { this.lastUsedTime = lastUsedTime; }
        
        public int getQueryCount() { return queryCount; }
        public void setQueryCount(int queryCount) { this.queryCount = queryCount; }
        
        public double getTotalQueryTime() { return totalQueryTime; }
        public void setTotalQueryTime(double totalQueryTime) { this.totalQueryTime = totalQueryTime; }
    }
}
