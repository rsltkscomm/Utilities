package core.selenium;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import core.interfaces.ClickInterface;
import reporting.ExtentManager;
import reporting.TestLogManager;
import seleniumUtils.LocatorUtil;

public class SeleniumClickUtil extends SeleniumScrollUtil implements ClickInterface {

    private final WebDriver driver;

    public SeleniumClickUtil(WebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    /* ---------------- NORMAL CLICK ---------------- */
    @Override
    public boolean clickElement(Object pr) {
        try {
            waitForClickable(getElement(pr), 120);
            WebElement element = getElement(pr);
            String attributeStyle = getAttributeStyle(element);
            highlightElement(element);
            element.click();
            removeHighlight(element, attributeStyle);

            ExtentManager.infoTest("Clicked : <b>" + LocatorUtil.logName.get() + "</b>");
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Click failed : <b>" + LocatorUtil.logName.get() + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* ---------------- SAFE CLICK ---------------- */
    @Override
    public boolean safeClick(Object pr) {
        try {
            WebElement element = waitForClickable(pr, 30);
            if (element != null) {
            	String attributeStyle = getAttributeStyle(element);
                highlightElement(element);
                element.click();
                removeHighlight(element, attributeStyle);

                ExtentManager.infoTest("Clicked : <b>" + LocatorUtil.logName.get() + "</b>");
                return true;
            }
        } catch (Exception e) {
            ExtentManager.failTest("Safe Click failed : " + e.getMessage());
        }
        return false;
    }

    /* ---------------- JS CLICK ---------------- */
    @Override
    public boolean jsClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            String attributeStyle = getAttributeStyle(element);
            highlightElement(element);

            ((JavascriptExecutor) driver).executeScript("arguments[0].click()", element);

            removeHighlight(element,attributeStyle);
            ExtentManager.infoTest("JS Click : <b>" + LocatorUtil.logName.get() + "</b>");
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("JS Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- CLICK WEBELEMENT ---------------- */
    @Override
    public boolean click(Object elementObj, String elementName) {
        try {
            WebElement element = (WebElement) elementObj;
            String attributeStyle = getAttributeStyle(element);
            highlightElement(element);
            element.click();
            removeHighlight(element,attributeStyle);

            ExtentManager.infoTest("Clicked : <b>" + elementName + "</b>");
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Click failed : " + elementName);
            return false;
        }
    }

    /* ---------------- DOUBLE CLICK ---------------- */
    @Override
    public boolean doubleClick(Object pr) {
        try {
            WebElement element = getElement(pr);

            new Actions(driver).doubleClick(element).perform();

            ExtentManager.infoTest("Double Click: " + LocatorUtil.logName.get());
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
            WebElement element = driver.findElement(autolocator(locator));
            TestLogManager.info("Located element: " + locator);

            new Actions(driver).click(element).build().perform();

            ExtentManager.infoTest("Clicked : <b>" + LocatorUtil.logName.get() + "</b>");
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Actions Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- RIGHT CLICK ---------------- */
    @Override
    public boolean rightClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).contextClick(element).perform();

            ExtentManager.infoTest("Right Click : " + LocatorUtil.logName.get());
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Right Click failed : " + e.getMessage());
            return false;
        }
    }

    /* ---------------- HOVER & CLICK ---------------- */
    @Override
    public boolean hoverAndClick(Object pr) {
        try {
            WebElement element = getElement(pr);
            new Actions(driver).moveToElement(element).click().perform();

            ExtentManager.infoTest("Hovered & Clicked : " + LocatorUtil.logName.get());
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Hover and Click failed : " + e.getMessage());
            return false;
        }
    }
}
