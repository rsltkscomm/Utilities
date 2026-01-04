package ai;

import ai.openai.OpenAIIntegration;
import config.ConfigurationManager;
import patterns.repository.TestData;
import reporting.TestLogManager;

import java.util.*;
import java.util.regex.Pattern;

/**
 * AI Test Case Generator
 * 
 * Automatically generates test cases from user stories, requirements, or application behavior.
 * 
 * Features:
 * - Natural language to test case conversion
 * - Test scenario generation from user stories
 * - Test data generation
 * - Test case optimization suggestions
 * 
 * Usage:
 *   AITestCaseGenerator generator = new AITestCaseGenerator();
 *   List<GeneratedTestCase> testCases = generator.generateFromUserStory(userStory);
 *   String testCode = generator.generateTestNGCode(testCases.get(0));
 */
public class AITestCaseGenerator {
    
    private final TestDataGenerator testDataGenerator;
    private final TestCaseOptimizer optimizer;
    private final Map<String, List<GeneratedTestCase>> generatedTestCases;
    private final OpenAIIntegration openAIIntegration;
    private final ConfigurationManager config;
    private final boolean useOpenAI;
    
    public AITestCaseGenerator() {
        this.testDataGenerator = new TestDataGenerator();
        this.optimizer = new TestCaseOptimizer();
        this.generatedTestCases = new HashMap<>();
        this.config = ConfigurationManager.getInstance();
        this.openAIIntegration = new OpenAIIntegration();
        this.useOpenAI = config.getBoolean("ai.test.generation.enabled", false) && 
                        config.getBoolean("ai.enabled", false) &&
                        openAIIntegration.isEnabled();
    }
    
    /**
     * Generate test cases from a user story
     * 
     * @param userStory The user story in natural language (e.g., "As a user, I want to login so that I can access my account")
     * @return List of generated test cases
     */
    public List<GeneratedTestCase> generateFromUserStory(String userStory) {
        TestLogManager.info("Generating test cases from user story: " + userStory);
        
        List<GeneratedTestCase> testCases = new ArrayList<>();
        
        try {
            // Try OpenAI if enabled
            if (useOpenAI) {
                try {
                    List<String> openAITestCases = openAIIntegration.generateTestCasesWithOpenAI(userStory);
                    TestLogManager.info("Generated " + openAITestCases.size() + " test cases using OpenAI");
                    // Convert OpenAI responses to GeneratedTestCase objects
                    for (String testCaseText : openAITestCases) {
                        GeneratedTestCase testCase = parseOpenAITestCase(testCaseText, userStory);
                        if (testCase != null) {
                            testCases.add(testCase);
                        }
                    }
                    if (!testCases.isEmpty()) {
                        return testCases;
                    }
                } catch (Exception e) {
                    TestLogManager.warning("OpenAI test generation failed, falling back to rule-based: " + e.getMessage());
                }
            }
            
            // Fall back to rule-based generation
            // Parse user story to extract components
            UserStoryComponents components = parseUserStory(userStory);
            
            // Generate test scenarios
            List<TestScenario> scenarios = generateTestScenarios(components);
            
            // Generate test cases for each scenario
            for (TestScenario scenario : scenarios) {
                GeneratedTestCase testCase = createTestCaseFromScenario(scenario, components);
                testCases.add(testCase);
            }
            
            // Store generated test cases
            String storyKey = generateStoryKey(userStory);
            generatedTestCases.put(storyKey, testCases);
            
            TestLogManager.success("Generated " + testCases.size() + " test cases from user story");
            
        } catch (Exception e) {
            TestLogManager.error("Error generating test cases from user story", e);
        }
        
        return testCases;
    }
    
    /**
     * Generate test cases from natural language requirements
     * 
     * @param requirements Natural language requirements
     * @return List of generated test cases
     */
    public List<GeneratedTestCase> generateFromRequirements(String requirements) {
        TestLogManager.info("Generating test cases from requirements");
        
        List<GeneratedTestCase> testCases = new ArrayList<>();
        
        try {
            // Split requirements into individual requirements
            String[] requirementLines = requirements.split("\n");
            
            for (String requirement : requirementLines) {
                if (requirement.trim().isEmpty()) continue;
                
                // Convert requirement to user story format if needed
                String userStory = convertRequirementToUserStory(requirement);
                List<GeneratedTestCase> cases = generateFromUserStory(userStory);
                testCases.addAll(cases);
            }
            
        } catch (Exception e) {
            TestLogManager.error("Error generating test cases from requirements", e);
        }
        
        return testCases;
    }
    
