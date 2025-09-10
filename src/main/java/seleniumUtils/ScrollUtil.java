package seleniumUtils;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import pages.PageFactory;
import reporting.ExtentManager;
import reporting.TestLogManager;

/**
 * Utility class for handling scroll actions (Selenium + JavaScript) with logging.
 */
public class ScrollUtil extends BrowserUtil {

	WebDriver driver;
    public ScrollUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- SCROLL INTO VIEW -------------------- */
    public boolean scrollToElement(Object pr) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            ExtentManager.infoTest("Scrolled into view: " + element.toString());
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll to element failed. Reason: " + e.getMessage());
            return false;
        }
    }
    
    /**
	 * 
	 * Scroll to the particular element
	 *
	 * @param pr - By calling autolocator method and object repository.
	 */
	public void javaScriptScrollIntoView(String pr)
	{
		try
		{
			By locator = autolocator(pr);

			// Wait for visibility before scrolling
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
			WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

			// Scroll element into view
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);

			// Optional: highlight or log
			highlightElement(element);
			ExtentManager.infoTest("Scrolled into view: " + logName.get());
			wait(1);
		} catch (NoSuchElementException e)
		{
			ExtentManager.infoTest("Scroll to element failed");
			TestLogManager.error("Exception occurred", e);
		}
	}

    /* -------------------- SCROLL BY PIXELS -------------------- */
    public boolean scrollBy(int x, int y) {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
            ExtentManager.infoTest("Scrolled by pixels - X: " + x + ", Y: " + y);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll by pixels failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL TO TOP -------------------- */
    public boolean scrollToTop() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            ExtentManager.infoTest("Scrolled to top of the page");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll to top failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL TO BOTTOM -------------------- */
    public boolean scrollToBottom() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            ExtentManager.infoTest("Scrolled to bottom of the page");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll to bottom failed. Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- SCROLL BY ELEMENT OFFSET -------------------- */
    public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollBy(arguments[1], arguments[2]);", element, xOffset, yOffset);
            ExtentManager.infoTest("Scrolled element " + element.toString() + " by offset X:" + xOffset + " Y:" + yOffset);
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Scroll by element offset failed. Reason: " + e.getMessage());
            return false;
        }
    }
    
	public void waitForScroll()
	{
		try
		{
			JavascriptExecutor js = (JavascriptExecutor) driver;
			long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

			int retries = 0;
			while (retries < 10)
			{ // max retries
				js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
				Thread.sleep(1000); // wait for page load / scroll

				long newHeight = (long) js.executeScript("return document.body.scrollHeight");
				if (newHeight == lastHeight)
				{
					break; // ✅ no more scrolling possible
				}
				lastHeight = newHeight;
				retries++;
			}
			ExtentManager.infoTest("✅ Page scrolled to the bottom successfully.");
		} catch (Exception e)
		{
			ExtentManager.failTest("❌ waitForScroll failed: " + e.getMessage());
		}
	}

}
