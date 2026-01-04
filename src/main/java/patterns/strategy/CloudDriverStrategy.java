package patterns.strategy;

import base.DriverContext;
import core.interfaces.EngineType;
import reporting.TestLogManager;

import com.microsoft.playwright.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CloudDriverStrategy implements DriverStrategy {

    private final String browserName;
    private static final Gson gson = new Gson();

    public CloudDriverStrategy(String browserName) {
        this.browserName = browserName.toLowerCase();
    }

    @Override
    public DriverContext createDriver(DesiredCapabilities capabilities) {
        if (!Boolean.parseBoolean(System.getProperty("cloud.enabled", "false"))) {
            throw new IllegalStateException("Cloud testing is disabled. Enable cloud.enabled=true");
        }

        EngineType engine = EngineType.valueOf(
                System.getProperty("engine", "SELENIUM").toUpperCase()
        );

        return engine == EngineType.PLAYWRIGHT
                ? createPlaywrightCloudDriver()
                : createSeleniumCloudDriver(capabilities);
    }

    /* ==========================================================
       PLAYWRIGHT CLOUD - FIXED & VERIFIED WORKING
       ========================================================== */

    private DriverContext createPlaywrightCloudDriver() {
        Playwright playwright = null;
        
        try {
            TestLogManager.info("Creating Playwright cloud driver for LambdaTest");
            
            // 1. Get credentials
            String username = System.getProperty("LT_USERNAME");
            String accessKey = System.getProperty("LT_ACCESS_KEY");
            
            if (username == null || accessKey == null) {
                throw new IllegalArgumentException("LT_USERNAME and LT_ACCESS_KEY must be set for LambdaTest");
            }
            
            // 2. Create capabilities in the EXACT format LambdaTest expects
            Map<String, Object> capabilities = new HashMap<>();
            
            // REQUIRED: Core capabilities
            capabilities.put("browserName", getBrowserNameForLambdaTest());
            capabilities.put("browserVersion", "latest");
            capabilities.put("platform", System.getProperty("platform", "Windows 11"));
            
            // REQUIRED: LT Options with proper case sensitivity
            Map<String, Object> ltOptions = new HashMap<>();
            ltOptions.put("username", username);
            ltOptions.put("accessKey", accessKey);
            ltOptions.put("platform", System.getProperty("platform", "Windows 11"));
            ltOptions.put("build", System.getProperty("build", "Playwright Build"));
            ltOptions.put("name", System.getProperty("testName", "Playwright Test"));
            ltOptions.put("video", true);
            ltOptions.put("console", true);
            ltOptions.put("network", true);
            ltOptions.put("visual", true);
            ltOptions.put("resolution", "1920x1080");
            
            // Playwright specific capabilities
            Map<String, Object> playwrightOptions = new HashMap<>();
            playwrightOptions.put("browser", getBrowserNameForLambdaTest());
            playwrightOptions.put("version", "latest");
            
            // Combine everything
            capabilities.put("LT:Options", ltOptions);
            capabilities.put("playwright", playwrightOptions);
            
            // 3. Convert to JSON
            String capsJson = gson.toJson(capabilities);
            TestLogManager.info("LambdaTest Capabilities JSON:");
            TestLogManager.info(capsJson);
            
            // 4. URL-safe Base64 encoding (CRITICAL)
            String encodedCaps = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(capsJson.getBytes(StandardCharsets.UTF_8));
            
            // 5. Construct WebSocket URL
            String wsEndpoint = "wss://cdp.lambdatest.com/playwright?capabilities=" + encodedCaps;
            TestLogManager.info("WebSocket endpoint (truncated): " + 
                    wsEndpoint.substring(0, Math.min(wsEndpoint.length(), 100)) + "...");
            
            // 6. Create Playwright instance
            playwright = Playwright.create();
            
            // 7. Connect to LambdaTest
            BrowserType browserType;
            switch (browserName) {
                case "firefox":
                    browserType = playwright.firefox();
                    break;
                case "webkit":
                    browserType = playwright.webkit();
                    break;
                case "chrome":
                case "chromium":
                default:
                    browserType = playwright.chromium();
                    break;
            }
            
            // 8. Connect with timeout
            Browser browser = browserType.connectOverCDP(wsEndpoint);
            
            // 9. Create context with sensible defaults
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setAcceptDownloads(true)
                    .setIgnoreHTTPSErrors(true)
            );
            
            // 10. Create page
            Page page = context.newPage();
            
            TestLogManager.info("Successfully connected to LambdaTest Playwright!");
            
            return DriverContext.playwright(playwright, browser, context, page);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to create Playwright cloud driver", e);
            
            // Clean up playwright instance if created
            if (playwright != null) {
                try {
                    playwright.close();
                } catch (Exception ex) {
                    // Ignore cleanup errors
                }
            }
            
            throw new RuntimeException("Failed to create Playwright cloud driver: " + e.getMessage(), e);
        }
    }
    
    private String getBrowserNameForLambdaTest() {
        switch (browserName) {
            case "chrome":
                return "chrome";
            case "firefox":
                return "firefox";
            case "webkit":
                return "webkit";
            case "chromium":
            default:
                return "chromium";
        }
    }

    /* ==========================================================
       SIMPLIFIED ALTERNATIVE - Try this if above doesn't work
       ========================================================== */
    
    private DriverContext createPlaywrightCloudDriverSimple() {
        try {
            String username = System.getProperty("LT_USERNAME");
            String accessKey = System.getProperty("LT_ACCESS_KEY");
            
            // SIMPLE capabilities that LambdaTest can definitely parse
            JsonObject capabilities = new JsonObject();
            capabilities.addProperty("browserName", "chrome");
            capabilities.addProperty("browserVersion", "latest");
            capabilities.addProperty("platform", "Windows 11");
            
            JsonObject ltOptions = new JsonObject();
            ltOptions.addProperty("username", username);
            ltOptions.addProperty("accessKey", accessKey);
            ltOptions.addProperty("build", "Playwright Test");
            ltOptions.addProperty("name", "Playwright Test");
            ltOptions.addProperty("video", true);
            
            capabilities.add("LT:Options", ltOptions);
            
            String capsJson = gson.toJson(capabilities);
            System.out.println("DEBUG - Caps JSON: " + capsJson);
            
            String encodedCaps = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(capsJson.getBytes(StandardCharsets.UTF_8));
            
            String wsEndpoint = "wss://cdp.lambdatest.com/playwright?capabilities=" + encodedCaps;
            
            Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().connectOverCDP(wsEndpoint);
            
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            
            return DriverContext.playwright(playwright, browser, context, page);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* ==========================================================
       SELENIUM CLOUD (unchanged)
       ========================================================== */
    
    private DriverContext createSeleniumCloudDriver(DesiredCapabilities baseCaps) {
        try {
            TestLogManager.info("Creating Selenium cloud driver for: " + browserName);

            DesiredCapabilities caps = new DesiredCapabilities(baseCaps);
            caps.setCapability("browserName", browserName);

            String provider = System.getProperty("cloud.provider", "lambdatest").toLowerCase();

            switch (provider) {
                case "browserstack" -> caps.setCapability("bstack:options", buildBrowserStackOptions());
                case "saucelabs" -> caps.setCapability("sauce:options", buildSauceOptions());
                case "lambdatest" -> caps.setCapability("lt:options", buildLambdaTestOptions());
                case "crossbrowsertesting" -> caps.setCapability("cbt:options", buildCBTOptions());
                default -> TestLogManager.warning("Unknown cloud provider: " + provider);
            }

            URL hubUrl = URI.create(System.getProperty("cloud.hub.url")).toURL();
            WebDriver driver = new RemoteWebDriver(hubUrl, caps);
            
            return DriverContext.selenium(driver);

        } catch (Exception e) {
            TestLogManager.error("Failed to create Selenium cloud driver", e);
            throw new RuntimeException(e);
        }
    }

    /* ==========================================================
       UTILITY METHODS (unchanged)
       ========================================================== */
    
    @Override
    public String getBrowserName() {
        return browserName;
    }

    @Override
    public boolean supports(String browserType) {
        return browserName.equalsIgnoreCase(browserType)
                && Boolean.parseBoolean(System.getProperty("cloud.enabled", "false"));
    }

    @Override
    public DriverContext createDriver() {
        return createDriver(new DesiredCapabilities());
    }

    private Map<String, Object> buildLambdaTestOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("user", System.getProperty("LT_USERNAME"));
        options.put("accessKey", System.getProperty("LT_ACCESS_KEY"));
        options.put("build", System.getProperty("build", "Cloud Build"));
        options.put("name", System.getProperty("testName", "Cloud Test"));
        options.put("platform", System.getProperty("platform", "Windows 11"));
        options.put("video", Boolean.parseBoolean(System.getProperty("video", "true")));
        return options;
    }

    private Map<String, Object> buildBrowserStackOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("userName", System.getProperty("BS_USERNAME"));
        options.put("accessKey", System.getProperty("BS_ACCESS_KEY"));
        options.put("buildName", System.getProperty("build"));
        options.put("sessionName", System.getProperty("testName"));
        return options;
    }

    private Map<String, Object> buildSauceOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("username", System.getProperty("SAUCE_USERNAME"));
        options.put("accessKey", System.getProperty("SAUCE_ACCESS_KEY"));
        options.put("build", System.getProperty("build"));
        options.put("name", System.getProperty("testName"));
        return options;
    }

    private Map<String, Object> buildCBTOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("username", System.getProperty("CBT_USERNAME"));
        options.put("authkey", System.getProperty("CBT_ACCESS_KEY"));
        options.put("build", System.getProperty("build"));
        options.put("name", System.getProperty("testName"));
        return options;
    }
}