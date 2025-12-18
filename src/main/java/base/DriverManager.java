package base;

import com.microsoft.playwright.*;

import core.interfaces.EngineType;
import patterns.strategy.DriverFactory;
import reporting.TestLogManager;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.time.Duration;

public final class DriverManager {

    private DriverManager() {}

    private static final ThreadLocal<DriverContext> CONTEXT = new ThreadLocal<>();

    /* =========================
       CREATE DRIVER
       ========================= */

    public static void createDriver(String browser) {

        EngineType engine = getEngineType();

        try {
            if (engine == EngineType.SELENIUM) {
                createSeleniumDriver(browser);
            } else {
                createPlaywrightDriver(browser);
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to create driver", e);
            throw new RuntimeException("Driver creation failed", e);
        }
    }

    public static void createDriver(String browser, DesiredCapabilities capabilities) {

        EngineType engine = getEngineType();

        if (engine == EngineType.PLAYWRIGHT) {
            throw new UnsupportedOperationException(
                    "DesiredCapabilities are not supported for Playwright");
        }

        try {
            WebDriver driver = DriverFactory.createDriver(browser, capabilities);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().window().maximize();

            CONTEXT.set(DriverContext.selenium(driver));
            TestLogManager.info("Selenium driver created with capabilities: " + browser);

        } catch (Exception e) {
            TestLogManager.error("Failed to create Selenium driver", e);
            throw new RuntimeException(e);
        }
    }

    /* =========================
       INTERNAL CREATORS
       ========================= */

    private static void createSeleniumDriver(String browser) {

        WebDriver driver = DriverFactory.createDriver(browser);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        CONTEXT.set(DriverContext.selenium(driver));
        TestLogManager.info("Selenium driver created: " + browser);
    }

    private static void createPlaywrightDriver(String browser) {

        Playwright playwright = Playwright.create();

        BrowserType browserType = switch (browser.toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit", "safari" -> playwright.webkit();
            default -> playwright.chromium();
        };

        Browser pwBrowser = browserType.launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(Boolean.parseBoolean(
                                System.getProperty("headless", "false")))
        );

        BrowserContext pwContext = pwBrowser.newContext();
        Page page = pwContext.newPage();

        CONTEXT.set(DriverContext.playwright(playwright, pwBrowser, pwContext, page));
        TestLogManager.info("Playwright browser launched: " + browser);
    }

    /* =========================
       GETTERS
       ========================= */

    public static DriverContext getContext() {
        return CONTEXT.get();
    }

    // ⚠️ Legacy Selenium support (DO NOT REMOVE)
    public static WebDriver getDriver() {
        DriverContext ctx = CONTEXT.get();
        return ctx != null && ctx.getEngineType() == EngineType.SELENIUM
                ? ctx.getWebDriver()
                : null;
    }

    public static Page getPage() {
        DriverContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getPage() : null;
    }

    public static AutomationContext getAutomationContext() {
        DriverContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getAutomationContext() : null;
    }

    public static EngineActions getActions() {
        DriverContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getEngineActions() : null;
    }

    /* =========================
       CLEANUP
       ========================= */

    public static void quitDriver() {

        DriverContext ctx = CONTEXT.get();
        if (ctx == null) return;

        try {
            if (ctx.getEngineType() == EngineType.SELENIUM) {
                ctx.getWebDriver().quit();
            } else {
                ctx.getPage().context().close();
                ctx.getBrowser().close();
                ctx.getPlaywright().close();
            }
        } catch (Exception e) {
            TestLogManager.warning("Error while quitting driver: " + e.getMessage());
        } finally {
            CONTEXT.remove();
        }
    }
    
    public static void setContext(DriverContext context) {
        CONTEXT.set(context);
    }

    /* =========================
       ENGINE RESOLUTION
       ========================= */

    private static EngineType getEngineType() {
        return EngineType.valueOf(
                System.getProperty("engine", "SELENIUM").toUpperCase()
        );
    }
}
