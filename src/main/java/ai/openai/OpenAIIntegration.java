package ai.openai;

import ai.cache.AIResponseCache;
import ai.limits.APIUsageLimiter;
import config.ConfigurationManager;
import reporting.TestLogManager;

import java.util.*;

/**
 * OpenAI Integration
 * 
 * Integrates OpenAI API with existing AI features
 */
public class OpenAIIntegration {
    
    private final OpenAIClient openAIClient;
    private final ConfigurationManager config;
    private final AIResponseCache cache;
    private final APIUsageLimiter usageLimiter;
    private final boolean enabled;
    private final boolean agentModeEnabled;
    
    public OpenAIIntegration() {
        this.config = ConfigurationManager.getInstance();
        this.openAIClient = OpenAIClient.getInstance();
        this.cache = AIResponseCache.getInstance();
        this.usageLimiter = APIUsageLimiter.getInstance();
        this.enabled = config.getBoolean("ai.enabled", true) && openAIClient.isEnabled();
        this.agentModeEnabled = config.getBoolean("agent.mode.enabled", false);
        
        if (enabled) {
            TestLogManager.info("OpenAI integration enabled");
            if (agentModeEnabled) {
                TestLogManager.info("Agent mode enabled");
            }
        }
    }
    
    /**
     * Check if OpenAI integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Generate test cases using OpenAI
     */
    public List<String> generateTestCasesWithOpenAI(String userStory) {
        if (!enabled || !config.getBoolean("ai.test.generation.enabled", false)) {
            throw new IllegalStateException("AI test generation is not enabled");
        }
        
        // Check cache first
        Map<String, Object> cacheInputs = new HashMap<>();
        cacheInputs.put("userStory", userStory);
        String cacheKey = AIResponseCache.generateKey("openai-test-generation", cacheInputs);
        
        @SuppressWarnings("unchecked")
        Optional<List<String>> cached = (Optional<List<String>>) (Optional<?>) cache.get(cacheKey, List.class);
        if (cached.isPresent()) {
            TestLogManager.info("Using cached test cases");
            return cached.get();
        }
        
        // Check API limits
        if (!usageLimiter.isAllowed("openai")) {
            throw new IllegalStateException("API rate limit exceeded");
        }
        
        try {
            // Generate using OpenAI
            String response = openAIClient.generateTestCases(userStory);
            
            // Parse response into test cases
            List<String> testCases = parseTestCases(response);
            
            // Cache result
            cache.put(cacheKey, testCases, 3600000); // 1 hour
            
            // Record usage
            usageLimiter.recordUsage("openai", 0.01); // Estimate cost
            
            return testCases;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate test cases with OpenAI", e);
            throw e;
        }
    }
    
    /**
     * Find alternative locator using OpenAI
     */
    public String findAlternativeLocatorWithOpenAI(String elementName, String description, String originalLocator) {
//        if (!enabled || !config.getBoolean("self.healing.enabled", false)) {
//            return null;
//        }
        
        // Check cache
        Map<String, Object> cacheInputs = new HashMap<>();
        cacheInputs.put("elementName", elementName);
        cacheInputs.put("description", description);
        cacheInputs.put("originalLocator", originalLocator);
        String cacheKey = AIResponseCache.generateKey("openai-locator-healing", cacheInputs);
        
        Optional<String> cached = cache.get(cacheKey, String.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // Check API limits
        if (!usageLimiter.isAllowed("openai")) {
            return null; // Fall back to non-AI healing
        }
        
        try {
            String alternative = openAIClient.findAlternativeLocator(elementName, description, originalLocator);
            
            // Cache result
            cache.put(cacheKey, alternative, 7200000); // 2 hours
            
            // Record usage
            usageLimiter.recordUsage("openai", 0.005); // Lower cost for simple queries
            
            return alternative;
            
        } catch (Exception e) {
            TestLogManager.warning("OpenAI locator healing failed, using fallback: " + e.getMessage());
            return null; // Fall back to non-AI healing
        }
    }
    
    /**
     * Analyze performance using OpenAI
     */
    public String analyzePerformanceWithOpenAI(Map<String, Object> performanceData) {
        if (!enabled) {
            return null;
        }
        
        // Check cache
        String cacheKey = AIResponseCache.generateKey("openai-performance-analysis", performanceData);
        Optional<String> cached = cache.get(cacheKey, String.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // Check API limits
        if (!usageLimiter.isAllowed("openai")) {
            return null;
        }
        
        try {
            String analysis = openAIClient.analyzePerformance(performanceData);
            
            // Cache result
            cache.put(cacheKey, analysis, 1800000); // 30 minutes
            
            // Record usage
            usageLimiter.recordUsage("openai", 0.02);
            
            return analysis;
            
        } catch (Exception e) {
            TestLogManager.error("OpenAI performance analysis failed", e);
            return null;
        }
    }
    
    /**
     * Parse test cases from OpenAI response
     */
    private List<String> parseTestCases(String response) {
        List<String> testCases = new ArrayList<>();
        
        // Simple parsing - split by test case markers
        String[] parts = response.split("(Test Case|Test Case:|##)");
        for (String part : parts) {
            part = part.trim();
            if (part.length() > 50) { // Filter out very short parts
                testCases.add(part);
            }
        }
        
        if (testCases.isEmpty()) {
            // If parsing fails, return the whole response as one test case
            testCases.add(response);
        }
        
        return testCases;
    }
    
    /**
     * Get OpenAI usage statistics
     */
    public Map<String, Object> getUsageStatistics() {
        APIUsageLimiter.UsageStatistics stats = usageLimiter.getUsageStatistics("openai");
        Map<String, Object> result = new HashMap<>();
        result.put("totalCalls", stats.getTotalCalls());
        result.put("successRate", stats.getSuccessRate());
        result.put("totalCost", stats.getTotalCost());
        result.put("remainingQuota", usageLimiter.getRemainingQuota("openai"));
        return result;
    }
}

