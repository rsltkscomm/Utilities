package patterns.strategy;

import cloud.CloudConfiguration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory class for creating WebDriver instances using the Strategy pattern.
 * This provides a centralized way to create drivers and makes it easy to add new browser types.
 * Now includes cloud testing support with automatic cloud driver registration.
 */
public class DriverFactory {
    
    private static final Map<String, DriverStrategy> strategies = new HashMap<>();
    private static boolean cloudStrategiesRegistered = false;
    
    static {
        // Register default strategies
        registerStrategy(new ChromeDriverStrategy());
        registerStrategy(new ChromeHeadlessDriverStrategy());
        registerStrategy(new FirefoxDriverStrategy());
        registerStrategy(new EdgeDriverStrategy());
        
        // Register cross-platform strategies
        registerStrategy(new CrossPlatformDriverStrategy("chrome"));
        registerStrategy(new CrossPlatformDriverStrategy("chromeheadless"));
        registerStrategy(new CrossPlatformDriverStrategy("firefox"));
        registerStrategy(new CrossPlatformDriverStrategy("edge"));

        // Register LambdaTest remote strategy
        registerStrategy(new LambdaTestDriverStrategy());
        registerStrategy(new BrowserStackDriverStrategy());
        registerStrategy(new SauceLabsDriverStrategy());
        
        // Register cloud strategies if cloud is enabled
        registerCloudStrategies();
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
    
    /**
     * Register cloud strategies if cloud testing is enabled
     */
    private static void registerCloudStrategies() {
        if (cloudStrategiesRegistered) {
            return;
        }
        
        try {
            CloudConfiguration cloudConfig = new CloudConfiguration();
            
            if (cloudConfig.isCloudEnabled()) {
                TestLogManager.info("Registering cloud driver strategies...");
                
                // Register cloud strategies for supported browsers
                String[] cloudBrowsers = {"chrome", "firefox", "edge", "safari"};
                
                for (String browser : cloudBrowsers) {
                    try {
                        CloudDriverStrategy cloudStrategy = new CloudDriverStrategy(browser);
                        registerStrategy(cloudStrategy);
                        TestLogManager.info("Registered cloud driver strategy for: " + browser);
                    } catch (Exception e) {
                        TestLogManager.warning("Failed to register cloud driver strategy for " + browser + ": " + e.getMessage());
                    }
                }
                
                // Register mobile cloud strategies
                try {
                    CloudDriverStrategy androidStrategy = new CloudDriverStrategy("android");
                    registerStrategy(androidStrategy);
                    TestLogManager.info("Registered cloud driver strategy for: android");
                } catch (Exception e) {
                    TestLogManager.warning("Failed to register cloud driver strategy for android: " + e.getMessage());
                }
                
                try {
                    CloudDriverStrategy iosStrategy = new CloudDriverStrategy("ios");
                    registerStrategy(iosStrategy);
                    TestLogManager.info("Registered cloud driver strategy for: ios");
                } catch (Exception e) {
                    TestLogManager.warning("Failed to register cloud driver strategy for ios: " + e.getMessage());
                }
                
                cloudStrategiesRegistered = true;
                TestLogManager.success("Cloud driver strategies registered successfully");
                
            } else {
                TestLogManager.info("Cloud testing is disabled, skipping cloud driver registration");
            }
            
        } catch (Exception e) {
            TestLogManager.warning("Failed to register cloud strategies: " + e.getMessage());
        }
    }
    
    /**
     * Force re-registration of cloud strategies
     * Useful when cloud configuration changes at runtime
     */
    public static void reRegisterCloudStrategies() {
        cloudStrategiesRegistered = false;
        
        // Remove existing cloud strategies
        strategies.entrySet().removeIf(entry -> 
            entry.getValue() instanceof CloudDriverStrategy);
        
        // Re-register cloud strategies
        registerCloudStrategies();
    }
    
    /**
     * Create cloud driver with specific browser and platform
     */
    public static WebDriver createCloudDriver(String browser, String platform, String version) {
        if (!cloudStrategiesRegistered) {
            registerCloudStrategies();
        }
        
        CloudDriverStrategy cloudStrategy = (CloudDriverStrategy) findStrategy(browser);
        if (cloudStrategy == null) {
            throw new UnsupportedOperationException("Cloud driver not available for browser: " + browser);
        }
        
        return cloudStrategy.createDriver(browser, platform, version);
    }
    
    /**
     * Create mobile cloud driver
     */
    public static WebDriver createMobileCloudDriver(String platform, String device, String version) {
        if (!cloudStrategiesRegistered) {
            registerCloudStrategies();
        }
        
        CloudDriverStrategy cloudStrategy = (CloudDriverStrategy) findStrategy(platform.toLowerCase());
        if (cloudStrategy == null) {
            throw new UnsupportedOperationException("Cloud mobile driver not available for platform: " + platform);
        }
        
        return cloudStrategy.createMobileDriver(platform, device, version);
    }
    
    /**
     * Check if cloud drivers are available
     */
    public static boolean isCloudDriversAvailable() {
        return cloudStrategiesRegistered && 
               strategies.values().stream().anyMatch(strategy -> strategy instanceof CloudDriverStrategy);
    }
    
    /**
     * Get cloud configuration
     */
    public static CloudConfiguration getCloudConfiguration() {
        try {
            return new CloudConfiguration();
        } catch (Exception e) {
            TestLogManager.warning("Failed to get cloud configuration: " + e.getMessage());
            return null;
        }
    }
}
