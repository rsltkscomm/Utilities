package ai;

import reporting.TestLogManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides optimization suggestions for generated test cases
 */
class TestCaseOptimizer {
    
    /**
     * Analyze test cases and provide optimization suggestions
     */
    public List<String> analyzeAndSuggest(List<GeneratedTestCase> testCases) {
        List<String> suggestions = new ArrayList<>();
        
        if (testCases == null || testCases.isEmpty()) {
            suggestions.add("No test cases to analyze");
            return suggestions;
        }
        
        try {
            // Analyze test case structure
            analyzeTestStructure(testCases, suggestions);
            
            // Analyze test data
            analyzeTestData(testCases, suggestions);
            
            // Analyze test steps
            analyzeTestSteps(testCases, suggestions);
            
            // Analyze coverage
            analyzeCoverage(testCases, suggestions);
            
            // Analyze maintainability
            analyzeMaintainability(testCases, suggestions);
            
        } catch (Exception e) {
            TestLogManager.error("Error analyzing test cases for optimization", e);
            suggestions.add("Error during analysis: " + e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * Analyze test case structure
     */
    private void analyzeTestStructure(List<GeneratedTestCase> testCases, List<String> suggestions) {
        // Check for duplicate test names
        Map<String, Integer> nameCount = new HashMap<>();
        for (GeneratedTestCase testCase : testCases) {
            String name = testCase.getTestName();
            nameCount.put(name, nameCount.getOrDefault(name, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : nameCount.entrySet()) {
            if (entry.getValue() > 1) {
                suggestions.add("⚠️ Duplicate test name found: " + entry.getKey() + 
                    " (appears " + entry.getValue() + " times). Consider renaming.");
            }
        }
        
        // Check for missing descriptions
        long missingDescriptions = testCases.stream()
            .filter(tc -> tc.getDescription() == null || tc.getDescription().trim().isEmpty())
            .count();
        
        if (missingDescriptions > 0) {
            suggestions.add("⚠️ " + missingDescriptions + " test case(s) missing descriptions. " +
                "Add descriptions for better documentation.");
        }
    }
    
    /**
     * Analyze test data
     */
    private void analyzeTestData(List<GeneratedTestCase> testCases, List<String> suggestions) {
        long missingTestData = testCases.stream()
            .filter(tc -> tc.getTestData() == null || 
                tc.getTestData().getAllData().isEmpty())
            .count();
        
        if (missingTestData > 0) {
            suggestions.add("⚠️ " + missingTestData + " test case(s) missing test data. " +
                "Consider adding test data for better test coverage.");
        }
        
        // Check for hardcoded values
        for (GeneratedTestCase testCase : testCases) {
            if (testCase.getTestData() != null) {
                Map<String, String> data = testCase.getTestData().getAllData();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    if (entry.getValue().equals("test") || entry.getValue().equals("test123")) {
                        suggestions.add("💡 Consider using more realistic test data instead of " +
                            "hardcoded values like '" + entry.getValue() + "' for " + entry.getKey());
                    }
                }
            }
        }
    }
    
    /**
     * Analyze test steps
     */
    private void analyzeTestSteps(List<GeneratedTestCase> testCases, List<String> suggestions) {
        // Check for test cases with too few steps
        long tooFewSteps = testCases.stream()
            .filter(tc -> tc.getSteps() == null || tc.getSteps().size() < 3)
            .count();
        
        if (tooFewSteps > 0) {
            suggestions.add("⚠️ " + tooFewSteps + " test case(s) have fewer than 3 steps. " +
                "Consider adding more detailed test steps.");
        }
        
        // Check for test cases with too many steps
        long tooManySteps = testCases.stream()
            .filter(tc -> tc.getSteps() != null && tc.getSteps().size() > 15)
            .count();
        
        if (tooManySteps > 0) {
            suggestions.add("💡 " + tooManySteps + " test case(s) have more than 15 steps. " +
                "Consider breaking them into smaller, focused test cases.");
        }
        
        // Check for missing expected results in steps
        for (GeneratedTestCase testCase : testCases) {
            if (testCase.getSteps() != null) {
                long stepsWithoutResults = testCase.getSteps().stream()
                    .filter(step -> step.getExpectedResult() == null || 
                        step.getExpectedResult().trim().isEmpty())
                    .count();
                
                if (stepsWithoutResults > 0) {
                    suggestions.add("⚠️ Test case '" + testCase.getTestName() + 
                        "' has " + stepsWithoutResults + " step(s) without expected results.");
                }
            }
        }
    }
    
    /**
     * Analyze test coverage
     */
    private void analyzeCoverage(List<GeneratedTestCase> testCases, List<String> suggestions) {
        // Count test types
        Map<String, Long> typeCount = new HashMap<>();
        for (GeneratedTestCase testCase : testCases) {
            String category = testCase.getCategory() != null ? testCase.getCategory() : "Unknown";
            typeCount.put(category, typeCount.getOrDefault(category, 0L) + 1);
        }
        
        // Check for coverage balance
        long positiveTests = testCases.stream()
            .filter(tc -> tc.getTags() != null && tc.getTags().contains("positive"))
            .count();
        
        long negativeTests = testCases.stream()
            .filter(tc -> tc.getTags() != null && tc.getTags().contains("negative"))
            .count();
        
        if (positiveTests > 0 && negativeTests == 0) {
            suggestions.add("💡 Consider adding negative test cases for better coverage. " +
                "Currently only positive test cases are present.");
        }
        
        if (negativeTests > positiveTests * 2) {
            suggestions.add("💡 Consider adding more positive test cases. " +
                "Negative tests outnumber positive tests significantly.");
        }
    }
    
    /**
     * Analyze maintainability
     */
    private void analyzeMaintainability(List<GeneratedTestCase> testCases, List<String> suggestions) {
        // Check for proper naming conventions
        long poorlyNamed = testCases.stream()
            .filter(tc -> tc.getTestName() == null || 
                !tc.getTestName().matches("^[a-z][a-zA-Z0-9]*$"))
            .count();
        
        if (poorlyNamed > 0) {
            suggestions.add("💡 " + poorlyNamed + " test case(s) don't follow Java naming conventions. " +
                "Use camelCase starting with lowercase letter.");
        }
        
        // Check for missing priorities
        long missingPriority = testCases.stream()
            .filter(tc -> tc.getPriority() == null || tc.getPriority().trim().isEmpty())
            .count();
        
        if (missingPriority > 0) {
            suggestions.add("💡 " + missingPriority + " test case(s) missing priority. " +
                "Set priorities (High/Medium/Low) for better test execution planning.");
        }
        
        // Check for missing categories
        long missingCategory = testCases.stream()
            .filter(tc -> tc.getCategory() == null || tc.getCategory().trim().isEmpty())
            .count();
        
        if (missingCategory > 0) {
            suggestions.add("💡 " + missingCategory + " test case(s) missing category. " +
                "Categorize tests for better organization.");
        }
    }
}


