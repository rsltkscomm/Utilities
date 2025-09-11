package advanced;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered test generation utilities for automated test case creation.
 */
public class AITestGenerator {
    
    private final ObjectMapper objectMapper;
    private final String outputDirectory;
    
    public AITestGenerator() {
        this.objectMapper = new ObjectMapper();
        this.outputDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("generated_tests").toString();
        createOutputDirectory();
    }
    
    /**
     * Generates test cases from user stories using AI-like pattern recognition.
     * @param userStory The user story text
     * @return List of generated test cases
     */
    public List<TestCase> generateTestsFromUserStories(String userStory) {
        TestLogManager.info("Generating tests from user story: " + userStory);
        
        List<TestCase> testCases = new ArrayList<>();
        
        // Extract user story components
        UserStoryComponents components = parseUserStory(userStory);
        
        // Generate positive test cases
        testCases.addAll(generatePositiveTestCases(components));
        
        // Generate negative test cases
        testCases.addAll(generateNegativeTestCases(components));
        
        // Generate edge case test cases
        testCases.addAll(generateEdgeCaseTestCases(components));
        
        // Save generated tests
        saveGeneratedTests(testCases, "user_story_tests");
        
        TestLogManager.success("Generated " + testCases.size() + " test cases from user story");
        return testCases;
    }
    
    /**
     * Optimizes an existing test case using AI-like analysis.
     * @param testCase The test case to optimize
     * @return Optimized test case
     */
    public TestCase optimizeTestCase(TestCase testCase) {
        TestLogManager.info("Optimizing test case: " + testCase.getTestName());
        
        TestCase optimized = new TestCase();
        optimized.setTestName(testCase.getTestName() + "_optimized");
        optimized.setDescription("Optimized version of: " + testCase.getDescription());
        
        // Optimize test steps
        List<TestStep> optimizedSteps = optimizeTestSteps(testCase.getSteps());
        optimized.setSteps(optimizedSteps);
        
        // Optimize test data
        Map<String, String> optimizedData = optimizeTestData(testCase.getTestData());
        optimized.setTestData(optimizedData);
        
        // Add performance optimizations
        optimized.setExpectedResult(testCase.getExpectedResult() + " (Performance optimized)");
        
        TestLogManager.success("Test case optimized: " + optimized.getTestName());
        return optimized;
    }
    
    /**
     * Generates test data for a given test scenario.
     * @param testScenario The test scenario description
     * @return Generated test data
     */
    public String generateTestData(String testScenario) {
        TestLogManager.info("Generating test data for scenario: " + testScenario);
        
        Map<String, String> testData = new HashMap<>();
        
        // Analyze scenario to determine data types needed
        if (testScenario.toLowerCase().contains("login") || testScenario.toLowerCase().contains("authentication")) {
            testData.put("username", generateUsername());
            testData.put("password", generatePassword());
            testData.put("email", generateEmail());
        }
        
        if (testScenario.toLowerCase().contains("registration") || testScenario.toLowerCase().contains("signup")) {
            testData.put("firstName", generateFirstName());
            testData.put("lastName", generateLastName());
            testData.put("email", generateEmail());
            testData.put("phone", generatePhoneNumber());
            testData.put("address", generateAddress());
        }
        
        if (testScenario.toLowerCase().contains("payment") || testScenario.toLowerCase().contains("billing")) {
            testData.put("cardNumber", generateCreditCardNumber());
            testData.put("cvv", generateCVV());
            testData.put("expiryDate", generateExpiryDate());
            testData.put("billingAddress", generateAddress());
        }
        
        if (testScenario.toLowerCase().contains("search") || testScenario.toLowerCase().contains("filter")) {
            testData.put("searchTerm", generateSearchTerm());
            testData.put("category", generateCategory());
            testData.put("priceRange", generatePriceRange());
        }
        
        // Convert to JSON string
        try {
            String jsonData = objectMapper.writeValueAsString(testData);
            TestLogManager.success("Generated test data: " + jsonData);
            return jsonData;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate test data", e);
            return "{}";
        }
    }
    
