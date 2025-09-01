package reporting;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Custom TestNG Listener for generating an HTML report. Tracks test execution results and duration.
 */
public class CutsomHTMLReport implements ITestListener, ISuiteListener
{

	// Atomic counters for thread-safe operation
	private static final AtomicInteger passCount = new AtomicInteger(0);
	private static final AtomicInteger failCount = new AtomicInteger(0);
	private static final AtomicInteger noRunCount = new AtomicInteger(0);

	// Lists to hold method names by result
	private final List<String> passMethods = new LinkedList<>();
	private final List<String> failMethods = new LinkedList<>();
	private final List<String> noRunMethods = new LinkedList<>();

	private long startTime;
	private String dateTime;

	/**
	 * Called when the suite starts. Captures start time.
	 */
	@Override
	public void onStart(ISuite suite)
	{
		dateTime=currentTime();
		startTime = System.currentTimeMillis();
		System.out.println("Test suite started: " + suite.getName());
	}

	/**
	 * Called when a test passes.
	 */
	@Override
	public void onTestSuccess(ITestResult result)
	{
		passMethods.add(System.getProperty("method_name"));
		passCount.incrementAndGet();
	}

	/**
	 * Called when a test fails.
	 */
	@Override
	public void onTestFailure(ITestResult result)
	{
		failMethods.add(System.getProperty("method_name"));
		failCount.incrementAndGet();
	}

	/**
	 * Called when a test is skipped.
	 */
	@Override
	public void onTestSkipped(ITestResult result)
	{
		noRunMethods.add(System.getProperty("method_name"));
		noRunCount.incrementAndGet();
	}

	/**
	 * Called when the suite finishes. Calculates execution time and triggers report generation.
	 */
	@Override
	public void onFinish(ISuite suite)
	{
		long endTime = System.currentTimeMillis();
		String durationStr = formatDuration(endTime - startTime);

		// Clean up the skipped list by removing methods that eventually passed or failed
		filterCount(passMethods, failMethods, noRunMethods);
		noRunCount.set(noRunMethods.size()); // Update the count after filtering

		// Load properties before generating report
		loadPropertiesFromJar();

		// Generate summary report
		SummaryReportGenerator.generateReport(passCount.get(), failCount.get(), noRunCount.get(), durationStr,dateTime);
	}

	public void filterCount(List<String> passMethod, List<String> failMethod, List<String> noRunMethod)
	{
		// Use a Set for faster lookup and cleaner logic
		Set<String> passSet = new HashSet<>(passMethod);
		Set<String> failSet = new HashSet<>(failMethod);

		// Use iterator to safely remove while iterating
		Iterator<String> iterator = noRunMethod.iterator();
		while (iterator.hasNext())
		{
			String method = iterator.next();
			if (passSet.contains(method) || failSet.contains(method))
			{
				iterator.remove(); // safe removal
			}
		}
	}

	/**
	 * Load properties from `object.properties` file located in the classpath (e.g., within a JAR).
	 */
	private void loadPropertiesFromJar()
	{
		String resourcePath = "/object.properties";
		try (InputStream is = CutsomHTMLReport.class.getResourceAsStream(resourcePath))
		{
			if (is == null)
			{
				throw new FileNotFoundException("Resource not found: " + resourcePath);
			}

			Properties props = new Properties();
			props.load(is);

			// Set all properties as system properties
			props.forEach((key, value) -> System.setProperty((String) key, (String) value));

			System.out.println("Properties loaded successfully from JAR.");
		} catch (IOException e)
		{
			System.err.println("Failed to load properties: " + e.getMessage());
		}
	}

	/**
	 * Loads properties from a given file path and sets them as system properties.
	 */
	public static void loadProperties(String propertyFilePath)
	{
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(propertyFilePath))
		{
			props.load(fis);
			props.forEach((key, value) -> System.setProperty((String) key, (String) value));
		} catch (FileNotFoundException e)
		{
			System.err.println("Properties file not found: " + e.getMessage());
		} catch (IOException e)
		{
			System.err.println("Error reading properties file: " + e.getMessage());
		}
	}

	/**
	 * Returns the custom report path by replacing the current directory's leaf with "CustomReport".
	 */
	public static String getCustomReportPath()
	{
		String userDir = System.getProperty("user.dir");
		String[] pathParts = userDir.split("\\\\");
		if (pathParts.length == 0)
			return userDir;
		return userDir.replace(pathParts[pathParts.length - 1], "CustomReport");
	}

	/**
	 * Formats the test suite duration in minutes and seconds.
	 */
	private String formatDuration(long durationMillis)
	{
	    long seconds = (durationMillis / 1000) % 60;
	    long minutes = (durationMillis / (1000 * 60)) % 60;
	    long hours = durationMillis / (1000 * 60 * 60);
	    return hours + " hr " + minutes + " min " + seconds + " sec";
	}

	private String currentTime(){
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
	}
}
