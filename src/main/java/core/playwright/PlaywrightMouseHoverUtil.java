package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.MouseButton;

import base.DriverContext;
import core.interfaces.MouseHoverInterface;
import reporting.ExtentManager;

public class PlaywrightMouseHoverUtil extends PlaywrightKeyboardUtil
        implements MouseHoverInterface {

    protected final DriverContext driverContext;

    public PlaywrightMouseHoverUtil(DriverContext driverContext) {
        super(driverContext); // ✅ FIXED
        this.driverContext = driverContext;
    }

    /**
     * Always returns the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    /* -------------------- BASIC HOVER -------------------- */

    @Override
    public boolean mouseHover(Object pr) {
        try {
            resolveLocator(pr).hover();
            ExtentManager.infoTest("Mouse hovered on element");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Mouse hover failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + CLICK -------------------- */

    @Override
    public boolean hoverAndClick(Object pr) {
        try {
            Locator loc = resolveLocator(pr);
            loc.hover();
            loc.click();
            ExtentManager.infoTest("Hover and click successful");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and click failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + DOUBLE CLICK -------------------- */

    @Override
    public boolean hoverAndDoubleClick(Object pr) {
        try {
            Locator loc = resolveLocator(pr);
            loc.hover();
            loc.dblclick();
            ExtentManager.infoTest("Hover and double click successful");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and double click failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + RIGHT CLICK -------------------- */

    @Override
    public boolean hoverAndRightClick(Object pr) {
        try {
            Locator loc = resolveLocator(pr);
            loc.hover();
            loc.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            ExtentManager.infoTest("Hover and right click successful");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and right click failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + SEND KEYS -------------------- */

    @Override
    public boolean hoverAndSendKeys(Object pr, CharSequence keys) {
        try {
            Locator loc = resolveLocator(pr);
            loc.hover();
            loc.press(keys.toString());
            ExtentManager.infoTest("Hover and send keys: " + keys);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and send keys failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER BY OFFSET -------------------- */

    @Override
    public boolean hoverByOffset(Object pr, int xOffset, int yOffset) {
        try {
            resolveLocator(pr)
                    .hover(new Locator.HoverOptions()
                            .setPosition(xOffset, yOffset));
            ExtentManager.infoTest(
                    "Hovered by offset X:" + xOffset + " Y:" + yOffset);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover by offset failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* ============================================================= */
    /* ===============   JS COMPATIBILITY METHODS   ================= */
    /* ============================================================= */

    @Override
    public boolean jsMouseHover(Object pr) {
        return mouseHover(pr);
    }

    @Override
    public boolean jsHoverAndClick(Object pr) {
        return hoverAndClick(pr);
    }

    @Override
    public boolean jsHoverByStyle(Object pr) {
        try {
            resolveLocator(pr).evaluate(
                "el => el.style.cssText += 'background: yellow; border: 2px solid red;'"
            );
            ExtentManager.infoTest("JS hover style applied");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS hover by style failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- CLICK & HOLD MOVE -------------------- */

    public void clickAndHoldMoveByOffset(Object selector, int xOffset, int yOffset) {

        Locator slider = page().locator(selector.toString());

        BoundingBox box = slider.boundingBox();
        if (box == null) {
            throw new RuntimeException("Element not visible: " + selector);
        }

        double startX = box.x + box.width / 2;
        double startY = box.y + box.height / 2;

        page().mouse().move(startX, startY);
        page().mouse().down();
        page().mouse().move(startX + xOffset, startY + yOffset);
        page().mouse().up();
    }
}
