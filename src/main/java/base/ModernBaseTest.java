package base;

import patterns.builder.TestConfiguration;
import patterns.command.CommandInvoker;
import patterns.config.TestFrameworkConfig;
import patterns.di.ServiceLocator;
import patterns.facade.TestExecutionFacade;
import patterns.facade.ReportingFacade;
import patterns.repository.TestData;
import patterns.repository.TestResult;
import reporting.TestLogManager;

import org.testng.annotations.*;

import pages.PageFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Modern base test class that uses the new architectural patterns.
 * This provides a clean, maintainable foundation for test classes.
 */
public class ModernBaseTest {
    
    protected TestExecutionFacade testExecutionFacade;
    protected ReportingFacade reportingFacade;
    protected CommandInvoker commandInvoker;
    protected TestConfiguration configuration;
    
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        TestLogManager.info("==== Modern Test Suite Started ====");
        
        // Initialize the framework
        TestFrameworkConfig frameworkConfig = TestFrameworkConfig.getInstance();
        frameworkConfig.initialize();
        
        // Get facades and configuration
        this.testExecutionFacade = frameworkConfig.getTestExecutionFacade();
        this.reportingFacade = frameworkConfig.getReportingFacade();
        this.commandInvoker = ServiceLocator.getRequiredService(CommandInvoker.class);
        this.configuration = frameworkConfig.getTestConfiguration();
        
        TestLogManager.success("Framework initialized successfully");
    }
    
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        TestLogManager.testStart(method.getName());
        
        // Initialize reporting for this test
        reportingFacade.initializeReporting();
        
        TestLogManager.info("Test method started: " + method.getName());
    }
    
    @AfterMethod(alwaysRun = true)
    public void afterMethod(org.testng.ITestResult result) {
        // Create test result
        TestResult testResult = createTestResult(result);
        
        // Report the result
        reportingFacade.reportTestResult(testResult);
        
        // Log the result
        switch (result.getStatus()) {
            case org.testng.ITestResult.SUCCESS -> {
                TestLogManager.success("Test passed: " + result.getName());
            }
            case org.testng.ITestResult.FAILURE -> {
                TestLogManager.error("Test failed: " + result.getName(), result.getThrowable());
            }
            case org.testng.ITestResult.SKIP -> {
                TestLogManager.warning("Test skipped: " + result.getName());
            }
        }
        
        TestLogManager.testEnd(result.getName(), getStatusString(result.getStatus()));
    }
    
    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        // Generate summary report
        var summary = reportingFacade.generateSummaryReport();
        TestLogManager.info("Test execution summary: " + summary);
        
        // Finalize reporting
        reportingFacade.finalizeReporting();
        
        // Shutdown framework
        TestFrameworkConfig.getInstance().shutdown();
        
        TestLogManager.info("==== Modern Test Suite Finished ====");
    }
    
    /**
     * Executes a test with the given test name.
     * @param testName The name of the test to execute
     * @return TestResult containing execution details
     */
    protected TestResult executeTest(String testName) {
        return testExecutionFacade.executeTest(testName);
    }
    
    /**
     * Executes a test with custom test data.
     * @param testName The name of the test
     * @param testData Custom test data
     * @return TestResult containing execution details
     */
    protected TestResult executeTestWithData(String testName, TestData testData) {
        return testExecutionFacade.executeTestWithData(testName, testData);
    }
    
    /**
     * Executes multiple tests in sequence.
     * @param testNames List of test names to execute
     * @return List of TestResult objects
     */
    protected List<TestResult> executeTests(List<String> testNames) {
        return testExecutionFacade.executeTests(testNames);
    }
    
    /**
     * Navigates to a URL.
     * @param url The URL to navigate to
     * @return true if successful, false otherwise
     */
    protected boolean navigateTo(String url) {
        return testExecutionFacade.navigateTo(url);
    }
    
    /**
     * Waits for page to load completely.
     * @return true if page loaded successfully, false otherwise
     */
    protected boolean waitForPageLoad() {
        return testExecutionFacade.waitForPageLoad();
    }
    
    /**
     * Gets the current WebDriver instance.
     * @return WebDriver instance
     */
    protected org.openqa.selenium.WebDriver getDriver() {
        return testExecutionFacade.getDriver();
    }
    
    /**
     * Gets the command invoker for advanced command operations.
     * @return CommandInvoker instance
     */
    protected CommandInvoker getCommandInvoker() {
        return commandInvoker;
    }
    
    /**
     * Gets the test configuration.
     * @return TestConfiguration instance
     */
    protected TestConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Gets test data for a specific test.
     * @param testName The name of the test
     * @return TestData instance or null if not found
     */
    protected TestData getTestData(String testName) {
        return ServiceLocator.getRequiredService(patterns.repository.TestDataRepository.class)
                .getTestData(testName)
                .orElse(null);
    }
    
    /**
     * Creates test data with the given name and data map.
     * @param testName The name of the test
     * @param data Map of test data
     * @return TestData instance
     */
    protected TestData createTestData(String testName, java.util.Map<String, String> data) {
        TestData testData = new TestData(testName);
        testData.setData(data);
        return testData;
    }
    
    private TestResult createTestResult(org.testng.ITestResult result) {
        TestResult.Builder builder = new TestResult.Builder()
                .testName(result.getName())
                .status(convertStatus(result.getStatus()))
                .startTime(java.time.LocalDateTime.now()) // You might want to track actual start time
                .endTime(java.time.LocalDateTime.now())
                .additionalInfo("method", result.getMethod().getMethodName())
                .additionalInfo("class", result.getTestClass().getName())
                .additionalInfo("browser", configuration.getBrowser())
                .additionalInfo("environment", configuration.getEnvironment());
        
        if (result.getThrowable() != null) {
            builder.errorMessage(result.getThrowable().getMessage())
                   .stackTrace(getStackTrace(result.getThrowable()));
        }
        
        return builder.build();
    }
    
    private TestResult.Status convertStatus(int testngStatus) {
        return switch (testngStatus) {
            case org.testng.ITestResult.SUCCESS -> TestResult.Status.PASS;
            case org.testng.ITestResult.FAILURE -> TestResult.Status.FAIL;
            case org.testng.ITestResult.SKIP -> TestResult.Status.SKIP;
            default -> TestResult.Status.ERROR;
        };
    }
    
    private String getStatusString(int testngStatus) {
        return switch (testngStatus) {
            case org.testng.ITestResult.SUCCESS -> "PASS";
            case org.testng.ITestResult.FAILURE -> "FAIL";
            case org.testng.ITestResult.SKIP -> "SKIP";
            default -> "ERROR";
        };
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    public static PageFactory getPageFactory()
	{
		return new PageFactory(DriverManager.getDriver());
	}
}
