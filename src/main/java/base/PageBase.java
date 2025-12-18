package base;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.WebElement;

import core.interfaces.BrowserInterface;
import core.interfaces.ClickInterface;
import core.interfaces.DateInterface;
import core.interfaces.DragAndDropInterface;
import core.interfaces.DropdownInterface;
import core.interfaces.ElementInterface;
import core.interfaces.EngineType;
import core.interfaces.FrameInterface;
import core.interfaces.KeyboardInterface;
import core.interfaces.MouseHoverInterface;
import core.interfaces.ScreenshotInterface;
import core.interfaces.ScrollInterface;
import core.interfaces.SelectInterface;
import core.interfaces.WaitInterface;
import core.interfaces.WindowInterface;
import reporting.ExtentManager;

public class PageBase
{

	protected final AutomationContext context;
	protected final EngineActions actions;

	protected PageBase(AutomationContext context) {
		this.context = context;
		DriverContext driverContext = DriverManager.getContext();
		this.actions = driverContext != null ? driverContext.getEngineActions() : null;
	}

	protected EngineActions getActions()
	{
		if (actions == null)
		{
			throw new IllegalStateException("EngineActions not initialised for current thread");
		}
		return actions;
	}

	public AutomationContext getContext()
	{
		return context;
	}

	public EngineType getEngineType()
	{
		return getActions().getEngineType();
	}

	public String getEnvironment()
	{
		return context.getEnvironment();
	}

	public String detectFilePath(String path)
	{
		return context.detectFilePath(path);
	}

	public void getDeviceSpecs()
	{
		try
		{
			Map<String, String> deviceInfo = context.getDeviceSpecs();

			ExtentManager.infoLabel("<b> DEVICE SPECIFICATIONS </b>");
			ExtentManager.getTest().assignDevice(deviceInfo.get("Platform / OS")).assignCategory(deviceInfo.get("Browser"));

			ExtentManager.customReport(deviceInfo);

		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to capture device specs: " + e.getMessage());
		}
	}

	public static String getNormalizedPath(String path)
	{
		return path.replace("/", File.separator).replace("\\", File.separator).trim();
	}

