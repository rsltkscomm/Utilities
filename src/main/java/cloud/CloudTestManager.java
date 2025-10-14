package cloud;

import cloud.execution.CloudExecutionEngine;
import cloud.execution.CloudExecutionTask;
import cloud.execution.impl.WebTestExecutionTask;
import cloud.execution.impl.MobileTestExecutionTask;
import cloud.providers.CloudProvider;
import cloud.providers.CloudProviderFactory;
import cloud.reporting.CloudReportGenerator;
import cloud.session.CloudSessionInfo;
import reporting.TestLogManager;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Cloud Test Manager
 * Main entry point for cloud testing operations
 * Provides high-level API for cloud testing management
 */
public class CloudTestManager {
    
    private final CloudConfiguration cloudConfig;
    private final CloudExecutionEngine executionEngine;
    private final CloudReportGenerator reporting;
    private final List<CloudSessionInfo> sessionHistory;
    
    private boolean initialized;
    
    public CloudTestManager() {
        this.cloudConfig = new CloudConfiguration();
        this.executionEngine = new CloudExecutionEngine(cloudConfig);
        this.reporting = new CloudReportGenerator();
        this.sessionHistory = new ArrayList<>();
        this.initialized = false;
        
        initialize();
    }
    
    /**
     * Initialize the cloud test manager
     */
    private void initialize() {
        try {
            TestLogManager.info("Initializing Cloud Test Manager...");
            
            // Validate cloud configuration
            if (!cloudConfig.validateConfiguration()) {
                throw new IllegalStateException("Cloud configuration validation failed");
            }
            
            // Print configuration
            cloudConfig.printConfiguration();
            
            this.initialized = true;
            TestLogManager.success("Cloud Test Manager initialized successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize Cloud Test Manager", e);
            throw new RuntimeException("Cloud Test Manager initialization failed", e);
        }
    }
    
    /**
     * Start cloud testing
     */
    public void startCloudTesting() {
        if (!initialized) {
            throw new IllegalStateException("Cloud Test Manager not initialized");
        }
        
        if (!cloudConfig.isCloudEnabled()) {
            throw new IllegalStateException("Cloud testing is not enabled");
        }
        
        try {
            executionEngine.start();
            TestLogManager.success("Cloud testing started");
        } catch (Exception e) {
            TestLogManager.error("Failed to start cloud testing", e);
            throw new RuntimeException("Failed to start cloud testing", e);
        }
    }
    
    /**
     * Stop cloud testing
     */
    public void stopCloudTesting() {
        try {
            executionEngine.stop();
            TestLogManager.success("Cloud testing stopped");
        } catch (Exception e) {
            TestLogManager.error("Failed to stop cloud testing", e);
        }
    }
    
    /**
     * Execute web test on cloud
     */
    public Future<CloudExecutionEngine.ExecutionResult> executeWebTest(String testId, String testDescription, 
                                                                      String browser, String platform, String version,
                                                                      java.util.function.Consumer<org.openqa.selenium.WebDriver> testAction) {
        if (!executionEngine.isRunning()) {
            throw new IllegalStateException("Cloud execution engine is not running");
        }
        
        try {
            WebTestExecutionTask task = WebTestExecutionTask.create(testId, testDescription, browser, platform, version, testAction);
            return executionEngine.executeTask(task);
        } catch (Exception e) {
            TestLogManager.error("Failed to execute web test: " + testId, e);
            throw new RuntimeException("Failed to execute web test", e);
        }
    }
    
    /**
     * Execute mobile test on cloud
     */
    public Future<CloudExecutionEngine.ExecutionResult> executeMobileTest(String testId, String testDescription,
                                                                         String platform, String device, String version,
                                                                         java.util.function.Consumer<org.openqa.selenium.WebDriver> testAction) {
        if (!executionEngine.isRunning()) {
            throw new IllegalStateException("Cloud execution engine is not running");
        }
        
        try {
            MobileTestExecutionTask task;
            
            if ("android".equalsIgnoreCase(platform)) {
                task = MobileTestExecutionTask.createAndroidTest(testId, testDescription, device, version, testAction);
            } else if ("ios".equalsIgnoreCase(platform)) {
                task = MobileTestExecutionTask.createiOSTest(testId, testDescription, device, version, testAction);
            } else {
                throw new IllegalArgumentException("Unsupported mobile platform: " + platform);
            }
            
            return executionEngine.executeTask(task);
        } catch (Exception e) {
            TestLogManager.error("Failed to execute mobile test: " + testId, e);
            throw new RuntimeException("Failed to execute mobile test", e);
        }
    }
    