    /**
     * Generates test cases from API specifications.
     * @param apiSpecPath Path to API specification file (OpenAPI/Swagger)
     * @return List of generated API test cases
     */
    public List<TestCase> generateAPITestsFromSpec(String apiSpecPath) {
        TestLogManager.info("Generating API tests from specification: " + apiSpecPath);
        
        List<TestCase> testCases = new ArrayList<>();
        
        try {
            JsonNode apiSpec = objectMapper.readTree(new File(apiSpecPath));
            JsonNode paths = apiSpec.get("paths");
            
            if (paths != null) {
                paths.fieldNames().forEachRemaining(path -> {
                    JsonNode pathNode = paths.get(path);
                    pathNode.fieldNames().forEachRemaining(method -> {
                        TestCase testCase = generateAPITestCase(path, method.toUpperCase(), pathNode.get(method));
                        if (testCase != null) {
                            testCases.add(testCase);
                        }
                    });
                });
            }
            
            saveGeneratedTests(testCases, "api_tests");
            TestLogManager.success("Generated " + testCases.size() + " API test cases");
            
        } catch (IOException e) {
            TestLogManager.error("Failed to read API specification", e);
        }
        
        return testCases;
    }
    
    /**
     * Generates test cases from database schema.
     * @param schemaPath Path to database schema file
     * @return List of generated database test cases
     */
    public List<TestCase> generateDatabaseTestsFromSchema(String schemaPath) {
        TestLogManager.info("Generating database tests from schema: " + schemaPath);
        
        List<TestCase> testCases = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(schemaPath));
            
            for (String line : lines) {
                if (line.trim().toUpperCase().startsWith("CREATE TABLE")) {
                    TestCase testCase = generateDatabaseTestCase(line);
                    if (testCase != null) {
                        testCases.add(testCase);
                    }
                }
            }
            
