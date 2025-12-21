package patterns.strategy;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import base.DriverContext;
import core.interfaces.EngineType;
import reporting.TestLogManager;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver strategy for running tests on BrowserStack Selenium Grid.
 * NOTE: Selenium ONLY. Playwright is NOT supported here.
 */
public class BrowserStackDriverStrategy implements DriverStrategy {

    @Override
    public DriverContext createDriver() {
        return createDriver(null);
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        EngineType engine =
                EngineType.valueOf(System.getProperty("engine", "SELENIUM"));

        if (engine == EngineType.PLAYWRIGHT) {
            throw new UnsupportedOperationException(
                "BrowserStackDriverStrategy supports SELENIUM only. " +
                "Playwright must use Playwright-native cloud providers."
            );
        }

        try {
            String username = System.getProperty(
                    "BROWSERSTACK_USERNAME",
                    System.getProperty("bs.username")
            );

            String accessKey = System.getProperty(
                    "BROWSERSTACK_ACCESS_KEY",
                    System.getProperty("bs.accessKey")
            );

            if (isNullOrEmpty(username) || isNullOrEmpty(accessKey)) {
                throw new IllegalArgumentException(
                        "BrowserStack credentials not provided. " +
                        "Set BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY"
                );
            }

            String gridUrl = System.getProperty(
                    "BROWSERSTACK_GRID_URL",
                    System.getProperty("bs.gridUrl")
            );

            if (isNullOrEmpty(gridUrl)) {
                gridUrl = "https://" + username + ":" + accessKey +
                          "@hub-cloud.browserstack.com/wd/hub";
            }

            URL remoteUrl = new URL(gridUrl);

            String browserName =
                    System.getProperty("BS_BROWSER",
                            System.getProperty("browserName", "chrome"));

            String browserVersion =
                    System.getProperty("BS_BROWSER_VERSION",
                            System.getProperty("browserVersion", "latest"));

            String platformName =
                    System.getProperty("BS_PLATFORM",
                            System.getProperty("platformName", "Windows 11"));

            MutableCapabilities options =
                    buildOptions(browserName, capabilities);

            Map<String, Object> bsOptions = new HashMap<>();
            bsOptions.put("os", platformName);
            bsOptions.put("browserVersion", browserVersion);
            bsOptions.put("projectName",
                    System.getProperty("BS_PROJECT", "Resulticks-Automation-Project"));
            bsOptions.put("buildName",
                    System.getProperty("BS_BUILD", "Resulticks-Automation-Build"));
            bsOptions.put("sessionName",
                    System.getProperty("BS_NAME", "BrowserStack-Test"));
            bsOptions.put("seleniumVersion",
                    System.getProperty("BS_SELENIUM_VERSION", "4.22.0"));

            putIfPresent(bsOptions, "resolution",
                    System.getProperty("BS_RESOLUTION", "1920x1080"));
            putIfPresent(bsOptions, "networkLogs",
                    System.getProperty("BS_NETWORK", "true"));
            putIfPresent(bsOptions, "video",
                    System.getProperty("BS_VIDEO", "true"));
            putIfPresent(bsOptions, "consoleLogs",
                    System.getProperty("BS_CONSOLE", "info"));
            putIfPresent(bsOptions, "geoLocation",
                    System.getProperty("BS_GEO_LOCATION", "IN"));
            putIfPresent(bsOptions, "debug",
                    System.getProperty("BS_DEBUG", "true"));

            attachBrowserStackOptions(options, browserVersion, bsOptions);

            TestLogManager.info("Connecting to BrowserStack Grid: " + remoteUrl);

            WebDriver driver = new RemoteWebDriver(remoteUrl, options);

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            TestLogManager.error("Failed to create BrowserStack RemoteWebDriver", e);
            throw new RuntimeException("BrowserStack driver creation failed", e);
        }
    }

    @Override
    public String getBrowserName() {
        return "browserstack";
    }

    @Override
    public boolean supports(String browserType) {
        return "browserstack".equalsIgnoreCase(browserType)
            || "bs".equalsIgnoreCase(browserType);
    }

    /* ===================== INTERNAL HELPERS ===================== */

    private static void attachBrowserStackOptions(
            MutableCapabilities options,
            String browserVersion,
            Map<String, Object> bsOptions
    ) {
        if (options instanceof ChromeOptions chrom) {
            chrom.setBrowserVersion(browserVersion);
            chrom.setCapability("bstack:options", bsOptions);
        } else if (options instanceof FirefoxOptions fox) {
            fox.setBrowserVersion(browserVersion);
            fox.setCapability("bstack:options", bsOptions);
        } else if (options instanceof EdgeOptions edge) {
            edge.setBrowserVersion(browserVersion);
            edge.setCapability("bstack:options", bsOptions);
        } else if (options instanceof SafariOptions safari) {
            safari.setBrowserVersion(browserVersion);
            safari.setCapability("bstack:options", bsOptions);
        } else {
            options.setCapability("bstack:options", bsOptions);
        }
    }

    private MutableCapabilities buildOptions(
            String browserName,
            DesiredCapabilities extra
    ) {
        boolean headless =
                Boolean.parseBoolean(
                        System.getProperty("BS_HEADLESS",
                                System.getProperty("headless", "false")));

        if (browserName == null || browserName.isBlank()) {
            browserName = "chrome";
        }

        MutableCapabilities opts;

        switch (browserName.toLowerCase()) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                opts = o;
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new");
                opts = o;
            }
            case "safari" -> opts = new SafariOptions();
            default -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) o.addArguments("--headless=new", "--disable-gpu");
                o.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                opts = o;
            }
        }

        if (extra != null) {
            opts.merge(extra);
        }

        return opts;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isBlank();
    }
}
