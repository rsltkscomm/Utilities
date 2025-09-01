package seleniumUtils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility for handling mouse hover actions (Selenium + JavaScript).
 */
public class MousehoverUtil extends KeyboardUtil {

	WebDriver driver;
    public MousehoverUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- BASIC HOVER (ACTIONS) -------------------- */
    public boolean mouseHover(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).perform();
            ExtentManager.infoTest("Mouse hovered on element (Actions): " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Mouse hover failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + CLICK (ACTIONS) -------------------- */
    public boolean hoverAndClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).click().perform();
            ExtentManager.infoTest("Hovered and clicked on element (Actions): " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and click failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + DOUBLE CLICK -------------------- */
    public boolean hoverAndDoubleClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).doubleClick().perform();
            ExtentManager.infoTest("Hovered and double clicked on element (Actions): " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and double click failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + RIGHT CLICK -------------------- */
    public boolean hoverAndRightClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).contextClick().perform();
            ExtentManager.infoTest("Hovered and right clicked on element (Actions): " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and right click failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER + SEND KEYS -------------------- */
    public boolean hoverAndSendKeys(Object pr, CharSequence keys) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).sendKeys(keys).perform();
            ExtentManager.infoTest("Hovered and sent keys '" + keys + "' (Actions) to element: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and send keys failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER BY OFFSET -------------------- */
    public boolean hoverByOffset(Object pr, int xOffset, int yOffset) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element, xOffset, yOffset).perform();
            ExtentManager.infoTest("Hovered on element with offset X:" + xOffset + " Y:" + yOffset + " (Actions)");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover by offset failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* ============================================================= */
    /* ===============   JAVASCRIPT HOVER METHODS   ================= */
    /* ============================================================= */

    /* -------------------- JS HOVER (dispatch mouseover event) -------------------- */
    public boolean jsMouseHover(Object pr) {
        try {
            WebElement element = getElement(pr);
            String script =
                    "var evObj = document.createEvent('MouseEvents');" +
                    "evObj.initMouseEvent('mouseover', true, true, window, 0, 0, 0, 0, 0," +
                    "false, false, false, false, 0, null);" +
                    "arguments[0].dispatchEvent(evObj);";
            ((JavascriptExecutor) driver).executeScript(script, element);
            ExtentManager.infoTest("JavaScript hover triggered (mouseover) on element: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS hover failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- JS HOVER + CLICK -------------------- */
    public boolean jsHoverAndClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            jsMouseHover(pr); // trigger hover first
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            ExtentManager.infoTest("JavaScript hover + click executed on element: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS hover and click failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- JS HOVER (CSS style change simulation) -------------------- */
    public boolean jsHoverByStyle(Object pr) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].setAttribute('style', arguments[0].getAttribute('style') + ';background: yellow; border: 2px solid red;');", element);
            ExtentManager.infoTest("JavaScript hover simulated via CSS highlight for element: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS hover via CSS failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- UTILITY: Resolve element -------------------- */
    private WebElement getElement(Object pr) {
        return (pr instanceof String)
                ? driver.findElement(autolocator(pr.toString()))
                : (WebElement) pr;
    }
}
