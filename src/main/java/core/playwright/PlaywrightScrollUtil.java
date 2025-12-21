package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.ScrollInterface;

public class PlaywrightScrollUtil extends PlaywrightBrowserUtil
        implements ScrollInterface {

    protected final DriverContext driverContext;

    public PlaywrightScrollUtil(DriverContext driverContext) {
        super(driverContext); // initial page for browser util
        this.driverContext = driverContext;
    }

    /**
     * Always returns the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    /**
     * Always returns locator util bound to CURRENT page
     */
    protected PlaywrightLocatorUtil locatorUtil() {
        return new PlaywrightLocatorUtil(driverContext);
    }

    private Locator resolve(Object obj) {
        if (obj instanceof PlaywrightWebElement)
            return ((PlaywrightWebElement) obj).locator;

        if (obj instanceof String)
            return locatorUtil().getLocator(obj.toString());

        return null;
    }

    @Override
    public boolean scrollToElement(Object pr) {
        try {
            resolve(pr).scrollIntoViewIfNeeded();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void javaScriptScrollIntoView(Object pr) {
        resolve(pr).scrollIntoViewIfNeeded();
    }

    @Override
    public boolean scrollBy(int x, int y) {
        try {
            page().evaluate("([a, b]) => window.scrollBy(a, b)", new Object[]{x, y});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean scrollToTop() {
        try {
            page().evaluate("window.scrollTo(0, 0)");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean scrollToBottom() {
        try {
            page().evaluate("window.scrollTo(0, document.body.scrollHeight)");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset) {
        try {
            resolve(pr).evaluate(
                "(el, args) => el.scrollBy(args[0], args[1])",
                new Object[]{xOffset, yOffset}
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void waitForScroll() {
        page().waitForLoadState();
    }

    @Override
    public void scrollStep(int pixels) {
        page().evaluate("window.scrollBy(0, arguments[0])", pixels);
    }

    @Override
    public void jsUpdate(String exp) {
        page().evaluate(exp);
    }
}
