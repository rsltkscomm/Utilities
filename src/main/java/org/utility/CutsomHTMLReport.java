package org.utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class CutsomHTMLReport implements ITestListener, ISuiteListener
{

	private static final AtomicInteger passCount = new AtomicInteger(0);
	private static final AtomicInteger failCount = new AtomicInteger(0);
	private static final AtomicInteger noRunCount = new AtomicInteger(0);
	public List<String> passMethods = new LinkedList<String>();
	public List<String> failMethods = new LinkedList<String>();
	public List<String> norunMethods = new LinkedList<String>();
	private long startTime;

	@Override
	public void onStart(ISuite suite)
	{
		startTime = System.currentTimeMillis();
		System.out.println("Suite started: " + suite.getName());
	}

	@Override
	public void onTestSuccess(ITestResult result)
	{
		passMethods.add(System.getProperty("method_name"));
		passCount.incrementAndGet();
	}

	@Override
	public void onTestFailure(ITestResult result)
	{
		failMethods.add(System.getProperty("method_name"));
		failCount.incrementAndGet();
	}

	@Override
	public void onTestSkipped(ITestResult result)
	{
		norunMethods.add(System.getProperty("method_name"));
		noRunCount.incrementAndGet();
	}

	@Override
	public void onFinish(ISuite suite)
	{
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);
		long seconds = (duration / 1000) % 60;
		long minutes = (duration / (1000 * 60)) % 60;
		String durationStr =  minutes + " min " + seconds + " sec";
		reader();
		SummaryReportGenerator.generateReport(passCount.get(), failCount.get(), noRunCount.get(),durationStr);
	}

	public void reader()
	{
		String resourcePath = "/object.properties";
	    try (InputStream is = CutsomHTMLReport.class.getResourceAsStream(resourcePath)) {
	        if (is == null) {
	            throw new FileNotFoundException("Resource not found: " + resourcePath);
	        }
	        Properties props = new Properties();
	        props.load(is);
	        for (String name : props.stringPropertyNames()) {
	            String value = props.getProperty(name);
	            System.setProperty(name, value);
	        }
	        System.out.println("Properties loaded successfully from JAR.");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	public static void getProperties(String propertyFile)
	{
		Properties obj = new Properties();
		try
		{
			obj.load(new FileInputStream(propertyFile));
		} catch (FileNotFoundException e)
		{
			System.err.println("getProperties file not found" + e.getMessage());
		} catch (IOException e)
		{
			System.err.println("getProperties file load" + e.getMessage());
		}
		for (String name : obj.stringPropertyNames())
		{
			String value = obj.getProperty(name);
			System.setProperty(name, value);
		}
	}

	public static String customPath()
	{
		String[] property = System.getProperty("user.dir").split("\\\\");
		return System.getProperty("user.dir").replaceAll(property[property.length - 1], "CustomReport");
	}

}
