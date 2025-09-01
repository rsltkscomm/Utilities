package base;

import io.github.bonigarcia.wdm.WebDriverManager;
import reporting.TestLogManager;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DriverManager
{

	private static ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

	public static WebDriver createDriver(String browser)
	{
		browser = browser.toLowerCase();
		WebDriver driver = null;
		
		try
		{
			switch (browser)
			{
			case "firefox":
				FirefoxOptions ffOptions = createFirefoxOptions();
				driver = GridManager.initializeRemoteDriverIfGrid(ffOptions) ? new RemoteWebDriver(new URL(GridManager.getRemoteWebDriverURL()), ffOptions) : new FirefoxDriver(ffOptions);
				break;

			case "edge":
				EdgeOptions edgeOptions = createEdgeOptions();
				driver = GridManager.initializeRemoteDriverIfGrid(edgeOptions) ? new RemoteWebDriver(new URL(GridManager.getRemoteWebDriverURL()), edgeOptions) : new EdgeDriver(edgeOptions);
				break;

			case "chromeheadless":
				ChromeOptions chromeHeadlessOptions = createChromeOptions(true);
				driver = GridManager.initializeRemoteDriverIfGrid(chromeHeadlessOptions) ? new RemoteWebDriver(new URL(GridManager.getRemoteWebDriverURL()), chromeHeadlessOptions) : new ChromeDriver(chromeHeadlessOptions);
				break;

			default:
				ChromeOptions chromeOptions = createChromeOptions(false);
				driver = GridManager.initializeRemoteDriverIfGrid(chromeOptions) ? new RemoteWebDriver(new URL(GridManager.getRemoteWebDriverURL()), chromeOptions) : new ChromeDriver(chromeOptions);
			}

			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			driver.manage().window().maximize();
			driverThread.set(driver);
		} catch (Exception e)
		{
			TestLogManager.info("Driver initialized");
		}

		
		return driver;
	}

	private static ChromeOptions createChromeOptions(boolean headless)
	{
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
		setCommonOptions(options);
		if (headless)
			options.addArguments("--headless=new");
		return options;
	}

	private static FirefoxOptions createFirefoxOptions()
	{
		WebDriverManager.firefoxdriver().setup();
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("--disable-notifications");
		options.addArguments("--width=1920", "--height=1080");
		options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
		options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
		return options;
	}

	private static EdgeOptions createEdgeOptions()
	{
		WebDriverManager.edgedriver().setup();
		EdgeOptions options = new EdgeOptions();
		setCommonOptions(options);
		return options;
	}

	private static void setCommonOptions(Object options)
	{
		Map<String, Object> prefs = new HashMap<>();
		String downloadPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "data", "downloadedFile").toAbsolutePath().toString();
		prefs.put("download.default_directory", downloadPath);
		prefs.put("download.prompt_for_download", false);
		prefs.put("profile.default_content_settings.popups", 0);
		prefs.put("credentials_enable_service", false);
		prefs.put("profile.password_manager_enabled", false);

		if (options instanceof ChromeOptions)
		{
			ChromeOptions chrome = (ChromeOptions) options;
			chrome.addArguments("--disable-notifications", "--no-sandbox", "--disable-gpu", "--incognito");
			chrome.setExperimentalOption("prefs", prefs);
			chrome.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
			chrome.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
		} else if (options instanceof EdgeOptions)
		{
			EdgeOptions edge = (EdgeOptions) options;
			edge.addArguments("--disable-notifications", "--no-sandbox", "--inprivate");
			edge.setExperimentalOption("prefs", prefs);
			edge.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
		}
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
