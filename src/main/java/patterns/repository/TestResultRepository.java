package patterns.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for test result operations.
 */
public interface TestResultRepository {
    
    /**
     * Saves a test result.
     * @param testResult The test result to save
     * @return true if saved successfully, false otherwise
     */
    boolean saveTestResult(TestResult testResult);
    
    /**
     * Retrieves a test result by test name.
     * @param testName The name of the test
     * @return Optional containing TestResult if found, empty otherwise
     */
    Optional<TestResult> getTestResult(String testName);
    
    /**
     * Retrieves all test results.
     * @return List of all TestResult instances
     */
    List<TestResult> getAllTestResults();
    
    /**
     * Retrieves test results by status.
     * @param status The status to filter by
     * @return List of TestResult instances with the specified status
     */
    List<TestResult> getTestResultsByStatus(TestResult.Status status);
    
    /**
     * Retrieves test results within a date range.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of TestResult instances within the date range
     */
    List<TestResult> getTestResultsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Retrieves the latest test result for a given test name.
     * @param testName The name of the test
     * @return Optional containing the latest TestResult if found, empty otherwise
     */
    Optional<TestResult> getLatestTestResult(String testName);
    
    /**
     * Updates an existing test result.
     * @param testResult The test result to update
     * @return true if updated successfully, false otherwise
     */
    boolean updateTestResult(TestResult testResult);
    
    /**
     * Deletes a test result.
     * @param testName The name of the test result to delete
     * @return true if deleted successfully, false otherwise
     */
    boolean deleteTestResult(String testName);
    
    /**
     * Gets test execution statistics.
     * @return TestExecutionStats object containing statistics
     */
    TestExecutionStats getExecutionStats();
    
    /**
     * Gets test execution statistics for a specific date range.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return TestExecutionStats object containing statistics for the date range
     */
    TestExecutionStats getExecutionStats(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Clears all test results.
     * @return true if cleared successfully, false otherwise
     */
    boolean clear();
    
    /**
     * Clears test results older than the specified date.
     * @param cutoffDate The cutoff date
     * @return Number of test results deleted
     */
    int clearOldResults(LocalDateTime cutoffDate);
}
