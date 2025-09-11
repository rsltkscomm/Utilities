package patterns.facade;

import patterns.builder.TestConfiguration;
import patterns.command.CommandInvoker;
import patterns.command.UICommand;
import patterns.repository.TestData;
import patterns.repository.TestDataRepository;
import patterns.repository.TestResult;
import patterns.repository.TestResultRepository;
import patterns.strategy.DriverFactory;
import reporting.TestLogManager;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Facade class that provides a simplified interface for complex test execution operations.
 * This hides the complexity of coordinating multiple subsystems.
 */
public class TestExecutionFacade {
    
    private final TestConfiguration configuration;
    private final TestDataRepository dataRepository;
    private final TestResultRepository resultRepository;
    private final CommandInvoker commandInvoker;
    private final WebDriver driver;
    
    public TestExecutionFacade(TestConfiguration configuration,
                             TestDataRepository dataRepository,
                             TestResultRepository resultRepository,
                             CommandInvoker commandInvoker) {
        this.configuration = configuration;
        this.dataRepository = dataRepository;
        this.resultRepository = resultRepository;
        this.commandInvoker = commandInvoker;
        this.driver = createDriver();
    }
    
    /**
     * Executes a test with the given test name.
     * @param testName The name of the test to execute
     * @return TestResult containing execution details
     */
    public TestResult executeTest(String testName) {
        TestLogManager.info("Starting test execution: " + testName);
        
        LocalDateTime startTime = LocalDateTime.now();
        TestResult result = null;
        
        try {
            // Load test data
            TestData testData = loadTestData(testName);
            
            // Initialize test environment
            initializeTestEnvironment();
            
            // Execute test steps
            boolean testPassed = executeTestSteps(testData);
            
            // Create result
            result = createTestResult(testName, testPassed, startTime, null);
            
        } catch (Exception e) {
            TestLogManager.error("Test execution failed: " + testName, e);
            result = createTestResult(testName, false, startTime, e);
        } finally {
            // Save result
            if (result != null) {
                resultRepository.saveTestResult(result);
            }
            
            // Cleanup
            cleanup();
        }
        
        return result;
    }
    
    /**
     * Executes multiple tests in sequence.
     * @param testNames List of test names to execute
     * @return List of TestResult objects
     */
    public List<TestResult> executeTests(List<String> testNames) {
        TestLogManager.info("Starting batch test execution for " + testNames.size() + " tests");
        
        return testNames.stream()
                .map(this::executeTest)
                .toList();
    }
    
    /**
     * Executes a test with custom test data.
     * @param testName The name of the test
     * @param testData Custom test data
     * @return TestResult containing execution details
     */
    public TestResult executeTestWithData(String testName, TestData testData) {
        TestLogManager.info("Starting test execution with custom data: " + testName);
        
        LocalDateTime startTime = LocalDateTime.now();
        TestResult result = null;
        
        try {
            // Initialize test environment
            initializeTestEnvironment();
            
            // Execute test steps with custom data
            boolean testPassed = executeTestSteps(testData);
            
            // Create result
            result = createTestResult(testName, testPassed, startTime, null);
            
        } catch (Exception e) {
            TestLogManager.error("Test execution failed: " + testName, e);
            result = createTestResult(testName, false, startTime, e);
        } finally {
            // Save result
            if (result != null) {
                resultRepository.saveTestResult(result);
            }
            
            // Cleanup
            cleanup();
        }
        
        return result;
    }
    
    /**
     * Performs a simple UI action (click, input, etc.).
     * @param command The UI command to execute
     * @return true if successful, false otherwise
     */
    public boolean performUIAction(UICommand command) {
        return commandInvoker.executeCommand(command);
    }
    
    /**
     * Performs multiple UI actions in sequence.
     * @param commands List of UI commands to execute
     * @return true if all successful, false otherwise
     */
    public boolean performUIActions(List<UICommand> commands) {
        return commandInvoker.executeCommands(commands);
    }
    
