package seleniumUtils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import pages.PageFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Utility class for handling all types of Selenium waits: - Implicit Wait - Explicit Wait (WebDriverWait + ExpectedConditions) - Fluent Wait - Custom
 * Page Load Waits
 */
public class WaitUtil extends LocatorUtil
{

	protected WebDriver driver;

	public WaitUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	// ---------------------------------------------------------
    // 🔹 IMPLICIT WAIT
    // ---------------------------------------------------------
    public void setImplicitWait(int sec) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(sec));
        System.out.println("Implicit wait set to " + sec + " seconds");
    }

    // ---------------------------------------------------------
    // 🔹 EXPLICIT WAITS
    // ---------------------------------------------------------

    public WebElement waitForClickable(String locator, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.elementToBeClickable(LocatorUtil.autolocator(locator)));
    }

    public WebElement waitForVisible(String locator, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.visibilityOfElementLocated(LocatorUtil.autolocator(locator)));
    }

    public WebElement waitForPresence(String locator, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.presenceOfElementLocated(LocatorUtil.autolocator(locator)));
    }

    public boolean waitForInvisibility(String locator, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.invisibilityOfElementLocated(LocatorUtil.autolocator(locator)));
    }

    public boolean waitForText(String locator, String text, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.textToBePresentInElementLocated(LocatorUtil.autolocator(locator), text));
    }

    public boolean waitForTitle(String title, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.titleIs(title));
    }

    public boolean waitForTitleContains(String partialTitle, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.titleContains(partialTitle));
    }

    public boolean waitForUrl(String url, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.urlToBe(url));
    }

    public boolean waitForUrlContains(String partialUrl, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.urlContains(partialUrl));
    }

    public Alert waitForAlert(int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.alertIsPresent());
    }

    public boolean waitForStaleness(WebElement element, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.stalenessOf(element));
    }

    public boolean waitForFrame(String locator, int sec) {
        new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(LocatorUtil.autolocator(locator)));
        return true;
    }

    // ---------------------------------------------------------
    // 🔹 FLUENT WAIT
    // ---------------------------------------------------------
    public WebElement fluentWait(String locator, int timeoutSec, int pollingSec) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSec))
                .pollingEvery(Duration.ofSeconds(pollingSec))
                .ignoreAll(Arrays.asList(
                        NoSuchElementException.class,
                        StaleElementReferenceException.class,
                        ElementClickInterceptedException.class
                ));

        return wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver driver) {
                WebElement element = driver.findElement(LocatorUtil.autolocator(locator));
                return element.isDisplayed() ? element : null;
            }
        });
    }

    // ---------------------------------------------------------
    // 🔹 CUSTOM WAITS
    // ---------------------------------------------------------
    public boolean waitForPageLoad(int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(webDriver -> ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState").equals("complete"));
    }

    public boolean waitForJQueryLoad(int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(webDriver -> (Boolean) ((JavascriptExecutor) webDriver)
                        .executeScript("return !!window.jQuery && jQuery.active == 0"));
    }

    public boolean waitForJSReady(int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(webDriver -> ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState").toString().equals("complete"));
    }
    
    public void turnOnImplicityWait()
	{
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
	}

	public void turnOffImplicityWait()
	{
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
	}

}
