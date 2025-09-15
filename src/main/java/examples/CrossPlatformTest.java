package examples;

import base.ModernBaseTest;
import patterns.repository.TestData;
import patterns.repository.TestResult;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Example test class demonstrating cross-platform capabilities.
 * This test validates that the framework works correctly across different operating systems.
 */
public class CrossPlatformTest extends ModernBaseTest {
    
    @BeforeClass
    public void beforeClass() {
        // Log system information for debugging
        CrossPlatformUtils.logSystemInfo();
    }
    
    @Test(description = "Test cross-platform system detection")
    public void testSystemDetection() {
        TestLogManager.info("Testing cross-platform system detection");
        
        // Test OS detection
        CrossPlatformUtils.OperatingSystem os = CrossPlatformUtils.getCurrentOS();
        TestLogManager.info("Detected OS: " + os);
        
        // Verify OS is detected
        assert os != CrossPlatformUtils.OperatingSystem.UNKNOWN : "OS should be detected";
        
        // Test path operations
        String pathSeparator = CrossPlatformUtils.getPathSeparator();
        String lineSeparator = CrossPlatformUtils.getLineSeparator();
        
        TestLogManager.info("Path separator: " + pathSeparator);
        TestLogManager.info("Line separator: " + lineSeparator);
        
        // Verify path separator is correct for OS
        switch (os) {
            case WINDOWS:
                assert pathSeparator.equals("\\") : "Windows should use backslash";
                break;
            case MACOS:
            case LINUX:
                assert pathSeparator.equals("/") : "Unix-like systems should use forward slash";
                break;
        }
        
        TestLogManager.success("System detection test passed");
    }
    
    @Test(description = "Test cross-platform path handling")
    public void testPathHandling() {
        TestLogManager.info("Testing cross-platform path handling");
        
        // Test path creation
        var projectDataPath = CrossPlatformUtils.getProjectDataDirectory();
        var downloadPath = CrossPlatformUtils.getProjectDownloadDirectory();
        var tempPath = CrossPlatformUtils.getTempDirectory();
        
        TestLogManager.info("Project data path: " + projectDataPath);
        TestLogManager.info("Download path: " + downloadPath);
        TestLogManager.info("Temp path: " + tempPath);
        
        // Verify paths are valid
        assert projectDataPath != null : "Project data path should not be null";
        assert downloadPath != null : "Download path should not be null";
        assert tempPath != null : "Temp path should not be null";
        
        // Test path creation with multiple parts
        var customPath = CrossPlatformUtils.createPath("src", "test", "resources", "data");
        TestLogManager.info("Custom path: " + customPath);
        
        assert customPath != null : "Custom path should not be null";
        
        TestLogManager.success("Path handling test passed");
    }
    
    @Test(description = "Test cross-platform browser detection")
    public void testBrowserDetection() {
        TestLogManager.info("Testing cross-platform browser detection");
        
        String[] browsers = {"chrome", "firefox", "edge"};
        
        for (String browser : browsers) {
            var browserPath = CrossPlatformUtils.getBrowserExecutablePath(browser);
            if (browserPath != null) {
                TestLogManager.info("Found " + browser + " at: " + browserPath);
                assert browserPath.toFile().exists() : browser + " executable should exist";
            } else {
                TestLogManager.info(browser + " not found in standard location");
            }
        }
        
        TestLogManager.success("Browser detection test completed");
    }
    
    @Test(description = "Test cross-platform environment variables")
    public void testEnvironmentVariables() {
        TestLogManager.info("Testing cross-platform environment variables");
        
        var envVars = CrossPlatformUtils.getOSEnvironmentVariables();
        
        TestLogManager.info("Environment variables:");
        envVars.forEach((key, value) -> 
            TestLogManager.info("  " + key + " = " + value)
        );
        
        // Verify essential environment variables exist
        CrossPlatformUtils.OperatingSystem os = CrossPlatformUtils.getCurrentOS();
        switch (os) {
            case WINDOWS:
                assert envVars.containsKey("PATH") : "PATH should exist on Windows";
                assert envVars.containsKey("USERPROFILE") : "USERPROFILE should exist on Windows";
                break;
            case MACOS:
            case LINUX:
                assert envVars.containsKey("PATH") : "PATH should exist on Unix-like systems";
                assert envVars.containsKey("HOME") : "HOME should exist on Unix-like systems";
                break;
        }
        
        TestLogManager.success("Environment variables test passed");
    }
    
