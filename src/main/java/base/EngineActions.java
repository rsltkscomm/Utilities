package base;

import org.openqa.selenium.WebDriver;

import core.interfaces.*;
import core.playwright.*;
import core.selenium.*;

/**
 * Engine aware action router that exposes the same capability surface
 * for both Selenium and Playwright implementations.
 */
public final class EngineActions {

    private final EngineType engineType;

    private final AlertInterface alerts;
    private final AssertInterface asserts;
    private final BrowserInterface browser;
    private final ClickInterface click;
    private final DateInterface dates;
    private final DragAndDropInterface dragAndDrop;
    private final DropdownInterface dropdown;
    private final ElementInterface elements;
    private final FrameInterface frames;
    private final KeyboardInterface keyboard;
    private final LocatorInterface locator;
    private final MouseHoverInterface mouseHover;
    private final ScrollInterface scroll;
    private final ScreenshotInterface screenshot;
    private final SelectInterface select;
    private final WaitInterface wait;
    private final WindowInterface window;

    private EngineActions(
            EngineType engineType,
            AlertInterface alerts,
            AssertInterface asserts,
            BrowserInterface browser,
            ClickInterface click,
            DateInterface dates,
            DragAndDropInterface dragAndDrop,
            DropdownInterface dropdown,
            ElementInterface elements,
            FrameInterface frames,
            KeyboardInterface keyboard,
            LocatorInterface locator,
            MouseHoverInterface mouseHover,
            ScrollInterface scroll,
            ScreenshotInterface screenshot,
            SelectInterface select,
            WaitInterface wait,
            WindowInterface window
    ) {
        this.engineType = engineType;
        this.alerts = alerts;
        this.asserts = asserts;
        this.browser = browser;
        this.click = click;
        this.dates = dates;
        this.dragAndDrop = dragAndDrop;
        this.dropdown = dropdown;
        this.elements = elements;
        this.frames = frames;
        this.keyboard = keyboard;
        this.locator = locator;
        this.mouseHover = mouseHover;
        this.scroll = scroll;
        this.screenshot = screenshot;
        this.select = select;
        this.wait = wait;
        this.window = window;
    }

    /* ==========================================================
       FACTORY
       ========================================================== */

    public static EngineActions from(DriverContext context) {

        /* ===================== SELENIUM ===================== */
        if (context.getEngineType() == EngineType.SELENIUM) {

            WebDriver driver = context.getWebDriver();
            SeleniumLocatorUtil locatorUtil = new SeleniumLocatorUtil(driver);

            return new EngineActions(
                    EngineType.SELENIUM,
                    new SeleniumAlertUtil(driver),
                    new SeleniumAssertUtil(driver),
                    new SeleniumBrowserUtil(driver),
                    new SeleniumClickUtil(driver),
                    new SeleniumDateUtils(driver),
                    new SeleniumDragAndDropUtil(driver),
                    new SeleniumDropdownUtil(driver),
                    new SeleniumElementUtil(driver),
                    new SeleniumFrameUtil(driver),
                    new SeleniumKeyboardUtil(driver),
                    locatorUtil,
                    new SeleniumMouseHoverUtil(driver),
                    new SeleniumScrollUtil(driver),
                    new SeleniumScreenshotUtil(driver),
                    new SeleniumSelectUtil(driver),
                    new SeleniumWaitUtil(driver),
                    new SeleniumWindowUtil(driver)
            );
        }

        /* ===================== PLAYWRIGHT ===================== */
        // 🔑 IMPORTANT: pass DriverContext, NOT Page

        return new EngineActions(
                EngineType.PLAYWRIGHT,
                new PlaywrightAlertUtil(context),
                new PlaywrightAssertUtil(context),
                new PlaywrightBrowserUtil(context),
                new PlaywrightClickUtil(context),
                new PlaywrightDateUtil(context),
                new PlaywrightDragAndDropUtil(context),
                new PlaywrightDropdownUtil(context),
                new PlaywrightElementUtil(context),
                new PlaywrightFrameUtil(context),
                new PlaywrightKeyboardUtil(context),
                new PlaywrightLocatorUtil(context),
                new PlaywrightMouseHoverUtil(context),
                new PlaywrightScrollUtil(context),
                new PlaywrightScreenshotUtil(context),
                new PlaywrightSelectUtil(context),
                new PlaywrightWaitUtil(context),
                new PlaywrightWindowUtil(context)
        );
    }

    /* ==========================================================
       GETTERS
       ========================================================== */

    public EngineType getEngineType() {
        return engineType;
    }

    public AlertInterface alerts() {
        return alerts;
    }

    public AssertInterface asserts() {
        return asserts;
    }

    public BrowserInterface browser() {
        return browser;
    }

    public ClickInterface click() {
        return click;
    }

    public DateInterface dates() {
        return dates;
    }

    public DragAndDropInterface dragAndDrop() {
        return dragAndDrop;
    }

    public DropdownInterface dropdown() {
        return dropdown;
    }

    public ElementInterface elements() {
        return elements;
    }

    public FrameInterface frames() {
        return frames;
    }

    public KeyboardInterface keyboard() {
        return keyboard;
    }

    public LocatorInterface locator() {
        return locator;
    }

    public MouseHoverInterface mouseHover() {
        return mouseHover;
    }

    public ScrollInterface scroll() {
        return scroll;
    }

    public ScreenshotInterface screenshot() {
        return screenshot;
    }

    public SelectInterface select() {
        return select;
    }

    public WaitInterface waitFor() {
        return wait;
    }

    public WindowInterface window() {
        return window;
    }
}
