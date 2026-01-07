package reporting;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import performanceTracker.PerformanceTracker;
import testManagement.JiraZephyrClient;
import zephyrIntegration.DefectReportingDemo;

/**
 * Custom TestNG Listener for generating HTML & JSON based reports.
 */
public class NewCutsomHTMLReport implements ITestListener, ISuiteListener {

    private final List<String> passMethods = new LinkedList<>();
    private final List<String> failMethods = new LinkedList<>();
    private final List<String> noRunMethods = new LinkedList<>();

    public static long startTime;
    public static String suiteStartTime;

    /* ===============================
       SUITE START
       =============================== */
    @Override
    public void onStart(ISuite suite) {
        suiteStartTime = currentTime();
        startTime = System.currentTimeMillis();

        DetailedTestReporter.createDetailReport();
        System.out.println("✅ Test suite started: " + suite.getName());
    }

    /* ===============================
       TEST EVENTS
       =============================== */
    @Override
    public void onTestSuccess(ITestResult result) {
        passMethods.add(System.getProperty("method_name"));
        NewSummaryReportGenerator.recordTestResult(result.getName(), "PASS");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        failMethods.add(System.getProperty("method_name"));
        NewSummaryReportGenerator.recordTestResult(result.getName(), "FAIL");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        noRunMethods.add(System.getProperty("method_name"));
        NewSummaryReportGenerator.recordTestResult(result.getName(), "SKIPPED");
    }

    /* ===============================
       SUITE FINISH
       =============================== */
    @Override
    public void onFinish(ISuite suite) {

        // --- External Integrations ---
        if ("yes".equalsIgnoreCase(System.getProperty("REPORT_BUG"))) {
            new DefectReportingDemo().defectReporting();
        }

        if ("yes".equalsIgnoreCase(System.getProperty("UPDATE_ZEPHYR_EXECUTION"))) {
            new JiraZephyrClient().zephyrUpdater();
        }

        // Generate detailed report first
//        DetailedTestReporter.getReport().generateReport();

        // Clean skipped list
//        filterCount(passMethods, failMethods, noRunMethods);

        // Aggregate summary stats
        NewSummaryReportGenerator.AggregatedStats agg =
                NewSummaryReportGenerator.aggregateStats();

        // Performance report (optional)
        if ("yes".equalsIgnoreCase(System.getProperty("performanceReport"))) {
            PerformanceTracker.generatePerformanceReportsForSuite(suite);
        }

        // ===============================
        // JSON → HTML REPORT FLOW
        // ===============================
        String reportJson = NewSummaryReportGenerator.generateReportJson(
                agg.totalPass,
                agg.totalFail,
                agg.totalSkip,
                String.valueOf(agg.totalDurationMillis),
                suiteStartTime
        );

        // Generate HTML from JSON
        NewSummaryReportGenerator.generateReportFromJson(reportJson);

        System.out.println("✅ Summary report generated successfully");
    }

    /* ===============================
       UTILS
       =============================== */
    private void filterCount(List<String> passMethod,
                             List<String> failMethod,
                             List<String> noRunMethod) {

        Set<String> passSet = new HashSet<>(passMethod);
        Set<String> failSet = new HashSet<>(failMethod);

        Iterator<String> iterator = noRunMethod.iterator();
        while (iterator.hasNext()) {
            String method = iterator.next();
            if (passSet.contains(method) || failSet.contains(method)) {
                iterator.remove();
            }
        }
    }

    /**
     * Load properties from object.properties inside JAR
     */
    private void loadPropertiesFromJar() {
        String resourcePath = "/object.properties";
        try (InputStream is = NewCutsomHTMLReport.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            Properties props = new Properties();
            props.load(is);
            props.forEach((k, v) -> System.setProperty(k.toString(), v.toString()));

        } catch (IOException e) {
            System.err.println("❌ Failed to load properties: " + e.getMessage());
        }
    }

    /**
     * Load properties from external file
     */
    public static void loadProperties(String propertyFilePath) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertyFilePath)) {
            props.load(fis);
            props.forEach((k, v) -> System.setProperty(k.toString(), v.toString()));
        } catch (Exception e) {
            System.err.println("❌ Property load error: " + e.getMessage());
        }
    }

    /**
     * Custom report path
     */
    public static String getCustomReportPath() {
        String userDir = System.getProperty("user.dir");
        String[] parts = userDir.split("\\\\");
        if (parts.length == 0) return userDir;
        return userDir.replace(parts[parts.length - 1], "CustomReport");
    }

    private String currentTime() {
        return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
                .format(new Date());
    }
}