            saveGeneratedTests(testCases, "database_tests");
            TestLogManager.success("Generated " + testCases.size() + " database test cases");
            
        } catch (IOException e) {
            TestLogManager.error("Failed to read database schema", e);
        }
        
        return testCases;
    }
    
    private UserStoryComponents parseUserStory(String userStory) {
        UserStoryComponents components = new UserStoryComponents();
        
        // Extract user role
        Pattern userPattern = Pattern.compile("As a (\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher userMatcher = userPattern.matcher(userStory);
        if (userMatcher.find()) {
            components.setUserRole(userMatcher.group(1));
        }
        
        // Extract action
        Pattern actionPattern = Pattern.compile("I want to (.*?)(?:so that|in order to)", Pattern.CASE_INSENSITIVE);
        Matcher actionMatcher = actionPattern.matcher(userStory);
        if (actionMatcher.find()) {
            components.setAction(actionMatcher.group(1).trim());
        }
        
        // Extract benefit
        Pattern benefitPattern = Pattern.compile("(?:so that|in order to) (.*)", Pattern.CASE_INSENSITIVE);
        Matcher benefitMatcher = benefitPattern.matcher(userStory);
        if (benefitMatcher.find()) {
            components.setBenefit(benefitMatcher.group(1).trim());
        }
        
        return components;
    }
    
    private List<TestCase> generatePositiveTestCases(UserStoryComponents components) {
        List<TestCase> testCases = new ArrayList<>();
        
        TestCase positiveTest = new TestCase();
        positiveTest.setTestName("test_" + components.getAction().replaceAll("\\s+", "_").toLowerCase() + "_positive");
        positiveTest.setDescription("Positive test case for: " + components.getAction());
        positiveTest.setExpectedResult("User should be able to " + components.getAction() + " successfully");
        
        List<TestStep> steps = new ArrayList<>();
        steps.add(new TestStep("Navigate to the application", "GET", "/"));
        steps.add(new TestStep("Perform action: " + components.getAction(), "ACTION", components.getAction()));
        steps.add(new TestStep("Verify successful completion", "VERIFY", "success"));
        
        positiveTest.setSteps(steps);
        testCases.add(positiveTest);
        
        return testCases;
    }
    
    private List<TestCase> generateNegativeTestCases(UserStoryComponents components) {
        List<TestCase> testCases = new ArrayList<>();
        
        TestCase negativeTest = new TestCase();
        negativeTest.setTestName("test_" + components.getAction().replaceAll("\\s+", "_").toLowerCase() + "_negative");
        negativeTest.setDescription("Negative test case for: " + components.getAction());
        negativeTest.setExpectedResult("User should receive appropriate error message");
        
        List<TestStep> steps = new ArrayList<>();
        steps.add(new TestStep("Navigate to the application", "GET", "/"));
        steps.add(new TestStep("Attempt action with invalid data", "ACTION", components.getAction() + " with invalid data"));
        steps.add(new TestStep("Verify error handling", "VERIFY", "error message displayed"));
        
        negativeTest.setSteps(steps);
        testCases.add(negativeTest);
        
        return testCases;
    }
    
    private List<TestCase> generateEdgeCaseTestCases(UserStoryComponents components) {
        List<TestCase> testCases = new ArrayList<>();
        
        TestCase edgeTest = new TestCase();
        edgeTest.setTestName("test_" + components.getAction().replaceAll("\\s+", "_").toLowerCase() + "_edge_case");
        edgeTest.setDescription("Edge case test for: " + components.getAction());
        edgeTest.setExpectedResult("Application should handle edge cases gracefully");
        
        List<TestStep> steps = new ArrayList<>();
        steps.add(new TestStep("Navigate to the application", "GET", "/"));
        steps.add(new TestStep("Perform action with boundary values", "ACTION", components.getAction() + " with boundary values"));
        steps.add(new TestStep("Verify edge case handling", "VERIFY", "graceful handling"));
        
        edgeTest.setSteps(steps);
        testCases.add(edgeTest);
        
        return testCases;
    }
    
    private List<TestStep> optimizeTestSteps(List<TestStep> originalSteps) {
        List<TestStep> optimizedSteps = new ArrayList<>();
        
        for (TestStep step : originalSteps) {
            TestStep optimizedStep = new TestStep();
            optimizedStep.setDescription(step.getDescription());
            optimizedStep.setAction(step.getAction());
            optimizedStep.setValue(step.getValue());
            
            // Add optimization hints
            if (step.getAction().equals("CLICK")) {
                optimizedStep.setOptimizationHint("Use explicit wait before clicking");
            } else if (step.getAction().equals("INPUT")) {
                optimizedStep.setOptimizationHint("Clear field before input");
            }
            
            optimizedSteps.add(optimizedStep);
        }
        
        return optimizedSteps;
    }
    
    private Map<String, String> optimizeTestData(Map<String, String> originalData) {
        Map<String, String> optimizedData = new HashMap<>(originalData);
        
        // Add data validation
        optimizedData.put("_validation", "true");
        optimizedData.put("_dataType", "optimized");
        
        return optimizedData;
    }
    
    private TestCase generateAPITestCase(String path, String method, JsonNode operation) {
        TestCase testCase = new TestCase();
        testCase.setTestName("test_api_" + method.toLowerCase() + "_" + path.replaceAll("[^a-zA-Z0-9]", "_"));
        testCase.setDescription("API test for " + method + " " + path);
        
        List<TestStep> steps = new ArrayList<>();
        steps.add(new TestStep("Send " + method + " request", method, path));
        
        if (operation.has("responses")) {
            JsonNode responses = operation.get("responses");
            if (responses.has("200")) {
                steps.add(new TestStep("Verify 200 response", "VERIFY", "status_code_200"));
            }
        }
        
        testCase.setSteps(steps);
        testCase.setExpectedResult("API should respond correctly");
        
        return testCase;
    }
    
    private TestCase generateDatabaseTestCase(String createTableStatement) {
        TestCase testCase = new TestCase();
        
        // Extract table name
        Pattern tablePattern = Pattern.compile("CREATE TABLE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(createTableStatement);
        
        if (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            testCase.setTestName("test_database_" + tableName.toLowerCase());
            testCase.setDescription("Database test for table: " + tableName);
            
            List<TestStep> steps = new ArrayList<>();
            steps.add(new TestStep("Insert test data", "INSERT", tableName));
            steps.add(new TestStep("Verify data integrity", "SELECT", tableName));
            steps.add(new TestStep("Clean up test data", "DELETE", tableName));
            
            testCase.setSteps(steps);
            testCase.setExpectedResult("Database operations should complete successfully");
        }
        
        return testCase;
    }
    
    private void saveGeneratedTests(List<TestCase> testCases, String category) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = category + "_" + timestamp + ".json";
            Path filePath = Paths.get(outputDirectory, fileName);
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), testCases);
            TestLogManager.info("Generated tests saved to: " + filePath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to save generated tests", e);
        }
    }
    
    private void createOutputDirectory() {
        try {
            Path dir = Paths.get(outputDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created output directory: " + outputDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create output directory", e);
        }
    }
    
    // Data generation methods
    private String generateUsername() {
        return "testuser_" + System.currentTimeMillis();
    }
    
    private String generatePassword() {
        return "TestPass123!";
    }
    
    private String generateEmail() {
        return "test_" + System.currentTimeMillis() + "@example.com";
    }
    
    private String generateFirstName() {
        String[] names = {"John", "Jane", "Mike", "Sarah", "David", "Lisa"};
        return names[new Random().nextInt(names.length)];
    }
    
    private String generateLastName() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"};
        return names[new Random().nextInt(names.length)];
    }
    
    private String generatePhoneNumber() {
        return "555-" + String.format("%03d", new Random().nextInt(1000)) + "-" + String.format("%04d", new Random().nextInt(10000));
    }
    
    private String generateAddress() {
        return "123 Test Street, Test City, TC 12345";
    }
    
    private String generateCreditCardNumber() {
        return "4111-1111-1111-1111"; // Test Visa number
    }
    
    private String generateCVV() {
        return String.format("%03d", new Random().nextInt(1000));
    }
    
    private String generateExpiryDate() {
        int year = LocalDateTime.now().getYear() + 2;
        int month = new Random().nextInt(12) + 1;
        return String.format("%02d/%d", month, year);
    }
    
    private String generateSearchTerm() {
        String[] terms = {"laptop", "phone", "book", "shirt", "shoes", "watch"};
        return terms[new Random().nextInt(terms.length)];
    }
    
    private String generateCategory() {
        String[] categories = {"Electronics", "Clothing", "Books", "Home", "Sports"};
        return categories[new Random().nextInt(categories.length)];
    }
    
    private String generatePriceRange() {
        return "$10 - $100";
    }
    
    /**
     * Test case data model.
     */
    public static class TestCase {
        private String testName;
        private String description;
        private List<TestStep> steps;
        private Map<String, String> testData;
        private String expectedResult;
        
        // Getters and setters
        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<TestStep> getSteps() { return steps; }
        public void setSteps(List<TestStep> steps) { this.steps = steps; }
        
        public Map<String, String> getTestData() { return testData; }
        public void setTestData(Map<String, String> testData) { this.testData = testData; }
        
        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    }
    
    /**
     * Test step data model.
     */
    public static class TestStep {
        private String description;
        private String action;
        private String value;
        private String optimizationHint;
        
        public TestStep() {}
        
        public TestStep(String description, String action, String value) {
            this.description = description;
            this.action = action;
            this.value = value;
        }
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getOptimizationHint() { return optimizationHint; }
        public void setOptimizationHint(String optimizationHint) { this.optimizationHint = optimizationHint; }
    }
    
    /**
     * User story components data model.
     */
    private static class UserStoryComponents {
        private String userRole;
        private String action;
        private String benefit;
        
        // Getters and setters
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getBenefit() { return benefit; }
        public void setBenefit(String benefit) { this.benefit = benefit; }
    }
}
