package seleniumUtils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.PageFactory;
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
   	 * 
   	 * Scroll to the particular element
   	 *
   	 * @param pr - By calling autolocator method and object repository.
   	 */
   	public void javaScriptScrollIntoView(Object pr)
   	{
   		try
   		{
   			waitForVisible(pr, 50);
   			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", getElement(pr));
   			wait(1);
   		} catch (NoSuchElementException e)
   		{
   			TestLogManager.error("Exception occurred", e);
   		}
   	}

    /* -------------------- SCROLL BY PIXELS -------------------- */
    public boolean scrollBy(int x, int y) {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- SCROLL TO TOP -------------------- */
    public boolean scrollToTop() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- SCROLL TO BOTTOM -------------------- */
    public boolean scrollToBottom() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* -------------------- SCROLL BY ELEMENT OFFSET -------------------- */
    public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset) {
        try {
            WebElement element = getElement(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollBy(arguments[1], arguments[2]);", element, xOffset, yOffset);
            return true;
        } catch (Exception e) {
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
			{
				js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
				Thread.sleep(1000); 

				long newHeight = (long) js.executeScript("return document.body.scrollHeight");
				if (newHeight == lastHeight)
				{
					break;
				}
				lastHeight = newHeight;
				retries++;
			}
		} catch (Exception e)
		{
		}
	}
	
	public void scrollStep(JavascriptExecutor jse, int pixels)
	{
		jse.executeScript("window.scrollBy(0," + pixels + ")");
	}
}
