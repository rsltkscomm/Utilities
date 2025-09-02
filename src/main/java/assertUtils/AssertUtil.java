package assertUtils;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import base.DriverManager;
import pages.PageFactory;
import reporting.ExtentManager;
import reporting.TestLogManager;
import seleniumUtils.LocatorUtil;
import seleniumUtils.ScreenshotUtil;

public class AssertUtil extends LocatorUtil
{
	public AssertUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
	}

	public boolean writeLog(boolean expression, String passLog, String failLog)
	{
		if (expression)
		{
			ExtentManager.infoTest(passLog);
			ScreenshotUtil.takeScreenshot();
		} else
		{
			ExtentManager.warningTest(failLog);
			ScreenshotUtil.takeScreenshot();
		}
		return expression;
	}
	
	public boolean checkIsElementNull(WebElement ele)
	{
		return ele == null ? false : true;
	}
	
	public boolean getUiBackgroundColour(String type, String pr)
	{
		boolean flag = false;
 
		try
		{
			// Get the CSS value (e.g., "rgba(255, 255, 255, 1)")
			String cssValue = DriverManager.getDriver().findElement(autolocator(pr)).getCssValue(type);
 
			if (cssValue == null || !cssValue.contains("("))
			{
				throw new IllegalArgumentException("Invalid CSS color value: " + cssValue);
			}
 
			// Extract the RGB values
			String[] rgbValues = StringUtils.substringBetween(cssValue, "(", ")").replaceAll("\\s+", "").split(",");
 
			int red = Integer.parseInt(rgbValues[0]);
			int green = Integer.parseInt(rgbValues[1]);
			int blue = Integer.parseInt(rgbValues[2]);
 
			// Convert to hex (#RRGGBB)
			String hexColour = String.format("#%02x%02x%02x", red, green, blue);
 
			System.out.println("Extracted color: " + cssValue + " -> " + hexColour);
 
			flag = true; // success
 
		} catch (Exception e)
		{
			System.err.println("Error extracting UI background colour: " + e.getMessage());
			e.printStackTrace();
			flag = false; // fail gracefully
		}
		return flag;
	}
	
	public void writeLoggerCombination(boolean expression, String passLog, String failLog)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stackTrace[2];
 
		String location = caller.getClassName() + "." + caller.getMethodName() + "():" + caller.getLineNumber();
 
		if (expression)
		{
			String message = passLog + " [" + location + "]";
			TestLogManager.info(message);
			ExtentManager.infoTest(message);
			ScreenshotUtil.takeScreenshot();
		} else
		{
			String message = failLog + " [" + location + "]";
			TestLogManager.error(message);
			ExtentManager.warningTest(message);
			ScreenshotUtil.takeScreenshot();
		}
	}
}
