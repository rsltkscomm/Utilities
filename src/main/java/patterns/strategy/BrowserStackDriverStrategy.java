package patterns.strategy;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import reporting.TestLogManager;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver strategy for running tests on BrowserStack Selenium Grid.
 * Usage: set system properties or environment variables, then select browser "browserstack" or "bs".
 * Required: BROWSERSTACK_USERNAME, BROWSERSTACK_ACCESS_KEY
 */
public class BrowserStackDriverStrategy implements DriverStrategy
{
    @Override
    public WebDriver createDriver()
    {
        return createDriver(null);
    }

    @Override
    public WebDriver createDriver(DesiredCapabilities capabilities)
    {
        try
        {
            String username = getEnvOrProperty("BROWSERSTACK_USERNAME", "bs.username");
            String accessKey = getEnvOrProperty("BROWSERSTACK_ACCESS_KEY", "bs.accessKey");

            if (isNullOrEmpty(username) || isNullOrEmpty(accessKey))
            {
                throw new IllegalArgumentException("BrowserStack credentials not provided. Set BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY");
            }

            String gridUrl = getEnvOrProperty("BROWSERSTACK_GRID_URL", "bs.gridUrl");
            if (isNullOrEmpty(gridUrl))
            {
                gridUrl = "https://" + username + ":" + accessKey + "@hub-cloud.browserstack.com/wd/hub";
            }
            URL remoteUrl = new URL(gridUrl);

            String browserName = System.getProperty("BS_BROWSER", System.getProperty("browserName", "chrome"));
            String browserVersion = System.getProperty("BS_BROWSER_VERSION", System.getProperty("browserVersion", "latest"));
            String platformName = System.getProperty("BS_PLATFORM", System.getProperty("platformName", "Windows 11"));

            MutableCapabilities options = buildOptions(browserName, capabilities);

            // BrowserStack-specific options
            Map<String, Object> bsOptions = new HashMap<>();
            bsOptions.put("os", platformName);
            bsOptions.put("browserVersion", browserVersion);
            bsOptions.put("projectName", System.getProperty("BS_PROJECT", "Resulticks-Automation-Project"));
            bsOptions.put("buildName", System.getProperty("BS_BUILD", "Resulticks-Automation-Build"));
            bsOptions.put("sessionName", System.getProperty("BS_NAME", "BrowserStack-Test"));
            bsOptions.put("seleniumVersion", System.getProperty("BS_SELENIUM_VERSION", "4.22.0"));
            putIfPresent(bsOptions, "resolution", System.getProperty("BS_RESOLUTION", "1920x1080"));
            putIfPresent(bsOptions, "networkLogs", System.getProperty("BS_NETWORK", "true"));
            putIfPresent(bsOptions, "video", System.getProperty("BS_VIDEO", "true"));
            putIfPresent(bsOptions, "consoleLogs", System.getProperty("BS_CONSOLE", "info"));
            putIfPresent(bsOptions, "geoLocation", System.getProperty("BS_GEO_LOCATION", "IN"));
            putIfPresent(bsOptions, "debug", System.getProperty("BS_DEBUG", "true"));

            // Attach BrowserStack options
            if (options instanceof ChromeOptions chrom)
            {
                chrom.setBrowserVersion(browserVersion);
                chrom.setCapability("bstack:options", bsOptions);
            }
            else if (options instanceof FirefoxOptions fox)
            {
                fox.setBrowserVersion(browserVersion);
                fox.setCapability("bstack:options", bsOptions);
            }
            else if (options instanceof EdgeOptions edge)
            {
                edge.setBrowserVersion(browserVersion);
                edge.setCapability("bstack:options", bsOptions);
            }
            else if (options instanceof SafariOptions safari)
            {
                safari.setBrowserVersion(browserVersion);
                safari.setCapability("bstack:options", bsOptions);
            }
            else
            {
                options.setCapability("bstack:options", bsOptions);
            }

            TestLogManager.info("Connecting to BrowserStack Grid: " + remoteUrl);
            return new RemoteWebDriver(remoteUrl, options);
        }
        catch (Exception e)
        {
            TestLogManager.error("Failed to create BrowserStack RemoteWebDriver", e);
            throw new RuntimeException("BrowserStack driver creation failed", e);
        }
    }

    @Override
    public String getBrowserName()
    {
        return "browserstack";
    }

    @Override
    public boolean supports(String browserType)
    {
        return "browserstack".equalsIgnoreCase(browserType) || "bs".equalsIgnoreCase(browserType);
    }

    private MutableCapabilities buildOptions(String browserName, DesiredCapabilities extra)
    {
        String headlessFlag = getEnvOrProperty("BS_HEADLESS", "bs.headless", System.getProperty("headless", "false"));
        boolean headless = Boolean.parseBoolean(headlessFlag);

        if (browserName == null || browserName.isBlank())
        {
            browserName = "chrome";
        }

        switch (browserName.toLowerCase())
        {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) options.addArguments("--headless=new", "--disable-gpu");
                options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                if (extra != null) options.merge(extra);
                return options;
            }

            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) options.addArguments("-headless");
                if (extra != null) options.merge(extra);
                return options;
            }

            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) options.addArguments("--headless=new");
                if (extra != null) options.merge(extra);
                return options;
            }

            case "safari" -> {
                SafariOptions options = new SafariOptions();
                if (extra != null) options.merge(extra);
                return options;
            }

            default -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) options.addArguments("--headless=new");
                if (extra != null) options.merge(extra);
                return options;
            }
        }
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value)
    {
        if (value != null) map.put(key, value);
    }

    private static String getEnvOrProperty(String envKey, String sysKey)
    {
        String v = System.getenv(envKey);
        if (v == null || v.isBlank()) v = System.getProperty(sysKey);
        return v;
    }

    private static String getEnvOrProperty(String envKey, String sysKey, String def)
    {
        String v = getEnvOrProperty(envKey, sysKey);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static boolean isNullOrEmpty(String s)
    {
        return s == null || s.isBlank();
    }
}