    /**
     * Generate test scenarios from user story components
     */
    private List<TestScenario> generateTestScenarios(UserStoryComponents components) {
        List<TestScenario> scenarios = new ArrayList<>();
        
        String action = components.getAction().toLowerCase();
        
        // Generate positive scenarios
        scenarios.add(createScenario("Happy Path", "Verify successful " + components.getAction(), 
            "positive", components));
        
        // Generate negative scenarios based on action type
        if (action.contains("login") || action.contains("authenticate")) {
            scenarios.add(createScenario("Invalid Credentials", 
                "Verify error handling for invalid credentials", "negative", components));
            scenarios.add(createScenario("Empty Fields", 
                "Verify validation for empty username/password", "negative", components));
        }
        
        if (action.contains("create") || action.contains("add") || action.contains("register")) {
            scenarios.add(createScenario("Duplicate Entry", 
                "Verify error handling for duplicate entries", "negative", components));
            scenarios.add(createScenario("Invalid Data", 
                "Verify validation for invalid input data", "negative", components));
        }
        
        if (action.contains("delete") || action.contains("remove")) {
            scenarios.add(createScenario("Non-existent Item", 
                "Verify error handling when deleting non-existent item", "negative", components));
        }
        
        if (action.contains("search") || action.contains("find") || action.contains("filter")) {
            scenarios.add(createScenario("No Results", 
                "Verify behavior when no results found", "negative", components));
            scenarios.add(createScenario("Special Characters", 
                "Verify handling of special characters in search", "edge", components));
        }
        
        // Generate edge case scenarios
        scenarios.add(createScenario("Boundary Values", 
            "Verify handling of boundary values", "edge", components));
        scenarios.add(createScenario("Concurrent Access", 
            "Verify behavior under concurrent access", "edge", components));
        
        return scenarios;
    }
    
    /**
     * Create a test scenario
     */
    private TestScenario createScenario(String name, String description, 
                                       String type, UserStoryComponents components) {
        TestScenario scenario = new TestScenario();
        scenario.setName(name);
        scenario.setDescription(description);
        scenario.setType(type);
        scenario.setUserStory(components.getFullStory());
        scenario.setActor(components.getActor());
        scenario.setAction(components.getAction());
        scenario.setGoal(components.getGoal());
        return scenario;
    }
    
    /**
     * Create a test case from a scenario
     */
    private GeneratedTestCase createTestCaseFromScenario(TestScenario scenario, 
                                                         UserStoryComponents components) {
        GeneratedTestCase testCase = new GeneratedTestCase();
        
        // Generate test case name
        String testName = generateTestName(scenario);
        testCase.setTestName(testName);
        
        // Generate test description
        testCase.setDescription(scenario.getDescription());
        
        // Generate test steps
        List<TestStep> steps = generateTestSteps(scenario, components);
        testCase.setSteps(steps);
        
        // Generate test data
        TestData testData = testDataGenerator.generateTestData(scenario, components);
        testCase.setTestData(testData);
        
        // Generate expected results
        List<String> expectedResults = generateExpectedResults(scenario, components);
        testCase.setExpectedResults(expectedResults);
        
        // Set metadata
        testCase.setPriority(determinePriority(scenario.getType()));
        testCase.setCategory(extractCategory(components.getAction()));
        testCase.setTags(generateTags(scenario));
        
        return testCase;
    }
    
