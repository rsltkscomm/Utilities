package patterns.strategy;

import org.openqa.selenium.remote.DesiredCapabilities;

import base.DriverContext;

/**
 * Strategy interface for creating drivers.
 * Supports BOTH Selenium and Playwright via DriverContext.
 */
public interface DriverStrategy {

    /**
     * Creates a driver (Selenium or Playwright) based on EngineType.
     *
     * @return DriverContext (single source of truth)
     */
    DriverContext createDriver();

    /**
     * Creates a driver with custom capabilities.
     * Only applicable for Selenium.
     *
     * @param capabilities Selenium DesiredCapabilities
     * @return DriverContext
     */
    DriverContext createDriver(DesiredCapabilities capabilities);

    /**
     * Gets the browser name associated with this strategy.
     *
     * @return Browser name
     */
    String getBrowserName();

    /**
     * Checks if this strategy supports the given browser type.
     *
     * @param browserType Browser type to check
     * @return true if supported
     */
    boolean supports(String browserType);
}
