package patterns.repository;

import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Excel-based implementation of TestDataRepository.
 * This implementation reads test data from Excel files.
 */
public class ExcelTestDataRepository implements TestDataRepository {
    
    private final String excelPath;
    private final String sheetName;
    private final Map<String, TestData> testDataCache;
    
    public ExcelTestDataRepository(String excelPath, String sheetName) {
        this.excelPath = excelPath;
        this.sheetName = sheetName;
        this.testDataCache = new HashMap<>();
        
        TestLogManager.info("ExcelTestDataRepository initialized with path: " + excelPath + ", sheet: " + sheetName);
    }
    
    @Override
    public Optional<TestData> getTestData(String testName) {
        // Check cache first
        if (testDataCache.containsKey(testName)) {
            return Optional.of(testDataCache.get(testName));
        }
        
        // For demonstration, create sample test data
        // In a real implementation, you would read from Excel file
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
                testData.setData("username", "testuser");
                testData.setData("password", "testpass");
                testData.setData("email", "test@example.com");
                break;
            case "coretest":
                testData.setData("testName", "Core Test");
                testData.setData("description", "Core functionality test");
                testData.setData("priority", "high");
                break;
            case "logintest":
                testData.setData("username", "admin");
                testData.setData("password", "admin123");
                testData.setData("expectedResult", "success");
                break;
            default:
                testData.setData("defaultKey", "defaultValue");
                testData.setData("testType", "generic");
                break;
        }
        
        // Add metadata
        testData.setMetadata("createdAt", java.time.LocalDateTime.now());
        testData.setMetadata("source", "ExcelTestDataRepository");
        
        return testData;
    }
}