    /**
     * Generate test steps from scenario
     */
    private List<TestStep> generateTestSteps(TestScenario scenario, 
                                            UserStoryComponents components) {
        List<TestStep> steps = new ArrayList<>();
        String action = components.getAction().toLowerCase();
        String scenarioType = scenario.getType().toLowerCase();
        
        // Common setup steps
        steps.add(new TestStep(1, "Navigate to application", 
            "driver.navigate(\"https://example.com\");", "Application loads successfully"));
        
        // Action-specific steps
        if (action.contains("login") || action.contains("authenticate")) {
            if (scenarioType.equals("positive")) {
                steps.add(new TestStep(2, "Enter valid username", 
                    "elementUtil.sendValue(\"Username Field,id,username\", testData.getData(\"username\"));", 
                    "Username entered"));
                steps.add(new TestStep(3, "Enter valid password", 
                    "elementUtil.sendValue(\"Password Field,id,password\", testData.getData(\"password\"));", 
                    "Password entered"));
                steps.add(new TestStep(4, "Click login button", 
                    "clickUtil.click(\"Login Button,xpath,//button[@type='submit']\");", 
                    "Login button clicked"));
                steps.add(new TestStep(5, "Verify successful login", 
                    "Assert.assertTrue(elementUtil.isElementPresent(\"Dashboard,xpath,//div[@class='dashboard']\"));", 
                    "User logged in successfully"));
            } else if (scenarioType.equals("negative") && scenario.getName().contains("Invalid")) {
                steps.add(new TestStep(2, "Enter invalid username", 
                    "elementUtil.sendValue(\"Username Field,id,username\", \"invalid_user\");", 
                    "Invalid username entered"));
                steps.add(new TestStep(3, "Enter invalid password", 
                    "elementUtil.sendValue(\"Password Field,id,password\", \"invalid_pass\");", 
                    "Invalid password entered"));
                steps.add(new TestStep(4, "Click login button", 
                    "clickUtil.click(\"Login Button,xpath,//button[@type='submit']\");", 
                    "Login button clicked"));
                steps.add(new TestStep(5, "Verify error message", 
                    "Assert.assertTrue(elementUtil.getText(\"Error Message,xpath,//div[@class='error']\").contains(\"Invalid\"));", 
                    "Error message displayed"));
            }
        } else if (action.contains("create") || action.contains("add")) {
            steps.add(new TestStep(2, "Click create/add button", 
                "clickUtil.click(\"Create Button,xpath,//button[contains(text(),'Create')]\");", 
                "Create form opened"));
            steps.add(new TestStep(3, "Fill required fields", 
                "elementUtil.sendValue(\"Name Field,id,name\", testData.getData(\"name\"));", 
                "Fields filled"));
            steps.add(new TestStep(4, "Submit form", 
                "clickUtil.click(\"Submit Button,xpath,//button[@type='submit']\");", 
                "Form submitted"));
            steps.add(new TestStep(5, "Verify creation success", 
                "Assert.assertTrue(elementUtil.isElementPresent(\"Success Message,xpath,//div[@class='success']\"));", 
                "Item created successfully"));
        } else if (action.contains("search") || action.contains("find")) {
            steps.add(new TestStep(2, "Enter search term", 
                "elementUtil.sendValue(\"Search Field,id,search\", testData.getData(\"searchTerm\"));", 
                "Search term entered"));
            steps.add(new TestStep(3, "Click search button", 
                "clickUtil.click(\"Search Button,xpath,//button[@type='submit']\");", 
                "Search executed"));
            steps.add(new TestStep(4, "Verify search results", 
                "Assert.assertTrue(elementUtil.isElementPresent(\"Results,xpath,//div[@class='results']\"));", 
                "Search results displayed"));
        } else {
            // Generic steps
            steps.add(new TestStep(2, "Perform action: " + components.getAction(), 
                "// Action: " + components.getAction(), 
                "Action completed"));
            steps.add(new TestStep(3, "Verify result", 
                "Assert.assertTrue(/* Verify " + components.getGoal() + " */);", 
                "Result verified"));
        }
        
        return steps;
    }
    
    /**
     * Generate expected results
     */
    private List<String> generateExpectedResults(TestScenario scenario, 
                                                UserStoryComponents components) {
        List<String> results = new ArrayList<>();
        String scenarioType = scenario.getType().toLowerCase();
        
        if (scenarioType.equals("positive")) {
            results.add("Action completed successfully");
            results.add("User can " + components.getGoal());
            results.add("No errors displayed");
        } else if (scenarioType.equals("negative")) {
            results.add("Appropriate error message displayed");
            results.add("User remains on current page");
            results.add("Data is not saved/modified");
        } else {
            results.add("System handles edge case appropriately");
            results.add("No system crash or unexpected behavior");
        }
        
        return results;
    }
    
