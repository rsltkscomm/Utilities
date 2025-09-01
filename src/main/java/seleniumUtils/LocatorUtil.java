package seleniumUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import pages.PageFactory;

/**
 * Utility class to convert locator strings into Selenium By objects.
 * 
 * Format: "ElementName,locatorType,value" Example: "Login Button,xpath,//button[@id='login']"
 */
public class LocatorUtil
{
	WebDriver driver;
	public LocatorUtil(WebDriver driver, PageFactory pageFactory) {
		this.driver = driver;
	}
	public static ThreadLocal<String> logName = new ThreadLocal<String>();

	public static By autolocator(String key)
	{
		if (key == null || key.isEmpty())
		{
			throw new IllegalArgumentException("Locator string cannot be null or empty.");
		}

		String[] parts = key.split(",", 3); // Expecting 3 parts
		if (parts.length < 3)
		{
			throw new IllegalArgumentException("Invalid locator format: " + key + " | Expected format: 'ElementName,locatorType,value'");
		}

		String elementName = parts[0].trim();
		String locatorType = parts[1].trim().toLowerCase();
		String locatorValue = parts[2].trim();
		logName.set(elementName);
		switch (locatorType)
		{
		case "id":
			return By.id(locatorValue);
		case "name":
			return By.name(locatorValue);
		case "xpath":
			return By.xpath(locatorValue);
		case "css":
		case "cssselector":
			return By.cssSelector(locatorValue);
		case "link":
		case "linktext":
			return By.linkText(locatorValue);
		case "parlink":
		case "partiallinktext":
			return By.partialLinkText(locatorValue);
		case "class":
		case "classname":
			return By.className(locatorValue);
		case "tag":
		case "tagname":
			return By.tagName(locatorValue);
		default:
			throw new IllegalArgumentException("Unsupported locator type: " + locatorType + " in locator string: " + key);
		}
	}

}
