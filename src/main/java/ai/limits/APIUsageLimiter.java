package ai.limits;

import config.ConfigurationManager;
import reporting.TestLogManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API Usage Limiter
 * 
 * Tracks and limits AI API usage to prevent exceeding quotas
 * Supports rate limiting, daily limits, and cost tracking
 */
public class APIUsageLimiter {
    
    private static volatile APIUsageLimiter instance;
    private final ConfigurationManager config;
    
    // Rate limiting
    private final Map<String, RateLimiter> rateLimiters;
    
    // Daily limits
    private final Map<String, DailyLimit> dailyLimits;
    
    // Cost tracking
    private final Map<String, AtomicLong> costTrackers;
    
    // Usage statistics
    private final Map<String, UsageStatistics> usageStats;
    
    private APIUsageLimiter() {
        this.config = ConfigurationManager.getInstance();
        this.rateLimiters = new ConcurrentHashMap<>();
        this.dailyLimits = new ConcurrentHashMap<>();
        this.costTrackers = new ConcurrentHashMap<>();
        this.usageStats = new ConcurrentHashMap<>();
        initializeLimits();
    }
    
    public static APIUsageLimiter getInstance() {
        if (instance == null) {
            synchronized (APIUsageLimiter.class) {
                if (instance == null) {
                    instance = new APIUsageLimiter();
                }
            }
        }
        return instance;
    }
    
    /**
     * Check if API call is allowed
     */
    public boolean isAllowed(String apiName) {
        // Check rate limit
        if (!checkRateLimit(apiName)) {
            TestLogManager.warning("Rate limit exceeded for: " + apiName);
            return false;
        }
        
        // Check daily limit
        if (!checkDailyLimit(apiName)) {
            TestLogManager.warning("Daily limit exceeded for: " + apiName);
            return false;
        }
        
        return true;
    }
    
    /**
     * Record API usage
     */
    public void recordUsage(String apiName, double cost) {
        // Update rate limiter
        RateLimiter limiter = rateLimiters.get(apiName);
        if (limiter != null) {
            limiter.increment();
        }
        
        // Update daily limit
        DailyLimit dailyLimit = dailyLimits.get(apiName);
        if (dailyLimit != null) {
            dailyLimit.increment();
        }
        
        // Track cost
        costTrackers.computeIfAbsent(apiName, k -> new AtomicLong(0))
            .addAndGet((long) (cost * 1000)); // Store as millicents
        
        // Update statistics
        usageStats.computeIfAbsent(apiName, k -> new UsageStatistics())
            .increment(cost);
    }
    
    /**
     * Get remaining quota for today
     */
    public int getRemainingQuota(String apiName) {
        DailyLimit dailyLimit = dailyLimits.get(apiName);
        if (dailyLimit == null) return Integer.MAX_VALUE;
        return dailyLimit.getRemaining();
    }
    
    /**
     * Get usage statistics
     */
    public UsageStatistics getUsageStatistics(String apiName) {
        return usageStats.getOrDefault(apiName, new UsageStatistics());
    }
    
    /**
     * Get total cost for API
     */
    public double getTotalCost(String apiName) {
        AtomicLong cost = costTrackers.get(apiName);
        if (cost == null) return 0.0;
        return cost.get() / 1000.0; // Convert from millicents
    }
    
    /**
     * Reset daily limits (call at start of new day)
     */
    public void resetDailyLimits() {
        dailyLimits.values().forEach(DailyLimit::reset);
        TestLogManager.info("Daily API limits reset");
    }
    
    /**
     * Initialize limits from configuration
     */
    private void initializeLimits() {
        // Default rate limits (calls per minute)
        int defaultRateLimit = config.getInt("ai.api.rate.limit", 60);
        
        // Default daily limits
        int defaultDailyLimit = config.getInt("ai.api.daily.limit", 10000);
        
        // Initialize for common APIs
        String[] apis = {"openai", "anthropic", "test-generation", "self-healing", "performance-analysis"};
        for (String api : apis) {
            int rateLimit = config.getInt("ai.api." + api + ".rate.limit", defaultRateLimit);
            int dailyLimit = config.getInt("ai.api." + api + ".daily.limit", defaultDailyLimit);
            
            rateLimiters.put(api, new RateLimiter(rateLimit));
            dailyLimits.put(api, new DailyLimit(dailyLimit));
        }
    }
    
