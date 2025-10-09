package testManagement;


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

public class JiraZephyrClient {

    private static String JIRA_BASE_URL;
    private static String JIRA_EMAIL;
    private static String JIRA_API_KEY;
    private static String ZEPHYR_API_KEY;
    private static String PROJECT_KEY;
    private static String TEST_CYCLE_KEY;
    private static boolean REPORT_BUG;

    public JiraZephyrClient() {
        loadConfig();
    }

    private void loadConfig() {
            JIRA_BASE_URL = System.getProperty("JIRA_BASE_URL");
            JIRA_EMAIL = System.getProperty("JIRA_EMAIL");
            JIRA_API_KEY = System.getProperty("JIRA_API_KEY");
            ZEPHYR_API_KEY = System.getProperty("ZEPHYR_API_KEY");
            PROJECT_KEY = System.getProperty("PROJECT_KEY");
            TEST_CYCLE_KEY = System.getProperty("TEST_CYCLE_ID");
            REPORT_BUG = "No".equalsIgnoreCase(System.getProperty("ReportBug"));

    }

    // ------------------------------------------------------------------
    // Update multiple test results
    // ------------------------------------------------------------------
    public void updateTestResults(List<TestResult> testResults) {
        for (TestResult result : testResults) {
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
    private void updateSingleTestResult(TestResult result) throws IOException {
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
            for (TestResult.TestStep step : result.stepResults) {
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

        // Create Jira bug if failed and reporting enabled
        if (!result.isPass.equalsIgnoreCase("Fail") && REPORT_BUG) {
            createJiraBug(result);
        }
    }

    // ------------------------------------------------------------------
    // Create Jira Bug
    // ------------------------------------------------------------------
    private void createJiraBug(TestResult result) throws IOException {
        String apiUrl = JIRA_BASE_URL + "/rest/api/3/issue";

        JSONObject fields = new JSONObject();
        fields.put("summary", "Automation Bug - " + result.testCaseKey);
        fields.put("description", "Automated test failed for " + result.testCaseKey + ". See attached logs/screenshots.");
        fields.put("project", new JSONObject().put("key", PROJECT_KEY));
        fields.put("issuetype", new JSONObject().put("name", "Bug"));

        JSONObject payload = new JSONObject();
        payload.put("fields", fields);

        HttpsURLConnection conn = createJiraConnection(apiUrl, "POST");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);
        if (responseCode == 201) {
            JSONObject json = new JSONObject(response);
            String bugKey = json.getString("key");
            System.out.println("Jira bug created: " + bugKey);
            attachFilesToBug(bugKey, result);
        } else {
            System.out.println("Failed to create Jira bug. Response: " + response);
        }
    }

    // ------------------------------------------------------------------
    // Attach screenshot/log files to Jira Bug
    // ------------------------------------------------------------------
    private void attachFilesToBug(String issueKey, TestResult result) throws IOException {
        if ((result.screenshotFile == null || !result.screenshotFile.exists()) &&
                (result.logFile == null || !result.logFile.exists())) return;

        String apiUrl = JIRA_BASE_URL + "/rest/api/3/issue/" + issueKey + "/attachments";
        HttpsURLConnection conn = createJiraConnection(apiUrl, "POST");
        conn.setRequestProperty("X-Atlassian-Token", "no-check");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----Boundary");

        try (OutputStream os = conn.getOutputStream()) {
            if (result.screenshotFile != null && result.screenshotFile.exists()) {
                writeMultipartFile(os, result.screenshotFile);
            }
            if (result.logFile != null && result.logFile.exists()) {
                writeMultipartFile(os, result.logFile);
            }
            os.write("------Boundary--".getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("Attachments uploaded. Response code: " + conn.getResponseCode());
    }

    private void writeMultipartFile(OutputStream os, File file) throws IOException {
        os.write(("------Boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" +
                file.getName() + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(os);
        }

        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
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

    private HttpsURLConnection createJiraConnection(String apiUrl, String method) throws IOException {
        URL url = new URL(apiUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true); // MUST be true before getOutputStream
        String auth = JIRA_EMAIL + ":" + JIRA_API_KEY;
        String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encoded);
        return conn;
    }

    private String readResponse(HttpsURLConnection conn) throws IOException {
        InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    
    
    public static void zephyrUpdater() {
    	List<TestExecution> executions = DetailedTestReporter.getTestExecutions();
        JiraZephyrClient client = new JiraZephyrClient();
        ArrayList<TestResult> TestResults = new ArrayList<>();
        for (TestExecution t : executions) {
        	String testcaseID = t.getTestCaseId();
            if (t.getStatus() == ExecutionStatus.FAIL) {
//            	TestResult tr1 = new TestResult(testcaseID, false, null);
            	TestResults.add(new TestResult(testcaseID, "false", null));
            }else if (t.getStatus() == ExecutionStatus.PASS) {
            	TestResults.add(new TestResult(testcaseID, "true", null));
            }else {
            	TestResults.add(new TestResult(testcaseID, "Skipped", null));
            }
            client.updateTestResults(TestResults);
            
        }
    }
}
    
    class TestResult {
        public String testCaseKey;             // e.g., RS-T524
        public String isPass;                 // overall test status
        public List<TestStep> stepResults;     // individual step results
        public File screenshotFile;            // screenshot path
        public File logFile;                   // log file path

        public TestResult(String testCaseKey, String isPass,
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
