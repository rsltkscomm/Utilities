package base;

import org.openqa.selenium.WebDriver;

import com.microsoft.playwright.Page;

import core.interfaces.AlertInterface;
import core.interfaces.AssertInterface;
import core.interfaces.BrowserInterface;
import core.interfaces.ClickInterface;
import core.interfaces.DateInterface;
import core.interfaces.DragAndDropInterface;
import core.interfaces.DropdownInterface;
import core.interfaces.ElementInterface;
import core.interfaces.EngineType;
import core.interfaces.FrameInterface;
import core.interfaces.KeyboardInterface;
import core.interfaces.LocatorInterface;
import core.interfaces.MouseHoverInterface;
import core.interfaces.ScreenshotInterface;
import core.interfaces.ScrollInterface;
import core.interfaces.SelectInterface;
import core.interfaces.WaitInterface;
import core.interfaces.WindowInterface;
import core.playwright.PlaywrightAlertUtil;
import core.playwright.PlaywrightAssertUtil;
import core.playwright.PlaywrightBrowserUtil;
import core.playwright.PlaywrightClickUtil;
import core.playwright.PlaywrightDateUtil;
import core.playwright.PlaywrightDragAndDropUtil;
import core.playwright.PlaywrightDropdownUtil;
import core.playwright.PlaywrightElementUtil;
import core.playwright.PlaywrightFrameUtil;
import core.playwright.PlaywrightKeyboardUtil;
import core.playwright.PlaywrightLocatorUtil;
import core.playwright.PlaywrightMouseHoverUtil;
import core.playwright.PlaywrightScreenshotUtil;
import core.playwright.PlaywrightScrollUtil;
import core.playwright.PlaywrightSelectUtil;
import core.playwright.PlaywrightWaitUtil;
import core.playwright.PlaywrightWindowUtil;
import core.selenium.SeleniumAlertUtil;
import core.selenium.SeleniumAssertUtil;
import core.selenium.SeleniumBrowserUtil;
import core.selenium.SeleniumClickUtil;
import core.selenium.SeleniumDateUtils;
import core.selenium.SeleniumDragAndDropUtil;
import core.selenium.SeleniumDropdownUtil;
import core.selenium.SeleniumElementUtil;
import core.selenium.SeleniumFrameUtil;
import core.selenium.SeleniumKeyboardUtil;
import core.selenium.SeleniumLocatorUtil;
import core.selenium.SeleniumMouseHoverUtil;
import core.selenium.SeleniumScreenshotUtil;
import core.selenium.SeleniumScrollUtil;
import core.selenium.SeleniumSelectUtil;
import core.selenium.SeleniumWaitUtil;
import core.selenium.SeleniumWindowUtil;

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

    public static EngineActions from(DriverContext context) {
        if (context.getEngineType() == EngineType.SELENIUM) {
            WebDriver driver = context.getWebDriver();
            SeleniumLocatorUtil locatorUtil = new SeleniumLocatorUtil(driver);

            AlertInterface alerts = new SeleniumAlertUtil(driver);
            AssertInterface asserts = new SeleniumAssertUtil(driver);
            BrowserInterface browser = new SeleniumBrowserUtil(driver);
            ClickInterface click = new SeleniumClickUtil(driver);
            DateInterface dates = new SeleniumDateUtils(driver);
            DragAndDropInterface dragAndDrop = new SeleniumDragAndDropUtil(driver);
            DropdownInterface dropdown = new SeleniumDropdownUtil(driver);
            ElementInterface elements = new SeleniumElementUtil(driver);
            FrameInterface frames = new SeleniumFrameUtil(driver);
            KeyboardInterface keyboard = new SeleniumKeyboardUtil(driver);
            MouseHoverInterface mouseHover = new SeleniumMouseHoverUtil(driver);
            ScrollInterface scroll = new SeleniumScrollUtil(driver);
            ScreenshotInterface screenshot = new SeleniumScreenshotUtil(driver);
            SelectInterface select = new SeleniumSelectUtil(driver);
            WaitInterface wait = new SeleniumWaitUtil(driver);
            WindowInterface window = new SeleniumWindowUtil(driver);

            return new EngineActions(
                    EngineType.SELENIUM,
                    alerts,
                    asserts,
                    browser,
                    click,
                    dates,
                    dragAndDrop,
                    dropdown,
                    elements,
                    frames,
                    keyboard,
                    locatorUtil,
                    mouseHover,
                    scroll,
                    screenshot,
                    select,
                    wait,
                    window
            );
        }

        Page page = context.getPage();
        PlaywrightLocatorUtil locatorUtil = new PlaywrightLocatorUtil(page);

        AlertInterface alerts = new PlaywrightAlertUtil(page);
        AssertInterface asserts = new PlaywrightAssertUtil(page);
        BrowserInterface browser = new PlaywrightBrowserUtil(page);
        ClickInterface click = new PlaywrightClickUtil(page);
        DateInterface dates = new PlaywrightDateUtil(page);
        DragAndDropInterface dragAndDrop = new PlaywrightDragAndDropUtil(page);
        DropdownInterface dropdown = new PlaywrightDropdownUtil(page);
        ElementInterface elements = new PlaywrightElementUtil(page);
        FrameInterface frames = new PlaywrightFrameUtil(page);
        KeyboardInterface keyboard = new PlaywrightKeyboardUtil(page);
        MouseHoverInterface mouseHover = new PlaywrightMouseHoverUtil(page);
        ScrollInterface scroll = new PlaywrightScrollUtil(page);
        ScreenshotInterface screenshot = new PlaywrightScreenshotUtil(page);
        SelectInterface select = new PlaywrightSelectUtil(page);
        WaitInterface wait = new PlaywrightWaitUtil(page);
        WindowInterface window = new PlaywrightWindowUtil(page);

        return new EngineActions(
                EngineType.PLAYWRIGHT,
                alerts,
                asserts,
                browser,
                click,
                dates,
                dragAndDrop,
                dropdown,
                elements,
                frames,
                keyboard,
                locatorUtil,
                mouseHover,
                scroll,
                screenshot,
                select,
                wait,
                window
        );
    }

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

