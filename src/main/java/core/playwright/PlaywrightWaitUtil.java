package core.playwright;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import core.interfaces.WaitInterface;

public class PlaywrightWaitUtil extends PlaywrightDateUtil implements WaitInterface {

    private final Page page;
    private final PlaywrightLocatorUtil locatorUtil;

    public PlaywrightWaitUtil(Page page) {
    	super(page);
        this.page = page;
        this.locatorUtil = new PlaywrightLocatorUtil(page);
    }

    @Override
    public void setImplicitWait(int sec) {
        // Playwright does NOT support implicit waits
    }

    @Override
    public WebElement waitForClickable(Object pr, int sec) {
        PlaywrightWebElement el = (PlaywrightWebElement) locatorUtil.getElement(pr);

        el.locator.waitFor(new Locator.WaitForOptions()
                .setTimeout(sec * 1000)
                .setState(WaitForSelectorState.VISIBLE));

        // small stabilization delay (Selenium-like "clickable")
        page.waitForTimeout(150);

        return el;
    }

    @Override
    public WebElement waitForVisible(Object pr, int sec) {
        PlaywrightWebElement el = (PlaywrightWebElement) locatorUtil.getElement(pr);

        el.locator.waitFor(new Locator.WaitForOptions()
                .setTimeout(sec * 1000)
                .setState(WaitForSelectorState.VISIBLE));

        return el;
    }

    @Override
    public PlaywrightWebElement waitForPresence(Object pr, int sec) {

        PlaywrightWebElement el =
                (PlaywrightWebElement) locatorUtil.getElement(pr);

        el.locator.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(sec * 1000L)
                        .setState(WaitForSelectorState.ATTACHED)
        );

        return el;
    }


    @Override
    public boolean explicitWaitTextToBePresent(String text, Object pr, int sec) {
        PlaywrightWebElement el = (PlaywrightWebElement) locatorUtil.getElement(pr);
        long end = System.currentTimeMillis() + sec * 1000;

        while (System.currentTimeMillis() < end) {
            if (el.getText().contains(text)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForInvisibility(Object pr, int sec) {
        PlaywrightWebElement el = (PlaywrightWebElement) locatorUtil.getElement(pr);
        long end = System.currentTimeMillis() + sec * 1000;

        while (System.currentTimeMillis() < end) {
            if (!el.isDisplayed()) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForText(Object pr, String text, int sec) {
        PlaywrightWebElement el = (PlaywrightWebElement) locatorUtil.getElement(pr);
        long end = System.currentTimeMillis() + sec * 1000;

        while (System.currentTimeMillis() < end) {
            if (el.getText().contains(text)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForTitle(String title, int sec) {
        long end = System.currentTimeMillis() + sec * 1000;

        while (System.currentTimeMillis() < end) {
            if (page.title().equals(title)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForTitleContains(String partialTitle, int sec) {
        long end = System.currentTimeMillis() + sec * 1000;

        while (System.currentTimeMillis() < end) {
            if (page.title().contains(partialTitle)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForUrl(String url, int sec) {
        long end = System.currentTimeMillis() + sec * 1000;
        while (System.currentTimeMillis() < end) {
            if (page.url().equals(url)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForUrlContains(String partialUrl, int sec) {
        long end = System.currentTimeMillis() + sec * 1000;
        while (System.currentTimeMillis() < end) {
            if (page.url().contains(partialUrl)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public Alert waitForAlert(int sec) {
        throw new UnsupportedOperationException("Playwright does not expose Selenium Alert object.");
    }

    @Override
    public boolean waitForStaleness(WebElement element, int sec) {
        PlaywrightWebElement el = (PlaywrightWebElement) element;
        long end = System.currentTimeMillis() + sec * 1000;
        while (System.currentTimeMillis() < end) {
            if (!el.locator.isVisible()) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public boolean waitForFrame(Object pr, int sec) {
        long end = System.currentTimeMillis() + sec * 1000;

        while (System.currentTimeMillis() < end) {
            try {
                PlaywrightWebElement iframeEl =
                        (PlaywrightWebElement) locatorUtil.getElement(pr);

                // Playwright gives Frame directly from the element
                FrameLocator frame = iframeEl.locator.contentFrame();

                if (frame != null) {
                    // NOT switching like Selenium — Playwright operates directly on Frame
                    // You may store this frame in your page object if needed
                    return true;
                }
            } catch (Exception ignored) {}

            sleep(200);
        }
        return false;
    }


    @Override
    public WebElement fluentWait(Object pr, int timeoutSec, int pollingSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000;

        while (System.currentTimeMillis() < end) {
            WebElement el = locatorUtil.getElement(pr);
            if (el != null && el.isDisplayed())
                return el;
            sleep(pollingSec * 1000);
        }
        return null;
    }

    @Override
    public boolean waitForPageLoad(int sec) {
        return waitForJSReady(sec);
    }

    @Override
    public boolean waitForJQueryLoad(int sec) {
        return true; // Playwright pages may not use jQuery
    }

    @Override
    public boolean waitForJSReady(int sec) {
        long end = System.currentTimeMillis() + sec * 1000;
        while (System.currentTimeMillis() < end) {
            Object state = page.evaluate("() => document.readyState");
            if ("complete".equals(state)) return true;
            sleep(200);
        }
        return false;
    }

    @Override
    public void turnOnImplicityWait() {}

    @Override
    public void turnOffImplicityWait() {}

    @Override
    public void wait(int seconds) {
        sleep(seconds * 1000L);
    }

    @Override
    public void wait_Milli_Seconds(int ms) {
        sleep(ms);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }
}
