package base;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import pages.PageFactory;
import reporting.ExtentManager;
import seleniumUtils.AlertUtil;

public class PageBase extends AlertUtil {

    WebDriver driver;
    PageFactory pageFactory;

    public PageBase(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
        this.pageFactory = pageFactory;
    }

    /* -------------------- URL & Environment -------------------- */

    public static String getLoginURL() {
        String key = System.getProperty("Environment").toUpperCase() + "_" + System.getProperty("ReleaseVersion");
        return System.getProperty(key);
    }

    public static String getEnvironment() {
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
        return pathString.replace("/", File.separator).replace("\\", File.separator).trim();
    }

    public static String detectFilePath(String path) {
        path = getNormalizedPath(path);
        try {
            if (GridManager.isGrid.get() && DriverManager.getDriver() instanceof RemoteWebDriver) {
                ((RemoteWebDriver) DriverManager.getDriver()).setFileDetector(new LocalFileDetector());
            }
        } catch (Exception e) {
            ExtentManager.failTest("Failed to set RemoteWebDriver file detector: " + e.getMessage());
        }
        return path;
    }

    /* -------------------- CAPABILITIES -------------------- */

    public static Capabilities getCapabilities() {
        WebDriver driver = DriverManager.getDriver();
        if (driver instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) driver).getCapabilities();
        }
        return null;
    }

    /* -------------------- DEVICE INFO -------------------- */

    public static void getDeviceSpecs() {
        Map<String, String> deviceInfo = new LinkedHashMap<>();

        Capabilities capabilities = getCapabilities();
        String platform = (capabilities != null) ? capabilities.getPlatformName().name().toUpperCase() : "LOCAL";
        String browser = (capabilities != null) ? capabilities.getBrowserName().toUpperCase() : "LOCAL_DRIVER";
        String browserVersion = (capabilities != null) ? capabilities.getBrowserVersion() : "N/A";

        ExtentManager.infoLabel("<b> DEVICE SPECIFICATIONS </b>");
        ExtentManager.getTest().assignDevice(platform).assignCategory(browser);
        ExtentManager.infoTest("The Device Specifications are listed below,");

        deviceInfo.put("Platform / OS", platform);
        deviceInfo.put("Browser", browser);
        deviceInfo.put("Browser Version", browserVersion);
        deviceInfo.put("Environment", getEnvironment());

        ExtentManager.customReport(deviceInfo);
    }

    /* -------------------- FOLDER SETUP -------------------- */

    public static void ensureScreenshotFolderExists() {
        try {
            String screenshotDir = System.getProperty("user.dir") + "/src/test/resources/ExtentReports/ScreenShots/";
            File dir = new File(getNormalizedPath(screenshotDir));
            if (!dir.exists()) {
                dir.mkdirs();
                ExtentManager.infoTest("Created screenshot folder: " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            ExtentManager.failTest("Failed to create screenshot folder: " + e.getMessage());
        }
    }
}
