package core.selenium;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import core.interfaces.ScreenshotInterface;

public class SeleniumScreenshotUtil extends SeleniumWaitUtil implements ScreenshotInterface
{

	WebDriver driver;

	public SeleniumScreenshotUtil(WebDriver driver) {
		super(driver);
		this.driver = driver;
	}

	// ------------------------------ UTIL: Resolve element ------------------------------

	private WebElement resolveElement(Object obj)
	{
		if (obj == null)
			return null;

		if (obj instanceof WebElement)
			return (WebElement) obj;

		if (obj instanceof String)
			return driver.findElement(autolocator(obj.toString()));

		return null;
	}

	// ------------------------------ INTERFACE IMPLEMENTATION ---------------------------

	@Override
	public String takeScreenshot(String screenshotName, Object element)
	{
		WebElement webEl = resolveElement(element);
		return takeScreenshot(screenshotName, webEl);
	}

	@Override
	public void takeScreenshot()
	{
		takeScreenshot("screenshot", (WebElement) null);
	}

	@Override
	public String takeScreenshotBase64(Object element)
	{
		WebElement webEl = resolveElement(element);
		return takeScreenshotBase64(webEl);
	}

	@Override
	public String takeScreenshot(String screenshotName)
	{
		return takeScreenshot(screenshotName, (WebElement) null);
	}

	@Override
	public void javaScriptHighLightwithScrnShot(Object obj)
	{

		WebElement element = resolveElement(obj);
		if (element == null)
		{
			takeScreenshot("highlight-null", null);
			return;
		}

		String originalStyle = getAttributeStyle(element);

		try
		{
			// Apply highlight
			highlightElement(element);

			// Capture screenshot
			takeScreenshot("highlight", element);

		} finally
		{
			// Restore original style
			removeHighlight(element, originalStyle);
		}
	}
	
	public void highlightElement(WebElement element)
	{
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].setAttribute('style','border:2px solid red; background:yellow;')", element);
	}
	
	public void removeHighlight(WebElement element,String originalStyle){
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, originalStyle);
	}
	
	public String getAttributeStyle(WebElement element)
	{
		return element.getAttribute("style");
	}
}
