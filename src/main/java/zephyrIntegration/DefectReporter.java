package zephyrIntegration;

import org.json.JSONArray;
import org.json.JSONObject;

import reporting.DetailedTestReporter;
import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.StepStatus;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

/**
 * Dedicated utility class for reporting defects in Jira when test cases fail. This class provides comprehensive bug reporting functionality with file
 * attachments, detailed descriptions, and proper Jira integration.
 */
public class DefectReporter
{

	private static String JIRA_BASE_URL;
	private static String JIRA_EMAIL;
	private static String JIRA_API_KEY;
	private static String PROJECT_KEY;
	private static boolean REPORT_BUG;
	private static String BUG_ASSIGNEE;
	private static String BUG_PRIORITY;
	private static String BUG_COMPONENT;
	private static String SCREENSHOT_PATH;
	private static String LOG_PATH;
	private static String TEST_CASE_ID_FIELD;
	private static boolean CHECK_DUPLICATES;
	private static String DUPLICATE_STRATEGY;

	private DuplicateDefectChecker duplicateChecker;

	public DefectReporter() {
		loadConfig();

		// Initialize duplicate checker if enabled
		if (CHECK_DUPLICATES)
		{
			DuplicateDefectChecker.DuplicateStrategy strategy = getDuplicateStrategy();
			duplicateChecker = new DuplicateDefectChecker(strategy);
		}
	}

	/**
	 * Load configuration from config.properties file
	 */
	private void loadConfig()
	{
		try
		{
			JIRA_BASE_URL = System.getProperty("JIRA_BASE_URL");
			JIRA_EMAIL = System.getProperty("JIRA_EMAIL");
			JIRA_API_KEY = System.getProperty("JIRA_API_KEY");
			PROJECT_KEY = System.getProperty("PROJECT_KEY");
			String reportDefectValue = System.getProperty("reportdefect", "No");
			REPORT_BUG = "Yes".equalsIgnoreCase(reportDefectValue);
			BUG_ASSIGNEE = System.getProperty("BUG_ASSIGNEE", "");
			BUG_PRIORITY = System.getProperty("BUG_PRIORITY", "High");
			BUG_COMPONENT = System.getProperty("BUG_COMPONENT", "Automation Testing");
			SCREENSHOT_PATH = System.getProperty("SCREENSHOT_PATH", "./screenshots/");
			LOG_PATH = System.getProperty("LOG_PATH", "./logs/");
			TEST_CASE_ID_FIELD = System.getProperty("TEST_CASE_ID_FIELD", "customfield_10001");
			String val = System.getProperty("CHECK_DUPLICATES", "true");
			CHECK_DUPLICATES = "true".equalsIgnoreCase(System.getProperty("CHECK_DUPLICATES", "true"));
			DUPLICATE_STRATEGY = System.getProperty("DUPLICATE_STRATEGY", "HYBRID");
		} catch (Exception e)
		{
			throw new RuntimeException("Failed to load config.properties", e);
		}
	}

	/**
	 * Get duplicate detection strategy from configuration
	 */
	private DuplicateDefectChecker.DuplicateStrategy getDuplicateStrategy()
	{
		try
		{
			return DuplicateDefectChecker.DuplicateStrategy.valueOf(DUPLICATE_STRATEGY.toUpperCase());
		} catch (Exception e)
		{
			System.out.println("⚠️  Invalid duplicate strategy: " + DUPLICATE_STRATEGY + ", using HYBRID");
			return DuplicateDefectChecker.DuplicateStrategy.HYBRID;
		}
	}

