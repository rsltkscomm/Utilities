package core.selenium;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import core.interfaces.ScrollInterface;
import reporting.TestLogManager;

public class SeleniumScrollUtil extends SeleniumBrowserUtil implements ScrollInterface {

    WebDriver driver;

    public SeleniumScrollUtil(WebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    private WebElement resolve(Object pr) {
        if (pr instanceof WebElement) return (WebElement) pr;
        if (pr instanceof String) return driver.findElement(autolocator(pr.toString()));
        return null;
    }

    @Override
    public boolean scrollToElement(Object pr) {
        try {
            WebElement el = resolve(pr);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public void javaScriptScrollIntoView(Object pr) {
        try {
            waitForVisible(pr, 50);
            WebElement el = resolve(pr);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", el);
            wait(1);
        } catch (NoSuchElementException e) {
            TestLogManager.error("Exception occurred", e);
        }
    }

    @Override
    public boolean scrollBy(int x, int y) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.scrollBy(arguments[0], arguments[1]);", x, y);
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public boolean scrollToTop() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public boolean scrollToBottom() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.scrollTo(0, document.body.scrollHeight);");
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public boolean scrollByElementOffset(Object pr, int xOffset, int yOffset) {
        try {
            WebElement el = resolve(pr);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollBy(arguments[1],arguments[2]);",
                    el, xOffset, yOffset);
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public void waitForScroll() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            int retries = 0;
            while (retries < 10) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(1000);

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) break;

                lastHeight = newHeight;
                retries++;
            }
        } catch (Exception e) {}
    }

    @Override
    public void scrollStep(int pixels) {
        ((JavascriptExecutor) driver)
                .executeScript("window.scrollBy(0," + pixels + ")");
    }
}
