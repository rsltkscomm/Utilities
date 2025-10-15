package zephyrIntegration;


import reporting.DetailedTestReporter;
import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.TestExecution;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JiraZephyrClient {

    private static String ZEPHYR_API_KEY;
    private static String PROJECT_KEY;
    private static String TEST_CYCLE_KEY;

    public JiraZephyrClient() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            ZEPHYR_API_KEY = prop.getProperty("ZEPHYR_API_KEY");
            PROJECT_KEY = prop.getProperty("PROJECT_KEY");
            TEST_CYCLE_KEY = prop.getProperty("TEST_CYCLE_ID");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    // ------------------------------------------------------------------
    // Update multiple test results
    // ------------------------------------------------------------------
    public void updateTestResults(List<TestResults> testResults) {
        for (TestResults result : testResults) {
            try {
                updateSingleTestResult(result);
            } catch (Exception e) {
                System.err.println("Error updating " + result.testCaseKey + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ------------------------------------------------------------------
    // Update single test result in Zephyr Scale Cloud
    // ------------------------------------------------------------------
    private void updateSingleTestResult(TestResults result) throws IOException {
        String apiUrl = "https://api.zephyrscale.smartbear.com/v2/testexecutions";

        JSONObject payload = new JSONObject();
        payload.put("projectKey", PROJECT_KEY);
        payload.put("testCycleKey", TEST_CYCLE_KEY);
        payload.put("testCaseKey", result.testCaseKey);
//        payload.put("statusName", result.isPass ? "Pass" : "Fail");
        if(result.isPass.equalsIgnoreCase("false")) {
			payload.put("statusName","Fail");
		}else if(result.isPass.equalsIgnoreCase("true")) {
			payload.put("statusName","Pass");
		}else {
			payload.put("statusName","Blocked");
		}

        // Add step results if available
        if (result.stepResults != null && !result.stepResults.isEmpty()) {
            JSONArray steps = new JSONArray();
            for (TestResults.TestStep step : result.stepResults) {
                JSONObject s = new JSONObject();
                s.put("statusName", step.isPass.equalsIgnoreCase("Pass") ? "Pass" : "Fail");
                steps.put(s);
            }
            payload.put("testStepResults", steps);
        }

        HttpsURLConnection conn = createZephyrConnection(apiUrl, "POST", "application/json");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);
       // System.out.println("Zephyr updated: " + result.testCaseKey + " | Status: " + (result.isPass.equalsIgnoreCase("true") ? "Pass" : "Fail"));
        System.out.println("Response: " + response);

    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private HttpsURLConnection createZephyrConnection(String apiUrl, String method, String contentType) throws IOException {
        URL url = new URL(apiUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true); // MUST be true before getOutputStream
        conn.setRequestProperty("Authorization", "Bearer " + ZEPHYR_API_KEY);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String readResponse(HttpsURLConnection conn) throws IOException {
        InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    
    
    public void zephyrUpdater() {
    	List<TestExecution> executions = DetailedTestReporter.getTestExecutions();
        JiraZephyrClient client = new JiraZephyrClient();
        ArrayList<TestResults> TestResults = new ArrayList<>();
        for (TestExecution t : executions) {
        	String testcaseID = t.getTestCaseId();
            if (t.getStatus() == ExecutionStatus.FAIL) {
//            	TestResult tr1 = new TestResult(testcaseID, false, null);
            	TestResults.add(new TestResults(testcaseID, "false", null));
            }else if (t.getStatus() == ExecutionStatus.PASS) {
            	TestResults.add(new TestResults(testcaseID, "true", null));
            }else {
            	TestResults.add(new TestResults(testcaseID, "Skipped", null));
            }
            client.updateTestResults(TestResults);
            
        }
    }
}
    
    class TestResults {
        public String testCaseKey;             // e.g., RS-T524
        public String isPass;                 // overall test status
        public List<TestStep> stepResults;     // individual step results
        public File screenshotFile;            // screenshot path
        public File logFile;                   // log file path

        public TestResults(String testCaseKey, String isPass,
                          List<TestStep> stepResults) {
            this.testCaseKey = testCaseKey;
            this.isPass = isPass;
            this.stepResults = stepResults;
      
        }

        static class TestStep {
            public String stepName;
            public String isPass;

            public TestStep(String stepName, String isPass) {
                this.stepName = stepName;
                this.isPass = isPass;
            }
        }
}