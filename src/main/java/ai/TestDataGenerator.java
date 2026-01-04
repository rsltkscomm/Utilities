package ai;

import com.github.javafaker.Faker;
import patterns.repository.TestData;
import reporting.TestLogManager;

import java.util.Random;

/**
 * Generates test data for test cases
 */
class TestDataGenerator {
    
    private final Faker faker;
    private final Random random;
    
    public TestDataGenerator() {
        this.faker = new Faker();
        this.random = new Random();
    }
    
    /**
     * Generate test data based on scenario and user story components
     */
    public TestData generateTestData(TestScenario scenario, UserStoryComponents components) {
        TestData testData = new TestData(scenario.getName());
        
        String action = components.getAction().toLowerCase();
        String scenarioType = scenario.getType().toLowerCase();
        
        try {
            // Generate data based on action type
            if (action.contains("login") || action.contains("authenticate")) {
                generateLoginTestData(testData, scenarioType);
            } else if (action.contains("register") || action.contains("signup")) {
                generateRegistrationTestData(testData, scenarioType);
            } else if (action.contains("create") || action.contains("add")) {
                generateCreateTestData(testData, action, scenarioType);
            } else if (action.contains("search") || action.contains("find")) {
                generateSearchTestData(testData, scenarioType);
            } else if (action.contains("update") || action.contains("edit")) {
                generateUpdateTestData(testData, scenarioType);
            } else {
                generateGenericTestData(testData, action);
            }
            
            // Add metadata
            testData.setMetadata("scenarioType", scenarioType);
            testData.setMetadata("action", action);
            testData.setMetadata("generatedAt", java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            TestLogManager.error("Error generating test data", e);
        }
        
        return testData;
    }
    
    /**
     * Generate login test data
     */
    private void generateLoginTestData(TestData testData, String scenarioType) {
        if (scenarioType.equals("positive")) {
            testData.setData("username", "testuser" + random.nextInt(1000));
            testData.setData("password", "Test@123");
            testData.setData("expectedResult", "success");
        } else if (scenarioType.equals("negative")) {
            testData.setData("username", "invalid_user");
            testData.setData("password", "wrong_password");
            testData.setData("expectedResult", "error");
        } else {
            testData.setData("username", "");
            testData.setData("password", "");
            testData.setData("expectedResult", "validation_error");
        }
    }
    
    /**
     * Generate registration test data
     */
    private void generateRegistrationTestData(TestData testData, String scenarioType) {
        if (scenarioType.equals("positive")) {
            testData.setData("firstName", faker.name().firstName());
            testData.setData("lastName", faker.name().lastName());
            testData.setData("email", faker.internet().emailAddress());
            testData.setData("phone", faker.phoneNumber().phoneNumber());
            testData.setData("password", "Test@123");
            testData.setData("confirmPassword", "Test@123");
        } else if (scenarioType.equals("negative")) {
            testData.setData("email", "invalid-email");
            testData.setData("password", "weak");
            testData.setData("confirmPassword", "different");
        } else {
            testData.setData("email", faker.internet().emailAddress());
            testData.setData("password", generateBoundaryValue("password"));
        }
    }
    
    /**
     * Generate create/add test data
     */
    private void generateCreateTestData(TestData testData, String action, String scenarioType) {
        if (scenarioType.equals("positive")) {
            testData.setData("name", faker.name().fullName());
            testData.setData("description", faker.lorem().sentence());
            testData.setData("category", faker.commerce().department());
        } else if (scenarioType.equals("negative")) {
            testData.setData("name", ""); // Empty name
            testData.setData("description", generateLongString(1000)); // Too long
        } else {
            testData.setData("name", generateBoundaryValue("name"));
            testData.setData("description", generateBoundaryValue("description"));
        }
    }
    
    /**
     * Generate search test data
     */
    private void generateSearchTestData(TestData testData, String scenarioType) {
        if (scenarioType.equals("positive")) {
            testData.setData("searchTerm", faker.lorem().word());
            testData.setData("expectedResults", ">0");
        } else if (scenarioType.equals("negative")) {
            testData.setData("searchTerm", "nonexistentterm12345");
            testData.setData("expectedResults", "0");
        } else {
            testData.setData("searchTerm", "!@#$%^&*()");
            testData.setData("expectedResults", "handled");
        }
    }
    
    /**
     * Generate update/edit test data
     */
    private void generateUpdateTestData(TestData testData, String scenarioType) {
        if (scenarioType.equals("positive")) {
            testData.setData("newName", faker.name().fullName());
            testData.setData("newDescription", faker.lorem().sentence());
        } else {
            testData.setData("newName", "");
            testData.setData("newDescription", generateLongString(1000));
        }
    }
    
    /**
     * Generate generic test data
     */
    private void generateGenericTestData(TestData testData, String action) {
        testData.setData("input1", faker.lorem().word());
        testData.setData("input2", faker.lorem().word());
        testData.setData("action", action);
    }
    
    /**
     * Generate boundary value for testing
     */
    private String generateBoundaryValue(String fieldType) {
        switch (fieldType.toLowerCase()) {
            case "password":
                return "a".repeat(255); // Max length
            case "name":
                return "A"; // Min length
            case "description":
                return faker.lorem().characters(5000); // Very long
            default:
                return faker.lorem().word();
        }
    }
    
    /**
     * Generate a long string
     */
    private String generateLongString(int length) {
        return faker.lorem().characters(length);
    }
}

