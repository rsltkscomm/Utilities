package patterns.repository;

import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-based implementation of TestDataRepository.
 * This implementation reads test data from JSON files.
 */
public class JsonTestDataRepository implements TestDataRepository {
    
    private final String jsonPath;
    private final Map<String, TestData> testDataCache;
    
    public JsonTestDataRepository(String jsonPath) {
        this.jsonPath = jsonPath;
        this.testDataCache = new HashMap<>();
        
        TestLogManager.info("JsonTestDataRepository initialized with path: " + jsonPath);
    }
    
    @Override
    public Optional<TestData> getTestData(String testName) {
        // Check cache first
        if (testDataCache.containsKey(testName)) {
            return Optional.of(testDataCache.get(testName));
        }
        
        // For demonstration, create sample test data
        // In a real implementation, you would read from JSON file
        TestData testData = createSampleTestData(testName);
            if (testData != null) {
            testDataCache.put(testName, testData);
                return Optional.of(testData);
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean saveTestData(TestData testData) {
        try {
            testDataCache.put(testData.getTestName(), testData);
            TestLogManager.info("Test data saved: " + testData.getTestName());
            return true;
        } catch (Exception e) {
            TestLogManager.error("Failed to save test data: " + testData.getTestName(), e);
            return false;
        }
    }
    
    @Override
    public boolean updateTestData(TestData testData) {
        return saveTestData(testData);
    }
    
    @Override
    public boolean deleteTestData(String testName) {
        TestData removed = testDataCache.remove(testName);
        if (removed != null) {
            TestLogManager.info("Test data deleted: " + testName);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean exists(String testName) {
        return testDataCache.containsKey(testName);
    }
    
    @Override
    public java.util.List<TestData> getAllTestData() {
        return new java.util.ArrayList<>(testDataCache.values());
    }
    
    @Override
    public boolean clear() {
        testDataCache.clear();
        TestLogManager.info("All test data cleared");
        return true;
    }
    
    private TestData createSampleTestData(String testName) {
        TestData testData = new TestData(testName);
        
        // Add sample data based on test name
        switch (testName.toLowerCase()) {
            case "sampletest":
                testData.setData("username", "jsonuser");
                testData.setData("password", "jsonpass");
                testData.setData("email", "json@example.com");
                break;
            case "coretest":
                testData.setData("testName", "JSON Core Test");
                testData.setData("description", "Core functionality test from JSON");
                testData.setData("priority", "medium");
                break;
            case "logintest":
                testData.setData("username", "jsonadmin");
                testData.setData("password", "jsonadmin123");
                testData.setData("expectedResult", "success");
                break;
            default:
                testData.setData("defaultKey", "jsonDefaultValue");
                testData.setData("testType", "json");
                break;
        }
        
        // Add metadata
        testData.setMetadata("createdAt", java.time.LocalDateTime.now());
        testData.setMetadata("source", "JsonTestDataRepository");
        
        return testData;
    }
}