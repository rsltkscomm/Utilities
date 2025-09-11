package seleniumUtils;

import java.util.Set;

import org.openqa.selenium.WebDriver;

import pages.PageFactory;
import reporting.ExtentManager;

public class BrowserUtil extends ScreenshotUtil
{

	private WebDriver driver;

	public BrowserUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	// Open a URL using driver.get()
	public void openUrl(String url)
	{
		driver.get(url);
		ExtentManager.infoTest("Url launched : "+url);
	}

	// Navigate to a URL
	public void navigateTo(String url)
	{
		driver.navigate().to(url);
		ExtentManager.infoTest("Navigated to: "+url);
	}

	// Browser navigation
	public void back()
	{
		driver.navigate().back();
		ExtentManager.infoTest("Navigated Back");
	}

	public void forward()
	{
		driver.navigate().forward();
		ExtentManager.infoTest("Navigated Forward");

	}

	public void refresh()
	{
		driver.navigate().refresh();
		ExtentManager.infoTest("Page Refreshed");

	}

	// Window operations
	public void maximizeWindow()
	{
		driver.manage().window().maximize();
		ExtentManager.infoTest("Window Maximized");

	}

	public void closeWindow()
	{
		driver.close();
		ExtentManager.infoTest("Closed current window");
	}

	// Info getters
	public String getCurrentUrl()
	{
		return driver.getCurrentUrl();
	}

	public String getTitle()
	{
		return driver.getTitle();
	}

	public String getPageSource()
	{
		return driver.getPageSource();
	}

}
