package seleniumUtils;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import assertUtils.AssertUtil;
import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling dropdown selections with logging.
 */
public class DropdownUtil extends AssertUtil
{

	WebDriver driver;

	public DropdownUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	/**
	 * Selects an element from a list of dropdown values.
	 *
	 * @param elementspath - Locator path for dropdown list elements
	 * @param input        - Value to match and select
	 * @return true if selection was successful, false otherwise
	 */
	public boolean selectListElements(String elementspath, String input)
	{
		try
		{
			List<WebElement> allElements = findElements(elementspath);
			boolean elementFound = false;

			if (!allElements.isEmpty())
			{
				for (int i = 0; i < allElements.size(); i++)
				{
					allElements = findElements(elementspath);
					highlightElement(allElements.get(i));
					String eleText = allElements.get(i).getText().trim();
					removeHighlight(allElements.get(i));
					String inputVal = input.trim();
					if (eleText.equalsIgnoreCase(inputVal) || eleText.toLowerCase().contains(inputVal.toLowerCase()))
					{
						highlightElement(allElements.get(i));
						allElements.get(i).click();
						removeHighlight(allElements.get(i));
						ExtentManager.passTest("Dropdown selection successful -> Selected: <b>" + eleText + "</b>");
						elementFound = true;
						ScreenshotUtil.takeScreenshot();
						break;
					}
				}

				if (!elementFound)
				{
					ExtentManager.failTest("Dropdown selection failed -> Value not found: <b>" + input+"</b>");
				}
				return elementFound;

			} else
			{
				ExtentManager.failTest("Dropdown selection failed -> No elements found for locator: " + elementspath);
				return false;
			}

		} catch (Exception e)
		{
			ExtentManager.failTest("Dropdown selection failed -> Locator: " + elementspath);
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Selects an element from a list of dropdown values.
	 *
	 * @param elementspath - Locator path for dropdown list elements
	 * @param input        - Value to match and select
	 * @return true if selection was successful, false otherwise
	 */
	public boolean selectExactListElements(String elementspath, String input)
	{
		try
		{
			List<WebElement> allElements = findElements(elementspath);
			boolean elementFound = false;

			if (!allElements.isEmpty())
			{
				for (int i = 0; i < allElements.size(); i++)
				{
					allElements = findElements(elementspath);
					highlightElement(allElements.get(i));
					String eleText = allElements.get(i).getText().trim();
					removeHighlight(allElements.get(i));
					String inputVal = input.trim();
					if (eleText.equalsIgnoreCase(inputVal))
					{
						highlightElement(allElements.get(i));
						allElements.get(i).click();
						removeHighlight(allElements.get(i));
						ExtentManager.passTest("Dropdown selection successful -> Selected: <b>" + eleText + "</b>");
						elementFound = true;
						ScreenshotUtil.takeScreenshot();
						break;
					}
				}

				if (!elementFound)
				{
					ExtentManager.failTest("Dropdown selection failed -> Value not found: <b>" + input + "</b>");
				}
				return elementFound;

			} else
			{
				ExtentManager.failTest("Dropdown selection failed -> No elements found for locator: " + elementspath);
				return false;
			}

		} catch (Exception e)
		{
			ExtentManager.failTest("Dropdown selection failed -> Locator: " + elementspath);
			ExtentManager.failTest("Reason: " + e.getMessage());
			return false;
		}
	}
	
	public boolean selectListElementByIndex(String elementsPath, int index)
	{
		List<WebElement> allElements = findElements(elementsPath);
 
		if (index < 0 || index >= allElements.size())
		{
			throw new IllegalArgumentException("Invalid index: " + index + ". List size: " + allElements.size());
		}
 
		int attempts = 0;
		while (attempts < 3)
		{
			try
			{
				// Re-find elements in case DOM changed
				allElements = findElements(elementsPath);
				if (index < allElements.size())
				{
					allElements.get(index).click();
					return true; // Success - exit method
				}
			} catch (StaleElementReferenceException e)
			{
				// Ignore and retry
				return false;
			}
			attempts++;
		}
 
		// If we get here, all attempts failed
		throw new RuntimeException("Failed to click element at index " + index + " after 3 attempts");
	}
	
	public List<String> getDropdownValuesasList(String dropdownLocator, String dropdownlistLocator)
	{
		List<String> list = new ArrayList<String>();
		scrollToElement(dropdownLocator);
		clickElement(dropdownLocator);
		List<WebElement> dropdownlists = findElements(dropdownlistLocator);
		if (dropdownlists.size() > 1)
		{
			for (WebElement webElement : dropdownlists)
			{
				list.add(webElement.getText());
			}
		}
		return list;
	}
	
	public boolean selectListElementByAttribute(Object pr, String attribute, String value)
	{
		List<WebElement> elements = findElements(pr);
		for (int i = 0; i < elements.size(); i++)
		{
			String attrValue = elements.get(i).getAttribute(attribute).trim();
			if (attrValue.contains(value))
			{
				elements.get(i).click();
				ExtentManager.infoTest("Clicked on element with " + attribute + ": " + value);
				return true;
			}
		}
		return false;
	}
	
	
}
