package examples;

import utils.CrossPlatformUtils;
import reporting.TestLogManager;

/**
 * Simple runner for CrossPlatformTest functionality.
 * This demonstrates the cross-platform capabilities without TestNG.
 */
public class CrossPlatformTestRunner {
    
    public static void main(String[] args) {
        TestLogManager.info("=== Cross Platform Test Runner ===");
        
        try {
            // Test system detection
            testSystemDetection();
            
            // Test path handling
            testPathHandling();
            
            // Test browser detection
            testBrowserDetection();
            
            // Test environment variables
            testEnvironmentVariables();
            
            // Test command availability
            testCommandAvailability();
            
            // Test file operations
            testFileOperations();
            
            TestLogManager.success("All cross-platform tests completed successfully!");
            
        } catch (Exception e) {
            TestLogManager.error("Cross-platform test failed", e);
        }
    }
    
    private static void testSystemDetection() {
        TestLogManager.info("Testing cross-platform system detection");
        
        // Test OS detection
        CrossPlatformUtils.OperatingSystem os = CrossPlatformUtils.getCurrentOS();
        TestLogManager.info("Detected OS: " + os);
        
        // Test path operations
        String pathSeparator = CrossPlatformUtils.getPathSeparator();
        String lineSeparator = CrossPlatformUtils.getLineSeparator();
        
        TestLogManager.info("Path separator: " + pathSeparator);
        TestLogManager.info("Line separator: " + lineSeparator);
        
        TestLogManager.success("System detection test passed");
    }
    
    private static void testPathHandling() {
        TestLogManager.info("Testing cross-platform path handling");
        
        // Test path creation
        var projectDataPath = CrossPlatformUtils.getProjectDataDirectory();
        var downloadPath = CrossPlatformUtils.getProjectDownloadDirectory();
        var tempPath = CrossPlatformUtils.getTempDirectory();
        
        TestLogManager.info("Project data path: " + projectDataPath);
        TestLogManager.info("Download path: " + downloadPath);
        TestLogManager.info("Temp path: " + tempPath);
        
        // Test path creation with multiple parts
        var customPath = CrossPlatformUtils.createPath("src", "test", "resources", "data");
        TestLogManager.info("Custom path: " + customPath);
        
        TestLogManager.success("Path handling test passed");
    }
    
    private static void testBrowserDetection() {
        TestLogManager.info("Testing cross-platform browser detection");
        
        String[] browsers = {"chrome", "firefox", "edge"};
        
        for (String browser : browsers) {
            var browserPath = CrossPlatformUtils.getBrowserExecutablePath(browser);
            if (browserPath != null) {
                TestLogManager.info("Found " + browser + " at: " + browserPath);
            } else {
                TestLogManager.info(browser + " not found in standard location");
            }
        }
        
        TestLogManager.success("Browser detection test completed");
    }
    
    private static void testEnvironmentVariables() {
        TestLogManager.info("Testing cross-platform environment variables");
        
        var envVars = CrossPlatformUtils.getOSEnvironmentVariables();
        
        TestLogManager.info("Environment variables:");
        envVars.forEach((key, value) -> 
            TestLogManager.info("  " + key + " = " + value)
        );
        
        TestLogManager.success("Environment variables test passed");
    }
    
    private static void testCommandAvailability() {
        TestLogManager.info("Testing cross-platform command availability");
        
        // Test common commands
        String[] commands = {"java", "echo", "ls", "dir"};
        
        for (String command : commands) {
            boolean available = CrossPlatformUtils.isCommandAvailable(command);
            TestLogManager.info("Command '" + command + "' available: " + available);
        }
        
        TestLogManager.success("Command availability test passed");
    }
    
    private static void testFileOperations() {
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
            
            TestLogManager.success("Cross-platform file operations test passed");
            
        } catch (Exception e) {
            TestLogManager.error("File operations test failed", e);
            throw e;
        }
    }
}
