package core.selenium;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.WebDriver;

import core.interfaces.WindowInterface;
import reporting.ExtentManager;

public class SeleniumWindowUtil extends SeleniumFrameUtil implements WindowInterface {

    private final WebDriver driver;

    public SeleniumWindowUtil(WebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    /* -------------------- GET CURRENT WINDOW HANDLE -------------------- */

    @Override
    public String getCurrentWindowHandle() {
        try {
            String handle = driver.getWindowHandle();
            ExtentManager.infoTest("Current window handle: " + handle);
            return handle;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to get current window handle");
            return null;
        }
    }

    /* -------------------- GET ALL WINDOW HANDLES -------------------- */

    @Override
    public List<String> getAllWindowHandles() {
        try {
            Set<String> handles = driver.getWindowHandles();
            List<String> handleList = new ArrayList<>(handles);
            ExtentManager.infoTest("All window handles: " + handleList);
            return handleList;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to get all window handles");
            return new ArrayList<>();
        }
    }
    
    /* -------------------- SWITCH TO WINDOW BY HANDLE -------------------- */

    @Override
    public boolean switchToWindow(String windowHandle) {
        try {
            driver.switchTo().window(windowHandle);
            ExtentManager.infoTest("Switched to window: " + windowHandle);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to window: " + windowHandle);
            return false;
        }
    }

    /* -------------------- SWITCH TO WINDOW BY INDEX -------------------- */

    @Override
    public boolean switchToWindow(int index) {
        try {
            List<String> handles = getAllWindowHandles();
            if (index < 0 || index >= handles.size()) {
                throw new IndexOutOfBoundsException("Invalid window index: " + index);
            }
            driver.switchTo().window(handles.get(index));
            ExtentManager.infoTest("Switched to window index: " + index);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to window index: " + index);
            return false;
        }
    }

    /* -------------------- SWITCH TO PARENT WINDOW -------------------- */

    @Override
    public boolean switchToParentWindow() {
        try {
            String parent = getAllWindowHandles().get(0);
            driver.switchTo().window(parent);
            ExtentManager.infoTest("Switched to parent window");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to parent window");
            return false;
        }
    }

    /* -------------------- CLOSE CURRENT WINDOW -------------------- */

    @Override
    public boolean closeCurrentWindow() {
        try {
            driver.close();
            ExtentManager.infoTest("Closed current window");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to close current window");
            return false;
        }
    }

    /* -------------------- CLOSE ALL OTHER WINDOWS -------------------- */

    @Override
    public boolean closeAllOtherWindows() {
        try {
            List<String> handles = getAllWindowHandles();
            String parent = handles.get(0);

            for (String handle : handles) {
                if (!handle.equals(parent)) {
                    driver.switchTo().window(handle);
                    driver.close();
                    ExtentManager.infoTest("Closed window: " + handle);
                }
            }

            driver.switchTo().window(parent);
            ExtentManager.infoTest("Switched back to parent window");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to close other windows");
            return false;
        }
    }

    /* -------------------- OPEN NEW TAB -------------------- */

    @Override
    public boolean openNewTab() {
        try {
            driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
            ExtentManager.infoTest("Opened new browser tab");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to open new tab");
            return false;
        }
    }

    /* -------------------- SWITCH WINDOW (LAST OPENED) -------------------- */

    @Override
    public void switchWindow() {
        List<String> handles = getAllWindowHandles();
        driver.switchTo().window(handles.get(handles.size() - 1));
        ExtentManager.infoTest("Switched to last opened window");
    }

	@Override
	public boolean childWindowCloseIndex(int index)
	{
		Set<String> windowHandles = driver.getWindowHandles();
		for (int count = 0; count < windowHandles.size(); count++) {
			if (count == index) {
				driver.close();
				return true;
			}
		}
		return false;
	}
    
}
