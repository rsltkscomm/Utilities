package patterns.strategy;

import cloud.CloudConfiguration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import reporting.TestLogManager;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Cloud Driver Strategy
 * Creates WebDriver instances for cloud testing platforms
 */
public class CloudDriverStrategy implements DriverStrategy {
    
    private final String browserName;
    private final CloudConfiguration cloudConfig;
    
    public CloudDriverStrategy(String browserName) {
        this.browserName = browserName.toLowerCase();
        this.cloudConfig = new CloudConfiguration();
        
        if (!cloudConfig.isCloudEnabled()) {
            throw new IllegalStateException("Cloud testing is not enabled. Please enable cloud testing in configuration.");
        }
    }
    
    @Override
    public String getBrowserName() {
        return browserName;
    }
    
    @Override
    public boolean supports(String browserType) {
        return browserName.equalsIgnoreCase(browserType) && cloudConfig.isCloudEnabled();
    }
    
    @Override
    public WebDriver createDriver() {
        return createDriver(new DesiredCapabilities());
    }
    
    @Override
    public WebDriver createDriver(DesiredCapabilities capabilities) {
        try {
            TestLogManager.info("Creating cloud driver for browser: " + browserName);
            
            // Build cloud-specific capabilities
            DesiredCapabilities cloudCapabilities = buildCloudCapabilities(capabilities);
            
            // Create remote WebDriver
            URL hubUrl = URI.create(cloudConfig.getHubUrl()).toURL();
            WebDriver driver = new RemoteWebDriver(hubUrl, cloudCapabilities);
            
            return driver;
            
        } catch (MalformedURLException e) {
            TestLogManager.error("Invalid cloud hub URL: " + cloudConfig.getHubUrl(), e);
            throw new RuntimeException("Failed to create cloud driver", e);
        } catch (Exception e) {
            TestLogManager.error("Failed to create cloud driver for browser: " + browserName, e);
            throw new RuntimeException("Failed to create cloud driver", e);
        }
    }
    
    /**
     * Build cloud-specific capabilities
     */
    private DesiredCapabilities buildCloudCapabilities(DesiredCapabilities baseCapabilities) {
        DesiredCapabilities cloudCapabilities = new DesiredCapabilities(baseCapabilities);
        
        // Set basic browser capabilities
        cloudCapabilities.setCapability("browserName", browserName);
        
        // Add cloud provider specific capabilities
        switch (cloudConfig.getActiveProvider().toLowerCase()) {
            case "browserstack":
                addBrowserStackCapabilities(cloudCapabilities);
                break;
            case "saucelabs":
                addSauceLabsCapabilities(cloudCapabilities);
                break;
            case "lambdatest":
                addLambdaTestCapabilities(cloudCapabilities);
                break;
            case "crossbrowsertesting":
                addCrossBrowserTestingCapabilities(cloudCapabilities);
                break;
            default:
                TestLogManager.warning("Unknown cloud provider: " + cloudConfig.getActiveProvider());
        }
        
        return cloudCapabilities;
    }
    
    /**
     * Add BrowserStack specific capabilities
     */
    private void addBrowserStackCapabilities(DesiredCapabilities capabilities) {
        java.util.Map<String, Object> browserstackOptions = new java.util.HashMap<>();
        
        browserstackOptions.put("userName", cloudConfig.getUsername());
        browserstackOptions.put("accessKey", cloudConfig.getAccessKey());
        browserstackOptions.put("projectName", cloudConfig.getProjectName());
        browserstackOptions.put("buildName", cloudConfig.getBuildName());
        browserstackOptions.put("sessionName", cloudConfig.getSessionName());
        browserstackOptions.put("timezone", "UTC");
        
        if (cloudConfig.isVideoEnabled()) {
            browserstackOptions.put("video", true);
        }
        
        if (cloudConfig.isScreenshotEnabled()) {
            browserstackOptions.put("screenshot", true);
        }
        
        if (cloudConfig.isLocalTestingEnabled()) {
            browserstackOptions.put("local", true);
            if (!cloudConfig.getTunnelIdentifier().isEmpty()) {
                browserstackOptions.put("tunnelIdentifier", cloudConfig.getTunnelIdentifier());
            }
        }
        
        capabilities.setCapability("bstack:options", browserstackOptions);
    }
    
