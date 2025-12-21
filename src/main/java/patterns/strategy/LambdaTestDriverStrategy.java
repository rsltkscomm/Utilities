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

public class LambdaTestDriverStrategy implements DriverStrategy {

    /* =========================
       META
       ========================= */

    @Override
    public String getBrowserName() {
        return System.getProperty("LT_BROWSER", "chrome");
    }

    @Override
    public boolean supports(String browserType) {
        return "lambdatest".equalsIgnoreCase(browserType)
                || getBrowserName().equalsIgnoreCase(browserType);
    }

    /* =========================
       ENTRY
       ========================= */

    @Override
    public DriverContext createDriver() {
        return createDriver(new DesiredCapabilities());
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        EngineType engine = EngineType.valueOf(
                System.getProperty("engine", "SELENIUM").toUpperCase()
        );

        return engine == EngineType.PLAYWRIGHT
                ? createPlaywrightDriver()
                : createSeleniumDriver(capabilities);
    }

    /* =========================================================
       SELENIUM – LambdaTest Grid
       ========================================================= */

    private DriverContext createSeleniumDriver(DesiredCapabilities baseCaps) {

        try {
            TestLogManager.info("Creating LambdaTest Selenium driver");

            DesiredCapabilities caps = new DesiredCapabilities(baseCaps);
            caps.setCapability("browserName",
                    System.getProperty("LT_BROWSER", "chrome"));
            caps.setCapability("browserVersion",
                    System.getProperty("LT_BROWSER_VERSION", "latest"));
            caps.setCapability("platformName",
                    System.getProperty("LT_PLATFORM", "macOS 13"));

            caps.setCapability("LT:Options", buildSeleniumLTOptions());

            URL gridUrl = URI.create(
                    System.getProperty("LT_GRID_URL")
            ).toURL();

            WebDriver driver = new RemoteWebDriver(gridUrl, caps);
            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create LambdaTest Selenium driver", e);
        }
    }

    /* =========================================================
       PLAYWRIGHT – LambdaTest Playwright Grid
       ========================================================= */

    private DriverContext createPlaywrightDriver() {

        try {
            TestLogManager.info("Creating LambdaTest Playwright driver");

            Playwright playwright = Playwright.create();

            String capsJson = buildPlaywrightCapsJson();
            String encodedCaps = Base64.getEncoder()
                    .encodeToString(
                            capsJson.getBytes(StandardCharsets.UTF_8));

            String wsEndpoint =
                    "wss://cdp.lambdatest.com/playwright?capabilities="
                            + encodedCaps;

            Browser browser =
                    playwright.chromium().connect(wsEndpoint);

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1920, 1080)
                            .setAcceptDownloads(true)
            );

            Page page = context.newPage();

            return DriverContext.playwright(
                    playwright, browser, context, page);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create LambdaTest Playwright driver", e);
        }
    }

    /* =========================================================
       CAPABILITIES (LT_* based)
       ========================================================= */

    private Map<String, Object> buildSeleniumLTOptions() {

        Map<String, Object> options = new HashMap<>();

        options.put("user",
                System.getProperty("LT_USERNAME"));
        options.put("accessKey",
                System.getProperty("LT_ACCESS_KEY"));

        options.put("build",
                System.getProperty("LT_BUILD"));
        options.put("name",
                System.getProperty("LT_NAME"));

        options.put("selenium_version",
                System.getProperty("LT_SELENIUM_VERSION", "4.22.0"));

        options.put("resolution",
                System.getProperty("LT_RESOLUTION", "1920x1080"));

        options.put("network",
                Boolean.parseBoolean(
                        System.getProperty("LT_NETWORK", "true")));
        options.put("video",
                Boolean.parseBoolean(
                        System.getProperty("LT_VIDEO", "true")));
        options.put("console",
                Boolean.parseBoolean(
                        System.getProperty("LT_CONSOLE", "true")));
        options.put("visual",
                Boolean.parseBoolean(
                        System.getProperty("LT_VISUAL", "true")));

        options.put("geoLocation",
                System.getProperty("LT_GEO_LOCATION", "IN"));

        return options;
    }

    private String buildPlaywrightCapsJson() {

        Map<String, Object> caps = new HashMap<>();

        caps.put("browser",
                System.getProperty("LT_BROWSER", "chromium"));
        caps.put("version",
                System.getProperty("LT_BROWSER_VERSION", "latest"));
        caps.put("platform",
                System.getProperty("LT_PLATFORM", "macOS 13"));

        Map<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("user",
                System.getProperty("LT_USERNAME"));
        ltOptions.put("accessKey",
                System.getProperty("LT_ACCESS_KEY"));
        ltOptions.put("build",
                System.getProperty("LT_BUILD"));
        ltOptions.put("name",
                System.getProperty("LT_NAME"));
        ltOptions.put("video",
                Boolean.parseBoolean(
                        System.getProperty("LT_VIDEO", "true")));
        ltOptions.put("network",
                Boolean.parseBoolean(
                        System.getProperty("LT_NETWORK", "true")));
        ltOptions.put("console",
                Boolean.parseBoolean(
                        System.getProperty("LT_CONSOLE", "true")));
        ltOptions.put("visual",
                Boolean.parseBoolean(
                        System.getProperty("LT_VISUAL", "true")));

        caps.put("LT:Options", ltOptions);

        return new com.google.gson.Gson().toJson(caps);
    }
}
