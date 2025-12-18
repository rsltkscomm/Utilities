package base;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import core.interfaces.EngineType;

public class SeleniumContext implements AutomationContext {

    private final WebDriver driver;

    public SeleniumContext(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public EngineType getEngineType() {
        return EngineType.SELENIUM;
    }

    @Override
    public String getLoginURL() {
        String key = System.getProperty("Environment").toUpperCase() + "_"
                   + System.getProperty("ReleaseVersion");
        return System.getProperty(key);
    }

    @Override
    public String getEnvironment() {
        String env = System.getProperty("SuiteName").toUpperCase();
        String loginURL = getLoginURL();
        if (loginURL != null && loginURL.length() >= 8 &&
            loginURL.substring(8).toUpperCase().startsWith(env)) {
            return env;
        }
        return env;
    }

    @Override
    public String getNormalizedPath(String path) {
        return path.replace("/", File.separator)
                   .replace("\\", File.separator)
                   .trim();
    }

    @Override
    public String detectFilePath(String path) {
        return getNormalizedPath(path);
    }

    @Override
    public Map<String, String> getDeviceSpecs() {
        Map<String, String> deviceInfo = new LinkedHashMap<>();

        if (driver instanceof RemoteWebDriver remote) {
            Capabilities caps = remote.getCapabilities();
            deviceInfo.put("Platform / OS",
                    caps.getPlatformName().name().toUpperCase());
            deviceInfo.put("Browser",
                    caps.getBrowserName().toUpperCase());
            deviceInfo.put("Browser Version",
                    caps.getBrowserVersion());
        }

        deviceInfo.put("Environment", getEnvironment());
        return deviceInfo;
    }
    

    public WebDriver getDriver() {
        return driver;
    }
}
