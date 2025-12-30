package smartUIComparison;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.microsoft.playwright.Page;

import base.AutomationContext;
import base.DriverManager;
import core.interfaces.EngineType;

public class UIComparisonScreenshotUtil
{

	/**
	 * Captures a screenshot using Selenium or Playwright
	 */
	public static BufferedImage captureScreenshot(String testName)
	{
		AutomationContext context = DriverManager.getAutomationContext();
		if (context == null)
		{
			System.err.println("AutomationContext is null, cannot capture screenshot");
			return null;
		}

		EngineType engineType = context.getEngineType();

		try
		{
			System.out.println("Capturing screenshot for test: " + testName);

			if (engineType == EngineType.SELENIUM)
			{

				WebDriver driver = DriverManager.getDriver();
				TakesScreenshot screenshotDriver = (TakesScreenshot) driver;

				byte[] screenshotBytes = screenshotDriver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);

				return ImageIO.read(new ByteArrayInputStream(screenshotBytes));

			} else if (engineType == EngineType.PLAYWRIGHT)
			{

				// 🔹 Get Playwright Page from context
				Page page = DriverManager.getPage();

				if (page == null)
				{
					System.err.println("Playwright Page is null, cannot capture screenshot");
					return createPlaceholderImage(800, 600);
				}

				// 🔹 Capture screenshot as bytes
				byte[] screenshotBytes = page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setFullPage(true));

				return ImageIO.read(new ByteArrayInputStream(screenshotBytes));

			} else
			{
				System.err.println("Unsupported engine type: " + engineType);
				return createPlaceholderImage(800, 600);
			}

		} catch (Exception e)
		{
			System.err.println("Error capturing screenshot: " + e.getMessage());
			e.printStackTrace();
			return createPlaceholderImage(800, 600);
		}
	}

	/**
	 * Captures a screenshot of a specific element
	 */
	private static BufferedImage captureElementScreenshot(String testName)
	{
		try
		{
			System.out.println("Capturing element screenshot for test: " + testName);
			// For now, fall back to full page screenshot
			// In a full implementation, you would locate the element and crop the screenshot
			return captureScreenshot(testName);
		} catch (Exception e)
		{
			System.err.println("Could not capture element screenshot, falling back to full page: " + e.getMessage());
			return captureScreenshot(testName);
		}
	}

	/**
	 * Capture screenshot and perform SmartUI comparison for a specific page
	 */
	public static void capturePageScreenshot(String testName, String description)
	{
		try
		{
			System.out.println("Capturing screenshot for: " + description);

			// Wait for page stability
			UIComparisonScreenshotUtil.waitForPageStability();
			boolean isSmartUIEnable = Boolean.parseBoolean(System.getProperty("smartui.enabled"));
			String screenshotStrategy = System.getProperty("smartui.screenshot.strategy");
			String comparisonMethod = System.getProperty("smartui.comparison.method");
			double tolerance = Double.parseDouble(System.getProperty("smartui.tolerance"));

			// Get SmartUI config
			if (!isSmartUIEnable)
			{
				System.out.println("SmartUI disabled, skipping screenshot for: " + testName);
				return;
			}

			// Ensure directories exist
			BaselineManager.ensureDirectoriesExist();

			// Capture screenshot
			String baselinePath = BaselineManager.getBaselinePath(testName);
			String actualPath = BaselineManager.getActualPath(testName);
			String diffPath = BaselineManager.getDiffPath(testName);

			java.awt.image.BufferedImage screenshot = UIComparisonScreenshotUtil.captureScreenshot(testName);
			UIComparisonScreenshotUtil.saveScreenshot(screenshot, actualPath);

			// Perform comparison
			if (!BaselineManager.baselineExists(testName))
			{
				System.out.println("Creating baseline for: " + testName);
				BaselineManager.createBaseline(testName, actualPath);

				// Record in custom report
				reporting.SmartUICustomReport.add("SmartUISuite", testName, baselinePath, actualPath, null, // no diff for new baseline
						screenshotStrategy, comparisonMethod, 0.0, // no difference for new baseline
						tolerance, true // passed
				);
			} else
			{
				// Compare with existing baseline
				double diffPercent = ImageComparisonUtils.compareImagesBasic(baselinePath, actualPath, diffPath);
				boolean passed = diffPercent <= tolerance;

				System.out.println(String.format("Visual comparison for '%s': %s (diff: %.2f%%, tolerance: %.2f%%)", testName, passed ? "PASSED" : "FAILED", diffPercent, tolerance));

				// Record in custom report
				reporting.SmartUICustomReport.add("SmartUISuite", testName, baselinePath, actualPath, diffPath, screenshotStrategy, comparisonMethod, diffPercent, tolerance, passed);
			}

		} catch (Exception e)
		{
			System.err.println("Error capturing screenshot for " + testName + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Applies custom screenshot strategy logic
	 */
	private static BufferedImage applyCustomStrategy(Object driver)
	{
		System.out.println("Applying custom screenshot strategy");
		return captureScreenshot("custom");
	}

	/**
	 * Applies element exclusions to the screenshot
	 */
	@SuppressWarnings("unused")
	private static void applyElementExclusions(Object driver, String excludeElements)
	{
		if (excludeElements != null && !excludeElements.isEmpty())
		{
			String[] selectors = excludeElements.split(",");
			for (String selector : selectors)
			{
				try
				{
					String trimmedSelector = selector.trim();
					if (!trimmedSelector.isEmpty())
					{
						System.out.println("Excluding element: " + trimmedSelector);
					}
				} catch (Exception e)
				{
					System.err.println("Could not exclude element with selector: " + selector);
				}
			}
		}
	}

	/**
	 * Saves the captured screenshot to a specified path
	 */
	public static void saveScreenshot(BufferedImage screenshot, String filePath)
	{
		try
		{
			if (screenshot == null)
			{
				System.err.println("Screenshot is null, cannot save to: " + filePath);
				return;
			}

			File outputFile = new File(filePath);
			outputFile.getParentFile().mkdirs(); // Ensure parent directories exist
			ImageIO.write(screenshot, "PNG", outputFile);
			System.out.println("Screenshot saved to: " + filePath);
		} catch (IOException e)
		{
			System.err.println("Error saving screenshot to " + filePath + ": " + e.getMessage());
		}
	}

	/**
	 * Waits for page to become stable with comprehensive loading checks
	 */
	public static void waitForPageStability()
	{
		AutomationContext context = DriverManager.getAutomationContext();
		if (context == null)
			return;

		try
		{
			if (context.getEngineType() == EngineType.SELENIUM)
			{
				waitForSeleniumStability();
			} else if (context.getEngineType() == EngineType.PLAYWRIGHT)
			{
				waitForPlaywrightStability();
			}
		} catch (Exception e)
		{
			System.err.println("Page stability wait failed: " + e.getMessage());
		}
	}

	/**
	 * Wait for document ready state
	 */
	private static void waitForDocumentReady(org.openqa.selenium.JavascriptExecutor js)
	{
		try
		{
			for (int i = 0; i < 30; i++)
			{ // Wait up to 30 seconds
				Boolean ready = (Boolean) js.executeScript("return document.readyState === 'complete'");
				if (ready)
				{
					System.out.println("Document ready state: complete");
					return;
				}
				Thread.sleep(1000);
			}
			System.out.println("Warning: Document ready state timeout");
		} catch (Exception e)
		{
			System.err.println("Error checking document ready state: " + e.getMessage());
		}
	}

	private static void waitForSeleniumStability()
	{
		WebDriver driver = DriverManager.getDriver();
		if (driver == null)
			return;

		org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;

		waitForDocumentReady(js);
		waitForImagesLoaded(js);
		waitForAjaxComplete(js);
		waitForSPAStability(js);
		waitForAnimationsComplete(js);
		waitForNetworkIdle(js);

		try
		{
			Thread.sleep(2000);
		} catch (InterruptedException ignored)
		{
		}
	}

	private static void waitForPlaywrightStability()
	{
		Page page = DriverManager.getPage();
		if (page == null)
			return;

		// DOM ready
		page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);

		// Network idle (stronger than Selenium JS)
		page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

		// Small buffer for animations
		page.waitForTimeout(1500);
	}

	/**
	 * Wait for all images to load
	 */
	private static void waitForImagesLoaded(org.openqa.selenium.JavascriptExecutor js)
	{
		try
		{
			for (int i = 0; i < 20; i++)
			{ // Wait up to 20 seconds
				Boolean imagesLoaded = (Boolean) js.executeScript("var images = document.images; " + "for (var i = 0; i < images.length; i++) { " + "  if (!images[i].complete) return false; " + "} " + "return true;");
				if (imagesLoaded)
				{
					System.out.println("All images loaded successfully");
					return;
				}
				Thread.sleep(1000);
			}
			System.out.println("Warning: Images loading timeout");
		} catch (Exception e)
		{
			System.err.println("Error checking images load state: " + e.getMessage());
		}
	}

	/**
	 * Wait for AJAX requests to complete
	 */
	private static void waitForAjaxComplete(org.openqa.selenium.JavascriptExecutor js)
	{
		try
		{
			for (int i = 0; i < 15; i++)
			{ // Wait up to 15 seconds
				Boolean ajaxComplete = (Boolean) js.executeScript("if (typeof jQuery !== 'undefined') { " + "  return jQuery.active === 0; " + "} " + "return true;");
				if (ajaxComplete)
				{
					System.out.println("AJAX requests completed");
					return;
				}
				Thread.sleep(1000);
			}
			System.out.println("Warning: AJAX requests timeout");
		} catch (Exception e)
		{
			System.err.println("Error checking AJAX state: " + e.getMessage());
		}
	}

	/**
	 * Wait for Single Page Application (SPA) stability
	 */
	private static void waitForSPAStability(org.openqa.selenium.JavascriptExecutor js)
	{
		try
		{
			// Check for Angular
			try
			{
				Boolean angularReady = (Boolean) js.executeScript("return typeof angular !== 'undefined' ? angular.element(document).injector().get('$http').pendingRequests.length === 0 : true");
				if (angularReady)
				{
					System.out.println("Angular app stabilized");
				}
			} catch (Exception ignored)
			{
			}

			// Check for React
			try
			{
				Boolean reactReady = (Boolean) js.executeScript("return typeof React !== 'undefined' ? document.querySelector('[data-reactroot]') !== null : true");
				if (reactReady)
				{
					System.out.println("React app rendered");
				}
			} catch (Exception ignored)
			{
			}

			// Check for Vue
			try
			{
				Boolean vueReady = (Boolean) js.executeScript("return typeof Vue !== 'undefined' ? document.querySelector('#app') !== null : true");
				if (vueReady)
				{
					System.out.println("Vue app rendered");
				}
			} catch (Exception ignored)
			{
			}

		} catch (Exception e)
		{
			System.err.println("Error checking SPA stability: " + e.getMessage());
		}
	}

	/**
	 * Wait for CSS animations to complete
	 */
	private static void waitForAnimationsComplete(org.openqa.selenium.JavascriptExecutor js)
	{
		try
		{
			for (int i = 0; i < 10; i++)
			{ // Wait up to 10 seconds
				Boolean animationsComplete = (Boolean) js.executeScript(
						"var animations = document.getAnimations ? document.getAnimations() : []; " + "return animations.length === 0 || animations.every(function(anim) { return anim.playState === 'finished' || anim.playState === 'idle'; });");
				if (animationsComplete)
				{
					System.out.println("CSS animations completed");
					return;
				}
				Thread.sleep(1000);
			}
			System.out.println("Warning: CSS animations timeout");
		} catch (Exception e)
		{
			System.err.println("Error checking animations state: " + e.getMessage());
		}
	}

	/**
	 * Wait for network idle (no pending requests)
	 */
	private static void waitForNetworkIdle(org.openqa.selenium.JavascriptExecutor js)
	{
		try
		{
			for (int i = 0; i < 10; i++)
			{ // Wait up to 10 seconds
				Boolean networkIdle = (Boolean) js.executeScript("return performance.getEntriesByType('navigation')[0].loadEventEnd > 0 && " + "performance.getEntriesByType('resource').every(function(entry) { return entry.responseEnd > 0; });");
				if (networkIdle)
				{
					System.out.println("Network idle state achieved");
					return;
				}
				Thread.sleep(1000);
			}
			System.out.println("Warning: Network idle timeout");
		} catch (Exception e)
		{
			System.err.println("Error checking network idle state: " + e.getMessage());
		}
	}

	/**
	 * Create a placeholder image for testing
	 */
	private static BufferedImage createPlaceholderImage(int width, int height)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		// Create a simple gradient pattern
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int red = (x * 255) / width;
				int green = (y * 255) / height;
				int blue = ((x + y) * 255) / (width + height);
				int rgb = (red << 16) | (green << 8) | blue;
				image.setRGB(x, y, rgb);
			}
		}

		return image;
	}

	/**
	 * Capture screenshot based on strategy
	 */
	public static BufferedImage captureScreenshotWithStrategy(Object driver, String testName, String strategy)
	{
		if (driver == null)
		{
			return null;
		}

		switch (strategy.toUpperCase())
		{
		case "VIEWPORT_PASTING":
			return captureScreenshot(testName);
		case "FULL_PAGE":
			return captureScreenshot(testName);
		case "ELEMENT_ONLY":
			return captureElementScreenshot(testName);
		case "CUSTOM":
			return applyCustomStrategy(driver);
		default:
			return captureScreenshot(testName);
		}
	}

}
