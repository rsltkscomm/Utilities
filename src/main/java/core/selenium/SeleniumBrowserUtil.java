package core.selenium;

import org.openqa.selenium.WebDriver;

import core.interfaces.BrowserInterface;
import reporting.ExtentManager;

public class SeleniumBrowserUtil extends SeleniumScreenshotUtil implements BrowserInterface {

    private final WebDriver driver;

    public SeleniumBrowserUtil(WebDriver driver) {
    	super(driver);
        this.driver = driver;
    }

    @Override
    public void openUrl(String url) {
        driver.get(url);
        ExtentManager.infoTest("Url launched: " + url);
    }

    @Override
    public void navigateTo(String url) {
        driver.navigate().to(url);
        ExtentManager.infoTest("Navigated to: " + url);
    }

    @Override
    public void back() {
        driver.navigate().back();
        ExtentManager.infoTest("Navigated Back");
    }

    @Override
    public void forward() {
        driver.navigate().forward();
        ExtentManager.infoTest("Navigated Forward");
    }

    @Override
    public void refresh() {
        driver.navigate().refresh();
        ExtentManager.infoTest("Page Refreshed");
    }

    @Override
    public void maximizeWindow() {
        driver.manage().window().maximize();
        ExtentManager.infoTest("Window Maximized");
    }

    @Override
    public void closeWindow() {
        driver.close();
        ExtentManager.infoTest("Closed current window");
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public String getPageSource() {
        return driver.getPageSource();
    }

    @Override
    public void deleteAllCookies() {
        driver.manage().deleteAllCookies();
    }
}