	public void ensureScreenshotFolderExists()
	{
		try
		{
			String screenshotDir = System.getProperty("user.dir") + "/src/test/resources/ExtentReports/ScreenShots/";

			File dir = new File(context.getNormalizedPath(screenshotDir));
			if (!dir.exists())
			{
				dir.mkdirs();
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("Failed to create screenshot folder: " + e.getMessage());
		}
	}

	/* ========= Delegates to engine-specific utilities ========= */

	protected ClickInterface click()
	{
		return getActions().click();
	}

	protected ElementInterface element()
	{
		return getActions().elements();
	}

	protected WaitInterface waitFor()
	{
		return getActions().waitFor();
	}

	protected WindowInterface window()
	{
		return getActions().window();
	}

	protected ScreenshotInterface screenshot()
	{
		return getActions().screenshot();
	}

	protected ScrollInterface scroll()
	{
		return getActions().scroll();
	}

	protected DropdownInterface dropdown()
	{
		return getActions().dropdown();
	}

	protected SelectInterface select()
	{
		return getActions().select();
	}

	protected FrameInterface frame()
	{
		return getActions().frames();
	}

	protected KeyboardInterface keyboard()
	{
		return getActions().keyboard();
	}

	protected MouseHoverInterface hover()
	{
		return getActions().mouseHover();
	}

	protected DragAndDropInterface dragAndDrop()
	{
		return getActions().dragAndDrop();
	}

	protected DateInterface dates()
	{
		return getActions().dates();
	}

	protected BrowserInterface browser()
	{
		return getActions().browser();
	}

	/* ========= Legacy compatibility helpers ========= */

	// Mouse hover
	public boolean mouseHover(Object pr)
	{
		return hover().mouseHover(pr);
	}

	// Text helpers
	public String getText(Object locator)
	{
		return element().getText(locator);
	}

	// Screenshots
	public void takeScreenshot()
	{
		screenshot().takeScreenshot();
	}

	// Wait helpers
	public WebElement waitForClickable(Object locator, int sec)
	{
		return waitFor().waitForClickable(locator, sec);
	}

	public WebElement waitForVisible(Object locator, int sec)
	{
		return waitFor().waitForVisible(locator, sec);
	}

	public boolean waitForInvisibility(Object locator, int sec)
	{
		return waitFor().waitForInvisibility(locator, sec);
	}

	public void turnOffImplicityWait()
	{
		waitFor().turnOffImplicityWait();
	}

	public void turnOnImplicityWait()
	{
		waitFor().turnOnImplicityWait();
	}

	public void wait_Milli_Seconds(int milliSeconds)
	{
		waitFor().wait_Milli_Seconds(milliSeconds);
	}

	public void wait(int seconds)
	{
		waitFor().wait(seconds);
	}

	// Dropdown helpers
	public boolean selectListElements(String elementsPath, String input)
	{
		return dropdown().selectListElements(elementsPath, input);
	}

	public boolean selectExactListElements(String elementsPath, String input)
	{
		return dropdown().selectExactListElements(elementsPath, input);
	}

	// Locator helpers
	public WebElement getElement(Object locator)
	{
		return getActions().locator().getElement(locator);
	}

	public List<?> findElements(Object locator)
	{
		return getActions().locator().getElements(locator);
	}

	public String replacePlaceHolder(String locator, String placeHolder)
	{
		return getActions().locator().replacePlaceHolder(locator, placeHolder);
	}

	public String replacePlaceHolder(String locator, int placeHolder)
	{
		return getActions().locator().replacePlaceHolder(locator, placeHolder);
	}

	public String replacePlaceHolder(String locator, String placeHolder, String placeHolder1)
	{
		return getActions().locator().replacePlaceHolder(locator, placeHolder, placeHolder1);
	}

	// Element helpers
	public boolean isElementPresent(String locator)
	{
		return element().isElementPresent(locator);
	}

	public boolean enterValue(Object locator, String value)
	{
		return element().enterValue(locator, value);
	}

	// Scroll helpers
	public boolean scrollToBottom()
	{
		return scroll().scrollToBottom();
	}

	public void javaScriptScrollIntoView(Object pr)
	{
		scroll().javaScriptScrollIntoView(pr);
	}

	// Browser helpers
	public void openUrl(String url)
	{
		browser().openUrl(url);
	}

	public void navigateTo(String url)
	{
		browser().navigateTo(url);
	}

	public String getCurrentUrl()
	{
		return browser().getCurrentUrl();
	}

	public void refresh()
	{
		browser().refresh();
	}

	// Window helpers
	public String getCurrentWindowHandle()
	{
		return window().getCurrentWindowHandle();
	}

	public boolean openNewTab()
	{
		return window().openNewTab();
	}

	public void switchWindow()
	{
		window().switchWindow();
	}

	public boolean switchToWindow(String handle)
	{
		return window().switchToWindow(handle);
	}

	public boolean switchToWindow(int index)
	{
		return window().switchToWindow(index);
	}

	public boolean switchToParentWindow()
	{
		return window().switchToParentWindow();
	}

	public boolean closeAllOtherWindows()
	{
		return window().closeAllOtherWindows();
	}

	public boolean closeCurrentWindow()
	{
		browser().closeWindow();
		return true;
	}

	// Date helpers
	public String addTimeToShort()
	{
		return dates().addTimeToShort();
	}

	// Assert helpers
	public boolean checkIsElementNull(Object element)
	{
		return getActions().asserts().checkIsElementNull(element);
	}

	public boolean writeLog(boolean expression, String passLog, String failLog)
	{
		return getActions().asserts().writeLog(expression, passLog, failLog);
	}

	public boolean writeLogger(boolean expression, String passLog, String failLog)
	{
		return getActions().asserts().writeLogger(expression, passLog, failLog);
	}

	public boolean placeholderValueCheck(String locator, String placeHolderText)
	{
		return getActions().asserts().placeholderValueCheck(locator, placeHolderText);
	}

	public boolean validateUiBackgroundColour(String cssProperty, String locator, String expectedHex)
	{
		return getActions().asserts().validateUiBackgroundColour(cssProperty, locator, expectedHex);
	}

	public boolean uiPageEqualsWithMultipleInputValue(String locator, String testDatas)
	{
		return getActions().asserts().uiPageEqualsWithMultipleInputValue(locator, testDatas);
	}

	public boolean uiPageEqualswithInputValue(String txt, String actualText)
	{
		return getActions().asserts().uiPageEqualswithInputValue(txt, actualText);
	}

	public String decodeBase64ToText(String base64Text)
	{
		return getActions().asserts().decodeBase64ToText(base64Text);
	}

	// Locator helpers (additional)
	public org.openqa.selenium.By autolocator(String key)
	{
		return getActions().locator().autolocator(key);
	}

	// Click helpers
	public boolean clickElement(Object pr)
	{
		return click().clickElement(pr);
	}

	public boolean safeClick(Object pr)
	{
		return click().safeClick(pr);
	}

	public boolean jsClick(Object pr)
	{
		return click().jsClick(pr);
	}

	public boolean doubleClick(Object pr)
	{
		return click().doubleClick(pr);
	}

	public boolean actionsClickElement(String locator)
	{
		return click().actionsClickElement(locator);
	}

	public String getAttribute(Object locator, String attribute)
	{
		return element().getAttribute(locator, attribute);
	}

	public boolean isDisplayed(String locator)
	{
		return element().isDisplayed(locator);
	}

	public boolean isEnabled(String locator)
	{
		return element().isEnabled(locator);
	}

	public boolean isSelected(String locator)
	{
		return element().isSelected(locator);
	}

	public boolean sendValue(Object locator, String value)
	{
		return element().sendValue(locator, value);
	}

	public boolean sendKeys(Object locator, String value)
	{
		return element().sendValue(locator, value);
	}

	public void tabAction()
	{
		element().tabAction();
	}

	public void clickEnter()
	{
		element().clickEnter();
	}

	public void enterAction()
	{
		element().clickEnter();
	}

	public void clearField(Object locator)
	{
		element().clearField(locator);
	}

	public String getCssValue(String locator, String property)
	{
		return element().getCssValue(locator, property);
	}

	public String getTextBoxValue(Object locator, String attribute)
	{
		return element().getTextBoxValue(locator, attribute);
	}

	public boolean javaScriptEnterValue(Object locator, String text)
	{
		return element().javaScriptEnterValue(locator, text);
	}

	public String getAllDropdownValues(String locator)
	{
		return element().getAllDropdownValues(locator);
	}

	public java.util.List<java.util.Map<String, String>> getWebTable(String tableLocator)
	{
		return element().getWebTable(tableLocator);
	}

	// Additional Wait helpers
	public org.openqa.selenium.WebElement waitForPresence(Object pr, int sec)
	{
		return waitFor().waitForPresence(pr, sec);
	}

	public boolean explicitWaitTextToBePresent(String text, Object pr, int sec)
	{
		return waitFor().explicitWaitTextToBePresent(text, pr, sec);
	}

	public boolean waitForText(Object pr, String text, int sec)
	{
		return waitFor().waitForText(pr, text, sec);
	}

	public boolean waitForTitle(String title, int sec)
	{
		return waitFor().waitForTitle(title, sec);
	}

	public boolean waitForTitleContains(String partialTitle, int sec)
	{
		return waitFor().waitForTitleContains(partialTitle, sec);
	}

	public boolean waitForUrl(String url, int sec)
	{
		return waitFor().waitForUrl(url, sec);
	}

	public boolean waitForUrlContains(String partialUrl, int sec)
	{
		return waitFor().waitForUrlContains(partialUrl, sec);
	}

	public org.openqa.selenium.Alert waitForAlert(int sec)
	{
		return waitFor().waitForAlert(sec);
	}

	public boolean waitForStaleness(org.openqa.selenium.WebElement element, int sec)
	{
		return waitFor().waitForStaleness(element, sec);
	}

	public boolean waitForFrame(Object pr, int sec)
	{
		return waitFor().waitForFrame(pr, sec);
	}

	public org.openqa.selenium.WebElement fluentWait(Object pr, int timeoutSec, int pollingSec)
	{
		return waitFor().fluentWait(pr, timeoutSec, pollingSec);
	}

	public boolean waitForPageLoad(int sec)
	{
		return waitFor().waitForPageLoad(sec);
	}

	public boolean waitForJQueryLoad(int sec)
	{
		return waitFor().waitForJQueryLoad(sec);
	}

	public boolean waitForJSReady(int sec)
	{
		return waitFor().waitForJSReady(sec);
	}

	public void setImplicitWait(int sec)
	{
		waitFor().setImplicitWait(sec);
	}

	public void waitHalfSecond()
	{
		waitFor().wait_Milli_Seconds(500);
	}

	// Additional Scroll helpers
	public boolean scrollToElement(Object pr)
	{
		return scroll().scrollToElement(pr);
	}

	public boolean scrollBy(int x, int y)
	{
		return scroll().scrollBy(x, y);
	}

	public boolean scrollToTop()
	{
		return scroll().scrollToTop();
	}

	public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset)
	{
		return scroll().scrollByElementOffset(pr, xOffset, yOffset);
	}

