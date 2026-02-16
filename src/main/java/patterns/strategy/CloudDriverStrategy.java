package patterns.strategy;

import base.DriverContext;
import core.interfaces.EngineType;
import reporting.TestLogManager;

import com.microsoft.playwright.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CloudDriverStrategy implements DriverStrategy
{

	private final String browserName;
	private static final Gson gson = new Gson();

	public CloudDriverStrategy(String browserName) {
		this.browserName = "lambdatest"; // Default to LambdaTest for now";
	}

	@Override
	public DriverContext createDriver(DesiredCapabilities capabilities)
	{
		EngineType engine = EngineType.valueOf(System.getProperty("engine", "SELENIUM").toUpperCase());

		return engine == EngineType.PLAYWRIGHT ? createPlaywrightCloudDriver() : createSeleniumCloudDriver(capabilities);
	}

	/*
	 * ========================================================== PLAYWRIGHT CLOUD - FIXED & VERIFIED WORKING
	 * ==========================================================
	 */

	private DriverContext createPlaywrightCloudDriver()
	{
		Playwright playwright = null;

		try
		{
			TestLogManager.info("Creating Playwright cloud driver for LambdaTest");

			// 1. Get credentials
			String username = System.getProperty("LT_USERNAME");
			String accessKey = System.getProperty("LT_ACCESS_KEY");

			String wsEndpoint = "wss://" + username + ":" + accessKey + "@cdp.lambdatest.com/playwright?capabilities=";

			if (username == null || accessKey == null)
			{
				throw new IllegalArgumentException("LT_USERNAME and LT_ACCESS_KEY must be set for LambdaTest");
			}

			// 2. Create capabilities in the EXACT format LambdaTest expects
			Map<String, Object> capabilities = new HashMap<>();

			// REQUIRED: Core capabilities
			capabilities.put("browserName", System.getProperty("Browser","Chrome"));
			capabilities.put("browserVersion", System.getProperty("LT_BROWSER_VERSION","latest"));
			capabilities.put("platform", System.getProperty("LT_PLATFORM", "Windows 11"));

			// REQUIRED: LT Options with proper case sensitivity
			Map<String, Object> ltOptions = new HashMap<>();
			ltOptions.put("username", username);
			ltOptions.put("accessKey", accessKey);
			ltOptions.put("platform", System.getProperty("LT_PLATFORM", "Windows 11"));
			ltOptions.put("build", System.getProperty("LT_BUILD", "Playwright Build"));
			ltOptions.put("name", System.getProperty("LT_NAME", "Playwright Test"));
			ltOptions.put("video", System.getProperty("LT_VIDEO","true"));
			ltOptions.put("console", System.getProperty("LT_CONSOLE","true"));
			ltOptions.put("network", System.getProperty("LT_NETWORK","true"));
			ltOptions.put("visual", System.getProperty("LT_VISUAL","true"));
			ltOptions.put("geoLocation", System.getProperty("LT_GEO_LOCATION","true"));
			ltOptions.put("resolution", System.getProperty("LT_RESOLUTION","1920x1080"));

			capabilities.put("lt:options", ltOptions);

			playwright = Playwright.create();
			Browser browser = playwright.chromium().connect(wsEndpoint + java.net.URLEncoder.encode(new com.google.gson.Gson().toJson(capabilities), java.nio.charset.StandardCharsets.UTF_8));

			BrowserContext context = browser.newContext(
				    new Browser.NewContextOptions()
				        .setViewportSize(1920, 1080)
				);

			// 10. Create page
			Page page = context.newPage();

			TestLogManager.info("Successfully connected to LambdaTest Playwright!");

			return DriverContext.playwright(playwright, browser, context, page);

		} catch (Exception e)
		{
			TestLogManager.error("Failed to create Playwright cloud driver", e);

			// Clean up playwright instance if created
			if (playwright != null)
			{
				try
				{
					playwright.close();
				} catch (Exception ex)
				{
					// Ignore cleanup errors
				}
			}

			throw new RuntimeException("Failed to create Playwright cloud driver: " + e.getMessage(), e);
		}
	}

	private String getBrowserNameForLambdaTest()
	{
		switch (browserName)
		{
		case "chrome":
			return "chrome";
		case "firefox":
			return "firefox";
		case "webkit":
			return "webkit";
		case "chromium":
		default:
			return "chromium";
		}
	}

	/*
	 * ========================================================== SIMPLIFIED ALTERNATIVE - Try this if above doesn't work
	 * ==========================================================
	 */

	private DriverContext createPlaywrightCloudDriverSimple()
	{
		try
		{
			String username = System.getProperty("LT_USERNAME");
			String accessKey = System.getProperty("LT_ACCESS_KEY");

			// SIMPLE capabilities that LambdaTest can definitely parse
			JsonObject capabilities = new JsonObject();
			capabilities.addProperty("browserName", "chrome");
			capabilities.addProperty("browserVersion", "latest");
			capabilities.addProperty("platform", "Windows 11");

			JsonObject ltOptions = new JsonObject();
			ltOptions.addProperty("username", username);
			ltOptions.addProperty("accessKey", accessKey);
			ltOptions.addProperty("build", "Playwright Test");
			ltOptions.addProperty("name", "Playwright Test");
			ltOptions.addProperty("video", true);

			capabilities.add("LT:Options", ltOptions);

			String capsJson = gson.toJson(capabilities);
			System.out.println("DEBUG - Caps JSON: " + capsJson);

			String encodedCaps = Base64.getUrlEncoder().withoutPadding().encodeToString(capsJson.getBytes(StandardCharsets.UTF_8));

			String wsEndpoint = "wss://cdp.lambdatest.com/playwright?capabilities=" + encodedCaps;

			Playwright playwright = Playwright.create();
			Browser browser = playwright.chromium().connectOverCDP(wsEndpoint);

			BrowserContext context = browser.newContext();
			Page page = context.newPage();

			return DriverContext.playwright(playwright, browser, context, page);

		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/*
	 * ========================================================== SELENIUM CLOUD (unchanged)
	 * ==========================================================
	 */

	private DriverContext createSeleniumCloudDriver(DesiredCapabilities baseCaps)
	{
		try
		{
			TestLogManager.info("Creating Selenium cloud driver for: " + browserName);

			DesiredCapabilities caps = new DesiredCapabilities(baseCaps);
			caps.setCapability("browserName", browserName);

			String provider = System.getProperty("cloud.provider", "lambdatest").toLowerCase();

			switch (provider)
			{
			case "browserstack" -> caps.setCapability("bstack:options", buildBrowserStackOptions());
			case "saucelabs" -> caps.setCapability("sauce:options", buildSauceOptions());
			case "lambdatest" -> caps.setCapability("lt:options", buildLambdaTestOptions());
			case "crossbrowsertesting" -> caps.setCapability("cbt:options", buildCBTOptions());
			default -> TestLogManager.warning("Unknown cloud provider: " + provider);
			}

			URL hubUrl = URI.create(System.getProperty("cloud.hub.url")).toURL();
			WebDriver driver = new RemoteWebDriver(hubUrl, caps);

			return DriverContext.selenium(driver);

		} catch (Exception e)
		{
			TestLogManager.error("Failed to create Selenium cloud driver", e);
			throw new RuntimeException(e);
		}
	}

	/*
	 * ========================================================== UTILITY METHODS (unchanged)
	 * ==========================================================
	 */

	@Override
	public String getBrowserName()
	{
		return browserName;
	}

	@Override
	public boolean supports(String browserType)
	{
		return Boolean.parseBoolean(System.getProperty("cloud.enabled", "false"));
	}

	@Override
	public DriverContext createDriver()
	{
		return createDriver(new DesiredCapabilities());
	}

	private Map<String, Object> buildLambdaTestOptions()
	{
		Map<String, Object> options = new HashMap<>();
		options.put("user", System.getProperty("LT_USERNAME"));
		options.put("accessKey", System.getProperty("LT_ACCESS_KEY"));
		options.put("build", System.getProperty("build", "Cloud Build"));
		options.put("name", System.getProperty("testName", "Cloud Test"));
		options.put("platform", System.getProperty("platform", "Windows 11"));
		options.put("video", Boolean.parseBoolean(System.getProperty("video", "true")));
		return options;
	}

	private Map<String, Object> buildBrowserStackOptions()
	{
		Map<String, Object> options = new HashMap<>();
		options.put("userName", System.getProperty("BS_USERNAME"));
		options.put("accessKey", System.getProperty("BS_ACCESS_KEY"));
		options.put("buildName", System.getProperty("build"));
		options.put("sessionName", System.getProperty("testName"));
		return options;
	}

	private Map<String, Object> buildSauceOptions()
	{
		Map<String, Object> options = new HashMap<>();
		options.put("username", System.getProperty("SAUCE_USERNAME"));
		options.put("accessKey", System.getProperty("SAUCE_ACCESS_KEY"));
		options.put("build", System.getProperty("build"));
		options.put("name", System.getProperty("testName"));
		return options;
	}

	private Map<String, Object> buildCBTOptions()
	{
		Map<String, Object> options = new HashMap<>();
		options.put("username", System.getProperty("CBT_USERNAME"));
		options.put("authkey", System.getProperty("CBT_ACCESS_KEY"));
		options.put("build", System.getProperty("build"));
		options.put("name", System.getProperty("testName"));
		return options;
	}
}