package ai.workflow;

import ai.AITestCaseGenerator;
import ai.GeneratedTestCase;
import reporting.TestLogManager;

import java.util.*;

/**
 * Test Generation Workflow
 * Generates test cases from user stories and requirements
 */
public class TestGenerationWorkflow implements AIWorkflowEngine.AIWorkflow {
    
    @Override
    public AIWorkflowEngine.WorkflowResult execute(Map<String, Object> inputs) {
        long startTime = System.currentTimeMillis();
        
        try {
            String userStory = (String) inputs.getOrDefault("userStory", "");
            String requirements = (String) inputs.getOrDefault("requirements", "");
            
            AITestCaseGenerator generator = new AITestCaseGenerator();
            List<GeneratedTestCase> testCases = new ArrayList<>();
            
            // Generate from user story
            if (!userStory.isEmpty()) {
                TestLogManager.info("Generating test cases from user story");
                testCases.addAll(generator.generateFromUserStory(userStory));
            }
            
            // Generate from requirements
            if (!requirements.isEmpty()) {
                TestLogManager.info("Generating test cases from requirements");
                testCases.addAll(generator.generateFromRequirements(requirements));
            }
            
            // Get optimization suggestions
            List<String> suggestions = generator.getOptimizationSuggestions(testCases);
            
            // Prepare outputs
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("testCases", testCases);
            outputs.put("testCaseCount", testCases.size());
            outputs.put("optimizationSuggestions", suggestions);
            outputs.put("generatedTestCode", generateTestCode(testCases));
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new AIWorkflowEngine.WorkflowResult(
                outputs,
                true,
                "Generated " + testCases.size() + " test cases",
                executionTime
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new AIWorkflowEngine.WorkflowResult(
                new HashMap<>(),
                false,
                "Test generation failed: " + e.getMessage(),
                executionTime
            );
        }
    }
    
    @Override
    public String getName() {
        return "test-generation";
    }
    
    @Override
    public String getDescription() {
        return "Generates test cases from user stories and requirements";
    }
    
    private String generateTestCode(List<GeneratedTestCase> testCases) {
        if (testCases.isEmpty()) return "";
        
        AITestCaseGenerator generator = new AITestCaseGenerator();
        return generator.generateTestNGCode(testCases.get(0));
    }
}

