package patterns.config;

import patterns.builder.TestConfiguration;
import patterns.di.ServiceLocator;
import patterns.di.TestContext;
import patterns.facade.ReportingFacade;
import patterns.facade.TestExecutionFacade;
import reporting.TestLogManager;

import java.util.Properties;

/**
 * Configuration class that manages the overall test framework configuration.
 * This provides a centralized way to configure and initialize the framework.
 */
public class TestFrameworkConfig {
    
    private static TestFrameworkConfig instance;
    private TestConfiguration testConfiguration;
    private TestContext testContext;
    private TestExecutionFacade testExecutionFacade;
    private ReportingFacade reportingFacade;
    private Properties systemProperties;
    
    private TestFrameworkConfig() {
        this.systemProperties = new Properties();
        loadSystemProperties();
    }
    
    /**
     * Gets the singleton instance of TestFrameworkConfig.
     * @return TestFrameworkConfig instance
     */
    public static synchronized TestFrameworkConfig getInstance() {
        if (instance == null) {
            instance = new TestFrameworkConfig();
        }
        return instance;
    }
    
    /**
     * Initializes the framework with default configuration.
     */
    public void initialize() {
        initialize(createDefaultConfiguration());
    }
    
    /**
     * Initializes the framework with custom configuration.
     * @param configuration The test configuration to use
     */
    public void initialize(TestConfiguration configuration) {
        TestLogManager.info("Initializing Test Framework");
        
        try {
            this.testConfiguration = configuration;
            
            // Initialize test context
            this.testContext = new TestContext(configuration);
            this.testContext.initialize();
            
            // Initialize service locator
            ServiceLocator.initialize(testContext);
            
            // Initialize facades
            initializeFacades();
            
            TestLogManager.success("Test Framework initialized successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize Test Framework", e);
            throw new RuntimeException("Failed to initialize Test Framework", e);
        }
    }
    
    /**
     * Gets the test configuration.
     * @return TestConfiguration instance
     */
    public TestConfiguration getTestConfiguration() {
        return testConfiguration;
    }
    
    /**
     * Gets the test context.
     * @return TestContext instance
     */
    public TestContext getTestContext() {
        return testContext;
    }
    
    /**
     * Gets the test execution facade.
     * @return TestExecutionFacade instance
     */
    public TestExecutionFacade getTestExecutionFacade() {
        return testExecutionFacade;
    }
    
    /**
     * Gets the reporting facade.
     * @return ReportingFacade instance
     */
    public ReportingFacade getReportingFacade() {
        return reportingFacade;
    }
    
    /**
     * Gets a system property.
     * @param key The property key
     * @return Property value or null if not found
     */
    public String getSystemProperty(String key) {
        return systemProperties.getProperty(key);
    }
    
    /**
     * Gets a system property with default value.
     * @param key The property key
     * @param defaultValue Default value if property not found
     * @return Property value or default value
     */
    public String getSystemProperty(String key, String defaultValue) {
        return systemProperties.getProperty(key, defaultValue);
    }
    
    /**
     * Sets a system property.
     * @param key The property key
     * @param value The property value
     */
    public void setSystemProperty(String key, String value) {
        systemProperties.setProperty(key, value);
        System.setProperty(key, value);
    }
    
    /**
     * Checks if the framework is initialized.
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return testContext != null && testContext.isInitialized();
    }
    
    /**
     * Shuts down the framework and cleans up resources.
     */
    public void shutdown() {
        TestLogManager.info("Shutting down Test Framework");
        
        try {
            if (testContext != null) {
                testContext.cleanup();
            }
            
            ServiceLocator.clear();
            
            testExecutionFacade = null;
            reportingFacade = null;
            testContext = null;
            testConfiguration = null;
            
            TestLogManager.success("Test Framework shut down successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Error during framework shutdown", e);
        }
    }
    
    private void loadSystemProperties() {
        // Load common system properties
        loadProperty("browser", "chrome");
        loadProperty("headless", "false");
        loadProperty("environment", "test");
        loadProperty("timeout", "30");
        loadProperty("baseUrl", "");
        loadProperty("threadCount", "1");
        loadProperty("retryEnabled", "true");
        loadProperty("maxRetries", "1");
        loadProperty("screenshotOnFailure", "true");
        loadProperty("videoRecording", "false");
        loadProperty("dataSource", "excel");
        loadProperty("excelPath", "src/test/resources/testdata.xlsx");
        loadProperty("sheetName", "TestData");
        loadProperty("jsonPath", "src/test/resources/testdata.json");
    }
    
    private void loadProperty(String key, String defaultValue) {
        String value = System.getProperty(key, defaultValue);
        systemProperties.setProperty(key, value);
    }
    
    private TestConfiguration createDefaultConfiguration() {
        return new TestConfiguration.Builder()
                .browser(getSystemProperty("browser", "chrome"))
                .headless(Boolean.parseBoolean(getSystemProperty("headless", "false")))
                .environment(getSystemProperty("environment", "test"))
                .timeoutSeconds(Integer.parseInt(getSystemProperty("timeout", "30")))
                .baseUrl(getSystemProperty("baseUrl", ""))
                .threadCount(Integer.parseInt(getSystemProperty("threadCount", "1")))
                .retryEnabled(Boolean.parseBoolean(getSystemProperty("retryEnabled", "true")))
                .maxRetries(Integer.parseInt(getSystemProperty("maxRetries", "1")))
                .screenshotOnFailure(Boolean.parseBoolean(getSystemProperty("screenshotOnFailure", "true")))
                .videoRecording(Boolean.parseBoolean(getSystemProperty("videoRecording", "false")))
                .customProperty("dataSource", getSystemProperty("dataSource", "excel"))
                .customProperty("excelPath", getSystemProperty("excelPath", "src/test/resources/testdata.xlsx"))
                .customProperty("sheetName", getSystemProperty("sheetName", "TestData"))
                .customProperty("jsonPath", getSystemProperty("jsonPath", "src/test/resources/testdata.json"))
                .build();
    }
    
    private void initializeFacades() {
        // Initialize test execution facade
        this.testExecutionFacade = new TestExecutionFacade(
                testConfiguration,
                testContext.getTestDataRepository(),
                testContext.getTestResultRepository(),
                testContext.getCommandInvoker()
        );
        
        // Initialize reporting facade
        this.reportingFacade = new ReportingFacade(
                testContext.getTestResultRepository()
        );
        
        // Initialize reporting
        reportingFacade.initializeReporting();
        
        TestLogManager.info("Facades initialized successfully");
    }
}
