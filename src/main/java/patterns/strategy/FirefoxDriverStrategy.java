package patterns.strategy;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy implementation for Firefox WebDriver creation.
 */
public class FirefoxDriverStrategy implements DriverStrategy {
    
    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;
    
    public FirefoxDriverStrategy() {
        this(false, false, null);
    }
    
    public FirefoxDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
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
        
        // Set common options
        setCommonOptions(options);
        
        // Set headless mode if required
        if (headless) {
            options.addArguments("--headless");
        }
        
        return options;
    }
    
    private void setCommonOptions(FirefoxOptions options) {
        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "data", "downloadedFile")
                .toAbsolutePath().toString();
        
        prefs.put("browser.download.folderList", 2);
        prefs.put("browser.download.dir", downloadPath);
        prefs.put("browser.download.useDownloadDir", true);
        prefs.put("browser.helperApps.neverAsk.saveToDisk", "application/octet-stream");
        prefs.put("pdfjs.disabled", true);
        
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("geo.enabled", false);
        options.addPreference("media.navigator.enabled", false);
        options.addPreference("media.peerconnection.enabled", false);
        options.addPreference("media.eme.enabled", false);
        options.addPreference("media.gmp-widevinecdm.enabled", false);
        options.addPreference("media.gmp-widevinecdm.visible", false);
        options.addPreference("media.gmp-manager.updateEnabled", false);
        options.addPreference("media.gmp-provider-widevinecdm.updateEnabled", false);
        options.addPreference("media.gmp-provider-widevinecdm.visible", false);
        options.addPreference("media.gmp-widevinecdm.visible", false);
        options.addPreference("media.gmp-widevinecdm.enabled", false);
        options.addPreference("media.eme.enabled", false);
        options.addPreference("media.peerconnection.enabled", false);
        options.addPreference("media.navigator.enabled", false);
        options.addPreference("geo.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("dom.webnotifications.enabled", false);
        
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadPath);
        options.addPreference("browser.download.useDownloadDir", true);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk", "application/octet-stream");
        options.addPreference("pdfjs.disabled", true);
    }
    
    @Override
    public String getBrowserName() {
        return "firefox";
    }
    
    @Override
    public boolean supports(String browserType) {
        return "firefox".equalsIgnoreCase(browserType) || 
               "firefoxheadless".equalsIgnoreCase(browserType);
    }
}