package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import core.interfaces.ScrollInterface;

public class PlaywrightScrollUtil extends PlaywrightBrowserUtil implements ScrollInterface {

    private final Page page;
    private final PlaywrightLocatorUtil locatorUtil;

    public PlaywrightScrollUtil(Page page) {
    	super(page);
        this.page = page;
        this.locatorUtil = new PlaywrightLocatorUtil(page);
    }

    private Locator resolve(Object obj) {
        if (obj instanceof PlaywrightWebElement)
            return ((PlaywrightWebElement) obj).locator;

        if (obj instanceof String)
            return locatorUtil.getLocator(obj.toString());

        return null;
    }

    @Override
    public boolean scrollToElement(Object pr) {
        try {
            Locator locator = resolve(pr);
            locator.scrollIntoViewIfNeeded();
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public void javaScriptScrollIntoView(Object pr) {
        Locator locator = resolve(pr);
        locator.scrollIntoViewIfNeeded();
    }

    @Override
    public boolean scrollBy(int x, int y) {
        try {
            page.evaluate("([a, b]) => window.scrollBy(a, b)", new Object[]{x, y});
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public boolean scrollToTop() {
        try {
            page.evaluate("window.scrollTo(0, 0)");
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public boolean scrollToBottom() {
        try {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset) {
        try {
            Locator locator = resolve(pr);

            locator.evaluate("(el, args) => el.scrollBy(args[0], args[1])",
                    new Object[]{xOffset, yOffset});

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void waitForScroll() {
        page.waitForLoadState();
    }

    @Override
    public void scrollStep(int pixels) {
        page.evaluate("window.scrollBy(0, arguments[0])", pixels);
    }
}
