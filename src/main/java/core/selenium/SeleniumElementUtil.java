package core.selenium;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.aventstack.extentreports.Status;

import base.DriverManager;
import core.interfaces.ElementInterface;
import reporting.ExtentManager;
import reporting.TestLogManager;

public class SeleniumElementUtil extends SeleniumClickUtil implements ElementInterface
{

	WebDriver driver;
	
	int invalidLinksCount = 0;
	int validLinksCount = 0;
	

	public SeleniumElementUtil(WebDriver driver) {
		super(driver);
		this.driver = driver;
	}

	public String getTextBoxValue(Object locator, String value)
	{
		try
		{
			WebElement element = getElement(locator);
			String text = (element != null) ? element.getAttribute(value).trim() : null;
			ExtentManager.infoTest("Get text from " + SeleniumLocatorUtil.logName.get() + " : <b>'" + text + "'</b>");
			return text;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get text from " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}
	
	public String getText(Object locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			String text = (element != null) ? element.getText().trim() : null;
			ExtentManager.infoTest("Get text from " + SeleniumLocatorUtil.logName.get() + " : <b>'" + text + "'</b>");
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
			ExtentManager.infoTest("Get attribute " + attribute + " from " + SeleniumLocatorUtil.logName.get() + " -> " + value);
			return value;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get attribute " + attribute + " from " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public boolean sendValue(Object locator, String dt)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			String attributeStyle = getAttributeStyle(element);
			highlightElement(element);
			element.sendKeys(dt);
			removeHighlight(element,attributeStyle);
			return true;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to enter value " + dt + " in " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public String getCssValue(String locator, String property)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			String css = (element != null) ? element.getCssValue(property) : null;
			ExtentManager.infoTest("Get CSS property " + property + " from " + SeleniumLocatorUtil.logName.get() + " -> " + css);
			return css;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to get CSS property " + property + " from " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public boolean isDisplayed(String locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			boolean displayed = element != null && element.isDisplayed();
			ExtentManager.infoTest("Element " + SeleniumLocatorUtil.logName.get() + " isDisplayed -> " + displayed);
			return displayed;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isDisplayed for " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean isEnabled(String locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			boolean enabled = element != null && element.isEnabled();
			ExtentManager.infoTest("Element " + SeleniumLocatorUtil.logName.get() + " isEnabled -> " + enabled);
			return enabled;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isEnabled for " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean isSelected(String locator)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			boolean selected = element != null && element.isSelected();
			ExtentManager.infoTest("Element " + SeleniumLocatorUtil.logName.get() + " isSelected -> " + selected);
			return selected;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to check isSelected for " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return false;
		}
	}

	public boolean enterValue(Object locator, String dt)
	{
		try
		{
			WebElement element = waitForVisible(getElement(locator), 60);
			element.clear();
			String attributeStyle = getAttributeStyle(element);
			highlightElement(element);
			element.sendKeys(dt);
			removeHighlight(element,attributeStyle);
			String attribute = element.getAttribute("value");
			boolean entered = attribute.equals(dt);
			ExtentManager.infoTest("Enter value <b>'" + dt + "'</b> in " + SeleniumLocatorUtil.logName.get());
			return entered;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to enter value " + dt + " in " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
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
			ExtentManager.infoTest("Element " + SeleniumLocatorUtil.logName.get() + " is present");
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
			ExtentManager.infoTest("Found element " + SeleniumLocatorUtil.logName.get() + "");
			return element;
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to find element " + SeleniumLocatorUtil.logName.get() + " : " + e.getMessage());
			return null;
		}
	}

	public List<WebElement> findElements(Object pr)
	{
		try
		{
			List<WebElement> elements = getElements(pr);
			return elements;
		} catch (Exception e)
		{
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

	// This method is used to find the goal ratio
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

	public String decodeBase64ToText(String base64Text)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
		return new String(decodedBytes);
	}
	
	public String getDescription()
	{
		ITestResult result = Reporter.getCurrentTestResult();
		return result.getMethod().getDescription();
	}
	
	public List<Map<String, String>> getWebTable(String tableValue)
	{
		List<Map<String, String>> list = new LinkedList<>();
		WebElement table = findElement(tableValue);
		List<WebElement> headers = table.findElements(By.tagName("th"));
		List<WebElement> rows = driver.findElements(autolocator(tableValue + "//tbody//tr"));
		for (WebElement row : rows)
		{
			Map<String, String> map = new LinkedHashMap<>();
			List<WebElement> datas = row.findElements(By.tagName("td"));
			int headerSize = headers.size();
			for (int i = 0; i < headerSize; i++)
			{

				String head = headers.get(i).getText();
				String dataValue = datas.get(i).getText();
				map.put(head, dataValue);
			}
			list.add(map);
		}
		ExtentManager.infoTest(list.toString());
		ExtentManager.customReport(list);
		return list;
	}
	
	public static boolean isValidURL(String urlStr)
	{
		try
		{
			new URL(urlStr);
			return true;
		} catch (MalformedURLException e)
		{
			return false;
		}
	}
	
	public String normalizeText(String input)
	{
		if (input == null)
		{
			return "";
		}
		return input.replace("\u00A0", " ") // Replace non-breaking spaces
				.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "") // Remove invisible characters
				.replaceAll("\\s+", " ") // Collapse multiple spaces
				.trim(); // Final trim
	}
	
	public boolean validateInvalidLinks(String pageName) {
		try {
			int invalidLinksCount = 0;
			int validLinksCount = 0;
			int totalLinks = 0;
			int linksWithJavascriptOrNull = 0;

			List<WebElement> anchorTagsList = driver.findElements(By.tagName("a"));
			ExtentManager.infoTest("Total <a> tags found on <b>" + pageName + "</b> page: " + anchorTagsList.size());

			for (WebElement anchorTagElement : anchorTagsList) {
				if (anchorTagElement == null) {
					continue;
				}

				String url = anchorTagElement.getAttribute("href");
				if (url != null && !url.contains("javascript") && !url.trim().isEmpty() && !url.equals("\"\"")) {
					totalLinks++;
					verifyURLStatus(url);
				} else {
					linksWithJavascriptOrNull++;
				}
			}

			ExtentManager.infoTest(
					"Page: <b>" + pageName + "</b> - Links with null/javascript: " + linksWithJavascriptOrNull);
			ExtentManager.infoTest("Page: <b>" + pageName + "</b> - Valid links: " + validLinksCount);
			ExtentManager.infoTest("Page: <b>" + pageName + "</b> - Invalid links: " + invalidLinksCount);
			ExtentManager
					.infoTest("Page: <b>" + pageName + "</b> - Total processed <a> tags: " + anchorTagsList.size());

			// if there are invalid links, log warning
			if (invalidLinksCount > 0) {
				ExtentManager.warningTest(
						"Found " + invalidLinksCount + " invalid links on the <b>" + pageName + "</b> page.");
				return false;
			}

			return true; // success case
		} catch (Exception e) {
			ExtentManager
					.failTest("Exception in validateInvalidLinks for page <b>" + pageName + "</b>: " + e.getMessage());
			TestLogManager.error("Exception in validateInvalidLinks for page " + pageName + ": " + e.getMessage(), e);
			return false;
		}
	}
	
	private boolean verifyURLStatus(String url) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);

		try {
			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != 200) {
				invalidLinksCount++;
				ExtentManager.warningTest("Link is not working / dormant: " + url);
				ExtentManager.infoTest("Request URL: " + request);
				ExtentManager.infoTest("Response status: " + response.getStatusLine());
				return false;
			} else {
				validLinksCount++;
				ExtentManager.infoTest("Valid link: " + url + " (Status: " + statusCode + ")");
				return true;
			}
		} catch (Exception e) {
			invalidLinksCount++;
			ExtentManager.failTest("Exception verifying URL: " + url + " | " + e.getMessage());
			TestLogManager.error("Exception in verifyURLStatus: " + e.getMessage(), e);
			return false;
		}
	}
	
	@Override
	public boolean uploadFile(String locator, String filePath)
	{
		try
		{
			driver.findElement(autolocator(locator)).sendKeys(filePath);
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}
	
}