    /**
     * Execute multiple tests in parallel
     */
    public Map<String, Future<CloudExecutionEngine.ExecutionResult>> executeTestsInParallel(List<CloudExecutionTask> tasks) {
        if (!executionEngine.isRunning()) {
            throw new IllegalStateException("Cloud execution engine is not running");
        }
        
        try {
            return executionEngine.executeTasks(tasks);
        } catch (Exception e) {
            TestLogManager.error("Failed to execute tests in parallel", e);
            throw new RuntimeException("Failed to execute tests in parallel", e);
        }
    }
    
    /**
     * Wait for test completion
     */
    public Map<String, CloudExecutionEngine.ExecutionResult> waitForTestCompletion(Map<String, Future<CloudExecutionEngine.ExecutionResult>> futures, 
                                                                                   long timeout, TimeUnit unit) {
        try {
            Map<String, CloudExecutionEngine.ExecutionResult> results = executionEngine.waitForCompletion(futures, timeout, unit);
            
            // Add session info to history
            for (CloudExecutionEngine.ExecutionResult executionResult : results.values()) {
                // TODO: Get session info from executionResult and add to history
                TestLogManager.info("Execution result: " + executionResult.getStatus());
            }
            
            return results;
        } catch (Exception e) {
            TestLogManager.error("Failed to wait for test completion", e);
            throw new RuntimeException("Failed to wait for test completion", e);
        }
    }
    
    /**
     * Get execution statistics
     */
    public CloudExecutionEngine.ExecutionStatistics getExecutionStatistics() {
        return executionEngine.getStatistics();
    }
    
    /**
     * Get active sessions
     */
    public Map<String, cloud.session.CloudSession> getActiveSessions() {
        return executionEngine.getActiveSessions();
    }
    
    /**
     * Stop specific session
     */
    public boolean stopSession(String sessionId) {
        return executionEngine.stopSession(sessionId);
    }
    
    /**
     * Get session information
     */
    public CloudSessionInfo getSessionInfo(String sessionId) {
        return executionEngine.getSessionInfo(sessionId);
    }
    
    /**
     * Generate comprehensive cloud report
     */
    public String generateCloudReport() {
        try {
            return reporting.generateCloudReport(executionEngine, sessionHistory);
        } catch (Exception e) {
            TestLogManager.error("Failed to generate cloud report", e);
            throw new RuntimeException("Failed to generate cloud report", e);
        }
    }
    
    /**
     * Generate Excel cloud report
     */
    public String generateExcelReport() {
        try {
            return reporting.generateExcelReport(sessionHistory);
        } catch (Exception e) {
            TestLogManager.error("Failed to generate Excel cloud report", e);
            throw new RuntimeException("Failed to generate Excel cloud report", e);
        }
    }
    
    /**
     * Generate JSON cloud report
     */
    public String generateJsonReport() {
        try {
            return reporting.generateJsonReport(executionEngine, sessionHistory);
        } catch (Exception e) {
            TestLogManager.error("Failed to generate JSON cloud report", e);
            throw new RuntimeException("Failed to generate JSON cloud report", e);
        }
    }
    
    /**
     * Get dashboard data for real-time monitoring
     */
    public Map<String, Object> getDashboardData() {
        return reporting.generateDashboardData(executionEngine, sessionHistory);
    }
    
    /**
     * Get cloud configuration
     */
    public CloudConfiguration getCloudConfiguration() {
        return cloudConfig;
    }
    
    /**
     * Get cloud provider
     */
    public CloudProvider getCloudProvider() {
        return CloudProviderFactory.createActiveProvider(cloudConfig);
    }
    
    /**
     * Check if cloud testing is running
     */
    public boolean isCloudTestingRunning() {
        return executionEngine.isRunning();
    }
    
    /**
     * Check if cloud testing is enabled
     */
    public boolean isCloudTestingEnabled() {
        return cloudConfig.isCloudEnabled();
    }
    
    /**
     * Get available browsers from cloud provider
     */
    public Map<String, Object> getAvailableBrowsers() {
        CloudProvider provider = getCloudProvider();
        return provider.getAvailableBrowsers();
    }
    
