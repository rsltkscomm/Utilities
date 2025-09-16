package patterns.strategy;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy implementation for Chrome WebDriver creation.
 */
public class ChromeDriverStrategy implements DriverStrategy {
    
    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;
    
    public ChromeDriverStrategy() {
        this(false, false, null);
    }
    
    public ChromeDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
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
        ChromeOptions options = createChromeOptions();
        
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
    
    private ChromeOptions createChromeOptions() {
//        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        // Set common options
        setCommonOptions(options);
        
        // Set headless mode if required
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        return options;
    }
    
    private void setCommonOptions(ChromeOptions options) {
        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "data", "downloadedFile")
                .toAbsolutePath().toString();
        
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        
        options.addArguments("--disable-notifications", "--no-sandbox", "--disable-gpu", "--incognito");
        options.setExperimentalOption("prefs", prefs);
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setCapability("acceptInsecureCerts", true);
    }
    
    @Override
    public String getBrowserName() {
        return "chrome";
    }
    
    @Override
    public boolean supports(String browserType) {
        return "chrome".equalsIgnoreCase(browserType) || 
               "chromeheadless".equalsIgnoreCase(browserType);
    }
}
