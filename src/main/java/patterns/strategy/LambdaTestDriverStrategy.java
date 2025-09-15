package patterns.strategy;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import reporting.TestLogManager;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver strategy for running tests on LambdaTest Selenium Grid.
 * Usage: set system properties or environment variables, then select browser "lambdatest".
 * Required: LT_USERNAME, LT_ACCESS_KEY
 */
public class LambdaTestDriverStrategy implements DriverStrategy
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
			String username = getEnvOrProperty("LT_USERNAME", "lt.username");
			String accessKey = getEnvOrProperty("LT_ACCESS_KEY", "lt.accessKey");
			if (isNullOrEmpty(username) || isNullOrEmpty(accessKey))
			{
				throw new IllegalArgumentException("LambdaTest credentials not provided. Set LT_USERNAME and LT_ACCESS_KEY");
			}

			String gridUrl = getEnvOrProperty("LT_GRID_URL", "lt.gridUrl");
			if (isNullOrEmpty(gridUrl))
			{
				gridUrl = "https://" + username + ":" + accessKey + "@hub.lambdatest.com/wd/hub";
			}
			URL remoteUrl = new URL(gridUrl);

			String browserName = getEnvOrProperty("LT_BROWSER", "lt.browser", System.getProperty("browserName", "chrome"));
			String browserVersion = getEnvOrProperty("LT_BROWSER_VERSION", "lt.browserVersion", System.getProperty("browserVersion", "latest"));
			String platformName = getEnvOrProperty("LT_PLATFORM", "lt.platform", System.getProperty("platformName", "Windows 11"));

			MutableCapabilities options = buildOptions(browserName, capabilities);

			Map<String, Object> ltOptions = new HashMap<>();
			ltOptions.put("user", username);
			ltOptions.put("accessKey", accessKey);
			ltOptions.put("build", getEnvOrProperty("LT_BUILD", "lt.build", "UtilityFramework-Build"));
			ltOptions.put("name", getEnvOrProperty("LT_NAME", "lt.name", "LambdaTest Example"));
			ltOptions.put("platformName", platformName);
			ltOptions.put("selenium_version", getEnvOrProperty("LT_SELENIUM_VERSION", "lt.seleniumVersion", "4.20.0"));
			putIfPresent(ltOptions, "resolution", getEnvOrProperty("LT_RESOLUTION", "lt.resolution", null));
			putIfPresent(ltOptions, "network", getBooleanFlag("LT_NETWORK", "lt.network"));
			putIfPresent(ltOptions, "video", getBooleanFlag("LT_VIDEO", "lt.video"));
			putIfPresent(ltOptions, "console", getEnvOrProperty("LT_CONSOLE", "lt.console", null));
			putIfPresent(ltOptions, "visual", getBooleanFlag("LT_VISUAL", "lt.visual"));
			putIfPresent(ltOptions, "geoLocation", getEnvOrProperty("LT_GEO_LOCATION", "lt.geoLocation", null));

			// Attach LambdaTest options based on browser type
			if (options instanceof ChromeOptions chrom)
			{
				chrom.setBrowserVersion(browserVersion);
				chrom.setCapability("LT:Options", ltOptions);
			}
			else if (options instanceof FirefoxOptions fox)
			{
				fox.setBrowserVersion(browserVersion);
				fox.setCapability("LT:Options", ltOptions);
			}
			else if (options instanceof EdgeOptions edge)
			{
				edge.setBrowserVersion(browserVersion);
				edge.setCapability("LT:Options", ltOptions);
			}
			else
			{
				options.setCapability("LT:Options", ltOptions);
			}

			TestLogManager.info("Connecting to LambdaTest Grid: " + remoteUrl);
			return new RemoteWebDriver(remoteUrl, options);
		}
		catch (Exception e)
		{
			TestLogManager.error("Failed to create LambdaTest RemoteWebDriver", e);
			throw new RuntimeException("LambdaTest driver creation failed", e);
		}
	}

	@Override
	public String getBrowserName()
	{
		return "lambdatest";
	}

	@Override
	public boolean supports(String browserType)
	{
		return "lambdatest".equalsIgnoreCase(browserType) || "lt".equalsIgnoreCase(browserType);
	}

	private MutableCapabilities buildOptions(String browserName, DesiredCapabilities extra)
	{
		String headlessFlag = getEnvOrProperty("LT_HEADLESS", "lt.headless", System.getProperty("headless", "false"));
		boolean headless = Boolean.parseBoolean(headlessFlag);
		switch (browserName.toLowerCase())
		{
			case "chrome" -> {
				ChromeOptions opts = new ChromeOptions();
				if (headless) opts.addArguments("--headless=new");
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

	private static boolean getBooleanFlag(String envKey, String sysKey)
	{
		String v = getEnvOrProperty(envKey, sysKey, null);
		return v != null && ("true".equalsIgnoreCase(v) || "1".equals(v));
	}

	private static String getEnvOrProperty(String envKey, String sysKey)
	{
		String v = System.getenv(envKey);
		if (v == null || v.isBlank()) v = System.getProperty(sysKey);
		return v;
	}

	private static String getEnvOrProperty(String envKey, String sysKey, String def)
	{
		String v = getEnvOrProperty(envKey, sysKey);
		return (v == null || v.isBlank()) ? def : v;
	}

	private static boolean isNullOrEmpty(String s)
	{
		return s == null || s.isBlank();
	}
}
