package listeners;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener
{

	public static String curr_Dir = System.getProperty("user.dir");
	private static final SimpleDateFormat df = new SimpleDateFormat("yyMMdd_HHmmss");
	private static final Map<String, ITestResult> finalResults = new LinkedHashMap<>();
	public static final List<String[]> testResults = new ArrayList<>();
	private static String finalSheetName = null;
	private static String outputPath = null;

	@Override
	public void onStart(ITestContext context)
	{
		if (finalSheetName == null)
		{
			String suiteName = context.getSuite().getName();
			String safeSuiteName = suiteName.replaceAll("[\\\\/*?\\[\\]:]", "_");

			String sheetBaseName = getShortSheetName(suiteName);
			String timestamp = df.format(new Date());
			finalSheetName = sheetBaseName + "_" + timestamp;

			String directoryPath = "C:\\AutomationResults";
			File directory = new File(directoryPath);
			if (!directory.exists())
			{
				directory.mkdirs();
			}
			outputPath =System.getProperty("user.dir") +"\\TestSummary.xlsx";
			System.setProperty("ResulExcelPath", outputPath);
		}
		testResults.clear();
		finalResults.clear();
	}

	@Override
	public void onTestSuccess(ITestResult result)
	{
		finalResults.put(getKey(result), result);
	}

	@Override
	public void onTestFailure(ITestResult result)
	{
		finalResults.put(getKey(result), result);
	}

	@Override
	public void onTestSkipped(ITestResult result)
	{
		finalResults.putIfAbsent(getKey(result), result);
	}

	private String getKey(ITestResult result)
	{
		return result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
	}

	@Override
	public void onFinish(ITestContext context)
	{
		for (ITestResult result : finalResults.values())
		{
			String status = switch (result.getStatus())
			{
			case ITestResult.SUCCESS -> "PASS";
			case ITestResult.FAILURE -> "FAIL";
			case ITestResult.SKIP -> "SKIP";
			default -> "UNKNOWN";
			};
			testResults.add(buildRow(result, status));
		}

		// ✅ Write to a single consistent sheet
		TestExecutionExcelReport.writeResultsToExcel(outputPath, testResults, finalSheetName);
	}

	private String getShortSheetName(String suiteName)
	{
		if (suiteName.contains("AudienceRegression"))
		{
			return "Aud";
		}
		if (suiteName.contains("Account"))
		{
			return "Account";
		}
		if (suiteName.contains("Communication"))
		{
			return "Comm";
		}
		if (suiteName.contains("Smoke"))
		{
			return "SmokeTest";
		}
		if (suiteName.contains("Preferences"))
		{
			return "Pref";
		}
		if (suiteName.contains("Analytics"))
		{
			return "Analytic";
		}
		return "TestSuite";
	}

	private String[] buildRow(ITestResult result, String status)
	{
		String testName = result.getName();
		String description = result.getMethod().getDescription() != null ? result.getMethod().getDescription() : "";
		return new String[] { testName, description, status };
	}
}
