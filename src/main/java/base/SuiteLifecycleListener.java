package base;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.testng.ISuite;
import org.testng.ISuiteListener;

import constants.FrameworkConstants;
import reporting.DetailedTestReporter;
import reporting.DetailedTestReporter.TestExecution;
import reporting.ExcelReportGenerator;
import reporting.ExtentManager;
import reporting.TestLogManager;
import seleniumUtils.DateUtils;

public class SuiteLifecycleListener implements ISuiteListener {

    public static String currentDate;
    public static String endDateTime;

    @Override
    public void onStart(ISuite suite) {
    	 ExtentManager.initReports();
         TestLogManager.reloadConfiguration();

         if (GridManager.checkIfGrid(System.getProperty("Browser"))) {
             AutoDockerInstallAndRun.dockerInstallAndRun();
             DockerManager.dockerContainterUp();
         }
         
         String suiteName = suite.getName();
         String timestamp = DateUtils.getCurrentDate("dd-MMM-yyyy_HH-mm-ss");
         System.setProperty("LT_BUILD", suiteName + "_Build_" + timestamp);

         currentDate = DateUtils.getCurrentDate("dd-MMM-yyyy HH:mm");
         TestLogManager.info("==== TEST SUITE STARTED ====");
    }
    
    @Override
    public void onFinish(ISuite suite) {
    	try {
            ExtentManager.flushReports();

            if (GridManager.isGrid.get().equals(true)) {
                DockerManager.dockerContainterDown();
            }
            
            List<TestExecution> testExecutions = DetailedTestReporter.getTestExecutions();
            
            
            ExcelReportGenerator.writeTestExecutionsToExcel(
            		FrameworkConstants.ONEDRIVE_BASE_PATH,
            		"Daily,Release,Account",
            		System.getProperty("DateWiseReport") + "," +
                            System.getProperty("ReleasewiseReport") + "," +
                            System.getProperty("AccountWiseReport"),
                            System.getProperty("ReleaseVersion"),
                            System.getProperty("Account") + "_" + System.getProperty("Environment"),
                            System.getProperty("SuiteName"),
                    testExecutions
            );
            
            ExtentManager.openExtentReport();
            endDateTime = DateUtils.getCurrentDate("HH:mm");

            TestLogManager.info("==== TEST SUITE FINISHED ====");

        } catch (Exception e) {
            TestLogManager.warning("AfterSuite error: " + e.getMessage());
        }
    }

    /**
     * Open the given URL in the system default browser, using a background daemon thread.
     * Attempts Desktop.browse() first and falls back to platform-specific commands.
     */
    private void openUrlInBackground(final String url) {
        Thread t = new Thread(() -> {
            try {
                openUrl(url);
                TestLogManager.info("Opened URL: " + url);
            } catch (Exception e) {
                TestLogManager.warning("Could not open URL " + url + " : " + e.getMessage());
            }
        }, "Open-URL-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void openUrl(String url) throws IOException, URISyntaxException {
        // Try Desktop API first
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception e) {
            // fall back
        }

        // OS-specific fallback
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows
            Runtime.getRuntime().exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", url});
        } else if (os.contains("mac")) {
            // macOS
            Runtime.getRuntime().exec(new String[] {"open", url});
        } else {
            // Linux / Unix
            Runtime.getRuntime().exec(new String[] {"xdg-open", url});
        }
    }
}
