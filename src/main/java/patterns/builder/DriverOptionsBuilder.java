package patterns.builder;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.CrossPlatformUtils;

/**
 * Builder class for creating browser-specific options.
 * This provides a fluent interface for configuring browser options.
 */
public class DriverOptionsBuilder {
    
    private final List<String> arguments = new ArrayList<>();
    private final Map<String, Object> experimentalOptions = new HashMap<>();
    private final Map<String, Object> preferences = new HashMap<>();
    private final DesiredCapabilities capabilities = new DesiredCapabilities();
    private boolean headless = false;
    private String downloadPath;
    private boolean disableNotifications = true;
    private boolean disableImages = false;
    private boolean disableJavaScript = false;
    private boolean incognito = false;
    private boolean noSandbox = true;
    private boolean disableGpu = true;
    private boolean acceptInsecureCerts = true;
    
    public DriverOptionsBuilder() {
        // Set default download path using cross-platform utility
        this.downloadPath = CrossPlatformUtils.getProjectDownloadDirectory().toString();
    }
    
    public DriverOptionsBuilder headless(boolean headless) {
        this.headless = headless;
        return this;
    }
    
    public DriverOptionsBuilder downloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        return this;
    }
    
    public DriverOptionsBuilder disableNotifications(boolean disable) {
        this.disableNotifications = disable;
        return this;
    }
    
    public DriverOptionsBuilder disableImages(boolean disable) {
        this.disableImages = disable;
        return this;
    }
    
    public DriverOptionsBuilder disableJavaScript(boolean disable) {
        this.disableJavaScript = disable;
        return this;
    }
    
    public DriverOptionsBuilder incognito(boolean incognito) {
        this.incognito = incognito;
        return this;
    }
    
    public DriverOptionsBuilder noSandbox(boolean noSandbox) {
        this.noSandbox = noSandbox;
        return this;
    }
    
    public DriverOptionsBuilder disableGpu(boolean disable) {
        this.disableGpu = disable;
        return this;
    }
    
    public DriverOptionsBuilder acceptInsecureCerts(boolean accept) {
        this.acceptInsecureCerts = accept;
        return this;
    }
    
    public DriverOptionsBuilder addArgument(String argument) {
        this.arguments.add(argument);
        return this;
    }
    
    public DriverOptionsBuilder addArguments(String... arguments) {
        for (String arg : arguments) {
            this.arguments.add(arg);
        }
        return this;
    }
    
    public DriverOptionsBuilder experimentalOption(String key, Object value) {
        this.experimentalOptions.put(key, value);
        return this;
    }
    
    public DriverOptionsBuilder preference(String key, Object value) {
        this.preferences.put(key, value);
        return this;
    }
    
    public DriverOptionsBuilder capability(String key, Object value) {
        this.capabilities.setCapability(key, value);
        return this;
    }
    
    public DriverOptionsBuilder windowSize(int width, int height) {
        return addArgument("--window-size=" + width + "," + height);
    }
    
    public DriverOptionsBuilder userAgent(String userAgent) {
        return addArgument("--user-agent=" + userAgent);
    }
    
    public DriverOptionsBuilder proxy(String proxy) {
        return addArgument("--proxy-server=" + proxy);
    }
    
    /**
     * Builds Chrome-specific options.
     */
    public ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        
        // Add common arguments
        addCommonArguments();
        
        // Add Chrome-specific arguments
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        if (incognito) {
            options.addArguments("--incognito");
        }
        
        if (noSandbox) {
            options.addArguments("--no-sandbox");
        }
        
        if (disableGpu) {
            options.addArguments("--disable-gpu");
        }
        
        if (disableNotifications) {
            options.addArguments("--disable-notifications");
        }
        
        if (disableImages) {
            options.addArguments("--disable-images");
        }
        
        if (disableJavaScript) {
            options.addArguments("--disable-javascript");
        }
        
        // Add custom arguments
        options.addArguments(arguments);
        
        // Set preferences
        Map<String, Object> chromePrefs = new HashMap<>(preferences);
        chromePrefs.put("download.default_directory", downloadPath);
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("credentials_enable_service", false);
        chromePrefs.put("profile.password_manager_enabled", false);
        
        options.setExperimentalOption("prefs", chromePrefs);
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        
        // Add experimental options
        experimentalOptions.forEach(options::setExperimentalOption);
        
        // Set capabilities
        capabilities.asMap().forEach(options::setCapability);
        
        return options;
    }
    
    /**
     * Builds Firefox-specific options.
     */
    public FirefoxOptions buildFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        
        // Add common arguments
        addCommonArguments();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        if (disableNotifications) {
            options.addArguments("--disable-notifications");
        }
        
        // Set window size
        options.addArguments("--width=1920", "--height=1080");
        
        // Add custom arguments
        options.addArguments(arguments);
        
        // Set preferences (Firefox-specific)
        preferences.forEach((key, value) -> {
            if (value instanceof String) {
                options.addPreference(key, (String) value);
            } else if (value instanceof Boolean) {
                options.addPreference(key, (Boolean) value);
            } else if (value instanceof Integer) {
                options.addPreference(key, (Integer) value);
            }
        });
        
        // Set capabilities
        capabilities.asMap().forEach(options::setCapability);
        
        return options;
    }
    
    /**
     * Builds Edge-specific options.
     */
    public EdgeOptions buildEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        
        // Add common arguments
        addCommonArguments();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        if (incognito) {
            options.addArguments("--inprivate");
        }
        
        if (noSandbox) {
            options.addArguments("--no-sandbox");
        }
        
        if (disableNotifications) {
            options.addArguments("--disable-notifications");
        }
        
        // Add custom arguments
        options.addArguments(arguments);
        
        // Set preferences
        Map<String, Object> edgePrefs = new HashMap<>(preferences);
        edgePrefs.put("download.default_directory", downloadPath);
        edgePrefs.put("download.prompt_for_download", false);
        edgePrefs.put("profile.default_content_settings.popups", 0);
        edgePrefs.put("credentials_enable_service", false);
        edgePrefs.put("profile.password_manager_enabled", false);
        
        options.setExperimentalOption("prefs", edgePrefs);
        
        // Add experimental options
        experimentalOptions.forEach(options::setExperimentalOption);
        
        // Set capabilities
        capabilities.asMap().forEach(options::setCapability);
        
        return options;
    }
    
    private void addCommonArguments() {
        if (acceptInsecureCerts) {
            capabilities.setCapability("acceptInsecureCerts", true);
        }
    }
    
    /**
     * Creates a new builder instance.
     */
    public static DriverOptionsBuilder builder() {
        return new DriverOptionsBuilder();
    }
}
