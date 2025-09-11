package seleniumUtils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for Drag & Drop operations using both Selenium Actions and JavaScript.
 */
public class DragDropUtil extends MousehoverUtil {

	WebDriver driver;
    public DragDropUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* ============================================================= */
    /* ===============   SELENIUM ACTIONS METHODS   ================= */
    /* ============================================================= */

    // Basic drag and drop
    public boolean dragAndDrop(Object sourcePr, Object targetPr) {
        try {
            WebElement source = getElement(sourcePr);
            WebElement target = getElement(targetPr);
            new Actions(driver).dragAndDrop(source, target).perform();
            ExtentManager.passTest("Dragged element " + source.toString() + " and dropped on " + target.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Drag and drop failed. Reason: " + e.getMessage());
            return false;
        }
    }

    // Drag and drop by offset
    public boolean dragAndDropByOffset(Object sourcePr, int xOffset, int yOffset) {
        try {
            WebElement source = getElement(sourcePr);
            new Actions(driver).dragAndDropBy(source, xOffset, yOffset).perform();
            ExtentManager.passTest("Dragged element " + source.toString() + " by offset X:" + xOffset + ", Y:" + yOffset);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Drag and drop by offset failed. Reason: " + e.getMessage());
            return false;
        }
    }

    // Click and hold -> move to target -> release
    public boolean clickHoldMoveRelease(Object sourcePr, Object targetPr) {
        try {
            WebElement source = getElement(sourcePr);
            WebElement target = getElement(targetPr);
            new Actions(driver).clickAndHold(source).moveToElement(target).release().perform();
            ExtentManager.passTest("Click-Hold-Move-Release from " + source.toString() + " to " + target.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Click-Hold-Move-Release failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* ============================================================= */
    /* ===============   JAVASCRIPT FALLBACK METHODS   ============== */
    /* ============================================================= */

    // JS Drag and Drop using custom event dispatch
    public boolean jsDragAndDrop(Object sourcePr, Object targetPr) {
        try {
            WebElement source = getElement(sourcePr);
            WebElement target = getElement(targetPr);

            String jsCode =
                    "function triggerDragAndDrop(sourceNode, destinationNode) {" +
                    "  var EVENT_TYPES = ['dragstart', 'dragenter', 'dragover', 'drop', 'dragend'];" +
                    "  function createCustomEvent(type) {" +
                    "    var event = new CustomEvent('CustomEvent');" +
                    "    event.initCustomEvent(type, true, true, null);" +
                    "    event.dataTransfer = {" +
                    "      data: {}," +
                    "      setData: function(key, value) { this.data[key] = value; }," +
                    "      getData: function(key) { return this.data[key]; }" +
                    "    };" +
                    "    return event;" +
                    "  }" +
                    "  EVENT_TYPES.forEach(function(type) {" +
                    "    var event = createCustomEvent(type);" +
                    "    if(type !== 'drop') sourceNode.dispatchEvent(event);" +
                    "    else destinationNode.dispatchEvent(event);" +
                    "  });" +
                    "}" +
                    "triggerDragAndDrop(arguments[0], arguments[1]);";

            ((JavascriptExecutor) driver).executeScript(jsCode, source, target);

            ExtentManager.passTest("JS Drag and Drop executed from " + source.toString() + " to " + target.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS drag and drop failed. Reason: " + e.getMessage());
            return false;
        }
    }

    // JS Drag by offset (manual style move)
    public boolean jsDragByOffset(Object sourcePr, int xOffset, int yOffset) {
        try {
            WebElement source = getElement(sourcePr);

            String jsCode =
                    "arguments[0].style.position='absolute';" +
                    "arguments[0].style.left = (arguments[0].offsetLeft + arguments[1]) + 'px';" +
                    "arguments[0].style.top = (arguments[0].offsetTop + arguments[2]) + 'px';";

            ((JavascriptExecutor) driver).executeScript(jsCode, source, xOffset, yOffset);

            ExtentManager.passTest("JS Drag by offset executed on " + source.toString() + " to X:" + xOffset + ", Y:" + yOffset);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("JS drag by offset failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- UTILITY: Resolve element -------------------- */
    private WebElement getElement(Object pr) {
        return (pr instanceof String)
                ? driver.findElement(autolocator(pr.toString()))
                : (WebElement) pr;
    }
}
