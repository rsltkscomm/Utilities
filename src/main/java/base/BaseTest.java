package base;

import org.testng.annotations.*;

import constants.FrameworkConstants;
import data.TestDataUtil;
import data.XLSReader;
import pages.PageFactory;
import reporting.ExtentManager;
import reporting.TestLogManager;

import java.lang.reflect.Method;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

public class BaseTest
{
	protected WebDriver driver;

	public static ThreadLocal<String> appName = new ThreadLocal<>();
	public static ThreadLocal<String> method_name = new ThreadLocal<>();
	public static ThreadLocal<String> browserName = new ThreadLocal<>();
	public static ThreadLocal<String> sheet_name = new ThreadLocal<>();
	public static ThreadLocal<XLSReader> datatable = new ThreadLocal<>();
	public static ThreadLocal<Integer> currentRow = new ThreadLocal<Integer>();

	@BeforeSuite(alwaysRun = true)
	@Parameters({ "runner" })
	public void beforeSuite(String runner)
	{
		ExtentManager.initReports();
		TestLogManager.reloadConfiguration();
		if (GridManager.checkIfGrid(runner))
		{
			DockerManager.dockerContainterUp();
		}
		TestLogManager.info("==== Test Suite Started ====");
	}

	@BeforeMethod(alwaysRun = true)
	@Parameters({ "applicationName", "sheetname", "browser" })
	public void beforeMethod(String applicationName, String sheetname, String browser, Method method)
	{

		// 1. Initialize logger
		TestLogManager.testStart(method.getName());

		// 2. Store metadata in ThreadLocal
		appName.set(applicationName);
		sheet_name.set(sheetname);
		method_name.set(method.getName());
		browserName.set(browser);

		// 3. Initialize driver
		DriverManager.createDriver(browser);
		DriverManager.getDriver().manage().window().maximize();

		// Start reporting
		String testName = method.getAnnotation(Test.class).testName();
		ExtentManager.startTest(method.getAnnotation(Test.class).description(), testName, browserName.get());

		// 4. Build path for Excel test data
		Map<String, String> appPropertyMap = Map.of("RegressionAccountSetup", System.getProperty("RegressionAccountSetup"), "RegressionAudience", System.getProperty("RegressionAudience"), "RegressionCommunication",
				System.getProperty("RegressionCommunication"), "RegressionPreferences", System.getProperty("RegressionPreferences"), "RegressionAnalytics", System.getProperty("RegressionAnalytics"), "Deploymentchecklist",
				System.getProperty("Deploymentchecklist"), "PageLoadTesting", System.getProperty("PageLoadTesting"), "NewAccountCreationChecklist", System.getProperty("NewAccountCreationChecklist"), "FeaturewiseChecklist",
				System.getProperty("FeaturewiseChecklist"));

		String dataFile = appPropertyMap.getOrDefault(appName.get(), "");
		String testDataFile = FrameworkConstants.DRIVE_TESTDATA_FILEPATH + "TestData\\" + System.getProperty("UserName") + "_" + System.getProperty("Environment").toLowerCase() + "\\Team\\" + dataFile;

		// 5. Set datatable (thread-safe)
		datatable.set(new XLSReader(PageBase.getNormalizedPath(testDataFile)));

		TestLogManager.info("Loaded test data from: " + testDataFile);
		TestDataUtil testDataUtil = new TestDataUtil();
		if (!testDataUtil.isTCIDFound(this))
		{
			throw new RuntimeException("TestMethodName not found in Excel sheet: " + method_name.get());
		}
		TestDataUtil.createDataRef();
		PageBase.getDeviceSpecs();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown(ITestResult result)
	{
		// Reporting
		switch (result.getStatus())
		{
		case ITestResult.SUCCESS -> {
			TestLogManager.success("Test passed: " + result.getName());
			ExtentManager.passLabel(result.getName());
		}
		case ITestResult.FAILURE -> {
			TestLogManager.error("Test failed: " + result.getName(), result.getThrowable());
			ExtentManager.failLabel(result.getName());
			ExtentManager.failLabel(result.getThrowable().toString());

		}
		case ITestResult.SKIP -> {
			TestLogManager.warning("Test skipped: " + result.getName());
			ExtentManager.skipLabel(result.getName());
			ExtentManager.skipLabel(result.getThrowable().toString());
		}
		}

		// Cleanup driver
		if (DriverManager.getDriver() != null)
		{
			DriverManager.quitDriver();
		}

		// Cleanup datatable
		if (datatable.get() != null)
		{
			datatable.remove();
		}
	}

	@AfterSuite(alwaysRun = true)
	public void afterSuite()
	{
		ExtentManager.flushReports();
		if (GridManager.isGrid.get().equals(true))
		{
			DockerManager.dockerContainterDown();
		}
		ExtentManager.openExtentReport();
		TestLogManager.info("==== Test Suite Finished ====");
	}

	public PageFactory getPageFactory()
	{
		return new PageFactory(DriverManager.getDriver());
	}
}
