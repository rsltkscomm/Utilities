package patterns.strategy;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory class for creating WebDriver instances using the Strategy pattern.
 * This provides a centralized way to create drivers and makes it easy to add new browser types.
 */
public class DriverFactory {
    
    private static final Map<String, DriverStrategy> strategies = new HashMap<>();
    
    static {
        // Register default strategies
        registerStrategy(new ChromeDriverStrategy());
        registerStrategy(new FirefoxDriverStrategy());
        registerStrategy(new EdgeDriverStrategy());
        
        // Register cross-platform strategies
        registerStrategy(new CrossPlatformDriverStrategy("chrome"));
        registerStrategy(new CrossPlatformDriverStrategy("firefox"));
        registerStrategy(new CrossPlatformDriverStrategy("edge"));

        // Register LambdaTest remote strategy
        registerStrategy(new LambdaTestDriverStrategy());
    }
    
    /**
     * Registers a new driver strategy.
     * @param strategy The strategy to register
     */
    public static void registerStrategy(DriverStrategy strategy) {
        strategies.put(strategy.getBrowserName().toLowerCase(), strategy);
    }
    
    /**
     * Creates a WebDriver instance for the specified browser type.
     * @param browserType The type of browser to create
     * @return WebDriver instance
     */
    public static WebDriver createDriver(String browserType) {
        return createDriver(browserType, null);
    }
    
    /**
     * Creates a WebDriver instance for the specified browser type with custom capabilities.
     * @param browserType The type of browser to create
     * @param capabilities Custom capabilities
     * @return WebDriver instance
     */
    public static WebDriver createDriver(String browserType, DesiredCapabilities capabilities) {
        if (browserType == null || browserType.trim().isEmpty()) {
            throw new IllegalArgumentException("Browser type cannot be null or empty");
        }
        
        String normalizedBrowserType = browserType.toLowerCase().trim();
        DriverStrategy strategy = findStrategy(normalizedBrowserType);
        
        if (strategy == null) {
            throw new UnsupportedOperationException("Unsupported browser type: " + browserType);
        }
        
        return strategy.createDriver(capabilities);
    }
    
    /**
     * Finds the appropriate strategy for the given browser type.
     * @param browserType The browser type to find strategy for
     * @return DriverStrategy instance or null if not found
     */
    private static DriverStrategy findStrategy(String browserType) {
        // Direct lookup first
        DriverStrategy strategy = strategies.get(browserType);
        if (strategy != null) {
            return strategy;
        }
        
        // Check if any strategy supports this browser type
        for (DriverStrategy s : strategies.values()) {
            if (s.supports(browserType)) {
                return s;
            }
        }
        
        return null;
    }
    
    /**
     * Gets all supported browser types.
     * @return Set of supported browser types
     */
    public static Set<String> getSupportedBrowsers() {
        return strategies.keySet();
    }
    
    /**
     * Checks if a browser type is supported.
     * @param browserType The browser type to check
     * @return true if supported, false otherwise
     */
    public static boolean isBrowserSupported(String browserType) {
        return findStrategy(browserType.toLowerCase()) != null;
    }
    
    /**
     * Removes a strategy from the factory.
     * @param browserType The browser type to remove
     */
    public static void removeStrategy(String browserType) {
        strategies.remove(browserType.toLowerCase());
    }
    
    /**
     * Clears all registered strategies.
     */
    public static void clearStrategies() {
        strategies.clear();
    }
}