    /**
     * Parse user story into components
     */
    private UserStoryComponents parseUserStory(String userStory) {
        UserStoryComponents components = new UserStoryComponents();
        components.setFullStory(userStory);
        
        // Pattern: "As a [actor], I want to [action] so that [goal]"
        Pattern pattern = Pattern.compile(
            "(?i)as\\s+a\\s+(.+?),\\s*i\\s+want\\s+to\\s+(.+?)\\s+so\\s+that\\s+(.+)",
            Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = pattern.matcher(userStory);
        if (matcher.find()) {
            components.setActor(matcher.group(1).trim());
            components.setAction(matcher.group(2).trim());
            components.setGoal(matcher.group(3).trim());
        } else {
            // Try alternative patterns
            if (userStory.toLowerCase().contains("user") || userStory.toLowerCase().contains("admin")) {
                components.setActor(extractActor(userStory));
            } else {
                components.setActor("user");
            }
            components.setAction(extractAction(userStory));
            components.setGoal(extractGoal(userStory));
        }
        
        return components;
    }
    
    /**
     * Extract actor from text
     */
    private String extractActor(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("admin")) return "administrator";
        if (lower.contains("customer")) return "customer";
        if (lower.contains("manager")) return "manager";
        return "user";
    }
    
    /**
     * Extract action from text
     */
    private String extractAction(String text) {
        String[] actionKeywords = {"login", "logout", "create", "add", "delete", "remove", 
                                   "update", "edit", "search", "find", "filter", "view", 
                                   "submit", "register", "reset", "verify"};
        
        String lower = text.toLowerCase();
        for (String keyword : actionKeywords) {
            if (lower.contains(keyword)) {
                return keyword;
            }
        }
        
        // Extract verb from sentence
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String word = words[i].toLowerCase();
            if (word.matches("(want|need|should|must|can|will).*")) {
                return words[i + 1].replaceAll("[^a-zA-Z]", "");
            }
        }
        
