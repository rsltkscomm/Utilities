package examples;

import cloud.CloudTestManager;
import cloud.execution.CloudExecutionEngine;
import cloud.execution.impl.WebTestExecutionTask;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.*;
import reporting.TestLogManager;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Cloud Testing Example
 * Demonstrates how to use the cloud execution layer for cross-platform testing
 */
public class CloudTestingExample {
    
    private CloudTestManager cloudTestManager;
    
    @BeforeClass
    public void setupCloudTesting() {
        TestLogManager.info("Setting up cloud testing example...");
        
        // Initialize cloud test manager
        cloudTestManager = new CloudTestManager();
        
        // Check if cloud testing is enabled
        if (!cloudTestManager.isCloudTestingEnabled()) {
            TestLogManager.warning("Cloud testing is not enabled. Please configure cloud settings in config.properties");
            return;
        }
        
        // Validate credentials
        if (!cloudTestManager.validateCredentials()) {
            TestLogManager.error("Cloud credentials validation failed. Please check your username and access key.");
            return;
        }
        
        // Print available browsers and platforms
        TestLogManager.info("Available browsers: " + cloudTestManager.getAvailableBrowsers());
        TestLogManager.info("Available platforms: " + cloudTestManager.getAvailablePlatforms());
        
        // Start cloud testing
        cloudTestManager.startCloudTesting();
        
        TestLogManager.success("Cloud testing setup completed");
    }
    
    @AfterClass
    public void teardownCloudTesting() {
        if (cloudTestManager != null) {
            TestLogManager.info("Tearing down cloud testing...");
            
            // Generate reports
            try {
                String htmlReport = cloudTestManager.generateCloudReport();
                String excelReport = cloudTestManager.generateExcelReport();
                String jsonReport = cloudTestManager.generateJsonReport();
                
                TestLogManager.info("Reports generated:");
                TestLogManager.info("- HTML Report: " + htmlReport);
                TestLogManager.info("- Excel Report: " + excelReport);
                TestLogManager.info("- JSON Report: " + jsonReport);
            } catch (Exception e) {
                TestLogManager.warning("Failed to generate reports: " + e.getMessage());
            }
            
            // Stop cloud testing
            cloudTestManager.stopCloudTesting();
            cloudTestManager.cleanup();
            
            TestLogManager.success("Cloud testing teardown completed");
        }
    }
    
    @Test(description = "Single Web Test on Cloud")
    public void testSingleWebTestOnCloud() {
        if (!cloudTestManager.isCloudTestingRunning()) {
            TestLogManager.warning("Skipping test - cloud testing not running");
            return;
        }
        
        TestLogManager.info("Executing single web test on cloud...");
        
        try {
            // Execute a simple web test on Chrome Windows
            Future<CloudExecutionEngine.ExecutionResult> future = cloudTestManager.executeWebTest(
                "single_web_test",
                "Single Web Test - Chrome Windows",
                "chrome",
                "Windows 10",
                "latest",
                this::performWebTest
            );
            
            // Wait for completion
            Map<String, Future<CloudExecutionEngine.ExecutionResult>> futures = new HashMap<>();
            futures.put("single_web_test", future);
            
            Map<String, CloudExecutionEngine.ExecutionResult> results = 
                cloudTestManager.waitForTestCompletion(futures, 5, TimeUnit.MINUTES);
            
            // Check result
            CloudExecutionEngine.ExecutionResult result = results.get("single_web_test");
            if (result.isSuccess()) {
                TestLogManager.success("Single web test completed successfully");
            } else {
                TestLogManager.error("Single web test failed: " + result.getException().getMessage());
            }
            
        } catch (Exception e) {
            TestLogManager.error("Failed to execute single web test", e);
        }
    }
    
    @Test(description = "Cross-Browser Test Suite")
    public void testCrossBrowserTestSuite() {
        if (!cloudTestManager.isCloudTestingRunning()) {
            TestLogManager.warning("Skipping test - cloud testing not running");
            return;
        }
        
        TestLogManager.info("Executing cross-browser test suite...");
        
        try {
            // Run cross-browser test suite
            Map<String, Future<CloudExecutionEngine.ExecutionResult>> futures = 
                cloudTestManager.runCrossBrowserTestSuite(
                    "cross_browser_suite",
                    "Cross-Browser Test Suite",
                    this::performWebTest
                );
            
            // Wait for completion
            Map<String, CloudExecutionEngine.ExecutionResult> results = 
                cloudTestManager.waitForTestCompletion(futures, 10, TimeUnit.MINUTES);
            
            // Analyze results
            int successCount = 0;
            int failureCount = 0;
            
            for (Map.Entry<String, CloudExecutionEngine.ExecutionResult> entry : results.entrySet()) {
                String testName = entry.getKey();
                CloudExecutionEngine.ExecutionResult result = entry.getValue();
                
                if (result.isSuccess()) {
                    successCount++;
                    TestLogManager.success("Test passed: " + testName);
                } else {
                    failureCount++;
                    TestLogManager.error("Test failed: " + testName + " - " + result.getException().getMessage());
                }
            }
            
            TestLogManager.info(String.format("Cross-browser test suite completed: %d passed, %d failed", 
                successCount, failureCount));
            
        } catch (Exception e) {
            TestLogManager.error("Failed to execute cross-browser test suite", e);
        }
    }
    
