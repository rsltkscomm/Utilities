package patterns.strategy;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import reporting.TestLogManager;

public class SeleniumGridStrategy implements DriverStrategy
{

	@Override
	public WebDriver createDriver()
	{
		return createDriver(null);
	}

	@Override
	public WebDriver createDriver(DesiredCapabilities capabilities)
	{
		MutableCapabilities options = buildOptions(getBrowserName());
		RemoteWebDriver remoteWebDriver = null;
		try
		{
			remoteWebDriver = new RemoteWebDriver(new URL(getRemoteWebDriverURL()), options);
		} catch (Exception e)
		{
		}
		return remoteWebDriver;
	}

	@Override
	public String getBrowserName()
	{
		return "grid";
	}

	@Override
	public boolean supports(String browserType)
	{
		 return "grid".equalsIgnoreCase(browserType) || "seleniumgrid".equalsIgnoreCase(browserType);
	}
	
	private static String getRemoteWebDriverURL()
	{
		String remoteURL = null;
		try (DatagramSocket socket = new DatagramSocket())
		{
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			String ipAddress = socket.getLocalAddress().getHostAddress();
			remoteURL = "http://" + ipAddress.concat(":") + System.getProperty("Grid_PORT");

		} catch (Exception e)
		{
			TestLogManager.error("Exception in getRemoteWebDriverURL", e);
		}
		return remoteURL;
	}
	
	private MutableCapabilities buildOptions(String browserName) {
	    // Fetch headless flag from property (default false)
	    boolean headless = Boolean.parseBoolean(System.getProperty("GRID_HEADLESS", "false"));
	    String resolution = System.getProperty("GRID_RESOLUTION", "1920x1080");

	    switch (browserName.toLowerCase()) {
	        case "chrome" -> {
	            ChromeOptions opts = new ChromeOptions();

	            // 🧱 Common arguments for stability (especially in Docker/Grid)
	            opts.addArguments("--no-sandbox");
	            opts.addArguments("--disable-dev-shm-usage");
	            opts.addArguments("--disable-gpu");
	            opts.addArguments("--disable-notifications");
	            opts.addArguments("--disable-popup-blocking");
	            opts.addArguments("--disable-extensions");
	            opts.addArguments("--incognito");
	            opts.addArguments("--window-size=" + resolution);

	            // ✅ Enable headless if requested
	            if (headless) opts.addArguments("--headless=new");

	            // 🧩 Preferences (downloads, security)
	            Map<String, Object> prefs = new HashMap<>();
	            prefs.put("download.prompt_for_download", false);
	            prefs.put("profile.default_content_settings.popups", 0);
	            prefs.put("credentials_enable_service", false);
	            prefs.put("profile.password_manager_enabled", false);
	            prefs.put("download.default_directory",
	                    Paths.get(System.getProperty("user.dir"), "target", "downloads").toString());

	            opts.setExperimentalOption("prefs", prefs);
	            opts.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
	            opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

	            return opts;
	        }

	        case "firefox" -> {
	            FirefoxOptions opts = new FirefoxOptions();

	            if (headless) opts.addArguments("-headless");

	            // 🧱 Set resolution if specified
	            String[] res = resolution.split("x");
	            if (res.length == 2) {
	                opts.addArguments("--width=" + res[0]);
	                opts.addArguments("--height=" + res[1]);
	            }

	            // 🧩 Download prefs
	            FirefoxProfile profile = new FirefoxProfile();
	            profile.setPreference("browser.download.folderList", 2);
	            profile.setPreference("browser.download.dir",
	                    Paths.get(System.getProperty("user.dir"), "target", "downloads").toString());
	            profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
	                    "application/pdf,application/octet-stream,text/csv");
	            profile.setPreference("pdfjs.disabled", true);
	            opts.setProfile(profile);

	            opts.setAcceptInsecureCerts(true);
	            return opts;
	        }

	        case "edge" -> {
	            EdgeOptions opts = new EdgeOptions();

	            opts.addArguments("--no-sandbox");
	            opts.addArguments("--disable-dev-shm-usage");
	            opts.addArguments("--disable-gpu");
	            opts.addArguments("--disable-notifications");
	            opts.addArguments("--window-size=" + resolution);

	            if (headless) opts.addArguments("--headless=new");

	            opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
	            return opts;
	        }

	        case "safari" -> {
	            SafariOptions opts = new SafariOptions();
	            // Safari currently does not support headless mode
	            opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
	            return opts;
	        }

	        default -> {
	            // Fallback: Chrome as default
	            ChromeOptions opts = new ChromeOptions();
	            opts.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
	            opts.addArguments("--window-size=" + resolution);
	            if (headless) opts.addArguments("--headless=new");
	            return opts;
	        }
	    }
	}


}
