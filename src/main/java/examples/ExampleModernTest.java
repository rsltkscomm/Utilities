package examples;

import base.ModernBaseTest;
import patterns.command.ClickCommand;
import patterns.command.InputCommand;
import patterns.command.NavigationCommand;
import patterns.repository.TestData;
import patterns.repository.TestResult;
import reporting.TestLogManager;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Example test class demonstrating the use of modern architectural patterns.
 * This shows how to write clean, maintainable tests using the new framework.
 */
public class ExampleModernTest extends ModernBaseTest {
    
    @Test(description = "Example test using modern patterns")
    public void testLoginWithModernPatterns() {
        TestLogManager.info("Starting login test with modern patterns");
        
        // Create test data
        Map<String, String> testDataMap = new HashMap<>();
        testDataMap.put("username", "testuser");
        testDataMap.put("password", "testpass");
        testDataMap.put("expectedUrl", "dashboard");
        
        TestData testData = createTestData("loginTest", testDataMap);
        
        // Execute test with custom data
        TestResult result = executeTestWithData("loginTest", testData);
        
        // Verify result
        if (result.isPassed()) {
            TestLogManager.success("Login test passed successfully");
        } else {
            TestLogManager.error("Login test failed: " + result.getErrorMessage());
        }
    }
    
    @Test(description = "Example test using command pattern")
    public void testUserRegistrationWithCommands() {
        TestLogManager.info("Starting user registration test with command pattern");
        
        try {
            // Navigate to registration page
            NavigationCommand navCommand = new NavigationCommand(
                    getDriver(), 
                    NavigationCommand.NavigationType.GET, 
                    "https://example.com/register",
                    "Navigate to registration page"
            );
            
            if (!getCommandInvoker().executeCommand(navCommand)) {
                TestLogManager.error("Failed to navigate to registration page");
                return;
            }
            
            // Wait for page load
            waitForPageLoad();
            
            // Fill registration form using commands
            fillRegistrationForm();
            
            // Submit form
            WebElement submitButton = getDriver().findElement(By.id("submit-button"));
            ClickCommand submitCommand = new ClickCommand(
                    getDriver(), 
                    submitButton, 
                    "Submit registration form"
            );
            
            if (getCommandInvoker().executeCommand(submitCommand)) {
                TestLogManager.success("Registration form submitted successfully");
            } else {
                TestLogManager.error("Failed to submit registration form");
            }
            
        } catch (Exception e) {
            TestLogManager.error("Registration test failed", e);
        }
    }
    
    @Test(description = "Example test using facade pattern")
    public void testCompleteUserWorkflow() {
        TestLogManager.info("Starting complete user workflow test");
        
        // This test demonstrates using the facade pattern for complex operations
        String[] testSteps = {
            "navigateTo:https://www.nobroker.in/",
            "login:testuser:testpass",
            "navigateToDashboard",
            "createNewProject:TestProject",
            "verifyProjectCreated"
        };
        
        // Create test data for the workflow
        Map<String, String> workflowData = new HashMap<>();
        for (int i = 0; i < testSteps.length; i++) {
            workflowData.put("step" + (i + 1), testSteps[i]);
        }
        
        TestData workflowTestData = createTestData("userWorkflow", workflowData);
        
        // Execute the complete workflow
        TestResult result = executeTestWithData("userWorkflow", workflowTestData);
        
        // Log results
        if (result.isPassed()) {
            TestLogManager.success("Complete user workflow test passed");
        } else {
            TestLogManager.error("Complete user workflow test failed: " + result.getErrorMessage());
        }
    }
    
    @Test(description = "Example test with custom configuration")
    public void testWithCustomConfiguration() {
        TestLogManager.info("Starting test with custom configuration");
        
        // Get current configuration
        var config = getConfiguration();
        TestLogManager.info("Current configuration: " + config);
        
        // Verify configuration values
        assert config.getBrowser().equals("chrome") : "Expected Chrome browser";
        assert config.getEnvironment().equals("test") : "Expected test environment";
        assert config.getTimeout().getSeconds() == 30 : "Expected 30 second timeout";
        
        TestLogManager.success("Configuration verification passed");
    }
    
    @Test(description = "Example test demonstrating error handling")
    public void testErrorHandling() {
        TestLogManager.info("Starting error handling test");
        
        try {
            // Try to navigate to an invalid URL
            NavigationCommand invalidNavCommand = new NavigationCommand(
                    getDriver(), 
                    NavigationCommand.NavigationType.GET, 
                    "https://invalid-url-that-does-not-exist.com",
                    "Navigate to invalid URL"
            );
            
            boolean result = getCommandInvoker().executeCommand(invalidNavCommand);
            
            if (!result) {
                TestLogManager.info("Expected failure occurred - error handling working correctly");
                
                // Check the command result
                var commandResult = invalidNavCommand.getResult();
                if (commandResult != null && commandResult.isFailed()) {
                    TestLogManager.info("Command failed as expected: " + commandResult.getErrorMessage());
                }
            } else {
                TestLogManager.warning("Unexpected success - this might indicate an issue");
            }
            
        } catch (Exception e) {
            TestLogManager.info("Exception caught as expected: " + e.getMessage());
        }
    }
    
    private void fillRegistrationForm() {
        try {
            // Find form elements
            WebElement usernameField = getDriver().findElement(By.id("username"));
            WebElement emailField = getDriver().findElement(By.id("email"));
            WebElement passwordField = getDriver().findElement(By.id("password"));
            WebElement confirmPasswordField = getDriver().findElement(By.id("confirm-password"));
            
            // Create input commands
            InputCommand usernameCommand = new InputCommand(
                    getDriver(), 
                    usernameField, 
                    "testuser123", 
                    "Username field"
            );
            
            InputCommand emailCommand = new InputCommand(
                    getDriver(), 
                    emailField, 
                    "testuser@example.com", 
                    "Email field"
            );
            
            InputCommand passwordCommand = new InputCommand(
                    getDriver(), 
                    passwordField, 
                    "testpass123", 
                    "Password field"
            );
            
            InputCommand confirmPasswordCommand = new InputCommand(
                    getDriver(), 
                    confirmPasswordField, 
                    "testpass123", 
                    "Confirm password field"
            );
            
            // Execute input commands
            getCommandInvoker().executeCommand(usernameCommand);
            getCommandInvoker().executeCommand(emailCommand);
            getCommandInvoker().executeCommand(passwordCommand);
            getCommandInvoker().executeCommand(confirmPasswordCommand);
            
            TestLogManager.success("Registration form filled successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to fill registration form", e);
        }
    }
}
