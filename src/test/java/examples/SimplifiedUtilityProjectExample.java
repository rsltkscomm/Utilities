package examples;

import base.ModernBaseTest;
import patterns.builder.TestConfiguration;
import patterns.command.*;
import patterns.repository.TestData;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.testng.annotations.Test;

/**
 * Simplified Utility Project Example - Demonstrates core capabilities without external dependencies.
 * 
 * This example showcases:
 * - Modern Architecture Patterns (Strategy, Builder, Repository, Command, Facade, DI)
 * - Multi-OS Support
 * - Core Testing Capabilities
 * - Cross-Platform Testing
 */
public class SimplifiedUtilityProjectExample extends ModernBaseTest {

    @Test
    public void testCompleteFramework() {
        TestLogManager.info("=== Starting Simplified Framework Demonstration ===");
        
        try {
            // 1. Modern Architecture Patterns
            demonstrateModernArchitecture();
            
            // 2. Multi-OS Support
            demonstrateMultiOSSupport();
            
            // 3. Core Testing Capabilities
            demonstrateCoreTesting();
            
            TestLogManager.success("Simplified framework test completed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Simplified framework test failed", e);
            throw e;
        } finally {
            cleanup();
        }
    }

    private void demonstrateModernArchitecture() {
        TestLogManager.info("=== Demonstrating Modern Architecture Patterns ===");
        
        try {
            // 1. Strategy Pattern - Driver Management
            TestLogManager.info("1. Strategy Pattern - Driver Management");
            TestConfiguration config = new TestConfiguration.Builder()
                .browser("chrome")
                .headless(false)
                .environment("test")
                .timeoutSeconds(30)
                .build();
            TestLogManager.info("Created config for browser: " + config.getBrowser());
            
            // 2. Builder Pattern - Configuration
            TestLogManager.info("2. Builder Pattern - Configuration");
            TestConfiguration customConfig = new TestConfiguration.Builder()
                .browser("firefox")
                .headless(true)
                .environment("staging")
                .timeoutSeconds(60)
                .build();
            TestLogManager.info("Created custom config for browser: " + customConfig.getBrowser());
            
            // 3. Repository Pattern - Data Access
            TestLogManager.info("3. Repository Pattern - Data Access");
            TestData testData = getTestData("sampleTest");
            TestLogManager.info("Retrieved test data: " + testData.getData("username"));
            
            // 4. Command Pattern - UI Actions
            TestLogManager.info("4. Command Pattern - UI Actions");
            if (getDriver() != null) {
                getDriver().get("https://example.com");
                
                CommandInvoker invoker = new CommandInvoker();
                
                // Create commands
                NavigationCommand navCommand = new NavigationCommand(getDriver(), NavigationCommand.NavigationType.GET, "https://www.bbc.com/weather/1264527", "Navigate to BBC Weather");
                // Note: ClickCommand and InputCommand require WebElement, not String selectors
                // For demonstration, we'll just show the navigation command
                
                // Execute commands
                invoker.executeCommand(navCommand);
                TestLogManager.info("Navigation command executed");
            }
            
            TestLogManager.success("Modern Architecture Patterns demonstrated successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Modern Architecture demonstration failed", e);
            throw e;
        }
    }

    private void demonstrateMultiOSSupport() {
        TestLogManager.info("=== Demonstrating Multi-OS Support ===");
        
        try {
            // 1. OS Detection
            TestLogManager.info("1. OS Detection");
            CrossPlatformUtils.OperatingSystem currentOS = CrossPlatformUtils.getCurrentOS();
            TestLogManager.info("Current OS: " + currentOS);
            
            // 2. Path Handling
            TestLogManager.info("2. Cross-Platform Path Handling");
            Path testPath = CrossPlatformUtils.getProjectDataDirectory();
            TestLogManager.info("Built path: " + testPath);
            
            // 3. Environment Variables
            TestLogManager.info("3. Environment Variables");
            Map<String, String> envVars = CrossPlatformUtils.getEnvironmentVariables();
            String javaHome = envVars.get("JAVA_HOME");
            TestLogManager.info("JAVA_HOME: " + (javaHome != null ? javaHome : "Not set"));
            
            // 4. File Operations
            TestLogManager.info("4. File Operations");
            Path pomPath = Paths.get("pom.xml");
            boolean isFile = Files.exists(pomPath) && Files.isRegularFile(pomPath);
            TestLogManager.info("pom.xml is file: " + isFile);
            
            Path srcPath = Paths.get("src");
            boolean isDirectory = Files.exists(srcPath) && Files.isDirectory(srcPath);
            TestLogManager.info("src is directory: " + isDirectory);
            
            TestLogManager.success("Multi-OS Support demonstrated successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Multi-OS Support demonstration failed", e);
            throw e;
        }
    }

    private void demonstrateCoreTesting() {
        TestLogManager.info("=== Demonstrating Core Testing Capabilities ===");
        
        try {
            // 1. Test Data Management
            TestLogManager.info("1. Test Data Management");
            TestData data = getTestData("coreTest");
            TestLogManager.info("Retrieved core test data: " + data.getData("testName"));
            
            // 2. Configuration Management
            TestLogManager.info("2. Configuration Management");
            TestConfiguration coreConfig = new TestConfiguration.Builder()
                .browser("chrome")
                .headless(true)
                .environment("core")
                .timeoutSeconds(45)
                .build();
            TestLogManager.info("Core configuration created for browser: " + coreConfig.getBrowser());
            
            // 3. Logging and Reporting
            TestLogManager.info("3. Logging and Reporting");
            TestLogManager.info("This is an info message");
            TestLogManager.warning("This is a warning message");
            TestLogManager.error("This is an error message (for demonstration)");
            
            // 4. Basic Web Operations (if driver available)
            if (getDriver() != null) {
                TestLogManager.info("4. Basic Web Operations");
                getDriver().get("https://httpbin.org/get");
                String title = getDriver().getTitle();
                TestLogManager.info("Page title: " + title);
            }
            
            TestLogManager.success("Core Testing Capabilities demonstrated successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Core Testing demonstration failed", e);
            throw e;
        }
    }

    private void cleanup() {
        TestLogManager.info("Cleaning up resources...");
        try {
            if (getDriver() != null) {
                getDriver().quit();
            }
        } catch (Exception e) {
            TestLogManager.warning("Error during cleanup: " + e.getMessage());
        }
    }
}

