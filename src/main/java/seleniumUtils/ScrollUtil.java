package seleniumUtils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling scroll actions (Selenium + JavaScript) with logging.
 */
public class ScrollUtil extends BrowserUtil {

	WebDriver driver;
    public ScrollUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- SCROLL INTO VIEW -------------------- */
    public boolean scrollToElement(Object pr) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            ExtentManager.infoTest("Scrolled into view: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll to element failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL BY PIXELS -------------------- */
    public boolean scrollBy(int x, int y) {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
            ExtentManager.infoTest("Scrolled by pixels - X: " + x + ", Y: " + y);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll by pixels failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL TO TOP -------------------- */
    public boolean scrollToTop() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            ExtentManager.infoTest("Scrolled to top of the page");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll to top failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL TO BOTTOM -------------------- */
    public boolean scrollToBottom() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            ExtentManager.infoTest("Scrolled to bottom of the page");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll to bottom failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL BY ELEMENT OFFSET -------------------- */
    public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollBy(arguments[1], arguments[2]);", element, xOffset, yOffset);
            ExtentManager.infoTest("Scrolled element " + element.toString() + " by offset X:" + xOffset + " Y:" + yOffset);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll by element offset failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- UTILITY: Resolve WebElement -------------------- */
    private WebElement getElement(Object pr) {
        try {
            return (pr instanceof String)
                    ? driver.findElement(autolocator(pr.toString()))
                    : (WebElement) pr;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to locate element for scroll: " + pr + ". Reason: " + e.getMessage());
            return null;
        }
    }
}
