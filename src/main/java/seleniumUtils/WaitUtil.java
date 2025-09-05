package seleniumUtils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import pages.PageFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Utility class for handling all types of Selenium waits.
 * Supports both String locators and WebElement objects.
 */
public class WaitUtil extends LocatorUtil {

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

    public WebElement waitForClickable(Object pr, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.elementToBeClickable(getElement(pr)));
    }

    public WebElement waitForVisible(Object pr, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
                .until(ExpectedConditions.visibilityOf(getElement(pr)));
    }

    public WebElement waitForPresence(Object pr, int sec) {
        if (pr instanceof String) {
            return new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.presenceOfElementLocated(autolocator(pr.toString())));
        } else {
            return new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.visibilityOf((WebElement) pr));
        }
    }

    public boolean waitForInvisibility(Object pr, int sec) {
        if (pr instanceof String) {
            return new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.invisibilityOfElementLocated(autolocator(pr.toString())));
        } else {
            return new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.invisibilityOf((WebElement) pr));
        }
    }

    public boolean waitForText(Object pr, String text, int sec) {
        if (pr instanceof String) {
            return new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.textToBePresentInElementLocated(autolocator(pr.toString()), text));
        } else {
            return new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.textToBePresentInElement((WebElement) pr, text));
        }
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

    public boolean waitForFrame(Object pr, int sec) {
        if (pr instanceof String) {
            new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(autolocator(pr.toString())));
        } else {
            new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt((WebElement) pr));
        }
        return true;
    }

    // ---------------------------------------------------------
    // 🔹 FLUENT WAIT
    // ---------------------------------------------------------
    public WebElement fluentWait(Object pr, int timeoutSec, int pollingSec) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSec))
                .pollingEvery(Duration.ofSeconds(pollingSec))
                .ignoreAll(Arrays.asList(
                        NoSuchElementException.class,
                        StaleElementReferenceException.class,
                        ElementClickInterceptedException.class
                ));

        return wait.until(d -> {
            WebElement element = getElement(pr);
            return (element != null && element.isDisplayed()) ? element : null;
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

    public void turnOnImplicityWait() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
    }

    public void turnOffImplicityWait() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
    }

    public void wait(int seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(d -> true);
    }

    // ---------------------------------------------------------
    // 🔹 PRIVATE HELPERS
    // ---------------------------------------------------------
    private WebElement getElement(Object pr) {
        return (pr instanceof String)
                ? driver.findElement(autolocator(pr.toString()))
                : (WebElement) pr;
    }
}
