package seleniumUtils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling frames/iframes with logging.
 */
public class FrameUtil extends SelectUtil {

	WebDriver driver;
    public FrameUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- SWITCH TO FRAME BY INDEX -------------------- */
    public boolean switchToFrame(int index) {
        try {
            driver.switchTo().frame(index);
            ExtentManager.infoTest("Switched to frame by index: " + index);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to frame by index failed: " + index);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO FRAME BY NAME OR ID -------------------- */
    public boolean switchToFrame(String nameOrId) {
        try {
            driver.switchTo().frame(nameOrId);
            ExtentManager.infoTest("Switched to frame by name/id: " + nameOrId);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to frame by name/id failed: " + nameOrId);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO FRAME BY WEBELEMENT -------------------- */
    public boolean switchToFrame(WebElement frameElement) {
        try {
            driver.switchTo().frame(frameElement);
            ExtentManager.infoTest("Switched to frame by WebElement: " + frameElement.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to frame by WebElement failed: " + frameElement.toString());
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH BACK TO PARENT FRAME -------------------- */
    public boolean switchToParentFrame() {
        try {
            driver.switchTo().parentFrame();
            ExtentManager.infoTest("Switched back to parent frame");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to parent frame failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH BACK TO DEFAULT CONTENT -------------------- */
    public boolean switchToDefaultContent() {
        try {
            driver.switchTo().defaultContent();
            ExtentManager.infoTest("Switched back to default content");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to default content failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }
}
