package assertUtils;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import base.DriverManager;
import pages.PageFactory;
import reporting.ExtentManager;
import reporting.TestLogManager;
import seleniumUtils.ElementUtil;
import seleniumUtils.ScreenshotUtil;

public class AssertUtil extends ElementUtil
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

	public boolean writeLoggerCombination(boolean expression, String passLog, String failLog)
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
		return expression;
	}
	
	public boolean placeholderValueCheck(String locator, String placeHolderText)
	{
		String uiPlaceholderText = getStrText(locator);
		boolean status = uiPlaceholderText.equals(placeHolderText);
		if (status)
		{
			ExtentManager.infoTest("UI Placeholder Text \"<b>" + uiPlaceholderText + "</b> is displayed as expected Input Placeholder text <b>" + placeHolderText + "</b>");
		} else
		{
			ExtentManager.failTest("UI Placeholder Text \"<b>" + uiPlaceholderText + "</b> is not displayed as expected Input Placeholder text <b>" + placeHolderText + "</b>");
		}
		return status;
	}
	
	public boolean validateUiBackgroundColour(String type, String pr, String expectedHex)
	{
		try
		{
			String cssValue = DriverManager.getDriver().findElement(autolocator(pr)).getCssValue(type);
			String[] RGBcolor = StringUtils.substringBetween(cssValue, "(", ")").replaceAll("\\s+", "").split(",");
 
			int redColorValue = Integer.parseInt(RGBcolor[0]);
			int greenColorValue = Integer.parseInt(RGBcolor[1]);
			int blueColorValue = Integer.parseInt(RGBcolor[2]);
 
			// Convert to hex
			String actualHex = String.format("#%02x%02x%02x", redColorValue, greenColorValue, blueColorValue);
 
			// Compare ignoring case
			boolean isMatch = actualHex.equalsIgnoreCase(expectedHex);
 
			if (isMatch)
			{
				ExtentManager.infoTest("Background colour matches expected: " + actualHex);
			} else
			{
				ExtentManager.warningTest("Background colour mismatch. Expected: " + expectedHex + ", Found: " + actualHex);
			}
			return isMatch;
		} catch (Exception e)
		{
			ExtentManager.warningTest("Error while validating background colour for locator [" + pr + "]: " + e.getMessage());
			return false;
		}
	}
 
 
}
