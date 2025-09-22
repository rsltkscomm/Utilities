package seleniumUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.aventstack.extentreports.Status;

import base.DriverManager;
import pages.PageFactory;
import reporting.ExtentManager;
import reporting.TestLogManager;

public class ElementUtil extends ClickUtil
{

	WebDriver driver;

	public ElementUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	public String getTextBoxValue(Object locator,String value)
	{
		try
		{
			WebElement element = getElement(locator);
			String text = (element != null) ? element.getAttribute(value).trim() : null;
			ExtentManager.infoTest("Get text from " + LocatorUtil.logName.get() + " : <b>'" + text + "'</b>");
			return text;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get text from " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public String getText(Object locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			String text = (element != null) ? element.getText().trim() : null;
			ExtentManager.infoTest("Get text from " + LocatorUtil.logName.get() + " : <b>'" + text + "'</b>");
			return text;
		} catch (Exception e)
		{
			return null;
		}
	}
	
	public String getAttribute(Object locator, String attribute)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			String value = (element != null) ? element.getAttribute(attribute) : null;
			ExtentManager.infoTest("Get attribute " + attribute + " from " + LocatorUtil.logName.get() + " -> " + value);
			return value;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get attribute " + attribute + " from " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public boolean sendValue(Object locator, String dt)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			element.sendKeys(dt);
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to enter value " + dt + " in " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public String getCssValue(String locator, String property)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			String css = (element != null) ? element.getCssValue(property) : null;
			ExtentManager.infoTest("Get CSS property " + property + " from " + LocatorUtil.logName.get() + " -> " + css);
			return css;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get CSS property " + property + " from " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public boolean isDisplayed(String locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			boolean displayed = element != null && element.isDisplayed();
			ExtentManager.infoTest("Element " + LocatorUtil.logName.get() + " isDisplayed -> " + displayed);
			return displayed;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isDisplayed for " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean isEnabled(String locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			boolean enabled = element != null && element.isEnabled();
			ExtentManager.infoTest("Element " + LocatorUtil.logName.get() + " isEnabled -> " + enabled);
			return enabled;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isEnabled for " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean isSelected(String locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			boolean selected = element != null && element.isSelected();
			ExtentManager.infoTest("Element " + LocatorUtil.logName.get() + " isSelected -> " + selected);
			return selected;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isSelected for " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean enterValue(Object locator, String dt)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			element.clear();
			element.sendKeys(dt);
			String attribute = element.getAttribute("value");
			boolean entered = attribute.equals(dt);
			ExtentManager.infoTest("Enter value <b>'" + dt + "'</b> in " + LocatorUtil.logName.get());
			return entered;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to enter value " + dt + " in " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}
	
	public boolean javaScriptEnterValue(Object locator, String content)
	{
		try
		{
			WebElement contentArea = findElement(getElement(locator));
			if (contentArea != null && contentArea.isDisplayed())
			{
				contentArea.click();
				((JavascriptExecutor) driver).executeScript("arguments[0].focus(); " + "document.execCommand('selectAll', false, null); " + "document.execCommand('insertText', false, arguments[1]);", contentArea, content);
				ExtentManager.passTest("Content inserted successfully.");
				return true;
			} else
			{
				ExtentManager.warningTest("Content area not found or not visible.");
				return false;
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("Exception while inserting the content");
			return false;
		}
	}

	public boolean isElementPresent(String element)
	{
		try
		{
			turnOffImplicityWait();
			driver.findElement(autolocator(element));
			turnOnImplicityWait();
			ExtentManager.infoTest("Element " + LocatorUtil.logName.get() + " is present");
			return true;
		} catch (Exception e)
		{
			turnOnImplicityWait();
			return false;
		}
	}

	public WebElement findElement(Object locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			ExtentManager.infoTest("Found element " + LocatorUtil.logName.get() + "");
			return element;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to find element " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public List<WebElement> findElements(Object pr)
	{
		try
		{
			List<WebElement> elements = getElements(pr);
			ExtentManager.infoTest("Found " + elements.size() + " elements for " + LocatorUtil.logName.get() + "");
			return elements;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to find elements " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public String getAllDropdownValues(String locator)
	{
		List<WebElement> dropdownValues = findElements(locator);

		return dropdownValues.stream().map(WebElement::getText).collect(Collectors.joining(","));
	}

	public void tabAction()
	{
		Actions action = new Actions(DriverManager.getDriver());
		action.sendKeys(Keys.TAB).build().perform();
	}
	
	public void clickEnter()
	{
		Actions action = new Actions(DriverManager.getDriver());
		action.sendKeys(Keys.ENTER).build().perform();
	}

	@SuppressWarnings("unchecked")
	public void clearField(Object pr)
	{
		List<WebElement> elements = new ArrayList<>();

		if (pr instanceof String)
		{
			elements = driver.findElements(autolocator((String) pr));
		} else if (pr instanceof WebElement)
		{
			elements = Collections.singletonList((WebElement) pr);
		} else if (pr instanceof List<?>)
		{
			elements = (List<WebElement>) pr;
		}
		if (elements == null || elements.isEmpty())
		{
			ExtentManager.getTest().log(Status.FAIL, "Unable to locate WebElement(s)");
			return;
		}

		for (WebElement ele : elements)
		{
			try
			{
				if (ele != null && ele.isDisplayed())
				{
					ele.click();
					ele.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);

					String placeholder = ele.getAttribute("placeholder");
					if (placeholder != null && !placeholder.isEmpty())
					{
						ExtentManager.getTest().log(Status.INFO, placeholder + " field text has been cleared");
					} else
					{
						ExtentManager.getTest().log(Status.INFO, "Field text has been cleared");
					}
				}
			} catch (Exception e)
			{
				TestLogManager.error("Unable to clear text from element", e);
				ExtentManager.getTest().log(Status.FAIL, "Failed to clear field text");
			}
		}
	}

	//This method is used to find the goal ratio
	public int findGCV(int a, int b)
	{
		if (b == 0)
			return a;
		return findGCV(b, a % b);
	}
	
	public static String rgbToHexColor(String cssValue)
	{
		String[] RGBcolor = cssValue.replace("rgb(", "").replace(" ", "").replace(")", "").split(",");
		int redColorValue = Integer.parseInt(RGBcolor[0]);
		int greenColorValue = Integer.parseInt(RGBcolor[1]);
		int blueColorValue = Integer.parseInt(RGBcolor[2]);
		Color color = new Color(redColorValue, greenColorValue, blueColorValue);
		String hexcolour = "#" + Integer.toHexString(color.getRGB()).substring(2);
		return hexcolour;
	}
	
	public static String decodeBase64ToText(String base64Text)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
		return new String(decodedBytes);
	}

}