    @Test(description = "Test cross-platform command availability")
    public void testCommandAvailability() {
        TestLogManager.info("Testing cross-platform command availability");
        
        // Test common commands
        String[] commands = {"java", "echo", "ls", "dir"};
        
        for (String command : commands) {
            boolean available = CrossPlatformUtils.isCommandAvailable(command);
            TestLogManager.info("Command '" + command + "' available: " + available);
        }
        
        // Java should always be available
        assert CrossPlatformUtils.isCommandAvailable("java") : "Java command should be available";
        
        TestLogManager.success("Command availability test passed");
    }
    
    @Test(description = "Test cross-platform test execution")
    public void testCrossPlatformExecution() {
        TestLogManager.info("Testing cross-platform test execution");
        
        // Create test data with OS-specific information
        Map<String, String> testDataMap = new HashMap<>();
        testDataMap.put("os", CrossPlatformUtils.getCurrentOS().name());
        testDataMap.put("osName", System.getProperty("os.name"));
        testDataMap.put("osVersion", System.getProperty("os.version"));
        testDataMap.put("javaVersion", System.getProperty("java.version"));
        testDataMap.put("userName", System.getProperty("user.name"));
        
        TestData testData = createTestData("crossPlatformTest", testDataMap);
        
        // Execute test with cross-platform data
        TestResult result = executeTestWithData("crossPlatformTest", testData);
        
        // Verify result
        if (result.isPassed()) {
            TestLogManager.success("Cross-platform test execution passed");
        } else {
            TestLogManager.error("Cross-platform test execution failed: " + result.getErrorMessage());
        }
        
        // Verify test data contains OS information
        assert testData.getData("os") != null : "OS information should be in test data";
        assert testData.getData("osName") != null : "OS name should be in test data";
    }
    
    @Test(description = "Test cross-platform configuration")
    public void testCrossPlatformConfiguration() {
        TestLogManager.info("Testing cross-platform configuration");
        
        // Get configuration
        var config = getConfiguration();
        
        // Verify configuration is valid for current OS
        assert config.getBrowser() != null : "Browser should be configured";
        assert config.getEnvironment() != null : "Environment should be configured";
        assert config.getTimeout() != null : "Timeout should be configured";
        
        TestLogManager.info("Configuration valid for " + CrossPlatformUtils.getCurrentOS());
        TestLogManager.success("Cross-platform configuration test passed");
    }
    
    @Test(description = "Test cross-platform file operations")
    public void testCrossPlatformFileOperations() {
        TestLogManager.info("Testing cross-platform file operations");
        
        try {
            // Test creating directories
            var dataDir = CrossPlatformUtils.getProjectDataDirectory();
            var downloadDir = CrossPlatformUtils.getProjectDownloadDirectory();
            
            // Create directories if they don't exist
            if (!dataDir.toFile().exists()) {
                dataDir.toFile().mkdirs();
                TestLogManager.info("Created data directory: " + dataDir);
            }
            
            if (!downloadDir.toFile().exists()) {
                downloadDir.toFile().mkdirs();
                TestLogManager.info("Created download directory: " + downloadDir);
            }
            
            // Verify directories exist
            assert dataDir.toFile().exists() : "Data directory should exist";
            assert downloadDir.toFile().exists() : "Download directory should exist";
            
            TestLogManager.success("Cross-platform file operations test passed");
            
        } catch (Exception e) {
            TestLogManager.error("File operations test failed", e);
            throw e;
        }
    }
}
