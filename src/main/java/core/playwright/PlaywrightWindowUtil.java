package core.playwright;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.WindowInterface;
import reporting.ExtentManager;

public class PlaywrightWindowUtil extends PlaywrightFrameUtil implements WindowInterface {

    private final DriverContext driverContext;
    private final BrowserContext context;

    public PlaywrightWindowUtil(DriverContext driverContext) {
        super(driverContext);
        this.driverContext = driverContext;
        this.context = driverContext.getBrowserContext();
    }

    protected Page page() {
        return driverContext.getPage();
    }

    /* -------------------- WINDOW HANDLES -------------------- */

    @Override
    public String getCurrentWindowHandle() {
        return "PAGE_" + context.pages().indexOf(page());
    }

    @Override
    public List<String> getAllWindowHandles() {
        List<String> handles = new ArrayList<>();
        for (int i = 0; i < context.pages().size(); i++) {
            handles.add("PAGE_" + i);
        }
        return handles;
    }

    /* -------------------- SWITCHING -------------------- */

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

    @Override
    public boolean switchToWindow(int index) {
        List<Page> pages = context.pages();
        if (index < 0 || index >= pages.size()) {
            ExtentManager.failTest("Invalid window index: " + index);
            return false;
        }

        Page target = pages.get(index);
        driverContext.setPage(target);
        super.updatePage(target); // ✅ keep parent in sync
        target.bringToFront();

        ExtentManager.infoTest("Switched to window index: " + index);
        return true;
    }

    @Override
    public boolean switchToParentWindow() {
        if (context.pages().isEmpty()) {
            return false;
        }

        Page parent = context.pages().get(0);
        driverContext.setPage(parent);
        super.updatePage(parent); // ✅
        parent.bringToFront();

        ExtentManager.infoTest("Switched to parent window");
        return true;
    }

    @Override
    public void switchWindow() {
        List<Page> pages = context.pages();

        if (pages.size() < 2) {
            ExtentManager.infoTest("No additional window to switch");
            return;
        }

        Page lastOpened = pages.get(pages.size() - 1);
        driverContext.setPage(lastOpened);
        super.updatePage(lastOpened); // ✅
        lastOpened.bringToFront();

        ExtentManager.infoTest("Switched to last opened window");
    }

    /* -------------------- OPEN / CLOSE -------------------- */

    @Override
    public boolean openNewTab() {
        Page newPage = context.newPage();
        driverContext.setPage(newPage);
        super.updatePage(newPage); // ✅
        newPage.bringToFront();

        ExtentManager.infoTest("Opened new tab");
        return true;
    }

    @Override
    public boolean closeCurrentWindow() {
        if (context.pages().isEmpty()) {
            return false;
        }

        Page current = page();
        current.close();

        Page fallback = context.pages().get(0);
        driverContext.setPage(fallback);
        super.updatePage(fallback); // ✅
        fallback.bringToFront();

        ExtentManager.infoTest("Closed current window");
        return true;
    }

    @Override
    public boolean closeAllOtherWindows() {
        if (context.pages().isEmpty()) {
            return false;
        }

        Page parent = context.pages().get(0);

        for (Page p : new ArrayList<>(context.pages())) {
            if (p != parent) {
                p.close();
            }
        }

        driverContext.setPage(parent);
        super.updatePage(parent); // ✅
        parent.bringToFront();

        ExtentManager.infoTest("Closed all other windows");
        return true;
    }

    @Override
    public boolean childWindowCloseIndex(int index) {
        List<Page> pages = context.pages();

        if (index <= 0 || index >= pages.size()) {
            ExtentManager.failTest("Invalid child window index: " + index);
            return false;
        }

        Page target = pages.get(index);
        target.close();

        Page parent = context.pages().get(0);
        driverContext.setPage(parent);
        super.updatePage(parent); // ✅
        parent.bringToFront();

        ExtentManager.infoTest("Closed child window at index: " + index);
        return true;
    }
}
