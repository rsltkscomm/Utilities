package patterns.strategy;

import base.DriverContext;
import core.interfaces.EngineType;
import reporting.TestLogManager;

import com.microsoft.playwright.*;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Driver Strategy
 * Supports:
 *  - Selenium Cloud (BrowserStack / Sauce / LambdaTest)
 *  - Playwright Cloud (LambdaTest Playwright)
 *
 * All configuration is read from System properties.
 */
public class CloudDriverStrategy implements DriverStrategy {

    private final String browserName;

    public CloudDriverStrategy(String browserName) {
        this.browserName = browserName.toLowerCase();
    }

    /* ==========================================================
       META
       ========================================================== */

    @Override
    public String getBrowserName() {
        return browserName;
    }

    @Override
    public boolean supports(String browserType) {
        return browserName.equalsIgnoreCase(browserType)
                && Boolean.parseBoolean(
                        System.getProperty("cloud.enabled", "false"));
    }

    /* ==========================================================
       ENTRY
       ========================================================== */

    @Override
    public DriverContext createDriver() {
        return createDriver(new DesiredCapabilities());
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        if (!Boolean.parseBoolean(
                System.getProperty("cloud.enabled", "false"))) {
            throw new IllegalStateException(
                "Cloud testing is disabled. Enable cloud.enabled=true");
        }

        EngineType engine = EngineType.valueOf(
                System.getProperty("engine", "SELENIUM").toUpperCase()
        );

        return engine == EngineType.PLAYWRIGHT
                ? createPlaywrightCloudDriver()
                : createSeleniumCloudDriver(capabilities);
    }

    /* ==========================================================
       SELENIUM CLOUD (RemoteWebDriver)
       ========================================================== */

    private DriverContext createSeleniumCloudDriver(
            DesiredCapabilities baseCaps) {

        try {
            TestLogManager.info(
                "Creating Selenium cloud driver for: " + browserName);

            DesiredCapabilities caps =
                    new DesiredCapabilities(baseCaps);
            caps.setCapability("browserName", browserName);

            String provider =
                    System.getProperty("cloud.provider", "lambdatest")
                            .toLowerCase();

            switch (provider) {
                case "browserstack" -> caps.setCapability(
                        "bstack:options", buildBrowserStackOptions());
                case "saucelabs" -> caps.setCapability(
                        "sauce:options", buildSauceOptions());
                case "lambdatest" -> caps.setCapability(
                        "lt:options", buildLambdaTestOptions());
                case "crossbrowsertesting" -> caps.setCapability(
                        "cbt:options", buildCBTOptions());
                default -> TestLogManager.warning(
                        "Unknown cloud provider: " + provider);
            }

            URL hubUrl = URI.create(
                    System.getProperty("cloud.hub.url")
            ).toURL();

            WebDriver driver = new RemoteWebDriver(hubUrl, caps);
            return DriverContext.selenium(driver);

        } catch (Exception e) {
            TestLogManager.error(
                "Failed to create Selenium cloud driver", e);
            throw new RuntimeException(e);
        }
    }

    /* ==========================================================
       PLAYWRIGHT CLOUD (WebSocket)
       ========================================================== */

    private DriverContext createPlaywrightCloudDriver() {

        try {
            TestLogManager.info("Creating Playwright cloud driver");

            Playwright playwright = Playwright.create();

            String capsJson = buildPlaywrightCapsJson();
            String encodedCaps = Base64.getEncoder()
                    .encodeToString(
                            capsJson.getBytes(StandardCharsets.UTF_8));

            String wsEndpoint =
                    System.getProperty(
                            "cloud.playwright.ws",
                            "wss://cdp.lambdatest.com/playwright"
                    ) + "?capabilities=" + encodedCaps;

            Browser browser =
                    playwright.chromium().connect(wsEndpoint);

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setAcceptDownloads(true)
            );

            Page page = context.newPage();

            return DriverContext.playwright(
                    playwright, browser, context, page);

        } catch (Exception e) {
            TestLogManager.error(
                "Failed to create Playwright cloud driver", e);
            throw new RuntimeException(e);
        }
    }

    /* ==========================================================
       CAPABILITIES (System Properties)
       ========================================================== */

    private Map<String, Object> buildLambdaTestOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("user", System.getProperty("LT_USERNAME"));
        options.put("accessKey", System.getProperty("LT_ACCESS_KEY"));
        options.put("build", System.getProperty("build", "Cloud Build"));
        options.put("name", System.getProperty("testName", "Cloud Test"));
        options.put("platform", System.getProperty("platform", "Windows 11"));
        options.put("video", Boolean.parseBoolean(
                System.getProperty("video", "true")));
        return options;
    }

    private Map<String, Object> buildBrowserStackOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("userName", System.getProperty("BS_USERNAME"));
        options.put("accessKey", System.getProperty("BS_ACCESS_KEY"));
        options.put("buildName", System.getProperty("build"));
        options.put("sessionName", System.getProperty("testName"));
        return options;
    }

    private Map<String, Object> buildSauceOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("username", System.getProperty("SAUCE_USERNAME"));
        options.put("accessKey", System.getProperty("SAUCE_ACCESS_KEY"));
        options.put("build", System.getProperty("build"));
        options.put("name", System.getProperty("testName"));
        return options;
    }

    private Map<String, Object> buildCBTOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("username", System.getProperty("CBT_USERNAME"));
        options.put("authkey", System.getProperty("CBT_ACCESS_KEY"));
        options.put("build", System.getProperty("build"));
        options.put("name", System.getProperty("testName"));
        return options;
    }

    private String buildPlaywrightCapsJson() {

        Map<String, Object> caps = new HashMap<>();

        caps.put("browser",
                System.getProperty("browser", "chromium"));
        caps.put("platform",
                System.getProperty("platform", "Windows 11"));
        caps.put("version",
                System.getProperty("browserVersion", "latest"));

        Map<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("user",
                System.getProperty("LT_USERNAME"));
        ltOptions.put("accessKey",
                System.getProperty("LT_ACCESS_KEY"));
        ltOptions.put("build",
                System.getProperty("build", "Cloud Build"));
        ltOptions.put("name",
                System.getProperty("testName", "Cloud Test"));
        ltOptions.put("video",
                Boolean.parseBoolean(
                        System.getProperty("video", "true")));

        caps.put("LT:Options", ltOptions);

        return new com.google.gson.Gson().toJson(caps);
    }
}
