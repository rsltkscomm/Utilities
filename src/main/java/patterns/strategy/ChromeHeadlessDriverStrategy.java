package patterns.strategy;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.microsoft.playwright.*;

import base.DriverContext;
import core.interfaces.EngineType;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Chrome Headless strategy supporting BOTH Selenium and Playwright
 * based on EngineType.
 */
public class ChromeHeadlessDriverStrategy implements DriverStrategy {

    private final boolean remote;
    private final String remoteUrl;

    public ChromeHeadlessDriverStrategy() {
        this(false, null);
    }

    public ChromeHeadlessDriverStrategy(boolean remote, String remoteUrl) {
        this.remote = remote;
        this.remoteUrl = remoteUrl;
    }

    /* ==========================================================
       ENTRY
       ========================================================== */

    @Override
    public DriverContext createDriver() {
        return createDriver(null);
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        EngineType engine =
                EngineType.valueOf(System.getProperty("engine", "SELENIUM"));

        if (engine == EngineType.PLAYWRIGHT) {
            return createPlaywrightChromeHeadless();
        }

        return createSeleniumChromeHeadless(capabilities);
    }

    /* ==========================================================
       SELENIUM – HEADLESS
       ========================================================== */

    private DriverContext createSeleniumChromeHeadless(
            DesiredCapabilities capabilities) {

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = createChromeOptions();

        if (capabilities != null) {
            options.merge(capabilities);
        }

        try {
            WebDriver driver;

            if (remote && remoteUrl != null) {
                driver = new RemoteWebDriver(
                        URI.create(remoteUrl).toURL(), options);
            } else {
                driver = new ChromeDriver(options);
            }

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create Selenium Chrome Headless driver", e);
        }
    }

    private ChromeOptions createChromeOptions() {

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();

        String downloadPath = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "resources", "data", "downloadedFile"
        ).toAbsolutePath().toString();

        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);

        options.setExperimentalOption("prefs", prefs);
        options.setExperimentalOption(
                "excludeSwitches", Collections.singletonList("enable-automation"));

        options.addArguments(
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-notifications",
                "--disable-dev-shm-usage"
        );

        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        return options;
    }

    /* ==========================================================
       PLAYWRIGHT – HEADLESS
       ========================================================== */

    private DriverContext createPlaywrightChromeHeadless() {

        Playwright playwright = Playwright.create();

        Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
        );

        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                        .setAcceptDownloads(true)
        );

        Page page = context.newPage();

        return DriverContext.playwright(
                playwright, browser, context, page);
    }

    /* ==========================================================
       META
       ========================================================== */

    @Override
    public String getBrowserName() {
        return "chromeheadless";
    }

    @Override
    public boolean supports(String browserType) {
        return "chromeheadless".equalsIgnoreCase(browserType)
            || "chrome-headless".equalsIgnoreCase(browserType);
    }
}