    @Test(description = "Mobile Test Suite")
    public void testMobileTestSuite() {
        if (!cloudTestManager.isCloudTestingRunning()) {
            TestLogManager.warning("Skipping test - cloud testing not running");
            return;
        }
        
        TestLogManager.info("Executing mobile test suite...");
        
        try {
            // Run mobile test suite
            Map<String, Future<CloudExecutionEngine.ExecutionResult>> futures = 
                cloudTestManager.runMobileTestSuite(
                    "mobile_test_suite",
                    "Mobile Test Suite",
                    this::performMobileTest
                );
            
            // Wait for completion
            Map<String, CloudExecutionEngine.ExecutionResult> results = 
                cloudTestManager.waitForTestCompletion(futures, 10, TimeUnit.MINUTES);
            
            // Analyze results
            int successCount = 0;
            int failureCount = 0;
            
            for (Map.Entry<String, CloudExecutionEngine.ExecutionResult> entry : results.entrySet()) {
                String testName = entry.getKey();
                CloudExecutionEngine.ExecutionResult result = entry.getValue();
                
                if (result.isSuccess()) {
                    successCount++;
                    TestLogManager.success("Mobile test passed: " + testName);
                } else {
                    failureCount++;
                    TestLogManager.error("Mobile test failed: " + testName + " - " + result.getException().getMessage());
                }
            }
            
            TestLogManager.info(String.format("Mobile test suite completed: %d passed, %d failed", 
                successCount, failureCount));
            
        } catch (Exception e) {
            TestLogManager.error("Failed to execute mobile test suite", e);
        }
    }
    
    @Test(description = "Custom Parallel Tests")
    public void testCustomParallelTests() {
        if (!cloudTestManager.isCloudTestingRunning()) {
            TestLogManager.warning("Skipping test - cloud testing not running");
            return;
        }
        
        TestLogManager.info("Executing custom parallel tests...");
        
        try {
            // Create custom test tasks
            List<WebTestExecutionTask> tasks = new ArrayList<>();
            
            // Test 1: Chrome on Windows
            tasks.add(cloudTestManager.createWebTestTask(
                "custom_chrome_win",
                "Custom Test - Chrome Windows",
                "chrome", "Windows 10", "latest",
                driver -> {
                    driver.get("https://www.google.com");
                    WebElement searchBox = driver.findElement(By.name("q"));
                    searchBox.sendKeys("Selenium Testing");
                    searchBox.submit();
                    TestLogManager.info("Chrome Windows test completed");
                }
            ));
            
            // Test 2: Firefox on macOS
            tasks.add(cloudTestManager.createWebTestTask(
                "custom_firefox_mac",
                "Custom Test - Firefox macOS",
                "firefox", "macOS Big Sur", "latest",
                driver -> {
                    driver.get("https://www.github.com");
                    WebElement searchBox = driver.findElement(By.name("q"));
                    searchBox.sendKeys("Test Automation");
                    searchBox.submit();
                    TestLogManager.info("Firefox macOS test completed");
                }
            ));
            
            // Test 3: Safari on macOS
            tasks.add(cloudTestManager.createWebTestTask(
                "custom_safari_mac",
                "Custom Test - Safari macOS",
                "safari", "macOS Big Sur", "latest",
                driver -> {
                    driver.get("https://www.stackoverflow.com");
                    WebElement searchBox = driver.findElement(By.name("q"));
                    searchBox.sendKeys("Java Testing");
                    searchBox.submit();
                    TestLogManager.info("Safari macOS test completed");
                }
            ));
            
            // Execute tasks in parallel
            Map<String, Future<CloudExecutionEngine.ExecutionResult>> futures = 
                cloudTestManager.executeTestsInParallel(new ArrayList<>(tasks));
            
            // Wait for completion
            Map<String, CloudExecutionEngine.ExecutionResult> results = 
                cloudTestManager.waitForTestCompletion(futures, 8, TimeUnit.MINUTES);
            
            // Analyze results
            int successCount = 0;
            for (CloudExecutionEngine.ExecutionResult result : results.values()) {
                if (result.isSuccess()) {
                    successCount++;
                }
            }
            
            TestLogManager.info(String.format("Custom parallel tests completed: %d/%d passed", 
                successCount, results.size()));
            
        } catch (Exception e) {
            TestLogManager.error("Failed to execute custom parallel tests", e);
        }
    }
    
