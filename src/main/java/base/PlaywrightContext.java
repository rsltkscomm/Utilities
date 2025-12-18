package base;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;

import core.interfaces.EngineType;


public class PlaywrightContext implements AutomationContext {

    private final Page page;

    public PlaywrightContext(Page page) {
        this.page = page;
    }

    @Override
    public EngineType getEngineType() {
        return EngineType.PLAYWRIGHT;
    }

    @Override
    public String getLoginURL() {
        String key = System.getProperty("Environment").toUpperCase() + "_"
                   + System.getProperty("ReleaseVersion");
        return System.getProperty(key);
    }

    @Override
    public String getEnvironment() {
        return System.getProperty("SuiteName").toUpperCase();
    }

    @Override
    public String getNormalizedPath(String path) {
        return path.replace("/", File.separator)
                   .replace("\\", File.separator)
                   .trim();
    }

    @Override
    public String detectFilePath(String path) {
        // Playwright does not need LocalFileDetector
        return getNormalizedPath(path);
    }

    @Override
    public Map<String, String> getDeviceSpecs() {
        Map<String, String> deviceInfo = new LinkedHashMap<>();

        Browser browser = page.context().browser();
        BrowserType type = browser.browserType();

        deviceInfo.put("Platform / OS",
                System.getProperty("os.name").toUpperCase());
        deviceInfo.put("Browser", type.name().toUpperCase());
        deviceInfo.put("Browser Version", browser.version());
        deviceInfo.put("Environment", getEnvironment());

        return deviceInfo;
    }
    
    public Page getPage() {
        return page;
    }
}
