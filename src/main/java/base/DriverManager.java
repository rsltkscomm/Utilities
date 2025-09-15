package base;

import patterns.strategy.DriverFactory;
import reporting.TestLogManager;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.time.Duration;

public class DriverManager
{

	private static ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

	public static WebDriver createDriver(String browser)
	{
		browser = browser.toLowerCase();
		WebDriver driver = null;
		
		try
		{
			// Use the new Strategy pattern for driver creation
			driver = DriverFactory.createDriver(browser);
			
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			driver.manage().window().maximize();
			driverThread.set(driver);
			
			TestLogManager.info("Driver created successfully: " + browser);
		} catch (Exception e)
		{
			TestLogManager.error("Failed to create driver: " + browser, e);
			throw new RuntimeException("Failed to create driver: " + browser, e);
		}

		return driver;
	}

	/**
	 * Creates a driver with custom capabilities.
	 * @param browser The browser type
	 * @param capabilities Custom capabilities
	 * @return WebDriver instance
	 */
	public static WebDriver createDriver(String browser, DesiredCapabilities capabilities)
	{
		browser = browser.toLowerCase();
		WebDriver driver = null;
		
		try
		{
			// Use the new Strategy pattern for driver creation with custom capabilities
			driver = DriverFactory.createDriver(browser, capabilities);
			
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			driver.manage().window().maximize();
			driverThread.set(driver);
			
			TestLogManager.info("Driver created successfully with custom capabilities: " + browser);
		} catch (Exception e)
		{
			TestLogManager.error("Failed to create driver with custom capabilities: " + browser, e);
			throw new RuntimeException("Failed to create driver with custom capabilities: " + browser, e);
		}

		return driver;
	}

	public static WebDriver getDriver()
	{
		return driverThread.get();
	}
	
	public static ThreadLocal<WebDriver> getDriverThread()
	{
		return driverThread;
	}

	public static void quitDriver()
	{
		if (driverThread.get() != null)
		{
			driverThread.get().quit();
			driverThread.remove();
		}
	}
	
	
}
