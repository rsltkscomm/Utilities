package core.playwright;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;

import core.interfaces.PageBaseInterface;
import reporting.ExtentManager;

public class PlaywrightPageBase extends PlaywrightAlertUtil
        implements PageBaseInterface {

    protected final Page page;

    public PlaywrightPageBase(Page page) {
        super(page);
        this.page = page;
    }

    /* -------------------- URL & ENVIRONMENT -------------------- */

    public String getLoginURL() {
        String key = System.getProperty("Environment").toUpperCase()
                + "_" + System.getProperty("ReleaseVersion");
        return System.getProperty(key);
    }

    public String getEnvironment() {
        String env = System.getProperty("SuiteName").toUpperCase();
        String loginURL = getLoginURL();

        if (loginURL != null && loginURL.length() >= 8) {
            loginURL = loginURL.substring(8).toUpperCase();
            if (loginURL.startsWith(env)) return env;
        }
        return env;
    }

    /* -------------------- PATH UTILITIES -------------------- */

    public static String getNormalizedPath(String pathString) {
        return pathString
                .replace("/", File.separator)
                .replace("\\", File.separator)
                .trim();
    }

    public static String detectFilePath(String path) {
        // Playwright DOES NOT need LocalFileDetector
        // File upload works automatically
        return getNormalizedPath(path);
    }

    /* -------------------- DEVICE / BROWSER INFO -------------------- */

    @Override
    public void getDeviceSpecs() {
        try {
            Browser browser = page.context().browser();
            BrowserType browserType = browser.browserType();

            String browserName = browserType.name().toUpperCase();
            String browserVersion = browser.version();
            String platform = System.getProperty("os.name").toUpperCase();

            ExtentManager.infoLabel("<b> DEVICE SPECIFICATIONS </b>");
            ExtentManager.getTest()
                    .assignDevice(platform)
                    .assignCategory(browserName);

            ExtentManager.infoTest("The Device Specifications are listed below,");

            Map<String, String> deviceInfo = new LinkedHashMap<>();
            deviceInfo.put("Platform / OS", platform);
            deviceInfo.put("Browser", browserName);
            deviceInfo.put("Browser Version", browserVersion);
            deviceInfo.put("Environment", getEnvironment());

            ExtentManager.customReport(deviceInfo);

        } catch (Exception e) {
            ExtentManager.failTest("Failed to capture device specs: " + e.getMessage());
        }
    }

    /* -------------------- SCREENSHOT FOLDER -------------------- */

    @Override
    public void ensureScreenshotFolderExists() {
        try {
            String screenshotDir =
                    System.getProperty("user.dir")
                            + "/src/test/resources/ExtentReports/ScreenShots/";

            File dir = new File(getNormalizedPath(screenshotDir));
            if (!dir.exists()) {
                dir.mkdirs();
                ExtentManager.infoTest(
                        "Created screenshot folder: " + dir.getAbsolutePath()
                );
            }
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Failed to create screenshot folder: " + e.getMessage()
            );
        }
    }
}