    /**
     * Check rate limit
     */
    private boolean checkRateLimit(String apiName) {
        RateLimiter limiter = rateLimiters.get(apiName);
        if (limiter == null) return true; // No limit configured
        return limiter.isAllowed();
    }
    
    /**
     * Check daily limit
     */
    private boolean checkDailyLimit(String apiName) {
        DailyLimit dailyLimit = dailyLimits.get(apiName);
        if (dailyLimit == null) return true; // No limit configured
        return dailyLimit.isAllowed();
    }
    
    /**
     * Rate limiter (sliding window)
     */
    private static class RateLimiter {
        private final int maxRequests;
        private final long windowMillis;
        private final Queue<Long> requestTimes;
        
        public RateLimiter(int maxRequestsPerMinute) {
            this.maxRequests = maxRequestsPerMinute;
            this.windowMillis = 60000; // 1 minute
            this.requestTimes = new ArrayDeque<>();
        }
        
        public synchronized boolean isAllowed() {
            long now = System.currentTimeMillis();
            
            // Remove old requests outside window
            while (!requestTimes.isEmpty() && now - requestTimes.peek() > windowMillis) {
                requestTimes.poll();
            }
            
            if (requestTimes.size() < maxRequests) {
                requestTimes.offer(now);
                return true;
            }
            
            return false;
        }
        
        public synchronized void increment() {
            isAllowed(); // This will add the request
        }
    }
    
    /**
     * Daily limit tracker
     */
    private static class DailyLimit {
        private final int maxRequests;
        private final AtomicInteger currentCount;
        private long resetTime;
        
        public DailyLimit(int maxRequests) {
            this.maxRequests = maxRequests;
            this.currentCount = new AtomicInteger(0);
            this.resetTime = getNextMidnight();
        }
        
        public boolean isAllowed() {
            checkAndReset();
            return currentCount.get() < maxRequests;
        }
        
        public void increment() {
            checkAndReset();
            currentCount.incrementAndGet();
        }
        
        public int getRemaining() {
            checkAndReset();
            return Math.max(0, maxRequests - currentCount.get());
        }
        
        public void reset() {
            currentCount.set(0);
            resetTime = getNextMidnight();
        }
        
        private void checkAndReset() {
            long now = System.currentTimeMillis();
            if (now >= resetTime) {
                reset();
            }
        }
        
        private long getNextMidnight() {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }
    }
    
    /**
     * Usage statistics
     */
    public static class UsageStatistics {
        private final AtomicInteger totalCalls;
        private final AtomicInteger successfulCalls;
        private final AtomicInteger failedCalls;
        private final AtomicLong totalCost;
        
        public UsageStatistics() {
            this.totalCalls = new AtomicInteger(0);
            this.successfulCalls = new AtomicInteger(0);
            this.failedCalls = new AtomicInteger(0);
            this.totalCost = new AtomicLong(0);
        }
        
        public void increment(double cost) {
            totalCalls.incrementAndGet();
            successfulCalls.incrementAndGet();
            totalCost.addAndGet((long) (cost * 1000));
        }
        
        public void incrementFailed() {
            totalCalls.incrementAndGet();
            failedCalls.incrementAndGet();
        }
        
        public int getTotalCalls() { return totalCalls.get(); }
        public int getSuccessfulCalls() { return successfulCalls.get(); }
        public int getFailedCalls() { return failedCalls.get(); }
        public double getTotalCost() { return totalCost.get() / 1000.0; }
        public double getSuccessRate() {
            int total = totalCalls.get();
            return total > 0 ? (double) successfulCalls.get() / total : 0.0;
        }
    }
}

