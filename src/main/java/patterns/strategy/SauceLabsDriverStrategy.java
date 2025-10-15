package patterns.strategy;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import reporting.TestLogManager;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver strategy for running tests on Sauce Labs Selenium Grid.
 * Usage: set system properties, then select browser "saucelabs" or "sauce".
 * Required: SAUCE_USERNAME, SAUCE_ACCESS_KEY
 */
public class SauceLabsDriverStrategy implements DriverStrategy
{
	@Override
	public WebDriver createDriver()
	{
		return createDriver(null);
	}

	@Override
	public WebDriver createDriver(DesiredCapabilities capabilities)
	{
		try
		{
			String username = System.getProperty("SAUCE_USERNAME", System.getProperty("sauce.username"));
			String accessKey = System.getProperty("SAUCE_ACCESS_KEY", System.getProperty("sauce.accessKey"));

			if (isNullOrEmpty(username) || isNullOrEmpty(accessKey))
			{
				throw new IllegalArgumentException("Sauce Labs credentials not provided. Set SAUCE_USERNAME and SAUCE_ACCESS_KEY");
			}

			String gridUrl = System.getProperty("SAUCE_GRID_URL", System.getProperty("sauce.gridUrl"));
			if (isNullOrEmpty(gridUrl))
			{
				gridUrl = "https://" + username + ":" + accessKey + "@ondemand.saucelabs.com:443/wd/hub";
			}
			URL remoteUrl = new URL(gridUrl);

			String browserName = System.getProperty("SAUCE_BROWSER", System.getProperty("browserName", "chrome"));
			String browserVersion = System.getProperty("SAUCE_BROWSER_VERSION", System.getProperty("browserVersion", "latest"));
			String platformName = System.getProperty("SAUCE_PLATFORM", System.getProperty("platformName", "Windows 11"));

			MutableCapabilities options = buildOptions(browserName, capabilities);

			Map<String, Object> sauceOptions = new HashMap<>();
			sauceOptions.put("username", username);
			sauceOptions.put("accessKey", accessKey);
			sauceOptions.put("build", System.getProperty("SAUCE_BUILD", "Resulticks-Automation-Build"));
			sauceOptions.put("name", System.getProperty("SAUCE_NAME", "SauceLabs-Test"));
			sauceOptions.put("seleniumVersion", System.getProperty("SAUCE_SELENIUM_VERSION", "4.22.0"));
			putIfPresent(sauceOptions, "screenResolution", System.getProperty("SAUCE_RESOLUTION", "1920x1080"));
			putIfPresent(sauceOptions, "recordVideo", System.getProperty("SAUCE_VIDEO", "true"));
			putIfPresent(sauceOptions, "recordLogs", System.getProperty("SAUCE_CONSOLE", "true"));
			putIfPresent(sauceOptions, "recordScreenshots", System.getProperty("SAUCE_VISUAL", "true"));
			putIfPresent(sauceOptions, "timezone", System.getProperty("SAUCE_TIMEZONE", "Asia/Kolkata"));
			putIfPresent(sauceOptions, "buildTag", System.getProperty("SAUCE_BUILD_TAG", "CI-Build"));

			// Attach Sauce Labs options
			if (options instanceof ChromeOptions chrom)
			{
				chrom.setBrowserVersion(browserVersion);
				chrom.setPlatformName(platformName);
				chrom.setCapability("sauce:options", sauceOptions);
			}
			else if (options instanceof FirefoxOptions fox)
			{
				fox.setBrowserVersion(browserVersion);
				fox.setPlatformName(platformName);
				fox.setCapability("sauce:options", sauceOptions);
			}
			else if (options instanceof EdgeOptions edge)
			{
				edge.setBrowserVersion(browserVersion);
				edge.setPlatformName(platformName);
				edge.setCapability("sauce:options", sauceOptions);
			}
			else if (options instanceof SafariOptions safari)
			{
				safari.setBrowserVersion(browserVersion);
				safari.setPlatformName(platformName);
				safari.setCapability("sauce:options", sauceOptions);
			}
			else
			{
				options.setCapability("sauce:options", sauceOptions);
			}

			TestLogManager.info("Connecting to Sauce Labs Grid: " + remoteUrl);
			return new RemoteWebDriver(remoteUrl, options);
		}
		catch (Exception e)
		{
			TestLogManager.error("Failed to create Sauce Labs RemoteWebDriver", e);
			throw new RuntimeException("Sauce Labs driver creation failed", e);
		}
	}

	@Override
	public String getBrowserName()
	{
		return "saucelabs";
	}

	@Override
	public boolean supports(String browserType)
	{
		return "saucelabs".equalsIgnoreCase(browserType) || "sauce".equalsIgnoreCase(browserType);
	}

	private MutableCapabilities buildOptions(String browserName, DesiredCapabilities extra)
	{
		String headlessFlag = System.getProperty("SAUCE_HEADLESS", System.getProperty("headless", "false"));
		boolean headless = Boolean.parseBoolean(headlessFlag);

		if (browserName == null || browserName.isBlank())
		{
			browserName = "chrome";
		}

		switch (browserName.toLowerCase())
		{
			case "chrome" -> {
				ChromeOptions opts = new ChromeOptions();
				if (headless) opts.addArguments("--headless=new", "--disable-gpu");
				opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
				if (extra != null) opts.merge(extra);
				return opts;
			}
			case "firefox" -> {
				FirefoxOptions opts = new FirefoxOptions();
				if (headless) opts.addArguments("-headless");
				if (extra != null) opts.merge(extra);
				return opts;
			}
			case "edge" -> {
				EdgeOptions opts = new EdgeOptions();
				if (headless) opts.addArguments("--headless=new");
				if (extra != null) opts.merge(extra);
				return opts;
			}
			case "safari" -> {
				SafariOptions opts = new SafariOptions();
				if (extra != null) opts.merge(extra);
				return opts;
			}
			default -> {
				ChromeOptions opts = new ChromeOptions();
				if (headless) opts.addArguments("--headless=new");
				if (extra != null) opts.merge(extra);
				return opts;
			}
		}
	}

	private static void putIfPresent(Map<String, Object> map, String key, Object value)
	{
		if (value != null) map.put(key, value);
	}

	private static boolean isNullOrEmpty(String s)
	{
		return s == null || s.isBlank();
	}
}
