package core.selenium;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import core.interfaces.MouseHoverInterface;
import reporting.ExtentManager;

/**
 * Utility for handling mouse hover actions (Selenium + JavaScript).
 */
public class SeleniumMouseHoverUtil extends SeleniumKeyboardUtil implements MouseHoverInterface {

	WebDriver driver;
    public SeleniumMouseHoverUtil(WebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    /* -------------------- BASIC HOVER (ACTIONS) -------------------- */
    public boolean mouseHover(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).perform();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- HOVER + CLICK (ACTIONS) -------------------- */
    public boolean hoverAndClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).click().perform();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- HOVER + DOUBLE CLICK -------------------- */
    public boolean hoverAndDoubleClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).doubleClick().perform();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- HOVER + RIGHT CLICK -------------------- */
    public boolean hoverAndRightClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).contextClick().perform();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- HOVER + SEND KEYS -------------------- */
    public boolean hoverAndSendKeys(Object pr, CharSequence keys) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).sendKeys(keys).perform();
            return true;
        } catch (Exception e) {
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- JS HOVER + CLICK -------------------- */
    public boolean jsHoverAndClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            jsMouseHover(pr); // trigger hover first
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- JS HOVER (CSS style change simulation) -------------------- */
    public boolean jsHoverByStyle(Object pr) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].setAttribute('style', arguments[0].getAttribute('style') + ';background: yellow; border: 2px solid red;');", element);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void clickAndHoldMoveByOffset(Object slider, int x, int y) {
		Actions action = new Actions(driver);
		action.clickAndHold(getElement(action)).moveByOffset(x, y).release().build().perform();
	}
}
