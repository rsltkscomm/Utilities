package base;

import org.testng.annotations.*;
import constants.FrameworkConstants;
import data.TestDataUtil;
import data.XLSReader;
import pages.PageFactory;
import reporting.ExcelReportGenerator;
import reporting.ExtentManager;
import reporting.TestLogManager;
import seleniumUtils.DateUtils;
import seleniumUtils.ScreenshotUtil;

import java.lang.reflect.Method;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;

public class BaseTest {
	protected WebDriver driver;

	public static ThreadLocal<String> appName = new ThreadLocal<>();
	public static ThreadLocal<String> method_name = new ThreadLocal<>();
	public static ThreadLocal<String> browserName = new ThreadLocal<>();
	public static ThreadLocal<String> sheet_name = new ThreadLocal<>();
	public static ThreadLocal<XLSReader> datatable = new ThreadLocal<>();
	public static ThreadLocal<Integer> currentRow = new ThreadLocal<>();

	public static String currentDate;
	public static String EndDateTime;

	@BeforeSuite(alwaysRun = true)
	public void beforeSuite(ITestContext context) {
		// Initialize reports and logging
		ExtentManager.initReports();
		TestLogManager.reloadConfiguration();

		// Start Docker Grid if enabled
		if (GridManager.checkIfGrid(System.getProperty("Browser"))) {
			DockerManager.dockerContainterUp();
		}

		// ✅ Create LambdaTest Build once per suite
		String suiteName = context.getSuite().getName();
		String timestamp = DateUtils.getCurrentDate("dd-MMM-yyyy_HH-mm-ss");
		String ltBuildName = suiteName + "_Build_" + timestamp;
		System.setProperty("LT_BUILD", ltBuildName);

		TestLogManager.info("LambdaTest Build (Suite-level): " + ltBuildName);
		currentDate = DateUtils.getCurrentDate("dd-MMM-yyyy HH:mm");
		TestLogManager.info("==== Test Suite Started ====");
	}

	@BeforeMethod(alwaysRun = true)
	@Parameters({ "applicationName", "sheetname" })
	public void beforeMethod(String applicationName, String sheetname, Method method) {
		try {
			// Start Test Logging
			TestLogManager.testStart(method.getName());
			System.setProperty("LT_NAME", method.getName());

			String browser = System.getProperty("Browser");

			// Set ThreadLocal metadata
			appName.set(applicationName);
			sheet_name.set(sheetname);
			method_name.set(method.getName());
			browserName.set(browser);

			// ✅ Ensure a fresh driver instance each time
			DriverManager.quitDriver();
			DriverManager.createDriver(browser);
			driver = DriverManager.getDriver();

			// Start Extent reporting
			String testName = method.getAnnotation(Test.class).testName();
			ExtentManager.startTest(method.getAnnotation(Test.class).description(), testName, browserName.get());

			// Load test data for this app/sheet
			Map<String, String> appPropertyMap = Map.of(
				"RegressionAccountSetup", System.getProperty("RegressionAccountSetup"),
				"RegressionAudience", System.getProperty("RegressionAudience"),
				"RegressionCommunication", System.getProperty("RegressionCommunication"),
				"RegressionPreferences", System.getProperty("RegressionPreferences"),
				"RegressionAnalytics", System.getProperty("RegressionAnalytics"),
				"Deploymentchecklist", System.getProperty("Deploymentchecklist"),
				"PageLoadTesting", System.getProperty("PageLoadTesting"),
				"NewAccountCreationChecklist", System.getProperty("NewAccountCreationChecklist"),
				"FeaturewiseChecklist", System.getProperty("FeaturewiseChecklist")
			);

			String dataFile = appPropertyMap.getOrDefault(appName.get(), "");
			String testDataFile = TestDataUtil.getDataFilesPath(dataFile);
			datatable.set(new XLSReader(PageBase.getNormalizedPath(testDataFile)));

			TestLogManager.info("Loaded test data from: " + testDataFile);

			TestDataUtil testDataUtil = new TestDataUtil();
			if (!testDataUtil.isTCIDFound(this)) {
				ExtentManager.failLabel("Test data not found for: " + method_name.get());
				Assert.fail("Test data not found in Excel for method: " + method_name.get());
			} else {
				ExtentManager.infoTest("METHOD NAME FOUND -> " + method_name.get());
			}

			TestDataUtil.createDataRef();
			PageBase.getDeviceSpecs();

			TestLogManager.info("Using LambdaTest Build: " + System.getProperty("LT_BUILD"));
		} catch (Exception e) {
			TestLogManager.error("Error in beforeMethod: " + e.getMessage(), e);
			Assert.fail("Setup failed for method: " + method.getName(), e);
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown(ITestResult result) {
		try {
			// Capture screenshot always
			ScreenshotUtil.takeScreenshot();

			// Reporting based on result
			switch (result.getStatus()) {
				case ITestResult.SUCCESS -> {
					TestLogManager.success("Test passed: " + result.getName());
					ExtentManager.passLabel(result.getName());
				}
				case ITestResult.FAILURE -> {
					TestLogManager.error("Test failed: " + result.getName(), result.getThrowable());
					ExtentManager.failLabel(result.getName());
					if (result.getThrowable() != null)
						ExtentManager.failLabel(result.getThrowable().toString());
				}
				case ITestResult.SKIP -> {
					TestLogManager.warning("Test skipped: " + result.getName());
					ExtentManager.skipLabel(result.getName());
					if (result.getThrowable() != null)
						ExtentManager.skipLabel(result.getThrowable().toString());
				}
			}

			// Excel reporting
			String flag = System.getProperty("DateWiseReport") + "," +
						  System.getProperty("ReleasewiseReport") + "," +
						  System.getProperty("AccountWiseReport");

			String methodname = result.getMethod().getMethodName().toUpperCase();
			String status = System.getProperty("Account") + "_" + System.getProperty("Environment");

			ExcelReportGenerator.writeToExcel(
					FrameworkConstants.ONEDRIVE_BASE_PATH,
					"Daily,Release,Account",
					flag,
					methodname,
					System.getProperty("ReleaseVersion"),
					status,
					System.getProperty("Account"),
					System.getProperty("SuiteName")
			);

		} catch (Exception e) {
			TestLogManager.warning("Error during teardown for: " + result.getName() + " -> " + e.getMessage());
		} finally {
			// ✅ Always cleanup
			try {
				DriverManager.quitDriver();
			} catch (Exception e) {
				TestLogManager.warning("Driver cleanup skipped or failed.");
			}

			try {
				if (datatable.get() != null) datatable.remove();
			} catch (Exception ignored) {}
		}
	}

	@AfterSuite(alwaysRun = true)
	public void afterSuite() {
		try {
			ExtentManager.flushReports();

			if (GridManager.isGrid.get().equals(true)) {
				DockerManager.dockerContainterDown();
			}

			ExtentManager.openExtentReport();
			EndDateTime = DateUtils.getCurrentDate(" HH:mm");
			TestLogManager.info("==== Test Suite Finished ====");
		} catch (Exception e) {
			TestLogManager.warning("Error in afterSuite: " + e.getMessage());
		}
	}

	public static PageFactory getPageFactory() {
		return new PageFactory(DriverManager.getDriver());
	}
}
