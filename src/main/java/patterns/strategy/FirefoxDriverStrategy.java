package patterns.strategy;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.remote.CapabilityType;

import java.net.URI;

/**
 * Strategy implementation for Firefox WebDriver creation.
 */
public class FirefoxDriverStrategy implements DriverStrategy {
    
    private final boolean remote;
    private final String remoteUrl;
    
    public FirefoxDriverStrategy() {
        this(false, null);
    }
    
    public FirefoxDriverStrategy(boolean remote, String remoteUrl) {
        this.remote = remote;
        this.remoteUrl = remoteUrl;
    }
    
    @Override
    public WebDriver createDriver() {
        return createDriver(null);
    }
    
    @Override
    public WebDriver createDriver(DesiredCapabilities capabilities) {
        FirefoxOptions options = createFirefoxOptions();
        
        if (capabilities != null) {
            options.merge(capabilities);
        }
        
        try {
            if (remote && remoteUrl != null) {
                return new RemoteWebDriver(URI.create(remoteUrl).toURL(), options);
            } else {
                return new FirefoxDriver(options);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Firefox driver", e);
        }
    }
    
    private FirefoxOptions createFirefoxOptions() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        
        options.addArguments("--disable-notifications");
        options.addArguments("--width=1920", "--height=1080");
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        
        return options;
    }
    
    @Override
    public String getBrowserName() {
        return "firefox";
    }
    
    @Override
    public boolean supports(String browserType) {
        return "firefox".equalsIgnoreCase(browserType);
    }
}
