package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import core.interfaces.KeyboardInterface;
import reporting.ExtentManager;

public class PlaywrightKeyboardUtil extends PlaywrightWindowUtil implements KeyboardInterface {

    private final Page page;

    public PlaywrightKeyboardUtil(Page page) {
        super(page);
        this.page = page;
    }

    /* -------------------- SEND KEYS -------------------- */

    @Override
    public boolean sendKeys(Object pr, CharSequence keys) {
        try {
            Locator locator = resolveLocator(pr);
            locator.fill(keys.toString());
            ExtentManager.infoTest("Sent keys '" + keys + "'");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to send keys '" + keys + "'");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SPECIAL KEYS -------------------- */

    @Override
    public boolean sendEnter(Object pr) {
        return pressKey(pr, "Enter");
    }

    @Override
    public boolean sendTab(Object pr) {
        return pressKey(pr, "Tab");
    }

    @Override
    public boolean sendEscape(Object pr) {
        return pressKey(pr, "Escape");
    }

    @Override
    public boolean sendArrowUp(Object pr) {
        return pressKey(pr, "ArrowUp");
    }

    @Override
    public boolean sendArrowDown(Object pr) {
        return pressKey(pr, "ArrowDown");
    }

    @Override
    public boolean sendArrowLeft(Object pr) {
        return pressKey(pr, "ArrowLeft");
    }

    @Override
    public boolean sendArrowRight(Object pr) {
        return pressKey(pr, "ArrowRight");
    }

    /* -------------------- INTERNAL HELPER -------------------- */

    private boolean pressKey(Object pr, String key) {
        try {
            Locator locator = resolveLocator(pr);
            locator.press(key);
            ExtentManager.infoTest("Pressed key '" + key + "'");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to press key '" + key + "'");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }
}
