package seleniumUtils;

import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.PageFactory;
import reporting.ExtentManager;

public class ElementUtil extends ClickUtil
{

	WebDriver driver;
	public ElementUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	public String getStrText(String locator)
	{
		try
		{
			WebElement element = waitForVisible(locator, 15);
			String text = (element != null) ? element.getText().trim() : null;
			ExtentManager.infoTest("Get text from " + LocatorUtil.logName.get() + " : <b>'" + text + "'</b>");
			return text;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get text from " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public String getAttribute(String locator, String attribute)
	{
		try
		{
			WebElement element = waitForPresence(locator, 15);
			String value = (element != null) ? element.getAttribute(attribute) : null;
			ExtentManager.infoTest("Get attribute " + attribute + " from " + LocatorUtil.logName.get() + " -> " + value);
			return value;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get attribute " + attribute + " from " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public String getCssValue(String locator, String property)
	{
		try
		{
			WebElement element = waitForVisible(locator, 15);
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
			WebElement element = waitForVisible(locator, 10);
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
			WebElement element = waitForPresence(locator, 10);
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
			WebElement element = waitForPresence(locator, 10);
			boolean selected = element != null && element.isSelected();
			ExtentManager.infoTest("Element " + LocatorUtil.logName.get() + " isSelected -> " + selected);
			return selected;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isSelected for " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean enterValue(String pr, String dt)
	{
		try
		{
			WebElement obj = driver.findElement(autolocator(pr));
			obj.clear();
			obj.sendKeys(dt);
			String attribute = obj.getAttribute("value");
			boolean entered = attribute.equals(dt);
			ExtentManager.infoTest("Enter value <b>'" + dt + "'</b> in " + LocatorUtil.logName.get());
			return entered;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to enter value " + dt + " in " + LocatorUtil.logName.get() + " : " + e.getMessage());
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
			ExtentManager.failTest("Element " + LocatorUtil.logName.get() + " is NOT present : " + e.getMessage());
			return false;
		}
	}

	public WebElement findElement(String pr)
	{
		try
		{
			WebElement element = driver.findElement(autolocator(pr));
			ExtentManager.infoTest("Found element " + LocatorUtil.logName.get() + "");
			return element;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to find element " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public List<WebElement> findElements(String pr)
	{
		try
		{
			List<WebElement> elements = driver.findElements(autolocator(pr));
			ExtentManager.infoTest("Found " + elements.size() + " elements for " + LocatorUtil.logName.get() + "");
			return elements;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to find elements " + LocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}
}
