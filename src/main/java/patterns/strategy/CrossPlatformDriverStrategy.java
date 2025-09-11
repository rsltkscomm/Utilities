package patterns.strategy;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import utils.CrossPlatformUtils;
import reporting.TestLogManager;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Cross-platform driver strategy that handles OS-specific configurations.
 */
public class CrossPlatformDriverStrategy implements DriverStrategy {
    
    private final String browser;
    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;
    
    public CrossPlatformDriverStrategy(String browser) {
        this(browser, false, false, null);
    }
    
    public CrossPlatformDriverStrategy(String browser, boolean headless, boolean remote, String remoteUrl) {
        this.browser = browser.toLowerCase();
        this.headless = headless;
        this.remote = remote;
        this.remoteUrl = remoteUrl;
    }
    
    @Override
    public WebDriver createDriver() {
        return createDriver(null);
    }
    
    @Override
    public WebDriver createDriver(DesiredCapabilities capabilities) {
        TestLogManager.info("Creating " + browser + " driver for " + CrossPlatformUtils.getCurrentOS());
        
        switch (browser) {
            case "chrome":
                return createChromeDriver(capabilities);
            case "firefox":
                return createFirefoxDriver(capabilities);
            case "edge":
                return createEdgeDriver(capabilities);
            default:
                throw new UnsupportedOperationException("Unsupported browser: " + browser);
        }
    }
    
    private WebDriver createChromeDriver(DesiredCapabilities capabilities) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        // Set cross-platform options
        setCrossPlatformChromeOptions(options);
        
        if (capabilities != null) {
            options.merge(capabilities);
        }
        
        try {
            if (remote && remoteUrl != null) {
                return new RemoteWebDriver(URI.create(remoteUrl).toURL(), options);
            } else {
                return new ChromeDriver(options);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Chrome driver", e);
        }
    }
    
    private WebDriver createFirefoxDriver(DesiredCapabilities capabilities) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        
        // Set cross-platform options
        setCrossPlatformFirefoxOptions(options);
        
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
    
    private WebDriver createEdgeDriver(DesiredCapabilities capabilities) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        
        // Set cross-platform options
        setCrossPlatformEdgeOptions(options);
        
        if (capabilities != null) {
            options.merge(capabilities);
        }
        
        try {
            if (remote && remoteUrl != null) {
                return new RemoteWebDriver(URI.create(remoteUrl).toURL(), options);
            } else {
                return new EdgeDriver(options);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Edge driver", e);
        }
    }
    
    private void setCrossPlatformChromeOptions(ChromeOptions options) {
        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = CrossPlatformUtils.getProjectDownloadDirectory().toString();
        
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        
        // OS-specific arguments
        CrossPlatformUtils.OperatingSystem os = CrossPlatformUtils.getCurrentOS();
        switch (os) {
            case WINDOWS:
                options.addArguments("--disable-notifications", "--no-sandbox", "--disable-gpu");
                break;
            case MACOS:
                options.addArguments("--disable-notifications", "--no-sandbox");
                break;
            case LINUX:
                options.addArguments("--disable-notifications", "--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage");
                break;
        }
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        options.setExperimentalOption("prefs", prefs);
        options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));
        options.setCapability("acceptInsecureCerts", true);
    }
    
    private void setCrossPlatformFirefoxOptions(FirefoxOptions options) {
        // OS-specific arguments
        CrossPlatformUtils.OperatingSystem os = CrossPlatformUtils.getCurrentOS();
        switch (os) {
            case WINDOWS:
                options.addArguments("--disable-notifications");
                break;
            case MACOS:
                options.addArguments("--disable-notifications");
                break;
            case LINUX:
                options.addArguments("--disable-notifications", "--disable-dev-shm-usage");
                break;
        }
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        options.addArguments("--width=1920", "--height=1080");
        options.setCapability("acceptInsecureCerts", true);
    }
    
    private void setCrossPlatformEdgeOptions(EdgeOptions options) {
        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = CrossPlatformUtils.getProjectDownloadDirectory().toString();
        
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        
        // OS-specific arguments
        CrossPlatformUtils.OperatingSystem os = CrossPlatformUtils.getCurrentOS();
        switch (os) {
            case WINDOWS:
                options.addArguments("--disable-notifications", "--no-sandbox", "--inprivate");
                break;
            case MACOS:
                options.addArguments("--disable-notifications", "--inprivate");
                break;
            case LINUX:
                options.addArguments("--disable-notifications", "--no-sandbox", "--disable-dev-shm-usage");
                break;
        }
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        options.setExperimentalOption("prefs", prefs);
        options.setCapability("acceptInsecureCerts", true);
    }
    
    @Override
    public String getBrowserName() {
        return browser;
    }
    
    @Override
    public boolean supports(String browserType) {
        return browser.equalsIgnoreCase(browserType);
    }
}
