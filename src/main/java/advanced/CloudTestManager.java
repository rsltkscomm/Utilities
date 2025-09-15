package advanced;

import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloud testing manager for BrowserStack, SauceLabs, and LambdaTest integration.
 */
public class CloudTestManager {
    
    private WebDriver driver;
    private final String cloudProvider;
    private final String username;
    private final String accessKey;
    private final String reportDirectory;
    
    public CloudTestManager(String cloudProvider, String username, String accessKey) {
        this.cloudProvider = cloudProvider.toLowerCase();
        this.username = username;
        this.accessKey = accessKey;
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("cloud_reports").toString();
        createReportDirectory();
    }
    
    /**
     * Sets up BrowserStack for cloud testing.
     * @param browser Browser name
     * @param browserVersion Browser version
     * @param os Operating system
     * @param osVersion OS version
     * @return Configured WebDriver for BrowserStack
     */
    public WebDriver setupBrowserStack(String browser, String browserVersion, String os, String osVersion) {
        TestLogManager.info("Setting up BrowserStack for cloud testing");
        
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            
            // BrowserStack specific capabilities
            capabilities.setCapability("browser", browser);
            capabilities.setCapability("browser_version", browserVersion);
            capabilities.setCapability("os", os);
            capabilities.setCapability("os_version", osVersion);
            capabilities.setCapability("browserstack.user", username);
            capabilities.setCapability("browserstack.key", accessKey);
            capabilities.setCapability("browserstack.local", "false");
            capabilities.setCapability("browserstack.debug", "true");
            capabilities.setCapability("browserstack.console", "info");
            capabilities.setCapability("browserstack.networkLogs", "true");
            capabilities.setCapability("browserstack.video", "true");
            capabilities.setCapability("browserstack.timezone", "UTC");
            
            // Build name and session name
            String buildName = "Test Build " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String sessionName = "Test Session " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            capabilities.setCapability("build", buildName);
            capabilities.setCapability("name", sessionName);
            
            // Create remote driver
            URL browserStackUrl = URI.create("https://hub-cloud.browserstack.com/wd/hub").toURL();
            driver = new RemoteWebDriver(browserStackUrl, capabilities);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            TestLogManager.success("BrowserStack setup completed successfully");
            return driver;
            
        } catch (MalformedURLException e) {
            TestLogManager.error("Failed to setup BrowserStack", e);
            throw new RuntimeException("BrowserStack setup failed", e);
        }
    }
    
    /**
     * Sets up SauceLabs for cloud testing.
     * @param browser Browser name
     * @param browserVersion Browser version
     * @param platform Platform name
     * @return Configured WebDriver for SauceLabs
     */
    public WebDriver setupSauceLabs(String browser, String browserVersion, String platform) {
        TestLogManager.info("Setting up SauceLabs for cloud testing");
        
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            
            // SauceLabs specific capabilities
            capabilities.setCapability("browserName", browser);
            capabilities.setCapability("version", browserVersion);
            capabilities.setCapability("platform", platform);
            capabilities.setCapability("username", username);
            capabilities.setCapability("accessKey", accessKey);
            capabilities.setCapability("seleniumVersion", "4.15.0");
            capabilities.setCapability("maxDuration", 1800);
            capabilities.setCapability("commandTimeout", 300);
            capabilities.setCapability("idleTimeout", 1000);
            capabilities.setCapability("videoUploadOnPass", false);
            capabilities.setCapability("recordVideo", true);
            capabilities.setCapability("recordScreenshots", true);
            capabilities.setCapability("recordLogs", true);
            
            // Build name and session name
            String buildName = "Test Build " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String sessionName = "Test Session " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            capabilities.setCapability("build", buildName);
            capabilities.setCapability("name", sessionName);
            
            // Create remote driver
            URL sauceLabsUrl = URI.create("https://ondemand.us-west-1.saucelabs.com:443/wd/hub").toURL();
            driver = new RemoteWebDriver(sauceLabsUrl, capabilities);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            TestLogManager.success("SauceLabs setup completed successfully");
            return driver;
            
        } catch (MalformedURLException e) {
            TestLogManager.error("Failed to setup SauceLabs", e);
            throw new RuntimeException("SauceLabs setup failed", e);
        }
    }
    
    /**
     * Sets up LambdaTest for cloud testing.
     * @param browser Browser name
     * @param browserVersion Browser version
     * @param platform Platform name
     * @param resolution Screen resolution
     * @return Configured WebDriver for LambdaTest
     */
    public WebDriver setupLambdaTest(String browser, String browserVersion, String platform, String resolution) {
        TestLogManager.info("Setting up LambdaTest for cloud testing");
        
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            
            // LambdaTest specific capabilities
            capabilities.setCapability("browserName", browser);
            capabilities.setCapability("version", browserVersion);
            capabilities.setCapability("platform", platform);
            capabilities.setCapability("resolution", resolution);
            capabilities.setCapability("user", username);
            capabilities.setCapability("accessKey", accessKey);
            capabilities.setCapability("build", "Test Build " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            capabilities.setCapability("name", "Test Session " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            capabilities.setCapability("network", true);
            capabilities.setCapability("visual", true);
            capabilities.setCapability("video", true);
            capabilities.setCapability("console", true);
            capabilities.setCapability("terminal", true);
            capabilities.setCapability("tunnel", false);
            capabilities.setCapability("geoLocation", "US");
            capabilities.setCapability("timezone", "UTC");
            
            // Create remote driver
            URL lambdaTestUrl = URI.create("https://" + username + ":" + accessKey + "@hub.lambdatest.com/wd/hub").toURL();
            driver = new RemoteWebDriver(lambdaTestUrl, capabilities);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            TestLogManager.success("LambdaTest setup completed successfully");
            return driver;
            
        } catch (MalformedURLException e) {
            TestLogManager.error("Failed to setup LambdaTest", e);
            throw new RuntimeException("LambdaTest setup failed", e);
        }
    }
    
    /**
     * Sets up mobile cloud testing on BrowserStack.
     * @param device Device name
     * @param osVersion OS version
     * @param appUrl App URL or app ID
     * @return Configured WebDriver for mobile cloud testing
     */
    public WebDriver setupMobileCloudTesting(String device, String osVersion, String appUrl) {
        TestLogManager.info("Setting up mobile cloud testing on " + cloudProvider);
        
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            
            if (cloudProvider.equals("browserstack")) {
                capabilities.setCapability("device", device);
                capabilities.setCapability("os_version", osVersion);
                capabilities.setCapability("app", appUrl);
                capabilities.setCapability("browserstack.user", username);
                capabilities.setCapability("browserstack.key", accessKey);
                capabilities.setCapability("browserstack.appium_version", "1.22.0");
                capabilities.setCapability("browserstack.debug", "true");
                capabilities.setCapability("browserstack.networkLogs", "true");
                capabilities.setCapability("browserstack.video", "true");
                
                URL mobileUrl = URI.create("https://hub-cloud.browserstack.com/wd/hub").toURL();
                driver = new RemoteWebDriver(mobileUrl, capabilities);
                
            } else if (cloudProvider.equals("saucelabs")) {
                capabilities.setCapability("deviceName", device);
                capabilities.setCapability("platformVersion", osVersion);
                capabilities.setCapability("app", appUrl);
                capabilities.setCapability("username", username);
                capabilities.setCapability("accessKey", accessKey);
                capabilities.setCapability("appiumVersion", "1.22.0");
                capabilities.setCapability("recordVideo", true);
                capabilities.setCapability("recordScreenshots", true);
                
                URL mobileUrl = URI.create("https://ondemand.us-west-1.saucelabs.com:443/wd/hub").toURL();
                driver = new RemoteWebDriver(mobileUrl, capabilities);
            }
            
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            TestLogManager.success("Mobile cloud testing setup completed");
            return driver;
            
        } catch (MalformedURLException e) {
            TestLogManager.error("Failed to setup mobile cloud testing", e);
            throw new RuntimeException("Mobile cloud testing setup failed", e);
        }
    }
    
    /**
     * Updates test status on cloud platform.
     * @param passed Test result (true for pass, false for fail)
     * @param reason Reason for test result
     */
    public void updateTestStatus(boolean passed, String reason) {
        TestLogManager.info("Updating test status on " + cloudProvider + ": " + (passed ? "PASSED" : "FAILED"));
        
        try {
            if (driver instanceof RemoteWebDriver) {
                RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
                
                if (cloudProvider.equals("browserstack")) {
                    if (passed) {
                        remoteDriver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"passed\", \"reason\": \"" + reason + "\"}}");
                    } else {
                        remoteDriver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \"" + reason + "\"}}");
                    }
                } else if (cloudProvider.equals("saucelabs")) {
                    if (passed) {
                        remoteDriver.executeScript("sauce:job-result=passed");
                    } else {
                        remoteDriver.executeScript("sauce:job-result=failed");
                        remoteDriver.executeScript("sauce:context=" + reason);
                    }
                } else if (cloudProvider.equals("lambdatest")) {
                    if (passed) {
                        remoteDriver.executeScript("lambda-status=passed");
                    } else {
                        remoteDriver.executeScript("lambda-status=failed");
                    }
                }
            }
            
            TestLogManager.success("Test status updated successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to update test status", e);
        }
    }
    
    /**
     * Adds annotation to cloud test session.
     * @param annotation Annotation text
     */
    public void addTestAnnotation(String annotation) {
        TestLogManager.info("Adding test annotation: " + annotation);
        
        try {
            if (driver instanceof RemoteWebDriver) {
                RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
                
                if (cloudProvider.equals("browserstack")) {
                    remoteDriver.executeScript("browserstack_executor: {\"action\": \"annotate\", \"arguments\": {\"data\": \"" + annotation + "\", \"level\": \"info\"}}");
                } else if (cloudProvider.equals("saucelabs")) {
                    remoteDriver.executeScript("sauce:context=" + annotation);
                } else if (cloudProvider.equals("lambdatest")) {
                    remoteDriver.executeScript("lambda-context=" + annotation);
                }
            }
            
            TestLogManager.success("Test annotation added successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to add test annotation", e);
        }
    }
    
    /**
     * Captures screenshot on cloud platform.
     * @param fileName Screenshot file name
     * @return Path to saved screenshot
     */
    public Path captureCloudScreenshot(String fileName) {
        TestLogManager.info("Capturing screenshot on " + cloudProvider);
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fullFileName = fileName + "_" + cloudProvider + "_" + timestamp + ".png";
            Path screenshotPath = Paths.get(reportDirectory, fullFileName);
            
            // Capture screenshot
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            java.nio.file.Files.write(screenshotPath, screenshot);
            
            TestLogManager.success("Cloud screenshot captured: " + screenshotPath);
            return screenshotPath;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to capture cloud screenshot", e);
            throw new RuntimeException("Cloud screenshot capture failed", e);
        }
    }
    
    /**
     * Gets cloud test session information.
     * @return CloudTestSessionInfo object with session details
     */
    public CloudTestSessionInfo getCloudTestSessionInfo() {
        TestLogManager.info("Getting cloud test session information");
        
        try {
            CloudTestSessionInfo sessionInfo = new CloudTestSessionInfo();
            sessionInfo.setCloudProvider(cloudProvider);
            sessionInfo.setUsername(username);
            sessionInfo.setSessionId(driver.getWindowHandle());
            sessionInfo.setTimestamp(LocalDateTime.now());
            
            if (driver instanceof RemoteWebDriver) {
                RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
                sessionInfo.setCapabilities(remoteDriver.getCapabilities().asMap());
            }
            
            TestLogManager.success("Cloud test session information retrieved");
            return sessionInfo;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to get cloud test session information", e);
            return new CloudTestSessionInfo();
        }
    }
    
    /**
     * Performs cross-browser testing on multiple cloud platforms.
     * @param testConfigurations List of test configurations
     * @return Map of test results by configuration
     */
    public Map<String, CloudTestResult> performCrossBrowserTesting(java.util.List<CloudTestConfiguration> testConfigurations) {
        TestLogManager.info("Starting cross-browser testing on cloud platforms");
        
        Map<String, CloudTestResult> results = new HashMap<>();
        
        for (CloudTestConfiguration config : testConfigurations) {
            try {
                TestLogManager.info("Testing configuration: " + config.getConfigurationName());
                
                // Setup driver based on configuration
                WebDriver testDriver = setupDriverForConfiguration(config);
                
                // Perform test
                CloudTestResult result = performTestOnConfiguration(testDriver, config);
                results.put(config.getConfigurationName(), result);
                
                // Cleanup
                testDriver.quit();
                
            } catch (Exception e) {
                TestLogManager.error("Failed to test configuration: " + config.getConfigurationName(), e);
                CloudTestResult errorResult = new CloudTestResult();
                errorResult.setPassed(false);
                errorResult.setErrorMessage(e.getMessage());
                results.put(config.getConfigurationName(), errorResult);
            }
        }
        
        TestLogManager.success("Cross-browser testing completed");
        return results;
    }
    
    /**
     * Generates cloud test report.
     * @param testResults Map of test results
     * @return Path to generated report
     */
    public Path generateCloudTestReport(Map<String, CloudTestResult> testResults) {
        TestLogManager.info("Generating cloud test report");
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "cloud_test_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader());
            report.append(generateCloudReportSummary(testResults));
            report.append(generateCloudTestResultsTable(testResults));
            report.append(generateCloudRecommendations());
            report.append(generateHTMLFooter());
            
            java.nio.file.Files.write(reportPath, report.toString().getBytes());
            TestLogManager.success("Cloud test report generated: " + reportPath);
            
            return reportPath;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate cloud test report", e);
            throw new RuntimeException("Cloud test report generation failed", e);
        }
    }
    
    /**
     * Closes cloud driver and cleanup.
     */
    public void closeCloudDriver() {
        TestLogManager.info("Closing cloud driver");
        
        try {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
            TestLogManager.success("Cloud driver closed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to close cloud driver", e);
        }
    }
    
    private WebDriver setupDriverForConfiguration(CloudTestConfiguration config) throws MalformedURLException {
        switch (config.getCloudProvider().toLowerCase()) {
            case "browserstack":
                return setupBrowserStack(config.getBrowser(), config.getBrowserVersion(), 
                                       config.getOs(), config.getOsVersion());
            case "saucelabs":
                return setupSauceLabs(config.getBrowser(), config.getBrowserVersion(), config.getPlatform());
            case "lambdatest":
                return setupLambdaTest(config.getBrowser(), config.getBrowserVersion(), 
                                     config.getPlatform(), config.getResolution());
            default:
                throw new IllegalArgumentException("Unsupported cloud provider: " + config.getCloudProvider());
        }
    }
    
    private CloudTestResult performTestOnConfiguration(WebDriver testDriver, CloudTestConfiguration config) {
        CloudTestResult result = new CloudTestResult();
        result.setConfigurationName(config.getConfigurationName());
        result.setCloudProvider(config.getCloudProvider());
        result.setStartTime(LocalDateTime.now());
        
        try {
            // Navigate to test URL
            testDriver.get(config.getTestUrl());
            
            // Perform basic validation
            String title = testDriver.getTitle();
            result.setPageTitle(title);
            
            // Check if page loaded successfully
            if (title != null && !title.isEmpty()) {
                result.setPassed(true);
                result.setErrorMessage(null);
            } else {
                result.setPassed(false);
                result.setErrorMessage("Page title is empty");
            }
            
        } catch (Exception e) {
            result.setPassed(false);
            result.setErrorMessage(e.getMessage());
        }
        
        result.setEndTime(LocalDateTime.now());
        return result;
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
                TestLogManager.info("Created cloud report directory: " + reportDirectory);
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to create cloud report directory", e);
        }
    }
    
    private String generateHTMLHeader() {
        return "<!DOCTYPE html><html><head><title>Cloud Test Report</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:20px;}table{border-collapse:collapse;width:100%;}" +
               "th,td{border:1px solid #ddd;padding:8px;text-align:left;}th{background-color:#f2f2f2;}" +
               ".passed{color:green;}.failed{color:red;}</style></head><body>";
    }
    
    private String generateCloudReportSummary(Map<String, CloudTestResult> testResults) {
        int totalTests = testResults.size();
        long passedTests = testResults.values().stream().mapToLong(r -> r.isPassed() ? 1 : 0).sum();
        long failedTests = totalTests - passedTests;
        
        return "<h1>Cloud Test Report Summary</h1>" +
               "<p>Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
               "<p>Total Tests: " + totalTests + "</p>" +
               "<p>Passed: <span class='passed'>" + passedTests + "</span></p>" +
               "<p>Failed: <span class='failed'>" + failedTests + "</span></p>" +
               "<p>Success Rate: " + String.format("%.2f", (double) passedTests / totalTests * 100) + "%</p>";
    }
    
    private String generateCloudTestResultsTable(Map<String, CloudTestResult> testResults) {
        StringBuilder table = new StringBuilder("<h2>Test Results</h2><table><tr><th>Configuration</th><th>Cloud Provider</th><th>Status</th><th>Page Title</th><th>Error Message</th></tr>");
        
        for (CloudTestResult result : testResults.values()) {
            String statusClass = result.isPassed() ? "passed" : "failed";
            String status = result.isPassed() ? "PASSED" : "FAILED";
            
            table.append("<tr>")
                 .append("<td>").append(result.getConfigurationName()).append("</td>")
                 .append("<td>").append(result.getCloudProvider()).append("</td>")
                 .append("<td class='").append(statusClass).append("'>").append(status).append("</td>")
                 .append("<td>").append(result.getPageTitle() != null ? result.getPageTitle() : "N/A").append("</td>")
                 .append("<td>").append(result.getErrorMessage() != null ? result.getErrorMessage() : "N/A").append("</td>")
                 .append("</tr>");
        }
        
        table.append("</table>");
        return table.toString();
    }
    
    private String generateCloudRecommendations() {
        return "<h2>Cloud Testing Recommendations</h2>" +
               "<ul>" +
               "<li>Use parallel execution to reduce test execution time</li>" +
               "<li>Implement proper error handling and retry mechanisms</li>" +
               "<li>Monitor cloud platform usage and costs</li>" +
               "<li>Use appropriate browser and OS combinations</li>" +
               "<li>Implement proper test data management</li>" +
               "</ul>";
    }
    
    private String generateHTMLFooter() {
        return "</body></html>";
    }
    
    /**
     * Cloud test configuration data model.
     */
    public static class CloudTestConfiguration {
        private String configurationName;
        private String cloudProvider;
        private String browser;
        private String browserVersion;
        private String os;
        private String osVersion;
        private String platform;
        private String resolution;
        private String testUrl;
        
        // Getters and setters
        public String getConfigurationName() { return configurationName; }
        public void setConfigurationName(String configurationName) { this.configurationName = configurationName; }
        
        public String getCloudProvider() { return cloudProvider; }
        public void setCloudProvider(String cloudProvider) { this.cloudProvider = cloudProvider; }
        
        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }
        
        public String getBrowserVersion() { return browserVersion; }
        public void setBrowserVersion(String browserVersion) { this.browserVersion = browserVersion; }
        
        public String getOs() { return os; }
        public void setOs(String os) { this.os = os; }
        
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
        
        public String getTestUrl() { return testUrl; }
        public void setTestUrl(String testUrl) { this.testUrl = testUrl; }
    }
    
    /**
     * Cloud test result data model.
     */
    public static class CloudTestResult {
        private String configurationName;
        private String cloudProvider;
        private boolean passed;
        private String errorMessage;
        private String pageTitle;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        // Getters and setters
        public String getConfigurationName() { return configurationName; }
        public void setConfigurationName(String configurationName) { this.configurationName = configurationName; }
        
        public String getCloudProvider() { return cloudProvider; }
        public void setCloudProvider(String cloudProvider) { this.cloudProvider = cloudProvider; }
        
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getPageTitle() { return pageTitle; }
        public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
    
    /**
     * Cloud test session information data model.
     */
    public static class CloudTestSessionInfo {
        private String cloudProvider;
        private String username;
        private String sessionId;
        private Map<String, Object> capabilities;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public String getCloudProvider() { return cloudProvider; }
        public void setCloudProvider(String cloudProvider) { this.cloudProvider = cloudProvider; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public Map<String, Object> getCapabilities() { return capabilities; }
        public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
