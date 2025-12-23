package patterns.strategy;

import java.nio.file.Paths;

import org.openqa.selenium.remote.DesiredCapabilities;

import com.microsoft.playwright.*;

import base.DriverContext;
import core.interfaces.EngineType;

public class WebKitDriverStrategy implements DriverStrategy {

    private final boolean headless;   // ignored
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

        Browser browser = playwright.webkit().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false) // WebKit does not support headless
        );

        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                        .setAcceptDownloads(true)
                        .setViewportSize(null)
        );

        Page page = context.newPage();

        return DriverContext.playwright(
                playwright, browser, context, page);
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
