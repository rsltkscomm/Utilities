package patterns.command;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import reporting.TestLogManager;

import java.time.Duration;

/**
 * Command for input operations.
 * This implements the Command pattern for element input.
 */
public class InputCommand implements Command {
    
    private final WebDriver driver;
    private final WebElement element;
    private final String inputText;
    private final String description;
    private String previousValue;
    private CommandResult result;
    
    public InputCommand(WebDriver driver, WebElement element, String inputText, String description) {
        this.driver = driver;
        this.element = element;
        this.inputText = inputText;
        this.description = description;
    }
    
    @Override
    public boolean execute() {
        if (driver == null) {
            TestLogManager.error("WebDriver is null, cannot execute input command");
            result = new CommandResult(false, "WebDriver is null");
            return false;
        }
        
        if (element == null) {
            TestLogManager.error("WebElement is null, cannot execute input command");
            result = new CommandResult(false, "WebElement is null");
            return false;
        }
        
        if (inputText == null) {
            TestLogManager.error("Input text is null, cannot execute input command");
            result = new CommandResult(false, "Input text is null");
            return false;
        }
        
        try {
            // Wait for element to be visible and enabled
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.elementToBeClickable(element));
            
            // Store previous value for undo operation
            previousValue = element.getAttribute("value");
            
            // Clear the field first
            element.clear();
            
            // Scroll element into view
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            
            // Input the text
            element.sendKeys(inputText);
            
            TestLogManager.info("Text input successfully: " + description + " - Value: " + inputText);
            result = new CommandResult(true, "Text input successfully");
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Input command failed: " + description, e);
            result = new CommandResult(false, "Input failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        if (driver == null || element == null) {
            TestLogManager.error("Cannot undo input command - driver or element is null");
            return false;
        }
        
        try {
            // Restore previous value
            if (previousValue != null) {
                element.clear();
                element.sendKeys(previousValue);
                TestLogManager.info("Input command undone: " + description);
                return true;
            } else {
                // If no previous value, just clear the field
                element.clear();
                TestLogManager.info("Input command undone (cleared): " + description);
                return true;
            }
            
        } catch (Exception e) {
            TestLogManager.error("Failed to undo input command: " + description, e);
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return description != null ? description : "Input command";
    }
    
    /**
     * Gets the command result.
     * @return CommandResult object
     */
    public CommandResult getResult() {
        return result;
    }
    
    /**
     * Gets the target element.
     * @return WebElement that was used for input
     */
    public WebElement getElement() {
        return element;
    }
    
    /**
     * Gets the input text.
     * @return Input text that was entered
     */
    public String getInputText() {
        return inputText;
    }
    
    /**
     * Gets the previous value.
     * @return Previous value before input
     */
    public String getPreviousValue() {
        return previousValue;
    }
    
    /**
     * Command result class to hold execution results.
     */
    public static class CommandResult {
        private final boolean success;
        private final String message;
        private final long timestamp;
        
        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public boolean isFailed() {
            return !success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getErrorMessage() {
            return success ? null : message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "CommandResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}