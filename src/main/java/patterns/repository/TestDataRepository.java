package patterns.repository;

import java.util.Optional;

/**
 * Repository interface for managing test data.
 * This provides a contract for different implementations of test data storage.
 */
public interface TestDataRepository {
    
    /**
     * Gets test data by test name.
     * @param testName The name of the test
     * @return Optional containing TestData if found
     */
    Optional<TestData> getTestData(String testName);
    
    /**
     * Saves test data.
     * @param testData The test data to save
     * @return true if successful, false otherwise
     */
    boolean saveTestData(TestData testData);
    
    /**
     * Updates existing test data.
     * @param testData The test data to update
     * @return true if successful, false otherwise
     */
    boolean updateTestData(TestData testData);
    
    /**
     * Deletes test data by test name.
     * @param testName The name of the test
     * @return true if successful, false otherwise
     */
    boolean deleteTestData(String testName);
    
    /**
     * Checks if test data exists for the given test name.
     * @param testName The name of the test
     * @return true if exists, false otherwise
     */
    boolean exists(String testName);
    
    /**
     * Gets all test data.
     * @return List of all TestData objects
     */
    java.util.List<TestData> getAllTestData();
    
    /**
     * Clears all test data.
     * @return true if successful, false otherwise
     */
    boolean clear();
}