    /**
     * Navigates to a URL.
     * @param url The URL to navigate to
     * @return true if successful, false otherwise
     */
    public boolean navigateTo(String url) {
        try {
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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(configuration.getTimeout().getSeconds()));
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            TestLogManager.info("Page loaded successfully");
            return true;
        } catch (Exception e) {
            TestLogManager.error("Page load timeout", e);
            return false;
        }
    }
    
    /**
     * Gets the current WebDriver instance.
     * @return WebDriver instance
     */
    public WebDriver getDriver() {
        return driver;
    }
    
    /**
     * Gets the command invoker for advanced command operations.
     * @return CommandInvoker instance
     */
    public CommandInvoker getCommandInvoker() {
        return commandInvoker;
    }
    
    /**
     * Gets test execution statistics.
     * @return TestExecutionStats object
     */
    public TestExecutionStats getExecutionStats() {
        return new TestExecutionStats(
                resultRepository.getExecutionStats(),
                commandInvoker.getExecutionStats()
        );
    }
    
    private WebDriver createDriver() {
        try {
            WebDriver webDriver = DriverFactory.createDriver(configuration.getBrowser());
            webDriver.manage().timeouts().implicitlyWait(configuration.getTimeout());
            webDriver.manage().window().maximize();
            
            TestLogManager.info("WebDriver created successfully: " + configuration.getBrowser());
            return webDriver;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to create WebDriver", e);
            throw new RuntimeException("Failed to create WebDriver", e);
        }
    }
    
    private TestData loadTestData(String testName) {
        Optional<TestData> testDataOpt = dataRepository.getTestData(testName);
        if (testDataOpt.isEmpty()) {
            throw new RuntimeException("Test data not found for: " + testName);
        }
        
        TestData testData = testDataOpt.get();
        TestLogManager.info("Test data loaded for: " + testName);
        return testData;
    }
    
    private void initializeTestEnvironment() {
        // Navigate to base URL if configured
        if (configuration.getBaseUrl() != null && !configuration.getBaseUrl().trim().isEmpty()) {
            navigateTo(configuration.getBaseUrl());
            waitForPageLoad();
        }
        
        TestLogManager.info("Test environment initialized");
    }
    
    private boolean executeTestSteps(TestData testData) {
        // This is a simplified implementation
        // In a real scenario, you would parse the test data and execute specific steps
        
        TestLogManager.info("Executing test steps for: " + testData.getTestName());
        
        // Example: Check if test data contains specific steps
        if (testData.containsKey("navigateTo")) {
            String url = testData.getData("navigateTo");
            if (!navigateTo(url)) {
                return false;
            }
        }
        
        if (testData.containsKey("waitForPageLoad")) {
            if (!waitForPageLoad()) {
                return false;
            }
        }
        
        // Add more test step execution logic here based on your requirements
        
        TestLogManager.success("Test steps executed successfully");
        return true;
    }
    
    private TestResult createTestResult(String testName, boolean passed, LocalDateTime startTime, Exception exception) {
        TestResult.Builder builder = new TestResult.Builder()
                .testName(testName)
                .status(passed ? TestResult.Status.PASS : TestResult.Status.FAIL)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .additionalInfo("browser", configuration.getBrowser())
                .additionalInfo("environment", configuration.getEnvironment())
                .additionalInfo("threadCount", String.valueOf(configuration.getThreadCount()));
        
        if (exception != null) {
            builder.errorMessage(exception.getMessage())
                   .stackTrace(getStackTrace(exception));
        }
        
        return builder.build();
    }
    
    private void cleanup() {
        try {
            if (driver != null) {
                driver.quit();
                TestLogManager.info("WebDriver closed successfully");
            }
        } catch (Exception e) {
            TestLogManager.error("Error during cleanup", e);
        }
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Data class for test execution statistics.
     */
    public static class TestExecutionStats {
        private final patterns.repository.TestExecutionStats testStats;
        private final CommandInvoker.CommandExecutionStats commandStats;
        
        public TestExecutionStats(patterns.repository.TestExecutionStats testStats,
                                CommandInvoker.CommandExecutionStats commandStats) {
            this.testStats = testStats;
            this.commandStats = commandStats;
        }
        
        public patterns.repository.TestExecutionStats getTestStats() {
            return testStats;
        }
        
        public CommandInvoker.CommandExecutionStats getCommandStats() {
            return commandStats;
        }
        
        @Override
        public String toString() {
            return "TestExecutionStats{" +
                    "testStats=" + testStats +
                    ", commandStats=" + commandStats +
                    '}';
        }
    }
}
