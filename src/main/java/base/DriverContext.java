package base;

import com.microsoft.playwright.*;
import core.interfaces.EngineType;
import org.openqa.selenium.WebDriver;

/**
 * Holds runtime driver state for Selenium or Playwright.
 * This is the single source of truth for the current engine.
 */
public final class DriverContext {

    private final EngineType engineType;

    // ===== Selenium =====
    private final WebDriver webDriver;

    // ===== Playwright =====
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext browserContext;
    private Page page; // MUST be mutable

    // ===== Common =====
    private final AutomationContext automationContext;
    private final EngineActions engineActions;

    private DriverContext(
            EngineType engineType,
            WebDriver webDriver,
            Playwright playwright,
            Browser browser,
            BrowserContext browserContext,
            Page page
    ) {
        this.engineType = engineType;
        this.webDriver = webDriver;
        this.playwright = playwright;
        this.browser = browser;
        this.browserContext = browserContext;
        this.page = page;

        // AutomationContext must read page dynamically
        this.automationContext = engineType == EngineType.PLAYWRIGHT
                ? new PlaywrightContext(this)
                : new SeleniumContext(webDriver);

        this.engineActions = EngineActions.from(this);
    }

    /* =========================
       FACTORY METHODS
       ========================= */

    public static DriverContext selenium(WebDriver driver) {
        return new DriverContext(
                EngineType.SELENIUM,
                driver,
                null,
                null,
                null,
                null
        );
    }

    public static DriverContext playwright(
            Playwright playwright,
            Browser browser,
            BrowserContext context,
            Page page
    ) {
        return new DriverContext(
                EngineType.PLAYWRIGHT,
                null,
                playwright,
                browser,
                context,
                page
        );
    }

    /* =========================
       GETTERS / SETTERS
       ========================= */

    public EngineType getEngineType() {
        return engineType;
    }

    // Selenium
    public WebDriver getWebDriver() {
        return webDriver;
    }

    // Playwright
    public Playwright getPlaywright() {
        return playwright;
    }

    public Browser getBrowser() {
        return browser;
    }

    public BrowserContext getBrowserContext() {
        return browserContext;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public AutomationContext getAutomationContext() {
        return automationContext;
    }

    public EngineActions getEngineActions() {
        return engineActions;
    }
}