    /**
     * Add SauceLabs specific capabilities
     */
    private void addSauceLabsCapabilities(DesiredCapabilities capabilities) {
        java.util.Map<String, Object> sauceOptions = new java.util.HashMap<>();
        
        sauceOptions.put("username", cloudConfig.getUsername());
        sauceOptions.put("accessKey", cloudConfig.getAccessKey());
        sauceOptions.put("name", cloudConfig.getSessionName());
        sauceOptions.put("build", cloudConfig.getBuildName());
        sauceOptions.put("tags", new String[]{cloudConfig.getProjectName()});
        
        if (cloudConfig.isVideoEnabled()) {
            sauceOptions.put("recordVideo", true);
        }
        
        if (cloudConfig.isScreenshotEnabled()) {
            sauceOptions.put("recordScreenshots", true);
        }
        
        capabilities.setCapability("sauce:options", sauceOptions);
    }
    
    /**
     * Add LambdaTest specific capabilities
     */
    private void addLambdaTestCapabilities(DesiredCapabilities capabilities) {
        java.util.Map<String, Object> lambdaOptions = new java.util.HashMap<>();
        
        lambdaOptions.put("username", cloudConfig.getUsername());
        lambdaOptions.put("accessKey", cloudConfig.getAccessKey());
        lambdaOptions.put("build", cloudConfig.getBuildName());
        lambdaOptions.put("name", cloudConfig.getSessionName());
        lambdaOptions.put("project", cloudConfig.getProjectName());
        
        if (cloudConfig.isVideoEnabled()) {
            lambdaOptions.put("video", true);
        }
        
        if (cloudConfig.isScreenshotEnabled()) {
            lambdaOptions.put("screenshot", true);
        }
        
        capabilities.setCapability("lt:options", lambdaOptions);
    }
    
    /**
     * Add CrossBrowserTesting specific capabilities
     */
    private void addCrossBrowserTestingCapabilities(DesiredCapabilities capabilities) {
        java.util.Map<String, Object> cbtOptions = new java.util.HashMap<>();
        
        cbtOptions.put("username", cloudConfig.getUsername());
        cbtOptions.put("authkey", cloudConfig.getAccessKey());
        cbtOptions.put("name", cloudConfig.getSessionName());
        cbtOptions.put("build", cloudConfig.getBuildName());
        
        if (cloudConfig.isVideoEnabled()) {
            cbtOptions.put("record_video", "true");
        }
        
        if (cloudConfig.isScreenshotEnabled()) {
            cbtOptions.put("record_network", "true");
        }
        
        capabilities.setCapability("cbt:options", cbtOptions);
    }
    
    /**
     * Create driver with specific browser and platform
     */
    public WebDriver createDriver(String browser, String platform, String version) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("browserName", browser);
        capabilities.setCapability("platform", platform);
        capabilities.setCapability("version", version);
        
        return createDriver(capabilities);
    }
    
    /**
     * Create mobile driver
     */
    public WebDriver createMobileDriver(String platform, String device, String version) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        
        if ("android".equalsIgnoreCase(platform)) {
            capabilities.setCapability("platformName", "Android");
            capabilities.setCapability("deviceName", device);
            capabilities.setCapability("platformVersion", version);
            capabilities.setCapability("browserName", "Chrome");
        } else if ("ios".equalsIgnoreCase(platform)) {
            capabilities.setCapability("platformName", "iOS");
            capabilities.setCapability("deviceName", device);
            capabilities.setCapability("platformVersion", version);
            capabilities.setCapability("browserName", "Safari");
        }
        
        return createDriver(capabilities);
    }
    
    /**
     * Get cloud configuration
     */
    public CloudConfiguration getCloudConfiguration() {
        return cloudConfig;
    }
    
}
