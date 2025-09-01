package seleniumUtils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import pages.PageFactory;
import reporting.ExtentManager;

import java.util.Iterator;
import java.util.Set;

/**
 * Utility class for handling browser windows and tabs with logging.
 */
public class WindowUtil extends FrameUtil {

	WebDriver driver;
    public WindowUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- GET CURRENT WINDOW HANDLE -------------------- */
    public String getCurrentWindowHandle() {
        try {
            String handle = driver.getWindowHandle();
            ExtentManager.infoTest("Current window handle: " + handle);
            return handle;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to get current window handle. Reason: " + e.getMessage());
            return null;
        }
    }

    /* -------------------- GET ALL WINDOW HANDLES -------------------- */
    public Set<String> getAllWindowHandles() {
        try {
            Set<String> handles = driver.getWindowHandles();
            ExtentManager.infoTest("All window handles: " + handles);
            return handles;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to get all window handles. Reason: " + e.getMessage());
            return null;
        }
    }

    /* -------------------- SWITCH TO WINDOW BY HANDLE -------------------- */
    public boolean switchToWindow(String windowHandle) {
        try {
            driver.switchTo().window(windowHandle);
            ExtentManager.infoTest("Switched to window: " + windowHandle);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to window: " + windowHandle + ". Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO WINDOW BY INDEX -------------------- */
    public boolean switchToWindow(int index) {
        try {
            Set<String> handles = driver.getWindowHandles();
            if (index < 0 || index >= handles.size()) {
                ExtentManager.failTest("Window index out of bounds: " + index);
                return false;
            }
            Iterator<String> it = handles.iterator();
            String handle = null;
            for (int i = 0; i <= index; i++) {
                handle = it.next();
            }
            driver.switchTo().window(handle);
            ExtentManager.infoTest("Switched to window at index: " + index + " with handle: " + handle);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to window at index: " + index + ". Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO PARENT WINDOW -------------------- */
    public boolean switchToParentWindow() {
        try {
            String parentHandle = driver.getWindowHandles().iterator().next();
            driver.switchTo().window(parentHandle);
            ExtentManager.infoTest("Switched to parent window: " + parentHandle);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to parent window. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- CLOSE CURRENT WINDOW -------------------- */
    public boolean closeCurrentWindow() {
        try {
            String handle = driver.getWindowHandle();
            driver.close();
            ExtentManager.infoTest("Closed current window: " + handle);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to close current window. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- CLOSE ALL OTHER WINDOWS EXCEPT PARENT -------------------- */
    public boolean closeAllOtherWindows() {
        try {
            String parentHandle = driver.getWindowHandles().iterator().next();
            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(parentHandle)) {
                    driver.switchTo().window(handle);
                    driver.close();
                    ExtentManager.infoTest("Closed window: " + handle);
                }
            }
            driver.switchTo().window(parentHandle);
            ExtentManager.infoTest("Switched back to parent window: " + parentHandle);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to close other windows. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- OPEN NEW TAB -------------------- */
    public boolean openNewTab() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.open();");
            ExtentManager.infoTest("Opened a new browser tab.");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to open new tab. Reason: " + e.getMessage());
            return false;
        }
    }
}