	/**
	 * Main method to report a defect when a test case fails
	 * 
	 * @param testCaseKey     The Jira test case key (e.g., "RS-T81")
	 * @param testCaseName    The name/description of the test case
	 * @param failureReason   The reason why the test failed
	 * @param stepResults     List of test step results for detailed failure analysis
	 * @param screenshotFile  Optional screenshot file to attach
	 * @param logFile         Optional log file to attach
	 * @param browserLogsFile Optional browser console logs file to attach
	 * @param harFile         Optional network traffic HAR file to attach
	 * @return The created bug key if successful, null if failed
	 */
	public String reportDefect(String testCaseKey, String testCaseName, String failureReason, java.util.List<DetailedTestReporter.TestStep> stepResults, File screenshotFile, File logFile, File browserLogsFile, File harFile)
	{

		if (!REPORT_BUG)
		{
			System.out.println("⚠️  Bug reporting is disabled in configuration");
			return null;
		}

		try
		{
			System.out.println("\n" + "=".repeat(80));
			System.out.println("🐛 REPORTING DEFECT FOR FAILED TEST CASE");
			System.out.println("=".repeat(80));
			System.out.println("Test Case: " + testCaseKey);
			System.out.println("Test Name: " + testCaseName);
			System.out.println("Failure Reason: " + failureReason);

			// Check for duplicate defects if enabled
			if (CHECK_DUPLICATES && duplicateChecker != null)
			{
				DuplicateDefectChecker.DuplicateCheckResult duplicateCheck = duplicateChecker.checkForDuplicate(testCaseKey, failureReason);

				if (duplicateCheck.isDuplicate())
				{
					System.out.println("\n" + "⚠".repeat(80));
					System.out.println("⚠️  DUPLICATE DEFECT DETECTED - SKIPPING CREATION");
					System.out.println("⚠".repeat(80));
					System.out.println("✅ Existing Defect: " + duplicateCheck.getExistingDefectId());
					System.out.println("🔗 Jira URL: " + duplicateCheck.getJiraUrl(JIRA_BASE_URL));
					System.out.println("💡 Message: " + duplicateCheck.getMessage());
					System.out.println("⚠".repeat(80));

					// Return existing defect ID instead of creating new one
					return duplicateCheck.getExistingDefectId();
				}
			}

			String bugKey = createJiraBug(testCaseKey, testCaseName, failureReason, stepResults);

			if (bugKey != null)
			{
				// Attach files if provided
				if (screenshotFile != null || logFile != null || browserLogsFile != null || harFile != null)
				{
					attachFilesToBug(bugKey, screenshotFile, logFile, browserLogsFile, harFile);
				}

				// Link the bug to the test case
				linkBugToTestCase(bugKey, testCaseKey);

				System.out.println("✅ DEFECT REPORTED SUCCESSFULLY!");
				System.out.println("🎯 Bug Key: " + bugKey);
				System.out.println("🔗 Bug URL: " + JIRA_BASE_URL + "/browse/" + bugKey);

				// Update duplicate cache if checker is enabled
				if (CHECK_DUPLICATES && duplicateChecker != null)
				{
					duplicateChecker.updateLocalCache(testCaseKey, failureReason, bugKey);
				}
			}

			return bugKey;

		} catch (Exception e)
		{
			System.err.println("❌ FAILED TO REPORT DEFECT: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Simplified method to report defect with minimal parameters
	 * 
	 * @param testCaseKey   The Jira test case key
	 * @param failureReason The reason why the test failed
	 * @return The created bug key if successful, null if failed
	 */
	public String reportDefect(String testCaseKey, String failureReason)
	{
		return reportDefect(testCaseKey, "Automated Test Case", failureReason, null, null, null, null, null);
	}

	/**
	 * Backward compatibility - without browser logs
	 */
	public String reportDefect(String testCaseKey, String testCaseName, String failureReason, java.util.List<DetailedTestReporter.TestStep> stepResults, File screenshotFile, File logFile)
	{
		return reportDefect(testCaseKey, testCaseName, failureReason, stepResults, screenshotFile, logFile, null, null);
	}

	/**
	 * Backward compatibility - without HAR file
	 */
	public String reportDefect(String testCaseKey, String testCaseName, String failureReason, java.util.List<DetailedTestReporter.TestStep> stepResults, File screenshotFile, File logFile, File browserLogsFile)
	{
		return reportDefect(testCaseKey, testCaseName, failureReason, stepResults, screenshotFile, logFile, browserLogsFile, null);
	}

	/**
	 * Create a Jira bug using the REST API
	 */
	private String createJiraBug(String testCaseKey, String testCaseName, String failureReason, java.util.List<DetailedTestReporter.TestStep> stepResults) throws IOException
	{

	    String apiUrl = JIRA_BASE_URL + "/rest/api/3/issue";

	    JSONObject fields = new JSONObject();
	    fields.put("summary", "AUTOMATION BUG: " + testCaseKey + " - " + failureReason);
	    fields.put("description", createBugDescription(testCaseKey, testCaseName, failureReason, stepResults));
	    fields.put("project", new JSONObject().put("key", PROJECT_KEY));
	    fields.put("issuetype", new JSONObject().put("name", "Bug"));

	    // Set Test Case ID field (URL field)
	    // Pass the full URL to the test case in Jira
	    String testCaseUrl = createTestCaseUrl(testCaseKey);
	    fields.put(TEST_CASE_ID_FIELD, testCaseUrl);

	    // Set priority
	    JSONObject priority = new JSONObject();
	    priority.put("name", BUG_PRIORITY);
	    fields.put("priority", priority);

	    // Set component if specified
	    if (BUG_COMPONENT != null && !BUG_COMPONENT.isEmpty())
	    {
	        JSONObject component = new JSONObject();
	        component.put("name", BUG_COMPONENT);
	        fields.put("components", new org.json.JSONArray().put(component));
	    }

	    // Set assignee if specified
	    if (BUG_ASSIGNEE != null && !BUG_ASSIGNEE.isEmpty())
	    {
	        JSONObject assignee = new JSONObject();
	        assignee.put("emailAddress", BUG_ASSIGNEE);
	        fields.put("assignee", assignee);
	    }

	    // Set labels for easy identification
	    fields.put("labels", new org.json.JSONArray().put(""));
	    
	    // ⭐ CHANGE 1: Use the correct custom field ID (customfield_10375)
	    // ⭐ CHANGE 2: Use the required format: {"value": "Version Name"}
	    String releaseVersion = System.getProperty("ReportVersion", "v1.0"); // fallback default
	    
	    JSONObject reportedVersion = new JSONObject();
	    reportedVersion.put("value", releaseVersion); // Use "value" key for single-select custom field
	    
	    fields.put("customfield_10375", reportedVersion); // Use the correct Custom Field ID
	    // Note: The previous lines using 'versions' and 'JSONArray' are removed/replaced.


	    JSONObject payload = new JSONObject();
	    payload.put("fields", fields);

	    System.out.println("🔧 Creating Jira Bug...");
	    System.out.println("   📋 Summary: " + fields.getString("summary"));
	    System.out.println("   🎯 Project: " + PROJECT_KEY);
	    System.out.println("   ⚡ Priority: " + BUG_PRIORITY);

	    HttpsURLConnection conn = createJiraConnection(apiUrl, "POST");

	    try (OutputStream os = conn.getOutputStream())
	    {
	        os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
	    }

	    int responseCode = conn.getResponseCode();
	    String response = readResponse(conn);

	    if (responseCode == 201)
	    {
	        JSONObject json = new JSONObject(response);
	        String bugKey = json.getString("key");
	        System.setProperty(testCaseName,bugKey);
	        System.out.println("✅ Jira bug created successfully: " + bugKey);
	        return bugKey;
	    } else
	    {
	        System.err.println("❌ Failed to create Jira bug. Response Code: " + responseCode);
	        System.err.println("❌ Response: " + response);
	        return null;
	    }
	}

	/**
	 * Create detailed bug description with test failure information Uses Atlassian Document Format (ADF) for Jira API v3
	 */
	private Object createBugDescription(String testCaseKey, String testCaseName, String failureReason, java.util.List<DetailedTestReporter.TestStep> stepResults)
	{

		// Create ADF (Atlassian Document Format) description
		JSONObject adfDoc = new JSONObject();
		adfDoc.put("version", 1);
		adfDoc.put("type", "doc");

		JSONArray content = new JSONArray();

		// Header
//		content.put(createAdfHeading("Test Case Failure Report", 2));
//		content.put(createAdfParagraph("This bug was automatically generated due to test case failure during automation execution."));

		// Test Information
		content.put(createAdfHeading("Test Information", 3));
		content.put(createAdfParagraph("Test Case Key: " + testCaseKey));
		content.put(createAdfParagraph("Test Case Name: " + testCaseName));
		content.put(createAdfParagraph("Failure Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
		content.put(createAdfParagraph("UserName: "+System.getProperty("UserName","")));
		content.put(createAdfParagraph("Environment: "+System.getProperty("Environment","QA")));

		// Failure Details
		content.put(createAdfHeading("Failure Details", 3));
		content.put(createAdfParagraph("Failure Reason: " + failureReason));

		// Steps to Replicate (REQUIRED SECTION)
		if (stepResults != null && !stepResults.isEmpty())
		{
			content.put(createAdfHeading("Steps to Replicate", 3));
			content.put(createAdfParagraph("Follow these steps to replicate the issue:"));

			int stepNumber = 1;
			for (DetailedTestReporter.TestStep step : stepResults)
			{
				String stepName = step.getAction() != null ? step.getAction() : "Step " + stepNumber;
				content.put(createAdfParagraph(stepNumber + ". " + stepName));
				stepNumber++;
			}

			// Add expected and actual results
			content.put(createAdfParagraph(""));
			content.put(createAdfParagraph("Expected Result: All test steps should pass"));

			// Find first failed step for actual result
			String actualResultForDefect = "Test execution failed";
			for (DetailedTestReporter.TestStep step : stepResults)
			{
				if (step.getStatus() != StepStatus.PASS && step.getActualResult() != null)
				{
					actualResultForDefect = step.getActualResult();
					break;
				}
			}
			content.put(createAdfParagraph("Actual Result: " + actualResultForDefect));
		}

		// Detailed Test Step Results
		if (stepResults != null && !stepResults.isEmpty())
		{
			content.put(createAdfHeading("Detailed Test Step Results", 3));

			int stepNumber = 1;
			for (DetailedTestReporter.TestStep step : stepResults)
			{
				String status = step.getStatus() == StepStatus.PASS ? "✓ PASS" : "✗ FAIL";
				String stepName = step.getAction() != null ? step.getAction() : "Step " + stepNumber;
				String actualResult = step.getActualResult() != null ? step.getActualResult() : "No description";

				content.put(createAdfParagraph(stepNumber + ". " + status + ": " + stepName));
				if (step.getStatus() != StepStatus.PASS)
				{
					content.put(createAdfParagraph("   → Error: " + actualResult));
				}
				stepNumber++;
			}
		}

		// Additional Information
		content.put(createAdfHeading("Additional Information", 3));
//		content.put(createAdfParagraph("Automation Framework: Selenium + TestNG"));
		content.put(createAdfParagraph("Browser: Chrome (Latest)"));
		content.put(createAdfParagraph("Operating System: Windows 10"));

		// Attachments Note
		content.put(createAdfHeading("Attachments", 3));
		content.put(createAdfParagraph("Please check the attached files for screenshots, logs, and error stack traces."));

		// Footer
		content.put(createAdfParagraph("---"));
		content.put(createAdfParagraph("This bug was automatically generated by the automation framework."));

		adfDoc.put("content", content);
		return adfDoc;
	}

	/**
	 * Create ADF heading node
	 */
	private JSONObject createAdfHeading(String text, int level)
	{
		JSONObject heading = new JSONObject();
		heading.put("type", "heading");
		heading.put("attrs", new JSONObject().put("level", level));

		JSONArray content = new JSONArray();
		content.put(new JSONObject().put("type", "text").put("text", text));

		heading.put("content", content);
		return heading;
	}

	/**
	 * Create ADF paragraph node
	 */
	private JSONObject createAdfParagraph(String text)
	{
		JSONObject paragraph = new JSONObject();
		paragraph.put("type", "paragraph");

		JSONArray content = new JSONArray();
		content.put(new JSONObject().put("type", "text").put("text", text));

		paragraph.put("content", content);
		return paragraph;
	}

	/**
	 * Link the created bug to the original test case
	 */
	private void linkBugToTestCase(String bugKey, String testCaseKey)
	{
		try
		{
			System.out.println("🔗 Linking bug " + bugKey + " to test case " + testCaseKey);

			String apiUrl = JIRA_BASE_URL + "/rest/api/3/issueLink";

			JSONObject payload = new JSONObject();
			payload.put("type", new JSONObject().put("name", "Relates"));
			payload.put("inwardIssue", new JSONObject().put("key", bugKey));
			payload.put("outwardIssue", new JSONObject().put("key", testCaseKey));

			HttpsURLConnection conn = createJiraConnection(apiUrl, "POST");

			try (OutputStream os = conn.getOutputStream())
			{
				os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
			}

			int responseCode = conn.getResponseCode();
			if (responseCode == 201)
			{
				System.out.println("✅ Successfully linked bug to test case");
			} else
			{
				System.out.println("⚠️  Failed to link bug to test case (Response: " + responseCode + ")");
			}

		} catch (Exception e)
		{
			System.out.println("⚠️  Failed to link bug to test case: " + e.getMessage());
		}
	}

	/**
	 * Attach files (screenshots, logs, browser logs, HAR files) to the created bug
	 */
	private void attachFilesToBug(String bugKey, File screenshotFile, File logFile, File browserLogsFile, File harFile)
	{
		try
		{
			System.out.println("📎 Attaching files to bug " + bugKey + "...");

			String apiUrl = JIRA_BASE_URL + "/rest/api/3/issue/" + bugKey + "/attachments";

			// Attach screenshot
			if (screenshotFile != null && screenshotFile.exists())
			{
				attachFileToBug(apiUrl, screenshotFile);
				System.out.println("📸 Attached screenshot: " + screenshotFile.getName());
			}

			// Attach log file
			if (logFile != null && logFile.exists())
			{
				attachFileToBug(apiUrl, logFile);
				System.out.println("📄 Attached log file: " + logFile.getName());
			}

			// Attach browser console logs
			if (browserLogsFile != null && browserLogsFile.exists())
			{
				attachFileToBug(apiUrl, browserLogsFile);
				System.out.println("📋 Attached browser logs: " + browserLogsFile.getName());
			}

			// Attach network traffic HAR file
			if (harFile != null && harFile.exists())
			{
				attachFileToBug(apiUrl, harFile);
				System.out.println("🌐 Attached network traffic: " + harFile.getName());
			}

		} catch (Exception e)
		{
			System.out.println("⚠️  Failed to attach files: " + e.getMessage());
		}
	}

	/**
	 * Attach a single file to the bug
	 */
	private void attachFileToBug(String apiUrl, File file) throws IOException
	{
		HttpsURLConnection conn = createJiraConnection(apiUrl, "POST");
		conn.setRequestProperty("X-Atlassian-Token", "no-check");
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----Boundary");

		try (OutputStream os = conn.getOutputStream())
		{
			writeMultipartFile(os, file);
			os.write("------Boundary--\r\n".getBytes(StandardCharsets.UTF_8));
		}

		int responseCode = conn.getResponseCode();
		if (responseCode == 200)
		{
			System.out.println("✅ File attached successfully: " + file.getName());
		} else
		{
			System.out.println("⚠️  Failed to attach file " + file.getName() + " (Response: " + responseCode + ")");
		}
	}

	/**
	 * Write multipart file data to output stream
	 */
	private void writeMultipartFile(OutputStream os, File file) throws IOException
	{
		String header = "------Boundary\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" + "Content-Type: application/octet-stream\r\n\r\n";

		os.write(header.getBytes(StandardCharsets.UTF_8));

		try (FileInputStream fis = new FileInputStream(file))
		{
			fis.transferTo(os);
		}

		os.write("\r\n".getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Create Jira API connection with proper authentication
	 */
	private HttpsURLConnection createJiraConnection(String apiUrl, String method) throws IOException
	{
		URL url = new URL(apiUrl);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setDoOutput(true);

		// Basic authentication
		String auth = JIRA_EMAIL + ":" + JIRA_API_KEY;
		String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
		conn.setRequestProperty("Authorization", "Basic " + encoded);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");

		// Set timeouts (configurable from properties)
		conn.setConnectTimeout(Integer.parseInt(System.getProperty("CONNECTION_TIMEOUT_MS")));
		conn.setReadTimeout(Integer.parseInt(System.getProperty("READ_TIMEOUT_MS")));
		return conn;
	}

	/**
	 * Read response from HTTP connection
	 */
	private String readResponse(HttpsURLConnection conn) throws IOException
	{
		InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
		{
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
			{
				response.append(line);
			}
			return response.toString();
		}
	}

	/**
	 * Create URL for the test case in Jira/Zephyr
	 */
	private String createTestCaseUrl(String testCaseKey)
	{
		// Create URL to the test case in Jira
		// This assumes your test cases are stored in Jira
		return JIRA_BASE_URL + "/browse/" + testCaseKey;
	}

	/**
	 * Utility method to create screenshot file path
	 */
	public static File createScreenshotFile(String testCaseKey, String timestamp)
	{
		String fileName = testCaseKey + "_failure_" + timestamp + ".png";
		return new File(SCREENSHOT_PATH + fileName);
	}

	/**
	 * Utility method to create log file path
	 */
	public static File createLogFile(String testCaseKey, String timestamp)
	{
		String fileName = testCaseKey + "_execution_" + timestamp + ".log";
		return new File(LOG_PATH + fileName);
	}

	/**
	 * Get current timestamp for file naming
	 */
	public static String getCurrentTimestamp()
	{
		return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	}
}
