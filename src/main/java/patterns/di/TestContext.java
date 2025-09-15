package patterns.di;

import patterns.builder.TestConfiguration;
import patterns.command.CommandInvoker;
import patterns.repository.TestDataRepository;
import patterns.repository.TestResultRepository;
import patterns.strategy.DriverFactory;
import reporting.TestLogManager;

import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context class that manages dependencies and provides dependency injection functionality.
 * This acts as a simple IoC container for the test automation framework.
 */
public class TestContext {
    
    private final Map<Class<?>, Object> dependencies;
    private final TestConfiguration configuration;
    private WebDriver driver;
    private boolean initialized;
    
    public TestContext(TestConfiguration configuration) {
        this.configuration = configuration;
        this.dependencies = new HashMap<>();
        this.initialized = false;
    }
    
    /**
     * Initializes the test context with default dependencies.
     */
    public void initialize() {
        if (initialized) {
            TestLogManager.warning("TestContext already initialized");
            return;
        }
        
        TestLogManager.info("Initializing TestContext");
        
        try {
            // Register core dependencies
            registerDependency(TestConfiguration.class, configuration);
            
            // Initialize and register repositories
            initializeRepositories();
            
            // Initialize command invoker
            initializeCommandInvoker();
            
            // Initialize driver
            initializeDriver();
            
            initialized = true;
            TestLogManager.success("TestContext initialized successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize TestContext", e);
            throw new RuntimeException("Failed to initialize TestContext", e);
        }
    }
    
    /**
     * Registers a dependency in the context.
     * @param type The type of the dependency
     * @param instance The instance to register
     */
    public <T> void registerDependency(Class<T> type, T instance) {
        dependencies.put(type, instance);
        TestLogManager.info("Registered dependency: " + type.getSimpleName());
    }
    
    /**
     * Gets a dependency from the context.
     * @param type The type of the dependency
     * @return Optional containing the dependency if found
     */
    public <T> Optional<T> getDependency(Class<T> type) {
        Object dependency = dependencies.get(type);
        if (dependency != null && type.isInstance(dependency)) {
            return Optional.of(type.cast(dependency));
        }
        return Optional.empty();
    }
    
    /**
     * Gets a dependency from the context, throwing an exception if not found.
     * @param type The type of the dependency
     * @return The dependency instance
     * @throws IllegalStateException if dependency not found
     */
    public <T> T getRequiredDependency(Class<T> type) {
        return getDependency(type)
                .orElseThrow(() -> new IllegalStateException("Required dependency not found: " + type.getSimpleName()));
    }
    
    /**
     * Gets the test configuration.
     * @return TestConfiguration instance
     */
    public TestConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Gets the WebDriver instance.
     * @return WebDriver instance
     */
    public WebDriver getDriver() {
        if (driver == null) {
            initializeDriver();
        }
        return driver;
    }
    
    /**
     * Gets the test data repository.
     * @return TestDataRepository instance
     */
    public TestDataRepository getTestDataRepository() {
        return getRequiredDependency(TestDataRepository.class);
    }
    
    /**
     * Gets the test result repository.
     * @return TestResultRepository instance
     */
    public TestResultRepository getTestResultRepository() {
        return getRequiredDependency(TestResultRepository.class);
    }
    
    /**
     * Gets the command invoker.
     * @return CommandInvoker instance
     */
    public CommandInvoker getCommandInvoker() {
        return getRequiredDependency(CommandInvoker.class);
    }
    
    /**
     * Checks if the context is initialized.
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Cleans up all resources.
     */
    public void cleanup() {
        TestLogManager.info("Cleaning up TestContext");
        
        try {
            // Close driver
            if (driver != null) {
                driver.quit();
                driver = null;
                TestLogManager.info("WebDriver closed");
            }
            
            // Clear dependencies
            dependencies.clear();
            
            initialized = false;
            TestLogManager.success("TestContext cleaned up successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Error during TestContext cleanup", e);
        }
    }
    
    private void initializeRepositories() {
        // Initialize test data repository based on configuration
        TestDataRepository dataRepository = createTestDataRepository();
        registerDependency(TestDataRepository.class, dataRepository);
        
        // Initialize test result repository
        TestResultRepository resultRepository = createTestResultRepository();
        registerDependency(TestResultRepository.class, resultRepository);
    }
    
    private void initializeCommandInvoker() {
        CommandInvoker commandInvoker = new CommandInvoker(
                configuration.isRetryEnabled(),
                configuration.getMaxRetries()
        );
        registerDependency(CommandInvoker.class, commandInvoker);
    }
    
