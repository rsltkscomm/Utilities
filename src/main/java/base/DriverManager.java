package base;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import core.interfaces.EngineType;
import patterns.strategy.DriverFactory;
import reporting.TestLogManager;

public final class DriverManager {

    private DriverManager() {}

    private static final ThreadLocal<DriverContext> CONTEXT = new ThreadLocal<>();

    /* =========================
       CREATE DRIVER
       ========================= */

    public static void createDriver(String browser) {
        try {
            DriverContext ctx = DriverFactory.createDriver(browser);

            if (ctx.getEngineType() == EngineType.SELENIUM) {
                WebDriver driver = ctx.getWebDriver();
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                driver.manage().window().maximize();
            }

            CONTEXT.set(ctx);
            TestLogManager.info(
                    ctx.getEngineType() + " driver created for browser: " + browser);

        } catch (Exception e) {
            TestLogManager.error("Failed to create driver", e);;
            throw new RuntimeException("Driver creation failed", e);
        }
    }

    public static void createDriver(String browser, DesiredCapabilities capabilities) {

        if (getEngineType() == EngineType.PLAYWRIGHT) {
            throw new UnsupportedOperationException(
                    "DesiredCapabilities are not supported for Playwright");
        }

        try {
            DriverContext ctx =
                    DriverFactory.createDriver(browser, capabilities);

            WebDriver driver = ctx.getWebDriver();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().window().maximize();

            CONTEXT.set(ctx);
            TestLogManager.info(
                    "Selenium driver created with capabilities: " + browser);

        } catch (Exception e) {
            TestLogManager.error("Failed to create Selenium driver", e);
            throw new RuntimeException(e);
        }
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

    public static com.microsoft.playwright.Page getPage() {
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
                ctx.getBrowserContext().close();
                ctx.getBrowser().close();
                ctx.getPlaywright().close();
            }
        } catch (Exception e) {
            TestLogManager.warning(
                    "Error while quitting driver: " + e.getMessage());
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
