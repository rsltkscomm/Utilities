package seleniumUtils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling different click actions safely with logging.
 */
public class ClickUtil extends ScrollUtil
{

	private WebDriver driver;

	public ClickUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	/* -------------------- NORMAL CLICK -------------------- */
	public boolean clickElement(Object pr)
	{
		try
		{
			waitForClickable(getElement(pr), 120);
			WebElement element = getElement(pr);
			element.click();
			ExtentManager.infoTest("Click : " + LocatorUtil.logName.get());
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Click failed : " + LocatorUtil.logName.get());
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}

	/* -------------------- SAFE CLICK -------------------- */
	public boolean safeClick(Object pr)
	{
		try
		{
			WebElement element = waitForClickable(pr, 30);
			if (element != null)
			{
				element.click();
				ExtentManager.infoTest("Safe Click : " + LocatorUtil.logName.get());
				return true;
			} else
			{
				ExtentManager.failTest("SafeClick failed -> Element not clickable within timeout");
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("SafeClick failed : " + LocatorUtil.logName.get());
			ExtentManager.failTest("Reason: " + e.getMessage());
		}
		return false;
	}

	/* -------------------- JS CLICK -------------------- */
	public boolean jsClick(Object pr)
	{
		try
		{
			WebElement element = getElement(pr);
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
			ExtentManager.passTest("JS Click : " + LocatorUtil.logName.get());
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("JS Click failed : " + LocatorUtil.logName.get());
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}

	/* -------------------- CLICK WEBELEMENT -------------------- */
	public boolean click(Object element, String elementName)
	{
		try
		{
			getElement(element).click();
			ExtentManager.passTest("Click : " + elementName);
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Click failed : " + elementName);
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}

	public boolean click(Object pr)
	{
		boolean flag = false;
		try
		{
			waitForClickable(getElement(pr), 120);
			WebElement element = getElement(pr);
			element.click();
			ExtentManager.infoTest("Click : " + LocatorUtil.logName.get());
			flag = true;
			return true;
		} catch (Exception e)
		{
			jsClick(pr);
			flag = true;
			return false;
		} finally
		{
			if (flag)
				ExtentManager.passTest("Click successful : " + LocatorUtil.logName.get());
			else
			{
				ExtentManager.failTest("Click failed : " + LocatorUtil.logName.get());
				ScreenshotUtil.takeScreenshot();
			}
		}
	}

	/* -------------------- DOUBLE CLICK -------------------- */
	public boolean doubleClick(Object pr)
	{
		try
		{
			WebElement element = getElement(pr);
			new Actions(driver).doubleClick(element).perform();
			ExtentManager.infoTest("Double Click : " + LocatorUtil.logName.get());
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Double Click failed : " + LocatorUtil.logName.get());
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}

	/* -------------------- RIGHT CLICK / CONTEXT CLICK -------------------- */
	public boolean rightClick(Object pr)
	{
		try
		{
			WebElement element = getElement(pr);
			new Actions(driver).contextClick(element).perform();
			ExtentManager.infoTest("Right Click : " + LocatorUtil.logName.get());
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Right Click failed : " + LocatorUtil.logName.get());
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}

	/* -------------------- HOVER AND CLICK -------------------- */
	public boolean hoverAndClick(Object pr)
	{
		try
		{
			WebElement element = getElement(pr);
			new Actions(driver).moveToElement(element).click().perform();
			ExtentManager.infoTest("Hovered and Clicked : " + LocatorUtil.logName.get());
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Hover and Click failed : " + LocatorUtil.logName.get());
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}
	
}