    private void initializeDriver() {
        try {
            driver = DriverFactory.createDriver(configuration.getBrowser());
            driver.manage().timeouts().implicitlyWait(configuration.getTimeout());
            driver.manage().window().maximize();
            
            TestLogManager.info("WebDriver initialized: " + configuration.getBrowser());
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize WebDriver", e);
            throw new RuntimeException("Failed to initialize WebDriver", e);
        }
    }
    
    private TestDataRepository createTestDataRepository() {
        // Create repository based on configuration
        String dataSource = configuration.getCustomProperty("dataSource", "excel");
        
        switch (dataSource.toLowerCase()) {
            case "excel":
                String excelPath = configuration.getCustomProperty("excelPath", "src/test/resources/testdata.xlsx");
                String sheetName = configuration.getCustomProperty("sheetName", "TestData");
                return new patterns.repository.ExcelTestDataRepository(excelPath, sheetName);
                
            case "json":
                String jsonPath = configuration.getCustomProperty("jsonPath", "src/test/resources/testdata.json");
                return new patterns.repository.JsonTestDataRepository(jsonPath);
                
            default:
                TestLogManager.warning("Unknown data source: " + dataSource + ", using Excel as default");
                return new patterns.repository.ExcelTestDataRepository("src/test/resources/testdata.xlsx", "TestData");
        }
    }
    
    private TestResultRepository createTestResultRepository() {
        // For now, return a simple in-memory implementation
        // In a real scenario, you might want to implement database or file-based repositories
        return new InMemoryTestResultRepository();
    }
    
    /**
     * Simple in-memory implementation of TestResultRepository for demonstration.
     */
    private static class InMemoryTestResultRepository implements TestResultRepository {
        private final Map<String, patterns.repository.TestResult> results = new HashMap<>();
        
        @Override
        public boolean saveTestResult(patterns.repository.TestResult testResult) {
            results.put(testResult.getTestName(), testResult);
            return true;
        }
        
        @Override
        public Optional<patterns.repository.TestResult> getTestResult(String testName) {
            return Optional.ofNullable(results.get(testName));
        }
        
        @Override
        public java.util.List<patterns.repository.TestResult> getAllTestResults() {
            return new java.util.ArrayList<>(results.values());
        }
        
        @Override
        public java.util.List<patterns.repository.TestResult> getTestResultsByStatus(patterns.repository.TestResult.Status status) {
            return results.values().stream()
                    .filter(result -> result.getStatus() == status)
                    .toList();
        }
        
        @Override
        public java.util.List<patterns.repository.TestResult> getTestResultsByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
            return results.values().stream()
                    .filter(result -> result.getStartTime().isAfter(startDate) && result.getStartTime().isBefore(endDate))
                    .toList();
        }
        
        @Override
        public Optional<patterns.repository.TestResult> getLatestTestResult(String testName) {
            return getTestResult(testName); // Simple implementation
        }
        
        @Override
        public boolean updateTestResult(patterns.repository.TestResult testResult) {
            return saveTestResult(testResult);
        }
        
        @Override
        public boolean deleteTestResult(String testName) {
            return results.remove(testName) != null;
        }
        
        @Override
        public patterns.repository.TestExecutionStats getExecutionStats() {
            int total = results.size();
            int passed = (int) results.values().stream().filter(patterns.repository.TestResult::isPassed).count();
            int failed = (int) results.values().stream().filter(patterns.repository.TestResult::isFailed).count();
            int skipped = (int) results.values().stream().filter(patterns.repository.TestResult::isSkipped).count();
            long totalTime = results.values().stream().mapToLong(patterns.repository.TestResult::getDurationInMillis).sum();
            
            return new patterns.repository.TestExecutionStats(
                    total, passed, failed, skipped, 0, totalTime,
                    null, null, new HashMap<>()
            );
        }
        
        @Override
        public patterns.repository.TestExecutionStats getExecutionStats(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
            return getExecutionStats(); // Simple implementation
        }
        
        @Override
        public boolean clear() {
            results.clear();
            return true;
        }
        
        @Override
        public int clearOldResults(java.time.LocalDateTime cutoffDate) {
            int removed = 0;
            var iterator = results.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().getStartTime().isBefore(cutoffDate)) {
                    iterator.remove();
                    removed++;
                }
            }
            return removed;
        }
    }
}
