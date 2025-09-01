package reporting;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.aventstack.extentreports.AnalysisStrategy;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentKlovReporter;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import base.GridManager;
import constants.FrameworkConstants;

public class ExtentManager
{
	private static ExtentReports extent;
	private static Map<Long, ExtentTest> extentTestMap = new TreeMap<>();
	private static String reportPath;
	private static String reportType;

	// Initialize reports
	public static ExtentReports initReports()
	{
		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyMMdd_HHmmss");
		String timeStamp = df.format(date);

		extent = new ExtentReports();
		reportType = System.getProperty("reportType");
		String klovFile = System.getProperty("klovpropertyFile");
		String currDir = System.getProperty("user.dir");

		if ("klov".equalsIgnoreCase(reportType))
		{
			ExtentKlovReporter klovReports = new ExtentKlovReporter();
			try
			{
				klovReports.loadInitializationParams(new FileInputStream(klovFile));
			} catch (FileNotFoundException e)
			{
				TestLogManager.error("No klov properties file", e);
			}
			extent.attachReporter(klovReports);
		} else
		{
			reportPath = currDir + "/src/test/resources/ExtentReports/Resul_Test_Suite" + timeStamp + ".html";
			final File CONF = new File("./src/main/java/utility/spark.json");
			ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
			try
			{
				spark.loadJSONConfig(CONF);
			} catch (IOException e)
			{
				TestLogManager.error("Load Json issue", e);
			}
			spark.config().setTheme(Theme.STANDARD);
			spark.config().setReportName("<img src='" + currDir + "/src/main/resources/data/uploadfiles/RESUL_5.0_logo.png'");
			extent.attachReporter(spark);
		}

		extent.setAnalysisStrategy(AnalysisStrategy.CLASS);
		return extent;
	}

	public static void updateKlovReportName(String newReportName)
	{
		try
		{
			String klovPropertiesPath = FrameworkConstants.KLOV_PROPERTIES_FILEPATH;
			FileInputStream inputStream = new FileInputStream(new File(klovPropertiesPath));
			Properties properties = new Properties();
			properties.load(inputStream);
			inputStream.close();
			properties.setProperty("klov.report.name", newReportName);
			FileOutputStream outputStream = new FileOutputStream(new File(klovPropertiesPath));
			properties.store(outputStream, "Updated klov.report.name dynamically");
			outputStream.close();
			TestLogManager.info("klov.report.name updated to: " + newReportName);
		} catch (Exception e)
		{
			TestLogManager.error("Failed to update klov report name", e);
		}
	}

	public static synchronized ExtentTest startTest(String method, String testName, String browser)
	{
		Random r = new Random();
		int randInt = r.nextInt(15 - 3) + 3;
		int a = randInt * 1000;
		ExtentTest test = null;
		try
		{
			Thread.sleep(Long.valueOf(a));
		} catch (InterruptedException e)
		{
			TestLogManager.error("InterruptedException in startTest: " + e.getMessage(), e);
		}
		try
		{
			method = GridManager.isGrid.get().equals(true) ? method + " - " + browser : method;
			test = extent.createTest(method, "<font color=#0000C0><b>SCENARIO ID : </font></b>" + "<i><u><b><font color=black>" + testName + "</i></u></b></font>");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		extentTestMap.put(Thread.currentThread().getId(), test);
		return test;
	}

	// Get current test
	public static ExtentTest getTest()
	{
		return extentTestMap.get(Thread.currentThread().getId());
	}

	// Mark test info
	public static void infoTest(String message)
	{
		getTest().log(Status.INFO, message);
	}
	
	public static void infoLabel(String message)
	{
		getTest().info(MarkupHelper.createLabel(message, ExtentColor.BLUE));
	}

	// Mark test pass
	public static void passTest(String message)
	{
		getTest().log(Status.PASS, message);
	}
	
	public static void passLabel(String message)
	{
		getTest().pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
	}

	// Mark test fail
	public static void failTest(String message)
	{
		getTest().log(Status.FAIL, message);
	}
	
	public static void failLabel(String message)
	{
		getTest().fail(MarkupHelper.createLabel(message, ExtentColor.RED));
	}
	
	public static void warningTest(String message)
	{
		getTest().log(Status.WARNING, message);
	}

	// Mark test skip
	public static void skipTest(String message)
	{
		getTest().log(Status.SKIP, message);
	}
	
	public static void skipLabel(String message)
	{
		getTest().skip(MarkupHelper.createLabel(message, ExtentColor.YELLOW));
	}

	// Flush reports
	public static void flushReports()
	{
		if (extent != null)
		{
			extent.flush();
		}
	}

	public static String getReportPath()
	{
		return reportPath;
	}

	public static void openExtentReport()
	{
		if (reportType.trim().equalsIgnoreCase("html"))
		{
			try
			{
				Desktop.getDesktop().browse(new File(reportPath).toURI());
			} catch (Exception e)
			{
				TestLogManager.error("Exception occured -> " + e.getMessage(), e);
			}
		}
	}
	
	public static Map<String, String> customReport(Map<String, String> object)
	{
		String fontColorStart = "<font color=#0000C0><b>";
		String fontColorEnd = "</font></b>";

		int row = object.size() + 1;
		String[][] builder = new String[row][3];
		builder[0][0] = fontColorStart + "S.No" + fontColorEnd;
		builder[0][1] = fontColorStart + "Items" + fontColorEnd;
		builder[0][2] = fontColorStart + "Description" + fontColorEnd;

		int count = 1;
		for (Map.Entry<String, String> entry : object.entrySet())
		{
			builder[count][0] = String.valueOf(count);
			builder[count][1] = entry.getKey().toString();
			builder[count][2] = entry.getValue().toString();
			count++;
		}
		getTest().info(MarkupHelper.createTable(builder, "text-center table-striped table-bordered table-condensed").getMarkup().replace("<td", "<td style='border: 1px solid #D3D3D3;'"));
		return object;
	}

	public static String[][] customReport(List<Map<String, String>> webtable)
	{
		Set<String> headers = webtable.get(0).keySet();
		int rows = webtable.size() + 1;
		int column = headers.size();
		String[][] object = new String[rows][column];

		int headerIndex = 0;
		for (String header : headers)
		{
			object[0][headerIndex++] = "<b>" + header + "</b>";
		}

		int rowIndex = 1;
		for (Map<String, String> row : webtable)
		{
			int cellIndex = 0;
			for (String header : headers)
			{
				object[rowIndex][cellIndex++] = row.get(header);
			}
			rowIndex++;
		}
		getTest().info(MarkupHelper.createTable(object, "text-center table-striped table-bordered table-condensed"));
		return object;
	}

}
