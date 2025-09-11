package patterns.repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for test data operations.
 * This provides a clean abstraction for data access operations.
 */
public interface TestDataRepository {
    
    /**
     * Retrieves test data for a specific test case.
     * @param testName The name of the test case
     * @return Optional containing TestData if found, empty otherwise
     */
    Optional<TestData> getTestData(String testName);
    
    /**
     * Retrieves test data for a specific test case with a default value.
     * @param testName The name of the test case
     * @param defaultValue Default TestData to return if not found
     * @return TestData instance
     */
    TestData getTestData(String testName, TestData defaultValue);
    
    /**
     * Retrieves all test data.
     * @return List of all TestData instances
     */
    List<TestData> getAllTestData();
    
    /**
     * Retrieves test data by sheet name.
     * @param sheetName The name of the sheet
     * @return List of TestData instances from the sheet
     */
    List<TestData> getTestDataBySheet(String sheetName);
    
    /**
     * Saves test data.
     * @param testData The test data to save
     * @return true if saved successfully, false otherwise
     */
    boolean saveTestData(TestData testData);
    
    /**
     * Updates existing test data.
     * @param testData The test data to update
     * @return true if updated successfully, false otherwise
     */
    boolean updateTestData(TestData testData);
    
    /**
     * Deletes test data.
     * @param testName The name of the test case to delete
     * @return true if deleted successfully, false otherwise
     */
    boolean deleteTestData(String testName);
    
    /**
     * Checks if test data exists for a given test name.
     * @param testName The name of the test case
     * @return true if exists, false otherwise
     */
    boolean exists(String testName);
    
    /**
     * Gets the count of test data entries.
     * @return Number of test data entries
     */
    int count();
    
    /**
     * Clears all test data.
     * @return true if cleared successfully, false otherwise
     */
    boolean clear();
}
