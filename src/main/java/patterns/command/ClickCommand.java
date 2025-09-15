package patterns.command;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import reporting.TestLogManager;

import java.time.Duration;

/**
 * Command for clicking operations.
 * This implements the Command pattern for element clicking.
 */
public class ClickCommand implements Command {
    
    private final WebDriver driver;
    private final WebElement element;
    private final String description;
    private CommandResult result;
    
    public ClickCommand(WebDriver driver, WebElement element, String description) {
        this.driver = driver;
        this.element = element;
        this.description = description;
    }
    
    @Override
    public boolean execute() {
        if (driver == null) {
            TestLogManager.error("WebDriver is null, cannot execute click command");
            result = new CommandResult(false, "WebDriver is null");
            return false;
        }
        
        if (element == null) {
            TestLogManager.error("WebElement is null, cannot execute click command");
            result = new CommandResult(false, "WebElement is null");
            return false;
        }
        
        try {
            // Wait for element to be clickable
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.elementToBeClickable(element));
            
            // Scroll element into view
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            
            // Click the element
            element.click();
            
            TestLogManager.info("Element clicked successfully: " + description);
            result = new CommandResult(true, "Element clicked successfully");
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Click command failed: " + description, e);
            result = new CommandResult(false, "Click failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        // Click operations are generally not undoable
        TestLogManager.info("Click command undo not supported: " + description);
        return false;
    }
    
    @Override
    public String getDescription() {
        return description != null ? description : "Click command";
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
     * @return WebElement that was clicked
     */
    public WebElement getElement() {
        return element;
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