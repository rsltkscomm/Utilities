package patterns.strategy;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import reporting.TestLogManager;

/**
 * Cross-platform driver strategy that delegates to appropriate browser-specific strategies.
 */
public class CrossPlatformDriverStrategy implements DriverStrategy {
    
    private final String browserType;
    private final DriverStrategy delegateStrategy;
    
    public CrossPlatformDriverStrategy(String browserType) {
        this.browserType = browserType.toLowerCase();
        this.delegateStrategy = createDelegateStrategy();
    }
    
    @Override
    public WebDriver createDriver() {
        return createDriver(null);
    }
    
    @Override
    public WebDriver createDriver(DesiredCapabilities capabilities) {
        if (delegateStrategy == null) {
            throw new UnsupportedOperationException("Unsupported browser type: " + browserType);
        }
        
        TestLogManager.info("Creating cross-platform driver for: " + browserType);
        return delegateStrategy.createDriver(capabilities);
    }
    
    @Override
    public String getBrowserName() {
        return browserType;
    }
    
    @Override
    public boolean supports(String browserType) {
        return this.browserType.equalsIgnoreCase(browserType);
    }
    
    private DriverStrategy createDelegateStrategy() {
        switch (browserType) {
            case "chrome":
                return new ChromeDriverStrategy();
            case "firefox":
                return new FirefoxDriverStrategy();
            case "edge":
                return new EdgeDriverStrategy();
            default:
                TestLogManager.warning("Unsupported browser type for cross-platform strategy: " + browserType);
                return null;
        }
    }
}