package patterns.strategy;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
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
 * Brave strategy supporting BOTH Selenium and Playwright
 * with guaranteed full-screen window on Windows.
 */
public class BraveDriverStrategy implements DriverStrategy {

    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;

    public BraveDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
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
            return createPlaywrightBrave();
        }

        return createSeleniumBrave(capabilities);
    }

    /* ===================== SELENIUM ===================== */

    private DriverContext createSeleniumBrave(DesiredCapabilities capabilities) {

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = createBraveOptions();

        if (capabilities != null) {
            options.merge(capabilities);
        }

        try {
            WebDriver driver;

            if (remote && remoteUrl != null && !remoteUrl.isBlank()) {
                driver = new RemoteWebDriver(
                        URI.create(remoteUrl).toURL(), options);
            } else {
                driver = new ChromeDriver(options);
            }

            // 🔥 REAL FIX – OS-level maximize
            if (!headless) {
                driver.manage().window().maximize();
            }

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create Selenium Brave driver", e);
        }
    }

    private ChromeOptions createBraveOptions() {

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();

        String downloadPath = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "resources",
                "data", "downloadedFile"
        ).toAbsolutePath().toString();

        // 🔴 REQUIRED: Brave binary path
        // Windows default:
        options.setBinary(
                "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe"
        );
        // (Mac: /Applications/Brave Browser.app/Contents/MacOS/Brave Browser)
        // (Linux: /usr/bin/brave-browser)

        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);

        options.setExperimentalOption("prefs", prefs);

        options.addArguments(
                "--disable-notifications",
                "--disable-gpu",
                "--no-sandbox",
                "--incognito"
        );

        if (headless) {
            options.addArguments(
                    "--headless=new",
                    "--window-size=1920,1080"
            );
        }

        options.setCapability(
                CapabilityType.ACCEPT_INSECURE_CERTS, true);

        return options;
    }

    /* ===================== PLAYWRIGHT ===================== */

    private DriverContext createPlaywrightBrave() {

        Playwright playwright = Playwright.create();

        Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setChannel("brave")   // 🔥 Brave channel
                        .setHeadless(headless)
                        .setArgs(List.of("--start-maximized"))
        );

        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
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
        return "brave";
    }

    @Override
    public boolean supports(String browserType) {
        return "brave".equalsIgnoreCase(browserType)
                || "braveheadless".equalsIgnoreCase(browserType);
    }
}
