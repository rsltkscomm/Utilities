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
 * Chrome strategy supporting BOTH Selenium and Playwright
 * with guaranteed full-screen window on Windows.
 */
public class ChromeDriverStrategy implements DriverStrategy {

    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;

    public ChromeDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
        this.headless = headless;
        this.remote = remote;
        this.remoteUrl = remoteUrl;
    }

    @Override
    public DriverContext createDriver() {
        return createDriver(null);
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        EngineType engine =
                EngineType.valueOf(System.getProperty("engine", "SELENIUM"));

        if (engine == EngineType.PLAYWRIGHT) {
            return createPlaywrightChrome();
        }

        return createSeleniumChrome(capabilities);
    }

    /* ===================== SELENIUM ===================== */

    private DriverContext createSeleniumChrome(DesiredCapabilities capabilities) {

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

            // 🔥 THIS IS THE REAL FIX (Windows)
            if (!headless) {
                driver.manage().window().maximize();
            }

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create Selenium Chrome driver", e);
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
                "excludeSwitches", Collections.singletonList("enable-automation")
        );

        options.addArguments(
                "--disable-notifications",
                "--disable-gpu",
                "--no-sandbox"
        );

        if (headless) {
            options.addArguments(
                    "--headless=new",
                    "--window-size=1920,1080"
            );
        }

        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        return options;
    }

    /* ===================== PLAYWRIGHT ===================== */

    private DriverContext createPlaywrightChrome() {

        Playwright playwright = Playwright.create();

        Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(Collections.singletonList("--start-maximized"))
        );

        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
                        .setAcceptDownloads(true)
                        .setViewportSize(null) // 🔥 REQUIRED
        );

        Page page = context.newPage();

        return DriverContext.playwright(
                playwright, browser, context, page
        );
    }

    /* ===================== META ===================== */

    @Override
    public String getBrowserName() {
        return "chrome";
    }

    @Override
    public boolean supports(String browserType) {
        return "chrome".equalsIgnoreCase(browserType);
    }
}
