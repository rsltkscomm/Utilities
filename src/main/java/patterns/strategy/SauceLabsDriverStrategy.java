package patterns.strategy;

import base.DriverContext;
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

public class SauceLabsDriverStrategy implements DriverStrategy {

    @Override
    public DriverContext createDriver() {
        return createDriver(null);
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {
        try {
            String username = System.getProperty("SAUCE_USERNAME");
            String accessKey = System.getProperty("SAUCE_ACCESS_KEY");

            if (isBlank(username) || isBlank(accessKey)) {
                throw new IllegalArgumentException(
                        "Sauce Labs credentials not provided. Set SAUCE_USERNAME and SAUCE_ACCESS_KEY");
            }

            String gridUrl = System.getProperty(
                    "SAUCE_GRID_URL",
                    "https://" + username + ":" + accessKey + "@ondemand.saucelabs.com:443/wd/hub"
            );

            String browser = System.getProperty("SAUCE_BROWSER", "chrome");
            String browserVersion = System.getProperty("SAUCE_BROWSER_VERSION", "latest");
            String platform = System.getProperty("SAUCE_PLATFORM", "Windows 11");

            MutableCapabilities options = buildOptions(browser, capabilities);

            Map<String, Object> sauceOptions = new HashMap<>();
            sauceOptions.put("username", username);
            sauceOptions.put("accessKey", accessKey);
            sauceOptions.put("build", System.getProperty("SAUCE_BUILD", "Automation-Build"));
            sauceOptions.put("name", System.getProperty("SAUCE_TEST_NAME", "Sauce Test"));

            options.setCapability("browserVersion", browserVersion);
            options.setCapability("platformName", platform);
            options.setCapability("sauce:options", sauceOptions);

            TestLogManager.info("Connecting to Sauce Labs: " + gridUrl);

            WebDriver driver = new RemoteWebDriver(new URL(gridUrl), options);

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            TestLogManager.error("SauceLabs driver creation failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBrowserName() {
        return "saucelabs";
    }

    @Override
    public boolean supports(String browserType) {
        return "saucelabs".equalsIgnoreCase(browserType)
                || "sauce".equalsIgnoreCase(browserType);
    }

    /* ------------------- helpers ------------------- */

    private MutableCapabilities buildOptions(String browser, DesiredCapabilities extra) {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        switch (browser.toLowerCase()) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                if (extra != null) o.merge(extra);
                return o;
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new");
                if (extra != null) o.merge(extra);
                return o;
            }
            case "safari" -> {
                SafariOptions o = new SafariOptions();
                if (extra != null) o.merge(extra);
                return o;
            }
            default -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) o.addArguments("--headless=new");
                if (extra != null) o.merge(extra);
                return o;
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