    @Test(description = "Performance Monitoring Test")
    public void testPerformanceMonitoring() {
        if (!cloudTestManager.isCloudTestingRunning()) {
            TestLogManager.warning("Skipping test - cloud testing not running");
            return;
        }
        
        TestLogManager.info("Testing performance monitoring...");
        
        try {
            // Execute test with performance monitoring
            Future<CloudExecutionEngine.ExecutionResult> future = cloudTestManager.executeWebTest(
                "performance_test",
                "Performance Monitoring Test",
                "chrome",
                "Windows 10",
                "latest",
                this::performPerformanceTest
            );
            
            // Wait for completion
            Map<String, Future<CloudExecutionEngine.ExecutionResult>> futures = new HashMap<>();
            futures.put("performance_test", future);
            
            Map<String, CloudExecutionEngine.ExecutionResult> results = 
                cloudTestManager.waitForTestCompletion(futures, 5, TimeUnit.MINUTES);
            
            // Get execution statistics
            CloudExecutionEngine.ExecutionStatistics stats = cloudTestManager.getExecutionStatistics();
            TestLogManager.info("Execution Statistics:");
            TestLogManager.info("- Total Sessions: " + stats.getTotalSessions());
            TestLogManager.info("- Success Rate: " + stats.getSuccessRate() + "%");
            TestLogManager.info("- Failed Executions: " + stats.getFailedExecutions());
            TestLogManager.info("- Max Parallel Sessions: " + stats.getMaxParallelSessions());
            
            // Get dashboard data
            Map<String, Object> dashboardData = cloudTestManager.getDashboardData();
            TestLogManager.info("Dashboard data generated for " + dashboardData.size() + " metrics");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to execute performance monitoring test", e);
        }
    }
    
    /**
     * Perform a simple web test
     */
    private void performWebTest(WebDriver driver) {
        TestLogManager.info("Starting web test execution...");
        
        try {
            // Navigate to a test website
            driver.get("https://www.example.com");
            
            // Perform some basic interactions
            WebElement heading = driver.findElement(By.tagName("h1"));
            String headingText = heading.getText();
            TestLogManager.info("Page heading: " + headingText);
            
            // Take a screenshot
            // ScreenshotUtil.captureScreenshot(driver, "cloud_test_screenshot");
            
            // Simulate some test actions
            Thread.sleep(2000);
            
            TestLogManager.success("Web test completed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Web test failed", e);
            throw new RuntimeException("Web test failed", e);
        }
    }
    
    /**
     * Perform a mobile test
     */
    private void performMobileTest(WebDriver driver) {
        TestLogManager.info("Starting mobile test execution...");
        
        try {
            // Navigate to a mobile-friendly website
            driver.get("https://m.wikipedia.org");
            
            // Perform mobile-specific interactions
            WebElement searchBox = driver.findElement(By.name("search"));
            searchBox.sendKeys("Mobile Testing");
            
            // Simulate mobile interactions
            Thread.sleep(2000);
            
            TestLogManager.success("Mobile test completed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Mobile test failed", e);
            throw new RuntimeException("Mobile test failed", e);
        }
    }
    
    /**
     * Perform a performance test
     */
    private void performPerformanceTest(WebDriver driver) {
        TestLogManager.info("Starting performance test execution...");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Navigate to a complex website
            driver.get("https://www.github.com");
            
            // Perform multiple operations
            for (int i = 0; i < 5; i++) {
                WebElement searchBox = driver.findElement(By.name("q"));
                searchBox.clear();
                searchBox.sendKeys("Performance Test " + i);
                Thread.sleep(500);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            TestLogManager.info("Performance test duration: " + duration + "ms");
            
            if (duration > 10000) { // 10 seconds
                TestLogManager.warning("Performance test took longer than expected: " + duration + "ms");
            } else {
                TestLogManager.success("Performance test completed within acceptable time: " + duration + "ms");
            }
            
        } catch (Exception e) {
            TestLogManager.error("Performance test failed", e);
            throw new RuntimeException("Performance test failed", e);
        }
    }
}
