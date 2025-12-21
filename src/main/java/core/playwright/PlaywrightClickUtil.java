package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.MouseButton;

import base.DriverContext;
import core.interfaces.ClickInterface;
import reporting.ExtentManager;

public class PlaywrightClickUtil extends PlaywrightScrollUtil
        implements ClickInterface {

    protected final DriverContext driverContext;

    public PlaywrightClickUtil(DriverContext driverContext) {
        super(driverContext); // ✅ parent also uses DriverContext
        this.driverContext = driverContext;
    }

    /**
     * Always get the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    /**
     * Always get locator util bound to CURRENT page
     */
    protected PlaywrightLocatorUtil locatorUtil() {
        return new PlaywrightLocatorUtil(driverContext);
    }

    private Locator resolve(Object pr) {
        if (pr instanceof PlaywrightWebElement)
            return ((PlaywrightWebElement) pr).locator;

        if (pr instanceof String)
            return locatorUtil().getLocator(pr.toString());

        return null;
    }

    /* ---------------- NORMAL CLICK ---------------- */
    @Override
    public boolean clickElement(Object pr) {
        try {
            Locator loc = resolve(pr);
            loc.click();

            ExtentManager.infoTest(
                "Clicked : <b>" + PlaywrightLocatorUtil.logName.get() + "</b>");
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- SAFE CLICK ---------------- */
    @Override
    public boolean safeClick(Object pr) {
        try {
            Locator loc = resolve(pr);
            loc.waitFor();
            loc.click();
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Safe Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- JS CLICK ---------------- */
    @Override
    public boolean jsClick(Object pr) {
        try {
            resolve(pr).evaluate("el => el.click()");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- CLICK WEBELEMENT ---------------- */
    @Override
    public boolean click(Object element, String name) {
        try {
            Locator loc = ((PlaywrightWebElement) element).locator;
            loc.click();
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Click failed : " + name);
            return false;
        }
    }

    /* ---------------- DOUBLE CLICK ---------------- */
    @Override
    public boolean doubleClick(Object pr) {
        try {
            resolve(pr).dblclick();
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Double Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- ACTIONS CLICK ---------------- */
    @Override
    public boolean actionsClickElement(String locator) {
        try {
            locatorUtil().getLocator(locator).click();
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Actions click failed");
            return false;
        }
    }

    /* ---------------- RIGHT CLICK ---------------- */
    @Override
    public boolean rightClick(Object pr) {
        try {
            resolve(pr)
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Right click failed");
            return false;
        }
    }

    /* ---------------- HOVER AND CLICK ---------------- */
    @Override
    public boolean hoverAndClick(Object pr) {
        try {
            Locator loc = resolve(pr);
            loc.hover();
            loc.click();
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and click failed");
            return false;
        }
    }
}
