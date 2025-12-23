package core.playwright;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.FrameInterface;
import reporting.ExtentManager;

public class PlaywrightFrameUtil extends PlaywrightSelectUtil
        implements FrameInterface {

    protected final DriverContext driverContext;
    protected Frame currentFrame;

    public PlaywrightFrameUtil(DriverContext driverContext) {
        super(driverContext); // initial page
        this.driverContext = driverContext;
        this.currentFrame = driverContext.getPage().mainFrame();
    }

    /**
     * Always get the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    /**
     * 🔁 Called when window/tab switches
     */
    protected void updatePage(Page page) {
        this.currentFrame = page.mainFrame();
    }

    /* -------------------- SWITCH TO FRAME BY INDEX -------------------- */

    @Override
    public boolean switchToFrame(int index) {
        try {
            if (index < 0 || index >= page().frames().size()) {
                throw new IndexOutOfBoundsException("Invalid frame index: " + index);
            }
            currentFrame = page().frames().get(index);
            ExtentManager.infoTest("Switched to frame by index: " + index);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to frame by index failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO FRAME BY NAME -------------------- */

    @Override
    public boolean switchToFrame(String nameOrId) {
        try {
            Frame frame = page().frame(nameOrId);
            if (frame == null) {
                throw new RuntimeException("Frame not found: " + nameOrId);
            }
            currentFrame = frame;
            ExtentManager.infoTest("Switched to frame by name/id: " + nameOrId);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to frame by name/id failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO FRAME BY LOCATOR -------------------- */

    @Override
    public boolean switchToFrame(Object frameElement) {
        try {
            if (!(frameElement instanceof String)) {
                throw new IllegalArgumentException(
                        "Playwright requires iframe locator STRING");
            }

            String locator =
                    new PlaywrightLocatorUtil(driverContext)
                            .getLocator(frameElement.toString())
                            .toString();

            // Remove invalid framework prefixes (CRITICAL)
            if (locator.startsWith("Locator@")) {
                locator = locator.replace("Locator@", "");
            }

            // Locate iframe properly
            ElementHandle iframeHandle =
                    page().locator(locator).first().elementHandle();

            if (iframeHandle == null) {
                throw new RuntimeException("Iframe not found: " + locator);
            }

            Frame frame = iframeHandle.contentFrame();
            if (frame == null) {
                throw new RuntimeException(
                        "Unable to resolve contentFrame for iframe: " + locator);
            }

            currentFrame = frame;
            ExtentManager.infoTest("Switched to frame: " + locator);
            return true;

        } catch (Exception e) {
            ExtentManager.failTest("Switch to frame failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }


    /* -------------------- SWITCH TO PARENT FRAME -------------------- */

    @Override
    public boolean switchToParentFrame() {
        try {
            if (currentFrame != null && currentFrame.parentFrame() != null) {
                currentFrame = currentFrame.parentFrame();
                ExtentManager.infoTest("Switched to parent frame");
                return true;
            }
            ExtentManager.warningTest("No parent frame available");
            return false;
        } catch (Exception e) {
            ExtentManager.failTest("Switch to parent frame failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SWITCH TO DEFAULT CONTENT -------------------- */

    @Override
    public boolean switchToDefaultContent() {
        currentFrame = page().mainFrame();
        ExtentManager.infoTest("Switched to default content");
        return true;
    }

    /* -------------------- INTERNAL -------------------- */

    public Frame getCurrentFrame() {
        return currentFrame;
    }
}
