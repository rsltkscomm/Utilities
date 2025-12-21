package core.playwright;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.ScreenshotInterface;
import reporting.ExtentManager;

public class PlaywrightScreenshotUtil extends PlaywrightWaitUtil
        implements ScreenshotInterface {

    protected final DriverContext driverContext;

    public PlaywrightScreenshotUtil(DriverContext driverContext) {
        super(driverContext); // ✅ parent now uses DriverContext
        this.driverContext = driverContext;
    }

    /**
     * Always returns the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    /**
     * Always returns locator util bound to CURRENT page
     */
    protected PlaywrightLocatorUtil locatorUtil() {
        return new PlaywrightLocatorUtil(driverContext);
    }

    // ---------------------- UTILITY: Resolve Element -----------------------

    private Locator resolve(Object obj) {
        if (obj == null) return null;

        if (obj instanceof PlaywrightWebElement)
            return ((PlaywrightWebElement) obj).locator;

        if (obj instanceof String)
            return locatorUtil().getLocator(obj.toString());

        return null;
    }

    private String timeStamp() {
        return new SimpleDateFormat("yyMMdd_HHmmssSSS").format(new Date());
    }

    // ------------------- HIGHLIGHT METHODS ---------------------------------

    private void highlight(Locator locator) {
        try {
            locator.evaluate(
                "el => { el.style.border='3px solid red'; el.style.background='yellow'; }"
            );
        } catch (Exception ignored) {}
    }

    private void unhighlight(Locator locator) {
        try {
            locator.evaluate(
                "el => { el.style.border=''; el.style.background=''; }"
            );
        } catch (Exception ignored) {}
    }

    // -------------------- SCREENSHOT WITH OPTIONAL ELEMENT ------------------

    @Override
    public String takeScreenshot(String screenshotName, Object element) {
        try {
            Locator loc = resolve(element);

            if (loc != null)
                highlight(loc);

            String fileName = screenshotName + "_" + timeStamp() + ".png";
            String folder = System.getProperty("user.dir") + "/screenshots/";
            File dir = new File(folder);
            if (!dir.exists()) dir.mkdirs();

            String path = folder + fileName;

            if (loc != null) {
                loc.screenshot(new Locator.ScreenshotOptions()
                        .setPath(Paths.get(path)));
            } else {
                page().screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(path)));
            }

            ExtentManager.getTest().log(
                Status.INFO,
                "Screenshot",
                MediaEntityBuilder.createScreenCaptureFromPath(path).build()
            );

            if (loc != null)
                unhighlight(loc);

            return path;

        } catch (Exception e) {
            ExtentManager.getTest().log(
                Status.WARNING,
                "Playwright screenshot failed: " + e.getMessage()
            );
            return null;
        }
    }

    // -------------------- SIMPLE SCREENSHOT --------------------

    @Override
    public void takeScreenshot() {
        takeScreenshot("Screenshot", null);
    }

    @Override
    public String takeScreenshot(String screenshotName) {
        return takeScreenshot(screenshotName, null);
    }

    // ---------------------- JS HIGHLIGHT + SCREENSHOT -----------------------

    @Override
    public void javaScriptHighLightwithScrnShot(Object obj) {
        Locator loc = resolve(obj);

        if (loc == null) {
            takeScreenshot("highlight", null);
            return;
        }

        highlight(loc);
        takeScreenshot("highlight", loc);
        unhighlight(loc);
    }

    // ---------------------- BASE64 SCREENSHOT -------------------------------

    @Override
    public String takeScreenshotBase64(Object element) {
        try {
            Locator loc = resolve(element);

            if (loc != null)
                highlight(loc);

            byte[] bytes = (loc != null)
                    ? loc.screenshot(new Locator.ScreenshotOptions())
                    : page().screenshot(new Page.ScreenshotOptions());

            if (loc != null)
                unhighlight(loc);

            return java.util.Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            ExtentManager.getTest().log(
                Status.WARNING,
                "Unable to take Playwright Base64 screenshot: " + e.getMessage()
            );
            return null;
        }
    }
}
