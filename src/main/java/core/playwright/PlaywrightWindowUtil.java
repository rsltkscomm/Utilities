package core.playwright;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import core.interfaces.WindowInterface;
import reporting.ExtentManager;

public class PlaywrightWindowUtil extends PlaywrightFrameUtil implements WindowInterface {

    private final BrowserContext context;
    private Page currentPage;

    public PlaywrightWindowUtil(Page page) {
        super(page);
        this.context = page.context();
        this.currentPage = page;
    }

    /* -------------------- GET CURRENT WINDOW HANDLE -------------------- */

    @Override
    public String getCurrentWindowHandle() {
        try {
            String handle = "PAGE_" + context.pages().indexOf(currentPage);
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
        List<String> handles = new ArrayList<>();
        try {
            for (int i = 0; i < context.pages().size(); i++) {
                handles.add("PAGE_" + i);
            }
            ExtentManager.infoTest("All window handles: " + handles);
            return handles;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to get all window handles");
            return handles;
        }
    }

    /* -------------------- SWITCH TO WINDOW BY HANDLE -------------------- */

    @Override
    public boolean switchToWindow(String windowHandle) {
        try {
            int index = Integer.parseInt(windowHandle.replace("PAGE_", ""));
            return switchToWindow(index);
        } catch (Exception e) {
            ExtentManager.failTest("Invalid window handle: " + windowHandle);
            return false;
        }
    }

    /* -------------------- SWITCH TO WINDOW BY INDEX -------------------- */

    @Override
    public boolean switchToWindow(int index) {
        try {
            List<Page> pages = context.pages();
            if (index < 0 || index >= pages.size()) {
                throw new IndexOutOfBoundsException("Window index out of bounds: " + index);
            }
            currentPage = pages.get(index);
            currentPage.bringToFront();
            ExtentManager.infoTest("Switched to window index: " + index);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Failed to switch to window index: " + index);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO PARENT WINDOW -------------------- */

    @Override
    public boolean switchToParentWindow() {
        try {
            currentPage = context.pages().get(0);
            currentPage.bringToFront();
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
            currentPage.close();
            currentPage = context.pages().get(0);
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
            Page parent = context.pages().get(0);
            for (Page page : new ArrayList<>(context.pages())) {
                if (page != parent) {
                    page.close();
                    ExtentManager.infoTest("Closed window");
                }
            }
            currentPage = parent;
            parent.bringToFront();
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
            currentPage = context.newPage();
            currentPage.bringToFront();
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
        List<Page> pages = context.pages();
        currentPage = pages.get(pages.size() - 1);
        currentPage.bringToFront();
        ExtentManager.infoTest("Switched to last opened window");
    }

    /* -------------------- INTERNAL ACCESS -------------------- */

    protected Page getCurrentPage() {
        return currentPage;
    }
    
    public boolean childWindowCloseIndex(int index) {
        try {
            List<Page> pages = currentPage.context().pages();

            if (index < 0 || index >= pages.size()) {
                return false;
            }
            Page targetPage = pages.get(index);

            // Do not close the main page accidentally
            if (!targetPage.equals(currentPage)) {
                targetPage.close();
                return true;
            }

        } catch (Exception ignored) {
        }
        return false;
    }
}
