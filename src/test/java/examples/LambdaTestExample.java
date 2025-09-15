package examples;

import base.ModernBaseTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import reporting.TestLogManager;

/**
 * Example TestNG test demonstrating LambdaTest integration driven by parameters.
 * Provide parameters via testng.xml or -D system properties.
 */
public class LambdaTestExample extends ModernBaseTest
{
	@Test(description = "Run a simple navigation on LambdaTest")
	@Parameters({
			"ltUsername", "ltAccessKey", "ltGridUrl",
			"ltBrowser", "ltBrowserVersion", "ltPlatform",
			"ltBuild", "ltName", "ltUrl"
	})
	public void runOnLambdaTest(
			@Optional("") String ltUsername,
			@Optional("") String ltAccessKey,
			@Optional("") String ltGridUrl,
			@Optional("chrome") String ltBrowser,
			@Optional("latest") String ltBrowserVersion,
			@Optional("Windows 11") String ltPlatform,
			@Optional("UtilityFramework-Build") String ltBuild,
			@Optional("LambdaTest Param Test") String ltName,
			@Optional("https://example.com") String ltUrl)
	{
		// Pass parameters as system properties so strategy can read them
		setIfNotBlank("lt.username", ltUsername);
		setIfNotBlank("lt.accessKey", ltAccessKey);
		setIfNotBlank("lt.gridUrl", ltGridUrl);
		setIfNotBlank("lt.browser", ltBrowser);
		setIfNotBlank("lt.browserVersion", ltBrowserVersion);
		setIfNotBlank("lt.platform", ltPlatform);
		setIfNotBlank("lt.build", ltBuild);
		setIfNotBlank("lt.name", ltName);

		try
		{
			// Force lambdatest strategy
			System.setProperty("browser", "lambdatest");

			TestLogManager.info("Starting LambdaTest navigation: " + ltUrl);
			getCommandInvoker().executeCommand(new patterns.command.NavigationCommand(
					getDriver(), patterns.command.NavigationCommand.NavigationType.GET, ltUrl, "Navigate to target URL"));
			waitForPageLoad();
			TestLogManager.success("Page title: " + getDriver().getTitle());
		}
		finally
		{
			if (getDriver() != null)
			{
				getDriver().quit();
			}
		}
	}

	private void setIfNotBlank(String key, String value)
	{
		if (value != null && !value.isBlank())
		{
			System.setProperty(key, value);
		}
	}
}
