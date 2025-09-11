package seleniumUtils;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for performing keyboard actions with logging.
 */
public class KeyboardUtil extends WindowUtil {

	WebDriver driver;
    public KeyboardUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- SEND KEYS -------------------- */
    public boolean sendKeys(Object pr, CharSequence keys) {
        try {
            WebElement element = getElement(pr);
            element.sendKeys(keys);
            ExtentManager.infoTest("Sent keys '" + keys + "' to element: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to send keys '" + keys + "' to element. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SEND ENTER -------------------- */
    public boolean sendEnter(Object pr) {
        return sendSpecialKey(pr, Keys.ENTER);
    }

    /* -------------------- SEND TAB -------------------- */
    public boolean sendTab(Object pr) {
        return sendSpecialKey(pr, Keys.TAB);
    }

    /* -------------------- SEND ESCAPE -------------------- */
    public boolean sendEscape(Object pr) {
        return sendSpecialKey(pr, Keys.ESCAPE);
    }

    /* -------------------- SEND ARROW KEYS -------------------- */
    public boolean sendArrowUp(Object pr) {
        return sendSpecialKey(pr, Keys.ARROW_UP);
    }

    public boolean sendArrowDown(Object pr) {
        return sendSpecialKey(pr, Keys.ARROW_DOWN);
    }

    public boolean sendArrowLeft(Object pr) {
        return sendSpecialKey(pr, Keys.ARROW_LEFT);
    }

    public boolean sendArrowRight(Object pr) {
        return sendSpecialKey(pr, Keys.ARROW_RIGHT);
    }

    /* -------------------- UTILITY: Send special key -------------------- */
    private boolean sendSpecialKey(Object pr, Keys key) {
        try {
            WebElement element = getElement(pr);
            element.sendKeys(key);
            ExtentManager.infoTest("Sent key '" + key.name() + "' to element: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to send key '" + key.name() + "' to element. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- UTILITY: Get WebElement -------------------- */
    private WebElement getElement(Object pr) {
        try {
            return (pr instanceof String)
                    ? driver.findElement(autolocator(pr.toString()))
                    : (WebElement) pr;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to locate element for keyboard action: " + pr + ". Reason: " + e.getMessage());
            return null;
        }
    }
}
