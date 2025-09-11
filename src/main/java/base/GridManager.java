package base;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import reporting.TestLogManager;

public class GridManager
{

	public static ThreadLocal<Boolean> isGrid = ThreadLocal.withInitial(() -> false);

	public static boolean checkIfGrid(String runner)
	{
		if ("SELENIUM GRID".equalsIgnoreCase(runner))
		{
			isGrid.set(true);
			TestLogManager.info("Test execution started on Selenium Grid");
		}
		return isGrid.get();
	}
	
	public static boolean initializeRemoteDriverIfGrid(Capabilities capabilities)
	{
		if (!isGrid.get())
			return false;

		try
		{
			String url = getRemoteWebDriverURL();
			if (url != null)
			{
				DriverManager.getDriverThread().set(new RemoteWebDriver(new URL(url), capabilities));
				return true;
			} else
			{
				TestLogManager.error("Remote WebDriver URL is null, cannot initialize RemoteWebDriver");
			}
		} catch (MalformedURLException e)
		{
			TestLogManager.error("MalformedURLException in initializeRemoteDriverIfGrid", e);
		}
		return false;
	}

	static String getRemoteWebDriverURL()
	{
		try (DatagramSocket socket = new DatagramSocket())
		{
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			String ipAddress = socket.getLocalAddress().getHostAddress();
			String port = System.getProperty("Grid_PORT", "4444");
			return String.format("http://%s:%s", ipAddress, port);
		} catch (Exception e)
		{
			TestLogManager.error("Exception in getRemoteWebDriverURL", e);
		}
		return null;
	}

}
