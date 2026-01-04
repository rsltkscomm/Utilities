package ai.workflow;

import ai.AITestCaseGenerator;
import performanceTracker.AIPerformanceAnalyzer;
import reporting.TestLogManager;

import java.util.*;

/**
 * Complete Test Analysis Workflow
 * Combines test generation, performance analysis, and self-healing analysis
 */
public class CompleteTestAnalysisWorkflow implements AIWorkflowEngine.AIWorkflow {
    
    @Override
    public AIWorkflowEngine.WorkflowResult execute(Map<String, Object> inputs) {
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> outputs = new HashMap<>();
            
            // Step 1: Test Generation
            if (inputs.containsKey("userStory") || inputs.containsKey("requirements")) {
                TestGenerationWorkflow testGen = new TestGenerationWorkflow();
                AIWorkflowEngine.WorkflowResult testResult = testGen.execute(inputs);
                outputs.put("testGeneration", testResult.getOutputs());
            }
            
            // Step 2: Performance Analysis
            PerformanceAnalysisWorkflow perfAnalysis = new PerformanceAnalysisWorkflow();
            AIWorkflowEngine.WorkflowResult perfResult = perfAnalysis.execute(inputs);
            outputs.put("performanceAnalysis", perfResult.getOutputs());
            
            // Step 3: Self-Healing Analysis
            SelfHealingAnalysisWorkflow healingAnalysis = new SelfHealingAnalysisWorkflow();
            AIWorkflowEngine.WorkflowResult healingResult = healingAnalysis.execute(inputs);
            outputs.put("selfHealingAnalysis", healingResult.getOutputs());
            
            // Summary
            outputs.put("workflowStatus", "completed");
            outputs.put("stepsCompleted", 3);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new AIWorkflowEngine.WorkflowResult(
                outputs,
                true,
                "Complete test analysis completed successfully",
                executionTime
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new AIWorkflowEngine.WorkflowResult(
                new HashMap<>(),
                false,
                "Complete test analysis failed: " + e.getMessage(),
                executionTime
            );
        }
    }
    
    @Override
    public String getName() {
        return "complete-test-analysis";
    }
    
    @Override
    public String getDescription() {
        return "Complete test analysis combining test generation, performance, and self-healing";
    }
}

