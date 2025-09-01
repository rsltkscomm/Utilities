package seleniumUtils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling different click actions safely with logging.
 */
public class ClickUtil extends ScrollUtil {

    private WebDriver driver;

    public ClickUtil(WebDriver driver, PageFactory pageFactory) {
    	super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- NORMAL CLICK -------------------- */
    public boolean clickElement(String locator) {
        try {
            WebElement element = driver.findElement(LocatorUtil.autolocator(locator));
            element.click();
            ExtentManager.infoTest("Click : " + LocatorUtil.logName.get());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Click failed : " + LocatorUtil.logName.get());
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SAFE CLICK -------------------- */
    public boolean safeClick(String locator) {
        try {
            WebElement element = waitForClickable(locator, 30);
            if (element != null) {
                element.click();
                ExtentManager.infoTest("Safe Click : " + LocatorUtil.logName.get());
                return true;
            } else {
                ExtentManager.failTest("SafeClick failed -> Element not clickable within timeout");
            }
        } catch (Exception e) {
            ExtentManager.failTest("SafeClick failed : " + LocatorUtil.logName.get());
            ExtentManager.failTest("Reason: " + e.getMessage());
        }
        return false;
    }

    /* -------------------- JS CLICK -------------------- */
    public boolean jsClick(String locator) {
        try {
            WebElement element = driver.findElement(LocatorUtil.autolocator(locator));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            ExtentManager.passTest("JS Click : " + LocatorUtil.logName.get());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS Click failed : " + LocatorUtil.logName.get());
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- CLICK WEBELEMENT -------------------- */
    public boolean click(WebElement element, String elementName) {
        try {
            element.click();
            ExtentManager.passTest("Click : " + elementName);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Click failed : " + elementName);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- DOUBLE CLICK -------------------- */
    public boolean doubleClick(String locator) {
        try {
            WebElement element = driver.findElement(LocatorUtil.autolocator(locator));
            new Actions(driver).doubleClick(element).perform();
            ExtentManager.infoTest("Double Click : " + LocatorUtil.logName.get());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Double Click failed : " + LocatorUtil.logName.get());
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- RIGHT CLICK / CONTEXT CLICK -------------------- */
    public boolean rightClick(String locator) {
        try {
            WebElement element = driver.findElement(LocatorUtil.autolocator(locator));
            new Actions(driver).contextClick(element).perform();
            ExtentManager.infoTest("Right Click : " + LocatorUtil.logName.get());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Right Click failed : " + LocatorUtil.logName.get());
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- HOVER AND CLICK -------------------- */
    public boolean hoverAndClick(String locator) {
        try {
            WebElement element = driver.findElement(LocatorUtil.autolocator(locator));
            Actions actions = new Actions(driver);
            actions.moveToElement(element).click().perform();
            ExtentManager.infoTest("Hovered and Clicked : " + LocatorUtil.logName.get());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Hover and Click failed : " + LocatorUtil.logName.get());
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- UTILITY: Resolve element -------------------- */
    private WebElement getElement(String locator) {
        return driver.findElement(LocatorUtil.autolocator(locator));
    }
}
