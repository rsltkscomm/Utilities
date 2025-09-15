package patterns.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing test results.
 * This provides a contract for different implementations of test result storage.
 */
public interface TestResultRepository {
    
    /**
     * Saves a test result.
     * @param testResult The test result to save
     * @return true if successful, false otherwise
     */
    boolean saveTestResult(TestResult testResult);
    
    /**
     * Gets a test result by test name.
     * @param testName The name of the test
     * @return Optional containing TestResult if found
     */
    Optional<TestResult> getTestResult(String testName);
    
    /**
     * Gets all test results.
     * @return List of all TestResult objects
     */
    List<TestResult> getAllTestResults();
    
    /**
     * Gets test results by status.
     * @param status The status to filter by
     * @return List of TestResult objects with the specified status
     */
    List<TestResult> getTestResultsByStatus(TestResult.Status status);
    
    /**
     * Gets test results within a date range.
     * @param startDate The start date
     * @param endDate The end date
     * @return List of TestResult objects within the date range
     */
    List<TestResult> getTestResultsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Gets the latest test result for a specific test.
     * @param testName The name of the test
     * @return Optional containing the latest TestResult if found
     */
    Optional<TestResult> getLatestTestResult(String testName);
    
    /**
     * Updates an existing test result.
     * @param testResult The test result to update
     * @return true if successful, false otherwise
     */
    boolean updateTestResult(TestResult testResult);
    
    /**
     * Deletes a test result by test name.
     * @param testName The name of the test
     * @return true if successful, false otherwise
     */
    boolean deleteTestResult(String testName);
    
    /**
     * Gets execution statistics.
     * @return TestExecutionStats object containing execution statistics
     */
    TestExecutionStats getExecutionStats();
    
    /**
     * Gets execution statistics within a date range.
     * @param startDate The start date
     * @param endDate The end date
     * @return TestExecutionStats object containing execution statistics
     */
    TestExecutionStats getExecutionStats(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Clears all test results.
     * @return true if successful, false otherwise
     */
    boolean clear();
    
    /**
     * Clears old test results before a cutoff date.
     * @param cutoffDate The cutoff date
     * @return Number of results removed
     */
    int clearOldResults(LocalDateTime cutoffDate);
}