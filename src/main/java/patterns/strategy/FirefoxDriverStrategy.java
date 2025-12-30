package patterns.strategy;

import java.net.URI;
import java.nio.file.Paths;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import base.DriverContext;
import core.interfaces.EngineType;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Firefox strategy supporting BOTH Selenium and Playwright
 * with guaranteed full-screen window on Windows.
 */
public class FirefoxDriverStrategy implements DriverStrategy {

    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;

    public FirefoxDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
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
            return createPlaywrightFirefox();
        }

        return createSeleniumFirefox(capabilities);
    }

    /* ===================== SELENIUM ===================== */

    private DriverContext createSeleniumFirefox(DesiredCapabilities capabilities) {

        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = createFirefoxOptions();

        if (capabilities != null) {
            options.merge(capabilities);
        }

        try {
            WebDriver driver;

            if (remote && remoteUrl != null && !remoteUrl.isBlank()) {
                driver = new RemoteWebDriver(
                        URI.create(remoteUrl).toURL(), options);
            } else {
                driver = new FirefoxDriver(options);
            }

            // 🔥 REAL FIX (OS-level maximize)
            if (!headless) {
                driver.manage().window().maximize();
            }

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create Selenium Firefox driver", e);
        }
    }

    private FirefoxOptions createFirefoxOptions() {

        FirefoxOptions options = new FirefoxOptions();

        String downloadPath = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "resources", "data", "downloadedFile"
        ).toAbsolutePath().toString();

        // ---------- Downloads ----------
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadPath);
        options.addPreference("browser.download.useDownloadDir", true);
        options.addPreference(
                "browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,application/csv,text/csv"
        );
        options.addPreference("pdfjs.disabled", true);

        // ---------- Notifications ----------
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("geo.enabled", false);

        if (headless) {
            options.addArguments("--headless");
            // Headless Firefox needs explicit size
            options.addArguments("--width=1920");
            options.addArguments("--height=1080");
        }

        return options;
    }

    /* ===================== PLAYWRIGHT ===================== */

    private DriverContext createPlaywrightFirefox() {

        Playwright playwright = Playwright.create();

        Browser browser = playwright.firefox().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
        );

        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
                        .setAcceptDownloads(true)
                        .setViewportSize(null) // 🔥 FULL SCREEN FIX
        );

        Page page = context.newPage();

        return DriverContext.playwright(
                playwright, browser, context, page
        );
    }

    /* ===================== META ===================== */

    @Override
    public String getBrowserName() {
        return "firefox";
    }

    @Override
    public boolean supports(String browserType) {
        return "firefox".equalsIgnoreCase(browserType);
    }
}
