package ai.cicd;

import ai.workflow.AIWorkflowEngine;
import ai.workflow.AIWorkflowEngine.WorkflowResult;
import ai.cache.AIResponseCache;
import ai.limits.APIUsageLimiter;
import config.ConfigurationManager;
import reporting.TestLogManager;

import java.util.*;

/**
 * CI/CD Integration for AI Features
 * 
 * Integrates AI capabilities into CI/CD pipeline
 */
public class CICDIntegration {
    
    private final AIWorkflowEngine workflowEngine;
    private final APIUsageLimiter usageLimiter;
    private final ConfigurationManager config;
    
    public CICDIntegration() {
        this.workflowEngine = new AIWorkflowEngine();
        this.usageLimiter = APIUsageLimiter.getInstance();
        this.config = ConfigurationManager.getInstance();
    }
    
    /**
     * Pre-build validation
     */
    public CICDResult preBuildValidation() {
        TestLogManager.info("Running pre-build AI validation");
        
        Map<String, Object> inputs = new HashMap<>();
        WorkflowResult result = workflowEngine.executeWorkflow("pre-test-validation", inputs);
        
        return new CICDResult(
            "pre-build-validation",
            result.isSuccess(),
            result.getMessage(),
            result.getOutputs()
        );
    }
    
    /**
     * Post-build analysis
     */
    public CICDResult postBuildAnalysis(Map<String, Object> buildData) {
        TestLogManager.info("Running post-build AI analysis");
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.putAll(buildData);
        
        WorkflowResult result = workflowEngine.executeWorkflow("complete-test-analysis", inputs);
        
        return new CICDResult(
            "post-build-analysis",
            result.isSuccess(),
            result.getMessage(),
            result.getOutputs()
        );
    }
    
    /**
     * Test generation for new features
     */
    public CICDResult generateTestsForFeature(String userStory) {
        TestLogManager.info("Generating tests for new feature");
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("userStory", userStory);
        
        WorkflowResult result = workflowEngine.executeWorkflow("test-generation", inputs);
        
        return new CICDResult(
            "test-generation",
            result.isSuccess(),
            result.getMessage(),
            result.getOutputs()
        );
    }
    
    /**
     * Performance monitoring
     */
    public CICDResult monitorPerformance() {
        TestLogManager.info("Running performance monitoring");
        
        Map<String, Object> inputs = new HashMap<>();
        WorkflowResult result = workflowEngine.executeWorkflow("performance-analysis", inputs);
        
        return new CICDResult(
            "performance-monitoring",
            result.isSuccess(),
            result.getMessage(),
            result.getOutputs()
        );
    }
    
    /**
     * Get API usage report
     */
    public Map<String, Object> getAPIUsageReport() {
        Map<String, Object> report = new HashMap<>();
        
        String[] apis = {"test-generation", "self-healing", "performance-analysis"};
        for (String api : apis) {
            APIUsageLimiter.UsageStatistics stats = usageLimiter.getUsageStatistics(api);
            Map<String, Object> apiStats = new HashMap<>();
            apiStats.put("totalCalls", stats.getTotalCalls());
            apiStats.put("successRate", stats.getSuccessRate());
            apiStats.put("totalCost", stats.getTotalCost());
            apiStats.put("remainingQuota", usageLimiter.getRemainingQuota(api));
            report.put(api, apiStats);
        }
        
        return report;
    }
    
    /**
     * Check if CI/CD AI features are enabled
     */
    public boolean isEnabled() {
        return config.getBoolean("ai.cicd.enabled", true);
    }
    
    /**
     * CI/CD Result
     */
    public static class CICDResult {
        private final String stage;
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;
        
        public CICDResult(String stage, boolean success, String message, Map<String, Object> data) {
            this.stage = stage;
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public String getStage() { return stage; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }
}

