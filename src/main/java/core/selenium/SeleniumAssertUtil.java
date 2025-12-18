package core.selenium;

import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import base.DriverManager;
import core.interfaces.AssertInterface;
import reporting.ExtentManager;
import reporting.TestLogManager;

public class SeleniumAssertUtil extends SeleniumElementUtil implements AssertInterface
{
	public SeleniumAssertUtil(WebDriver driver) {
		super(driver);
	}

	public boolean writeLog(boolean expression, String passLog, String failLog)
	{
		if (expression)
		{
			ExtentManager.infoTest(passLog);
			takeScreenshot();
		} else
		{
			ExtentManager.warningTest(failLog);
			takeScreenshot();
		}
		return expression;
	}

	@Override
	public boolean checkIsElementNull(Object ele)
	{
		return ele != null;
	}

	public boolean writeLogger(boolean expression, String passLog, String failLog)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stackTrace[2];

		String location = caller.getClassName() + "." + caller.getMethodName() + "():" + caller.getLineNumber();

		if (expression)
		{
			String message = passLog + " [" + location + "]";
			TestLogManager.info(message);
			ExtentManager.infoTest(message);
			takeScreenshot();
		} else
		{
			String message = failLog + " [" + location + "]";
			TestLogManager.error(message);
			ExtentManager.warningTest(message);
			takeScreenshot();
		}
		return expression;
	}
	
	public boolean placeholderValueCheck(String locator, String placeHolderText)
	{
		String uiPlaceholderText = getText(locator);
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
			String actualHex = String.format("#%02x%02x%02x", redColorValue, greenColorValue, blueColorValue);
			boolean isMatch = actualHex.equalsIgnoreCase(expectedHex);
			
			writeLog(isMatch, "Background colour matches expected: " + actualHex, "Background colour mismatch. Expected: " + expectedHex + ", Found: " + actualHex);
			return isMatch;
		} catch (Exception e)
		{
			ExtentManager.warningTest("Error while validating background colour for locator [" + pr + "]: " + e.getMessage());
			return false;
		}
	}
	
	public boolean uiPageEqualsWithMultipleInputValue(String locator, String testDatas) {
	    List<WebElement> elements = findElements(locator);
	    String[] expectedValues = testDatas.split(",");
	    
	    if (elements.isEmpty()) {
	        ExtentManager.getTest().fail("No elements found for locator: " + locator);
	        takeScreenshot();
	        return false;
	    }

	    if (elements.size() != expectedValues.length) {
	        ExtentManager.getTest().fail("UI values count (" + elements.size() + 
	                                     ") does not match expected count (" + expectedValues.length + ")");
	        return false;
	    }

	    boolean allMatch = true;
	    for (int i = 0; i < elements.size(); i++) {
	        String actual = elements.get(i).getText().trim();
	        String expected = expectedValues[i].trim();

	        if (expected.equals(actual)) {
	            ExtentManager.getTest().info("UI text <b>'" + actual + "'</b> matches expected.");
	        } else {
	            new Actions(DriverManager.getDriver()).scrollToElement(elements.get(i)).perform();
	            ExtentManager.getTest().fail("UI text <b>'" + actual + "'</b> does not match expected <b>'" + expected + "'</b>");
	            allMatch = false;
	        }
	    }
	    return allMatch;
	}
	
	public boolean uiPageEqualswithInputValue(String txt, String actualText)
	{
		boolean flag = false;

		if (txt.trim().equals(actualText.trim()))
		{
			ExtentManager.infoTest("UI text <b>'" + txt + "'</b> is displayed, As expected.");
			flag = true;
		} else
		{
			ExtentManager.warningTest("UI text <b>" + txt + "</b> is not displayed, As expected text <b>" + actualText + "</b>");
			flag = false;
		}
		return flag;
	}
	
	@Override
	public String decodeBase64ToText(String base64Text)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
		return new String(decodedBytes);
	}
 
}
