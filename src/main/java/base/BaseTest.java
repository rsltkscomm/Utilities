package base;

import constants.FrameworkConstants;
import core.interfaces.EngineType;
import data.TestDataUtil;
import data.XLSReader;
import pages.PageFactory;
import reporting.ExcelReportGenerator;
import reporting.ExtentManager;
import reporting.TestLogManager;
import seleniumUtils.DateUtils;
import seleniumUtils.ScreenshotUtil;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import java.lang.reflect.Method;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

public class BaseTest {

    protected DriverContext driverContext;
    protected WebDriver driver; // legacy Selenium support

    static {
        // Allow writing Excel reports with highly compressed templates without triggering zip bomb checks
        ZipSecureFile.setMinInflateRatio(0.0d);
    }

    /* =========================
       THREAD LOCALS
       ========================= */

    public static ThreadLocal<String> appName = new ThreadLocal<>();
    public static ThreadLocal<String> method_name = new ThreadLocal<>();
    public static ThreadLocal<String> browserName = new ThreadLocal<>();
    public static ThreadLocal<String> sheet_name = new ThreadLocal<>();
    public static ThreadLocal<XLSReader> datatable = new ThreadLocal<>();
    public static ThreadLocal<Integer> currentRow = new ThreadLocal<>();

    public static String currentDate;
    public static String endDateTime;

    /* =========================
       BEFORE SUITE
       ========================= */

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite(ITestContext context) {

        ExtentManager.initReports();
        TestLogManager.reloadConfiguration();

        if (GridManager.checkIfGrid(System.getProperty("Browser"))) {
            AutoDockerInstallAndRun.dockerInstallAndRun();
            DockerManager.dockerContainterUp();
        }

        String suiteName = context.getSuite().getName();
        String timestamp = DateUtils.getCurrentDate("dd-MMM-yyyy_HH-mm-ss");
        System.setProperty("LT_BUILD", suiteName + "_Build_" + timestamp);

        currentDate = DateUtils.getCurrentDate("dd-MMM-yyyy HH:mm");
        TestLogManager.info("==== TEST SUITE STARTED ====");
    }

    /* =========================
       BEFORE METHOD
       ========================= */

    @BeforeMethod(alwaysRun = true)
    @Parameters({ "applicationName", "sheetname" })
    public void beforeMethod(
            String applicationName,
            String sheetname,
            Method method
    ) {

        try {
            TestLogManager.testStart(method.getName());
            System.setProperty("LT_NAME", method.getName());

            String browser = System.getProperty("Browser");

            appName.set(applicationName);
            sheet_name.set(sheetname);
            method_name.set(method.getName());
            browserName.set(browser);

            // Always start clean
            DriverManager.quitDriver();
            DriverManager.createDriver(browser);

            driverContext = DriverManager.getContext();

            // Legacy Selenium support
            if (driverContext.getEngineType() == EngineType.SELENIUM) {
                driver = driverContext.getWebDriver();
            }

            Test test = method.getAnnotation(Test.class);
            ExtentManager.startTest(
                    test.description(),
                    test.testName(),
                    browser
            );

            loadTestData(method);
            
            // Device / browser info
            driverContext.getAutomationContext().getDeviceSpecs();

        } catch (Exception e) {
            TestLogManager.error("BeforeMethod failed", e);
            Assert.fail("Test setup failed for: " + method.getName(), e);
        }
    }

    /* =========================
       LOAD TEST DATA
       ========================= */

    private void loadTestData(Method method) {

        Map<String, String> appPropertyMap = Map.of(
                "RegressionAccountSetup", System.getProperty("RegressionAccountSetup"),
                "RegressionAudience", System.getProperty("RegressionAudience"),
                "RegressionCommunication", System.getProperty("RegressionCommunication"),
                "RegressionPreferences", System.getProperty("RegressionPreferences"),
                "RegressionAnalytics", System.getProperty("RegressionAnalytics"),
                "Deploymentchecklist", System.getProperty("Deploymentchecklist")
        );

        String dataFile = appPropertyMap.getOrDefault(appName.get(), "");
        String testDataFile = TestDataUtil.getDataFilesPath(dataFile);

        datatable.set(new XLSReader(PageBase.getNormalizedPath(testDataFile)));

        TestDataUtil util = new TestDataUtil();
        if (!util.isTCIDFound(this)) {
            ExtentManager.failLabel("Test data not found for: " + method_name.get());
            Assert.fail("No test data found for method: " + method_name.get());
        }

        TestDataUtil.createDataRef();
    }

    /* =========================
       AFTER METHOD
       ========================= */

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {

        try {
            ScreenshotUtil.takeScreenshot();

            switch (result.getStatus()) {
                case ITestResult.SUCCESS ->
                        ExtentManager.passLabel(result.getName());

                case ITestResult.FAILURE -> {
                    ExtentManager.failLabel(result.getName());
                    if (result.getThrowable() != null) {
                        ExtentManager.failLabel(result.getThrowable().toString());
                    }
                }

                case ITestResult.SKIP ->
                        ExtentManager.skipLabel(result.getName());
            }

            ExcelReportGenerator.writeToExcel(
                    FrameworkConstants.ONEDRIVE_BASE_PATH,
                    "Daily,Release,Account",
                    System.getProperty("DateWiseReport") + "," +
                            System.getProperty("ReleasewiseReport") + "," +
                            System.getProperty("AccountWiseReport"),
                    result.getMethod().getMethodName().toUpperCase(),
                    System.getProperty("ReleaseVersion"),
                    System.getProperty("Account") + "_" + System.getProperty("Environment"),
                    System.getProperty("Account"),
                    System.getProperty("SuiteName")
            );

        } finally {
            DriverManager.quitDriver();
            datatable.remove();
        }
    }

    /* =========================
       AFTER SUITE
       ========================= */

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {

        try {
            ExtentManager.flushReports();

            if (GridManager.isGrid.get().equals(true)) {
                DockerManager.dockerContainterDown();
            }

            ExtentManager.openExtentReport();
            endDateTime = DateUtils.getCurrentDate("HH:mm");

            TestLogManager.info("==== TEST SUITE FINISHED ====");

        } catch (Exception e) {
            TestLogManager.warning("AfterSuite error: " + e.getMessage());
        }
    }

    /* =========================
       PAGE FACTORY ACCESS
       ========================= */

    protected PageFactory getPageFactory() {
        return new PageFactory(driverContext);
    }

    protected DriverContext getDriverContext() {
        return driverContext;
    }
}
