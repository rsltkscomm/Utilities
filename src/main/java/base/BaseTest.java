package base;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import constants.FrameworkConstants;
import core.interfaces.EngineType;
import data.TestDataUtil;
import data.XLSReader;
import pages.PageFactory;
import reporting.ExcelReportGenerator;
import reporting.ExtentManager;
import reporting.TestLogManager;

/**
 * Project-specific override of the upstream BaseTest that ships with the
 * Utilities dependency. The original implementation attempted to inject an
 * {@link org.testng.ITestContext} into the {@code @BeforeSuite} hook, which is
 * not supported by TestNG's native injection for suite-level configuration
 * methods and resulted in "Native Injection is NOT supported for @BeforeSuite"
 * errors before any tests could start. Copying the implementation locally
 * allows us to keep all existing behaviour while correcting the hook
 * signature.
 */


public class BaseTest  {

    protected WebDriver driver;
    public static final ThreadLocal<String> appName = ThreadLocal.withInitial(() -> null);
    public static final ThreadLocal<String> method_name = ThreadLocal.withInitial(() -> null);
    public static final ThreadLocal<String> browserName = ThreadLocal.withInitial(() -> null);
    public static final ThreadLocal<String> sheet_name = ThreadLocal.withInitial(() -> null);
    public static final ThreadLocal<XLSReader> datatable = ThreadLocal.withInitial(() -> null);
    public static final ThreadLocal<Integer> currentRow = ThreadLocal.withInitial(() -> null);

    protected DriverContext driverContext;

    static {
        ZipSecureFile.setMinInflateRatio(0.0d);
    }

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

    protected PageFactory getPageFactory() {
        return new PageFactory(driverContext);
    }

    protected DriverContext getDriverContext() {
        return driverContext;
    }
}