	public void waitForScroll()
	{
		scroll().waitForScroll();
	}

	public void scrollStep(int pixels)
	{
		scroll().scrollStep(pixels);
	}

	// Highlight helpers (for debugging/testing)
	public void highlightElement(org.openqa.selenium.WebElement element)
	{
		try
		{
			org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) DriverManager.getDriver();
			js.executeScript("arguments[0].setAttribute('style','border:2px solid red; background:yellow;')", element);
		} catch (Exception e)
		{
			// Silently fail if highlighting doesn't work
		}
	}

	public void removeHighlight(org.openqa.selenium.WebElement element, String originalStyle)
	{
		try
		{
			org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) DriverManager.getDriver();
			js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, originalStyle);
		} catch (Exception e)
		{
			// Silently fail if removing highlight doesn't work
		}
	}

	// Static convenience to maintain legacy usage
	public static String getLoginURL()
	{
		AutomationContext ctx = DriverManager.getAutomationContext();
		if (ctx != null)
		{
			return ctx.getLoginURL();
		}
		String key = System.getProperty("Environment") != null ? System.getProperty("Environment").toUpperCase() + "_" + System.getProperty("ReleaseVersion") : null;
		return key != null ? System.getProperty(key) : null;
	}
	
	public String currentDateAndTime(String format)
	{
		return dates().currentDateAndTime(format);
	}
	
	public boolean childWindowCloseIndex(int index)
	{
		return window().childWindowCloseIndex(index);
	}
	
	public boolean switchWindowByIndex(int index)
	{
		return window().switchToWindow(index);
	}
	
	public boolean selectListElementByIndex(String locator,int index)
	{
		return dropdown().selectListElementByIndex(locator , index);
	}
	
	public String addTimeToName()
	{
		return dates().addTimeToName();
	}
}
