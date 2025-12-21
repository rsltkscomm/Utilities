package patterns.strategy;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import base.DriverContext;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Strategy implementation for Edge (Selenium).
 */
public class EdgeDriverStrategy implements DriverStrategy {

    private final boolean headless;
    private final boolean remote;
    private final String remoteUrl;

    public EdgeDriverStrategy(boolean headless, boolean remote, String remoteUrl) {
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

        EdgeOptions options = createEdgeOptions();

        if (capabilities != null) {
            options.merge(capabilities);
        }

        try {
            WebDriver driver;

            if (remote && remoteUrl != null && !remoteUrl.isBlank()) {
                driver = new RemoteWebDriver(
                        URI.create(remoteUrl).toURL(), options);
            } else {
                driver = new EdgeDriver(options);
            }

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Edge driver", e);
        }
    }

    /* ==========================================================
       OPTIONS
       ========================================================== */

    private EdgeOptions createEdgeOptions() {

        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();

        setCommonOptions(options);

        if (headless) {
            options.addArguments("--headless=new");
        }

        return options;
    }

    private void setCommonOptions(EdgeOptions options) {

        Map<String, Object> prefs = new HashMap<>();

        String downloadPath = Paths.get(
                System.getProperty("user.dir"),
                "src", "main", "resources",
                "data", "downloadedFile"
        ).toAbsolutePath().toString();

        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);

        options.setExperimentalOption("prefs", prefs);

        options.addArguments(
                "--enable-geolocation",
                "--disable-notifications",
                "--no-sandbox",
                "--disable-gpu",
                "--incognito"
        );

        options.setCapability(
                CapabilityType.ACCEPT_INSECURE_CERTS, true);
    }

    /* ==========================================================
       META
       ========================================================== */

    @Override
    public String getBrowserName() {
        return "edge";
    }

    @Override
    public boolean supports(String browserType) {
        return "edge".equalsIgnoreCase(browserType)
                || "edgeheadless".equalsIgnoreCase(browserType);
    }
}
