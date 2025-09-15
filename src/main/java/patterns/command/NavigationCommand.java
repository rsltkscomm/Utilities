package patterns.command;

import org.openqa.selenium.WebDriver;
import reporting.TestLogManager;

/**
 * Command for navigation operations.
 * This implements the Command pattern for browser navigation.
 */
public class NavigationCommand implements Command {
    
    public enum NavigationType {
        GET, BACK, FORWARD, REFRESH
    }
    
    private final WebDriver driver;
    private final NavigationType navigationType;
    private final String url;
    private final String description;
    private String previousUrl;
    private CommandResult result;
    
    public NavigationCommand(WebDriver driver, NavigationType navigationType, String url, String description) {
        this.driver = driver;
        this.navigationType = navigationType;
        this.url = url;
        this.description = description;
    }
    
    @Override
    public boolean execute() {
        if (driver == null) {
            TestLogManager.error("WebDriver is null, cannot execute navigation command");
            return false;
        }
        
        try {
            // Store current URL for undo operation
            previousUrl = driver.getCurrentUrl();
            
            switch (navigationType) {
                case GET:
                    if (url == null || url.trim().isEmpty()) {
                        TestLogManager.error("URL is null or empty for GET navigation");
                        return false;
                    }
                    driver.get(url);
                    TestLogManager.info("Navigated to: " + url);
                    break;
                    
                case BACK:
                    driver.navigate().back();
                    TestLogManager.info("Navigated back");
                    break;
                    
                case FORWARD:
                    driver.navigate().forward();
                    TestLogManager.info("Navigated forward");
                    break;
                    
                case REFRESH:
                    driver.navigate().refresh();
                    TestLogManager.info("Page refreshed");
                    break;
                    
                default:
                    TestLogManager.error("Unknown navigation type: " + navigationType);
                    return false;
            }
            
            result = new CommandResult(true, "Navigation successful");
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Navigation command failed: " + description, e);
            result = new CommandResult(false, "Navigation failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        if (driver == null) {
            TestLogManager.error("WebDriver is null, cannot undo navigation command");
            return false;
        }
        
        try {
            switch (navigationType) {
                case GET:
                    if (previousUrl != null && !previousUrl.trim().isEmpty()) {
                        driver.get(previousUrl);
                        TestLogManager.info("Undone navigation, returned to: " + previousUrl);
                    } else {
                        TestLogManager.warning("No previous URL available for undo");
                        return false;
                    }
                    break;
                    
                case BACK:
                    driver.navigate().forward();
                    TestLogManager.info("Undone back navigation");
                    break;
                    
                case FORWARD:
                    driver.navigate().back();
                    TestLogManager.info("Undone forward navigation");
                    break;
                    
                case REFRESH:
                    // Refresh undo is not meaningful, just log it
                    TestLogManager.info("Refresh undo - no action taken");
                    break;
                    
                default:
                    TestLogManager.error("Cannot undo unknown navigation type: " + navigationType);
                    return false;
            }
            
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to undo navigation command: " + description, e);
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return description != null ? description : "Navigation command: " + navigationType;
    }
    
    /**
     * Gets the navigation type.
     * @return Navigation type
     */
    public NavigationType getNavigationType() {
        return navigationType;
    }
    
    /**
     * Gets the URL (for GET navigation).
     * @return URL or null if not applicable
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Gets the previous URL.
     * @return Previous URL or null if not available
     */
    public String getPreviousUrl() {
        return previousUrl;
    }
    
    /**
     * Gets the command result.
     * @return CommandResult object
     */
    public CommandResult getResult() {
        return result;
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