    /**
     * Get available platforms from cloud provider
     */
    public Map<String, Object> getAvailablePlatforms() {
        CloudProvider provider = getCloudProvider();
        return provider.getAvailablePlatforms();
    }
    
    /**
     * Get provider statistics
     */
    public Map<String, Object> getProviderStatistics() {
        CloudProvider provider = getCloudProvider();
        return provider.getProviderStatistics();
    }
    
    /**
     * Validate cloud credentials
     */
    public boolean validateCredentials() {
        CloudProvider provider = getCloudProvider();
        return provider.validateCredentials();
    }
    
    /**
     * Create web test task
     */
    public WebTestExecutionTask createWebTestTask(String testId, String testDescription, 
                                                 String browser, String platform, String version,
                                                 java.util.function.Consumer<org.openqa.selenium.WebDriver> testAction) {
        return WebTestExecutionTask.create(testId, testDescription, browser, platform, version, testAction);
    }
    
    /**
     * Create mobile test task
     */
    public MobileTestExecutionTask createMobileTestTask(String testId, String testDescription,
                                                       String platform, String device, String version,
                                                       java.util.function.Consumer<org.openqa.selenium.WebDriver> testAction) {
        if ("android".equalsIgnoreCase(platform)) {
            return MobileTestExecutionTask.createAndroidTest(testId, testDescription, device, version, testAction);
        } else if ("ios".equalsIgnoreCase(platform)) {
            return MobileTestExecutionTask.createiOSTest(testId, testDescription, device, version, testAction);
        } else {
            throw new IllegalArgumentException("Unsupported mobile platform: " + platform);
        }
    }
    
    /**
     * Run cross-browser test suite
     */
    public Map<String, Future<CloudExecutionEngine.ExecutionResult>> runCrossBrowserTestSuite(String testId, String testDescription,
                                                                                             java.util.function.Consumer<org.openqa.selenium.WebDriver> testAction) {
        List<CloudExecutionTask> tasks = new ArrayList<>();
        
        // Chrome tests
        tasks.add(createWebTestTask(testId + "_chrome_win", testDescription + " - Chrome Windows", "chrome", "Windows 10", "latest", testAction));
        tasks.add(createWebTestTask(testId + "_chrome_mac", testDescription + " - Chrome macOS", "chrome", "macOS Big Sur", "latest", testAction));
        
        // Firefox tests
        tasks.add(createWebTestTask(testId + "_firefox_win", testDescription + " - Firefox Windows", "firefox", "Windows 10", "latest", testAction));
        tasks.add(createWebTestTask(testId + "_firefox_mac", testDescription + " - Firefox macOS", "firefox", "macOS Big Sur", "latest", testAction));
        
        // Safari test
        tasks.add(createWebTestTask(testId + "_safari_mac", testDescription + " - Safari macOS", "safari", "macOS Big Sur", "latest", testAction));
        
        // Edge test
        tasks.add(createWebTestTask(testId + "_edge_win", testDescription + " - Edge Windows", "edge", "Windows 10", "latest", testAction));
        
        return executeTestsInParallel(tasks);
    }
    
    /**
     * Run mobile test suite
     */
    public Map<String, Future<CloudExecutionEngine.ExecutionResult>> runMobileTestSuite(String testId, String testDescription,
                                                                                       java.util.function.Consumer<org.openqa.selenium.WebDriver> testAction) {
        List<CloudExecutionTask> tasks = new ArrayList<>();
        
        // Android tests
        tasks.add(createMobileTestTask(testId + "_android_s21", testDescription + " - Android Galaxy S21", "android", "Samsung Galaxy S21", "11", testAction));
        tasks.add(createMobileTestTask(testId + "_android_pixel", testDescription + " - Android Pixel 6", "android", "Google Pixel 6", "12", testAction));
        
        // iOS tests
        tasks.add(createMobileTestTask(testId + "_ios_iphone12", testDescription + " - iOS iPhone 12", "ios", "iPhone 12", "15", testAction));
        tasks.add(createMobileTestTask(testId + "_ios_iphone13", testDescription + " - iOS iPhone 13", "ios", "iPhone 13", "16", testAction));
        
        return executeTestsInParallel(tasks);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        try {
            TestLogManager.info("Cleaning up Cloud Test Manager...");
            
            if (executionEngine.isRunning()) {
                stopCloudTesting();
            }
            
            TestLogManager.success("Cloud Test Manager cleanup completed");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to cleanup Cloud Test Manager", e);
        }
    }
}
