package patterns.strategy;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Strategy interface for creating different types of WebDriver instances.
 * This allows for easy extension and modification of driver creation logic.
 */
public interface DriverStrategy {
    
    /**
     * Creates a WebDriver instance based on the strategy implementation.
     * @return WebDriver instance
     */
    WebDriver createDriver();
    
    /**
     * Creates a WebDriver instance with custom capabilities.
     * @param capabilities Custom capabilities for the driver
     * @return WebDriver instance
     */
    WebDriver createDriver(DesiredCapabilities capabilities);
    
    /**
     * Gets the browser name associated with this strategy.
     * @return Browser name
     */
    String getBrowserName();
    
    /**
     * Checks if this strategy supports the given browser type.
     * @param browserType Browser type to check
     * @return true if supported, false otherwise
     */
    boolean supports(String browserType);
}
