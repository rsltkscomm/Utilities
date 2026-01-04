package zephyrIntegration;

import reporting.DetailedTestReporter;
import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.StepStatus;
import reporting.DetailedTestReporter.TestExecution;
import reporting.DetailedTestReporter.TestStep;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;
import java.util.List;

/**
 * Sample test class demonstrating automated defect reporting functionality.
 * This class shows how to integrate defect reporting into your test automation framework.
 */
public class DefectReportingDemo {
    
    /**
     * Test case that demonstrates successful defect reporting when a test fails
     */
    public void defectReporting() {
    	try
		{
    		String failureReason = "Fail occured.";
            String screenshot = "";
            String logFilePath = "";
            File harFilePath = null;
            
            List<TestExecution> testExecutions = DetailedTestReporter.getTestExecutions();
           
            for (int i = 0; i < testExecutions.size(); i++)
    		{
            	ExecutionStatus executionStatus = testExecutions.get(i).getStatus();
            	if (executionStatus == ExecutionStatus.FAIL)
    			{
            		String testCaseKey = testExecutions.get(i).getTestCaseId();
                	String testCaseName = testExecutions.get(i).getShortDescription();
                	
                	List<TestStep> stepResults = testExecutions.get(i).getSteps();
                	
                	for (int j = 0; j < stepResults.size(); j++)
        			{
                		StepStatus status = stepResults.get(j).getStatus();
                		if (status == StepStatus.FAIL)
        				{
                			failureReason = stepResults.get(j).getActualResult();
                			screenshot = stepResults.get(j).getScreenshotPath();
                			logFilePath = stepResults.get(j).getLogFilePath();
                			harFilePath = stepResults.get(j).getHarFilePath();
        					break;
        				}
        			}
                	
                    File logFile = createDynamicLogFile(testCaseKey,testCaseName,stepResults);
                    File screenshotFile = base64ToFile(screenshot, new File(System.getProperty("user.dir")+"/logs/test_image.png").getAbsolutePath());
                    // Report the defect
                    String bugKey = new DefectReporter().reportDefect(
                        testCaseKey, 
                        testCaseName, 
                        failureReason, 
                        stepResults, 
                        screenshotFile, 
                        null,
                        null,
                        null
                    );
                    if (bugKey != null) {
                        System.out.println("✅ Defect reported successfully with key: " + bugKey);
                        System.setProperty(testCaseName, bugKey);
                    } else {
                        System.out.println("❌ Failed to report defect");
                    }
    			}
    		}
		} catch (Exception e)
		{
			StringWriter sw = new StringWriter();
		    e.printStackTrace(new PrintWriter(sw));
		    String exceptionAsString = sw.toString();
		    System.out.println("Unable to create the bug in jira -> " + exceptionAsString);
		}
        
    }
    
    
    /**
     * Creates a detailed test execution log file dynamically from TestStep data.
     */
    public File createDynamicLogFile(String testCaseKey, String testCaseName, List<TestStep> steps) {
        File logFile = null;

        try {
            String timestamp = DefectReporter.getCurrentTimestamp();
            logFile = new DefectReporter().createLogFile(testCaseKey, timestamp);

            // Ensure directories exist
            logFile.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write("=== AUTOMATION TEST EXECUTION LOG ===\n");
                writer.write("Test Case: " + testCaseKey + " - " + testCaseName + "\n");
                writer.write("Start Time: " + new java.util.Date() + "\n");
                writer.write("Browser: Chrome 120.0.6099.109\n");
                writer.write("Operating System: Windows 10\n\n");

                // Iterate through all steps and write details
                for (TestStep step : steps) {
                    writer.write("STEP " + step.getStepNo() + ": " + step.getAction() + "\n");
                    writer.write("Expected Result: " + safe(step.getExpectedResult()) + "\n");
                    writer.write("Actual Result: " + safe(step.getActualResult()) + "\n");
                    writer.write("Status: " + step.getStatus() + "\n");

                    if (step.getScreenshotPath() != null && !step.getScreenshotPath().isEmpty()) {
                        writer.write("Screenshot: " + step.getScreenshotPath() + "\n");
                    }

                    writer.write("-".repeat(80) + "\n");
                }

                // Overall Summary
                writer.write("\n=== TEST EXECUTION COMPLETED ===\n");
                writer.write("End Time: " + new java.util.Date() + "\n");

                // Determine final test status
                boolean failed = steps.stream().anyMatch(s -> s.getStatus() == StepStatus.FAIL);
                writer.write("Overall Status: " + (failed ? "FAILED" : "PASSED") + "\n");
            }

            System.out.println("📄 Log file created successfully: " + logFile.getAbsolutePath());
            return logFile;

        } catch (IOException e) {
            System.err.println("❌ Error creating log file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper to avoid null text entries in logs.
     */
    private String safe(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

    public static File base64ToFile(String base64String, String filePath) {
        File file = null;
        try {
            if (base64String.contains(",")) {
                base64String = base64String.split(",")[1];
            }

            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            file = new File(filePath);

            // Ensure directories exist
            file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(decodedBytes);
            }

            System.out.println("✅ File created successfully at: " + file.getAbsolutePath());
        } 
        catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid Base64 content: " + e.getMessage());
            e.printStackTrace();
        } 
        catch (Exception e) {
            System.err.println("❌ Failed to create file: " + e.getMessage());
            e.printStackTrace();
        }

        return file;
    }

}