        return "perform action";
    }
    
    /**
     * Extract goal from text
     */
    private String extractGoal(String text) {
        if (text.toLowerCase().contains("so that")) {
            String[] parts = text.split("(?i)so that");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        return "achieve the desired outcome";
    }
    
    /**
     * Convert requirement to user story format
     */
    private String convertRequirementToUserStory(String requirement) {
        requirement = requirement.trim();
        
        // If already in user story format, return as is
        if (requirement.toLowerCase().startsWith("as a")) {
            return requirement;
        }
        
        // Convert requirement to user story
        return "As a user, I want to " + requirement.toLowerCase() + 
               " so that I can complete my task";
    }
    
    /**
     * Generate test name from scenario
     */
    private String generateTestName(TestScenario scenario) {
        String name = scenario.getName()
            .replaceAll("[^a-zA-Z0-9]", "")
            .replaceAll("\\s+", "");
        
        String action = scenario.getAction()
            .replaceAll("[^a-zA-Z0-9]", "")
            .replaceAll("\\s+", "");
        
        return "test" + capitalize(name) + capitalize(action);
    }
    
    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * Determine priority based on scenario type
     */
    private String determinePriority(String scenarioType) {
        if (scenarioType.equalsIgnoreCase("positive")) return "High";
        if (scenarioType.equalsIgnoreCase("negative")) return "Medium";
        return "Low";
    }
    
    /**
     * Extract category from action
     */
    private String extractCategory(String action) {
        String lower = action.toLowerCase();
        if (lower.contains("login") || lower.contains("auth")) return "Authentication";
        if (lower.contains("create") || lower.contains("add")) return "CRUD";
        if (lower.contains("delete") || lower.contains("remove")) return "CRUD";
        if (lower.contains("update") || lower.contains("edit")) return "CRUD";
        if (lower.contains("search") || lower.contains("find")) return "Search";
        if (lower.contains("view") || lower.contains("display")) return "View";
        return "Functional";
    }
    
    /**
     * Generate tags for test case
     */
    private List<String> generateTags(TestScenario scenario) {
        List<String> tags = new ArrayList<>();
        tags.add(scenario.getType().toLowerCase());
        tags.add(scenario.getAction().toLowerCase().replaceAll("\\s+", "-"));
        return tags;
    }
    
    /**
     * Generate story key for storage
     */
    private String generateStoryKey(String userStory) {
        return "story_" + Math.abs(userStory.hashCode());
    }
    
    /**
     * Generate TestNG test code from generated test case
     */
    public String generateTestNGCode(GeneratedTestCase testCase) {
        StringBuilder code = new StringBuilder();
        
        code.append("package examples;\n\n");
        code.append("import base.BaseTest;\n");
        code.append("import base.DriverManager;\n");
        code.append("import com.microsoft.playwright.Page;\n");
        code.append("import org.testng.annotations.Test;\n");
        code.append("import org.testng.Assert;\n");
        code.append("import seleniumUtils.*;\n");
        code.append("import patterns.repository.TestData;\n");
        code.append("import reporting.ExtentManager;\n\n");
        
        code.append("/**\n");
        code.append(" * Generated Test Case: ").append(testCase.getTestName()).append("\n");
        code.append(" * ").append(testCase.getDescription()).append("\n");
        code.append(" */\n");
        code.append("public class ").append(testCase.getTestName()).append(" extends BaseTest {\n\n");
        
        code.append("    @Test(description = \"").append(testCase.getDescription()).append("\")\n");
        code.append("    public void ").append(testCase.getTestName().toLowerCase()).append("() {\n");
        code.append("        try {\n");
        code.append("            Page page = DriverManager.getDriver();\n");
        code.append("            BrowserUtil browserUtil = new BrowserUtil(page, getPageFactory());\n");
        code.append("            ElementUtil elementUtil = new ElementUtil(page, getPageFactory());\n");
        code.append("            ClickUtil clickUtil = new ClickUtil(page, getPageFactory());\n\n");
        
        // Add test data initialization
        if (testCase.getTestData() != null) {
            code.append("            // Test Data\n");
            code.append("            TestData testData = new TestData(\"").append(testCase.getTestName()).append("\");\n");
            for (Map.Entry<String, String> entry : testCase.getTestData().getAllData().entrySet()) {
                code.append("            testData.setData(\"").append(entry.getKey())
                    .append("\", \"").append(entry.getValue()).append("\");\n");
            }
            code.append("\n");
        }
        
        // Add test steps
        code.append("            // Test Steps\n");
        for (TestStep step : testCase.getSteps()) {
            code.append("            // Step ").append(step.getStepNumber())
                .append(": ").append(step.getDescription()).append("\n");
            code.append("            ").append(step.getCode()).append("\n");
            code.append("            ExtentManager.infoTest(\"").append(step.getExpectedResult()).append("\");\n\n");
        }
        
        code.append("            ExtentManager.passTest(\"Test completed successfully\");\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            ExtentManager.failTest(\"Test failed: \" + e.getMessage());\n");
        code.append("            throw e;\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    /**
     * Get optimization suggestions for test cases
     */
    public List<String> getOptimizationSuggestions(List<GeneratedTestCase> testCases) {
        return optimizer.analyzeAndSuggest(testCases);
    }
    
    /**
     * Get all generated test cases for a user story
     */
    public List<GeneratedTestCase> getGeneratedTestCases(String userStory) {
        String storyKey = generateStoryKey(userStory);
        return generatedTestCases.getOrDefault(storyKey, new ArrayList<>());
    }
    
    /**
     * Parse OpenAI test case response into GeneratedTestCase
     */
    private GeneratedTestCase parseOpenAITestCase(String testCaseText, String userStory) {
        try {
            GeneratedTestCase testCase = new GeneratedTestCase();
            
            // Extract test name
            if (testCaseText.contains("Test Name:") || testCaseText.contains("Test:")) {
                String[] lines = testCaseText.split("\n");
                for (String line : lines) {
                    if (line.contains("Test Name:") || line.contains("Test:")) {
                        String name = line.split(":")[1].trim();
                        testCase.setTestName(name.replaceAll("[^a-zA-Z0-9]", ""));
                        break;
                    }
                }
            } else {
                testCase.setTestName("test" + Math.abs(testCaseText.hashCode()));
            }
            
            testCase.setDescription(testCaseText.substring(0, Math.min(200, testCaseText.length())));
            testCase.setCategory("AI Generated");
            testCase.setPriority("Medium");
            
            // Generate basic steps from text
            List<TestStep> steps = new ArrayList<>();
            String[] sentences = testCaseText.split("[.!?]");
            int stepNum = 1;
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() > 20 && stepNum <= 10) {
                    steps.add(new TestStep(stepNum++, sentence, 
                        "// " + sentence, "Step completed"));
                }
            }
            testCase.setSteps(steps);
            
            return testCase;
        } catch (Exception e) {
            TestLogManager.warning("Failed to parse OpenAI test case: " + e.getMessage());
            return null;
        }
    }
}

