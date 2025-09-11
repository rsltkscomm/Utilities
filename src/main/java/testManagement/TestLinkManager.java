package testManagement;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.Platform;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;

public class TestLinkManager
{
	private static final String API_KEY = System.getProperty("API_KEY");
	private static final String API_URL = System.getProperty("API_URL");
	private static final String TEST_PROJECT_NAME = System.getProperty("ProductName");
	private static final String TEST_PLAN_NAME = System.getProperty("TEST_PLAN_NAME");
	private static final String BUILD_NAME = System.getProperty("BUILD_NAME");
	private static final Logger logger = Logger.getLogger(TestLinkManager.class.getName());

	private static TestLinkAPI testLinkAPI;

	static
	{
		try
		{
			testLinkAPI = new TestLinkAPI(new URL(API_URL), API_KEY);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void updateTestExecution(Map<String,String> finalResults)
	{
		if (System.getProperty("TESTLINK_UPDATE").toLowerCase().contains("yes"))
		{
			// Get Test Plan ID
			TestPlan testPlan = testLinkAPI.getTestPlanByName(TEST_PLAN_NAME, TEST_PROJECT_NAME);
			Integer testPlanId = testPlan.getId();

			Platform[] platforms = testLinkAPI.getTestPlanPlatforms(testPlanId);
			Integer platformId = null;
			String platformName = System.getProperty("platformName"); // Change this to your platform name

			for (Platform platform : platforms)
			{
				if (platform.getName().equalsIgnoreCase(platformName))
				{
					platformId = platform.getId();
					break;
				}
			}

			// Get Build ID
			Build[] builds = testLinkAPI.getBuildsForTestPlan(testPlanId);
			Integer buildId = null;
			for (Build build : builds)
			{
				if (build.getName().equals(BUILD_NAME))
				{
					buildId = build.getId();
					break;
				}
			}
			if (buildId == null)
			{
				throw new RuntimeException("Build not found: " + BUILD_NAME);
			}

//	        Platform[] platforms = testLinkAPI.getProjectPlatforms(testPlanId);

			// Loop through final results
			for (Map.Entry<String, String> entry : finalResults.entrySet())
			{
				String testCaseExternalId = entry.getKey();
				String statusString = entry.getValue();

				ExecutionStatus status = statusString.equalsIgnoreCase("PASS") ? ExecutionStatus.PASSED : ExecutionStatus.FAILED;

				String timeStamp = new SimpleDateFormat("yyMMdd_HHmmssSSS").format(new Date());
				System.out.println(timeStamp);

				try
				{
					// Fetch test case by external id + version (passing null for version)
					TestCase testCase = testLinkAPI.getTestCaseByExternalId(testCaseExternalId, null);
					Integer testCaseId = testCase.getId();

					Integer externalId1 = Integer.valueOf(testCaseExternalId.replaceAll("[^0-9]", ""));

					// Update execution
					testLinkAPI.reportTCResult(testCaseId, // Integer testCaseId
							externalId1, // Integer testCaseExternalId (you can pass null)
							testPlanId, // Integer testPlanId
							status, // ExecutionStatus status (PASS/FAIL)
							null, // List<TestCaseStepResult> steps (null if not needed)
							buildId, // Integer buildId
							BUILD_NAME, // String buildName
							"Executed by automation script", // String notes
							null, // Integer executionDuration (null if unknown)
							false, // Boolean guess
							null, // String bugId
							null, // Integer platformId
							platformName, // String platformName
							null, // Map<String, String> customFields
							false, // Boolean overwrite
							null, // String user
							null // String timestamp
					);

					System.out.println("Test case '" + testCaseExternalId + "' updated with status: " + status);
					logger.info("TestLink Update Success → Test Case: " + testCaseExternalId + " | Status: " + statusString);

				} catch (Exception e)
				{
					logger.severe("TestLink Update Failed → Test Case: " + testCaseExternalId + " | Error: " + e.getMessage());
				}
			}
		}
	}
}
