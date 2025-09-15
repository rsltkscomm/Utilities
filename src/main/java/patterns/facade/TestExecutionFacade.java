package patterns.facade;

import patterns.builder.TestConfiguration;
import patterns.command.CommandInvoker;
import patterns.repository.TestData;
import patterns.repository.TestDataRepository;
import patterns.repository.TestResult;
import patterns.repository.TestResultRepository;
import reporting.TestLogManager;

import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Optional;

/**
 * Facade for test execution operations.
 * This provides a simplified interface for complex test execution operations.
 */
public class TestExecutionFacade {
    
    private final TestConfiguration configuration;
    private final TestDataRepository testDataRepository;
    private final TestResultRepository testResultRepository;
    private final CommandInvoker commandInvoker;
    private WebDriver driver;
    
    public TestExecutionFacade(TestConfiguration configuration,
                              TestDataRepository testDataRepository,
                              TestResultRepository testResultRepository,
                             CommandInvoker commandInvoker) {
        this.configuration = configuration;
        this.testDataRepository = testDataRepository;
        this.testResultRepository = testResultRepository;
        this.commandInvoker = commandInvoker;
        
        TestLogManager.info("TestExecutionFacade initialized");
    }
    
    /**
     * Executes a test with the given test name.
     * @param testName The name of the test to execute
     * @return TestResult containing execution details
     */
    public TestResult executeTest(String testName) {
        TestLogManager.info("Executing test: " + testName);
        
        try {
            // Get test data
            Optional<TestData> testDataOpt = testDataRepository.getTestData(testName);
            if (testDataOpt.isEmpty()) {
                TestLogManager.warning("No test data found for: " + testName);
                return createFailedResult(testName, "No test data found");
            }
            
            TestData testData = testDataOpt.get();
            return executeTestWithData(testName, testData);
            
        } catch (Exception e) {
            TestLogManager.error("Error executing test: " + testName, e);
            return createFailedResult(testName, e.getMessage());
        }
    }
    
    /**
     * Executes a test with custom test data.
     * @param testName The name of the test
     * @param testData Custom test data
     * @return TestResult containing execution details
     */
    public TestResult executeTestWithData(String testName, TestData testData) {
        TestLogManager.info("Executing test with data: " + testName);
        
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        
        try {
            // Initialize driver if not already done
            if (driver == null) {
                initializeDriver();
            }
            
            // Execute test logic based on test name
            boolean success = performTestExecution(testName, testData);
            
            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            
            TestResult result = new TestResult.Builder()
                    .testName(testName)
                    .status(success ? TestResult.Status.PASS : TestResult.Status.FAIL)
                    .startTime(startTime)
                    .endTime(endTime)
                    .additionalInfo("browser", configuration.getBrowser())
                    .additionalInfo("environment", configuration.getEnvironment())
                    .build();
            
            // Save result
            testResultRepository.saveTestResult(result);
            
            TestLogManager.info("Test execution completed: " + testName + " - " + result.getStatus());
            return result;
            
        } catch (Exception e) {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            
            TestResult result = new TestResult.Builder()
                    .testName(testName)
                    .status(TestResult.Status.ERROR)
                    .startTime(startTime)
                    .endTime(endTime)
                    .errorMessage(e.getMessage())
                    .stackTrace(getStackTrace(e))
                    .additionalInfo("browser", configuration.getBrowser())
                    .additionalInfo("environment", configuration.getEnvironment())
                    .build();
            
            testResultRepository.saveTestResult(result);
            
            TestLogManager.error("Test execution failed: " + testName, e);
            return result;
        }
    }
    
    /**
     * Executes multiple tests in sequence.
     * @param testNames List of test names to execute
     * @return List of TestResult objects
     */
    public List<TestResult> executeTests(List<String> testNames) {
        TestLogManager.info("Executing multiple tests: " + testNames.size());
        
        return testNames.stream()
                .map(this::executeTest)
                .toList();
    }
    
    /**
     * Navigates to a URL.
     * @param url The URL to navigate to
     * @return true if successful, false otherwise
     */
    public boolean navigateTo(String url) {
        try {
            if (driver == null) {
                initializeDriver();
            }
            
            driver.get(url);
            TestLogManager.info("Navigated to: " + url);
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to navigate to: " + url, e);
            return false;
        }
    }
    
    /**
     * Waits for page to load completely.
     * @return true if page loaded successfully, false otherwise
     */
    public boolean waitForPageLoad() {
        try {
            if (driver == null) {
                return false;
            }
            
            // Simple wait implementation
            Thread.sleep(2000);
            TestLogManager.info("Page load wait completed");
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to wait for page load", e);
            return false;
        }
    }
    
    /**
     * Gets the current WebDriver instance.
     * @return WebDriver instance
     */
    public WebDriver getDriver() {
        if (driver == null) {
            initializeDriver();
        }
        return driver;
    }
    
    /**
     * Closes the WebDriver.
     */
    public void closeDriver() {
        if (driver != null) {
            try {
                driver.quit();
                TestLogManager.info("WebDriver closed");
            } catch (Exception e) {
                TestLogManager.error("Error closing WebDriver", e);
            } finally {
                driver = null;
            }
        }
    }
    
    private void initializeDriver() {
        try {
            // Use the strategy pattern to create driver
            driver = patterns.strategy.DriverFactory.createDriver(configuration.getBrowser());
            driver.manage().timeouts().implicitlyWait(configuration.getTimeout());
            driver.manage().window().maximize();
            
            TestLogManager.info("WebDriver initialized: " + configuration.getBrowser());
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize WebDriver", e);
            throw new RuntimeException("Failed to initialize WebDriver", e);
        }
    }
    
    private boolean performTestExecution(String testName, TestData testData) {
        // Simple test execution logic
        // In a real implementation, this would contain actual test logic
        
        TestLogManager.info("Performing test execution for: " + testName);
        
        // Simulate test execution
        try {
            // Example: Navigate to a test URL
            String testUrl = testData.getData("url", "https://example.com");
            if (!navigateTo(testUrl)) {
                return false;
        }
        
            // Example: Wait for page load
            if (!waitForPageLoad()) {
                return false;
            }
            
            // Example: Check page title
            String expectedTitle = testData.getData("expectedTitle", "");
            if (!expectedTitle.isEmpty()) {
                String actualTitle = driver.getTitle();
                if (!expectedTitle.equals(actualTitle)) {
                    TestLogManager.warning("Title mismatch - Expected: " + expectedTitle + ", Actual: " + actualTitle);
                return false;
            }
        }
        
            TestLogManager.success("Test execution completed successfully: " + testName);
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Test execution failed: " + testName, e);
            return false;
        }
    }
    
    private TestResult createFailedResult(String testName, String errorMessage) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        return new TestResult.Builder()
                .testName(testName)
                .status(TestResult.Status.FAIL)
                .startTime(now)
                .endTime(now)
                .errorMessage(errorMessage)
                .additionalInfo("browser", configuration.getBrowser())
                .additionalInfo("environment", configuration.getEnvironment())
                .build();
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}