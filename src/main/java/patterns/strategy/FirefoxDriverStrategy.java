package patterns.strategy;

import java.net.URI;
import java.nio.file.Paths;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import base.DriverContext;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Strategy implementation for Firefox (Selenium).
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

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Firefox driver", e);
        }
    }

    /* ==========================================================
       OPTIONS
       ========================================================== */

    private FirefoxOptions createFirefoxOptions() {

        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();

        setCommonPreferences(options);

        if (headless) {
            options.addArguments("--headless");
        }

        return options;
    }

    private void setCommonPreferences(FirefoxOptions options) {

        String downloadPath = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "resources",
                "data", "downloadedFile"
        ).toAbsolutePath().toString();

        // ---------- Downloads ----------
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadPath);
        options.addPreference("browser.download.useDownloadDir", true);
        options.addPreference(
                "browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,application/csv,text/csv");

        options.addPreference("pdfjs.disabled", true);

        // ---------- Notifications & Media ----------
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("geo.enabled", false);

        options.addPreference("media.navigator.enabled", false);
        options.addPreference("media.peerconnection.enabled", false);
        options.addPreference("media.eme.enabled", false);

        // ---------- DRM / Widevine ----------
        options.addPreference("media.gmp-widevinecdm.enabled", false);
        options.addPreference("media.gmp-widevinecdm.visible", false);
        options.addPreference("media.gmp-manager.updateEnabled", false);
        options.addPreference("media.gmp-provider-widevinecdm.updateEnabled", false);
        options.addPreference("media.gmp-provider-widevinecdm.visible", false);
    }

    /* ==========================================================
       META
       ========================================================== */

    @Override
    public String getBrowserName() {
        return "firefox";
    }

    @Override
    public boolean supports(String browserType) {
        return "firefox".equalsIgnoreCase(browserType)
                || "firefoxheadless".equalsIgnoreCase(browserType);
    }
}
