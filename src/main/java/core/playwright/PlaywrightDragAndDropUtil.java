package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import core.interfaces.DragAndDropInterface;
import reporting.ExtentManager;

public class PlaywrightDragAndDropUtil extends PlaywrightMouseHoverUtil
        implements DragAndDropInterface {

    private final Page page;

    public PlaywrightDragAndDropUtil(Page page) {
        super(page);
        this.page = page;
    }

    /* ============================================================= */
    /* ================= PLAYWRIGHT NATIVE METHODS ================= */
    /* ============================================================= */

    @Override
    public boolean dragAndDrop(Object sourcePr, Object targetPr) {
        try {
            Locator source = resolveLocator(sourcePr);
            Locator target = resolveLocator(targetPr);

            source.dragTo(target);

            ExtentManager.passTest("Dragged element and dropped on target");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Drag and drop failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean dragAndDropByOffset(Object sourcePr, int xOffset, int yOffset) {
        try {
            Locator source = resolveLocator(sourcePr);

            source.hover();
            page.mouse().down();
            page.mouse().move(
                    source.boundingBox().x + xOffset,
                    source.boundingBox().y + yOffset
            );
            page.mouse().up();

            ExtentManager.passTest(
                    "Dragged element by offset X:" + xOffset + " Y:" + yOffset);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Drag by offset failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean clickHoldMoveRelease(Object sourcePr, Object targetPr) {
        try {
            Locator source = resolveLocator(sourcePr);
            Locator target = resolveLocator(targetPr);

            source.hover();
            page.mouse().down();

            target.hover();
            page.mouse().up();

            ExtentManager.passTest("Click-Hold-Move-Release successful");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Click-Hold-Move-Release failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* ============================================================= */
    /* =============== JS FALLBACK (COMPATIBILITY) ================= */
    /* ============================================================= */

    /**
     * Playwright already fires proper drag events.
     * This method simply delegates to native dragAndDrop.
     */
    @Override
    public boolean jsDragAndDrop(Object sourcePr, Object targetPr) {
        return dragAndDrop(sourcePr, targetPr);
    }

    /**
     * Playwright mouse movement replaces JS offset hacks.
     */
    @Override
    public boolean jsDragByOffset(Object sourcePr, int xOffset, int yOffset) {
        return dragAndDropByOffset(sourcePr, xOffset, yOffset);
    }
}
