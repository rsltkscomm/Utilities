package patterns.strategy;

import java.nio.file.Paths;

import org.openqa.selenium.remote.DesiredCapabilities;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import base.DriverContext;
import core.interfaces.EngineType;

/**
 * WebKit strategy (Playwright only).
 * WebKit does NOT support headless or OS-level maximize.
 * Full screen is achieved by disabling viewport.
 */
public class WebKitDriverStrategy implements DriverStrategy {

    private final boolean headless;   // ignored (WebKit does not support headless)
    private final boolean remote;     // ignored
    private final String remoteUrl;   // ignored

    public WebKitDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
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
                EngineType.valueOf(System.getProperty("engine", "PLAYWRIGHT"));

        if (engine != EngineType.PLAYWRIGHT) {
            throw new UnsupportedOperationException(
                    "WebKit is supported only with Playwright");
        }

        return createPlaywrightWebKit();
    }

    /* ===================== PLAYWRIGHT ===================== */

    private DriverContext createPlaywrightWebKit() {

        String downloadPath = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "resources",
                "data", "downloadedFile"
        ).toAbsolutePath().toString();

        Playwright playwright = Playwright.create();

        // 🔴 WebKit MUST be headed
        Browser browser = playwright.webkit().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
        );

        // 🔥 This is the ONLY full-screen control for WebKit
        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                        .setAcceptDownloads(true)
                        .setViewportSize(null) // ✅ FULL SCREEN
        );

        Page page = context.newPage();

        return DriverContext.playwright(
                playwright, browser, context, page
        );
    }

    /* ===================== META ===================== */

    @Override
    public String getBrowserName() {
        return "webkit";
    }

    @Override
    public boolean supports(String browserType) {
        return "webkit".equalsIgnoreCase(browserType)
                || "safariengine".equalsIgnoreCase(browserType);
    }
}
