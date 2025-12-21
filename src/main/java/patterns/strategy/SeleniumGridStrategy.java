package patterns.strategy;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import base.DriverContext;
import reporting.TestLogManager;

public class SeleniumGridStrategy implements DriverStrategy {

    @Override
    public DriverContext createDriver() {
        return createDriver(null);
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {

        try {
            String browser =
                    System.getProperty("GRID_BROWSER", "chrome").toLowerCase();

            MutableCapabilities options = buildOptions(browser);

            if (capabilities != null) {
                options.merge(capabilities);
            }

            String gridUrl = getRemoteWebDriverURL();
            TestLogManager.info("Connecting to Selenium Grid: " + gridUrl);

            WebDriver driver =
                    new RemoteWebDriver(new URL(gridUrl), options);

            return DriverContext.selenium(driver);

        } catch (Exception e) {
            TestLogManager.error("Failed to create Selenium Grid driver", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBrowserName() {
        return "grid";
    }

    @Override
    public boolean supports(String browserType) {
        return "grid".equalsIgnoreCase(browserType)
                || "seleniumgrid".equalsIgnoreCase(browserType);
    }

    /* =========================================================
       GRID URL
       ========================================================= */

    private static String getRemoteWebDriverURL() {

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ipAddress = socket.getLocalAddress().getHostAddress();
            return "http://" + ipAddress + ":" + System.getProperty("Grid_PORT", "4444") + "/wd/hub";
        } catch (Exception e) {
            TestLogManager.error("Exception in getRemoteWebDriverURL", e);
            throw new RuntimeException(e);
        }
    }

    /* =========================================================
       OPTIONS
       ========================================================= */

    private MutableCapabilities buildOptions(String browserName) {

        boolean headless =
                Boolean.parseBoolean(System.getProperty("GRID_HEADLESS", "false"));
        String resolution =
                System.getProperty("GRID_RESOLUTION", "1920x1080");

        switch (browserName) {

            case "firefox" -> {
                FirefoxOptions opts = new FirefoxOptions();
                if (headless) opts.addArguments("-headless");

                FirefoxProfile profile = new FirefoxProfile();
                profile.setPreference("browser.download.folderList", 2);
                profile.setPreference("browser.download.dir",
                        Paths.get(System.getProperty("user.dir"), "target", "downloads").toString());
                profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                        "application/pdf,application/octet-stream,text/csv");
                profile.setPreference("pdfjs.disabled", true);

                opts.setProfile(profile);
                opts.setAcceptInsecureCerts(true);
                return opts;
            }

            case "edge" -> {
                EdgeOptions opts = new EdgeOptions();
                opts.addArguments("--window-size=" + resolution);
                if (headless) opts.addArguments("--headless=new");
                opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
                return opts;
            }

            case "safari" -> {
                SafariOptions opts = new SafariOptions();
                opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
                return opts;
            }

            default -> {
                ChromeOptions opts = new ChromeOptions();
                opts.addArguments(
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-notifications",
                        "--window-size=" + resolution
                );

                if (headless) opts.addArguments("--headless=new");

                Map<String, Object> prefs = new HashMap<>();
                prefs.put("download.prompt_for_download", false);
                prefs.put("profile.default_content_settings.popups", 0);
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                prefs.put("download.default_directory",
                        Paths.get(System.getProperty("user.dir"), "target", "downloads").toString());

                opts.setExperimentalOption("prefs", prefs);
                opts.setExperimentalOption(
                        "excludeSwitches", Collections.singletonList("enable-automation"));
                opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

                return opts;
            }
        }
    }
}
