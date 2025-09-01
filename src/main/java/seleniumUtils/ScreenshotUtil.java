package seleniumUtils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;

import base.DriverManager;
import base.PageBase;
import pages.PageFactory;
import reporting.ExtentManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for capturing screenshots with optional element highlight.
 */
public class ScreenshotUtil extends WaitUtil {

	WebDriver driver;
    public ScreenshotUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /**
     * Capture screenshot with optional element highlight.
     *
     * @param screenshotName custom screenshot name
     * @param element        element to highlight (can be null)
     * @return path of saved screenshot
     */
    public static synchronized String takeScreenshot(String screenshotName, WebElement element) {
        String screenshotPath = null;
        try {
            // Highlight element if provided
            if (element != null) {
                highlightElement(element);
            }

            // Generate timestamp
            String timeStamp = new SimpleDateFormat("yyMMdd_HHmmssSSS").format(new Date());

            // Define screenshot path
            String screenshotDir = PageBase.detectFilePath(System.getProperty("user.dir")
                    + "/src/test/resources/ExtentReports/ScreenShots/");
            File destPath = new File(screenshotDir + screenshotName + "_" + timeStamp + ".jpg");

            // Create directory if it does not exist
            if (!destPath.getParentFile().exists()) {
                destPath.getParentFile().mkdirs();
            }

            // Capture screenshot
            File srcFile = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(srcFile, destPath);
            screenshotPath = destPath.getAbsolutePath();

            // Attach screenshot to Extent report
            ExtentManager.getTest().log(Status.INFO, "Screenshot captured",
                    MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());

            // Remove highlight
            if (element != null) {
                removeHighlight(element);
            }

        } catch (IOException e) {
            ExtentManager.getTest().log(Status.WARNING, "Unable to take screenshot: " + e.getMessage());
        }
        return screenshotPath;
    }
    
    public static synchronized void takeScreenshot()
	{
		try
		{
			// Directly capture as Base64
			String base64Screenshot = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BASE64);

			// Embed in report
			ExtentManager.getTest().log(Status.INFO, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
			ExtentManager.getTest().info("<img src='data:image/png;base64," + base64Screenshot + "' height='400' width='800'/>");

		} catch (Exception e)
		{
			ExtentManager.getTest().log(Status.WARNING, "Failed to take screenshot: " + e.getMessage());
		}
	}

    /**
     * Highlight element by adding red border using JavaScript.
     */
    private static void highlightElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
            js.executeScript("arguments[0].setAttribute('style', arguments[0].getAttribute('style') + '; border: 3px solid red; background: yellow;');", element);
        } catch (Exception e) {
            ExtentManager.getTest().log(Status.WARNING, "Highlight failed: " + e.getMessage());
        }
    }

    /**
     * Remove element highlight.
     */
    private static void removeHighlight(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
            js.executeScript("arguments[0].setAttribute('style', arguments[0].getAttribute('style').replace(/; border: 3px solid red; background: yellow;/g, ''));", element);
        } catch (Exception e) {
        }
    }

    /**
     * Capture Base64 screenshot (optional for CI/CD) with highlight.
     */
    public static synchronized String takeScreenshotBase64(WebElement element) {
        try {
            if (element != null) highlightElement(element);
            String base64 = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BASE64);
            if (element != null) removeHighlight(element);
            return base64;
        } catch (Exception e) {
            ExtentManager.getTest().log(Status.WARNING, "Unable to take Base64 screenshot: " + e.getMessage());
            return null;
        }
    }

    // Overloaded method for old usage (no element)
    public static synchronized String takeScreenshot(String screenshotName) {
        return takeScreenshot(screenshotName, null);
    }
}
