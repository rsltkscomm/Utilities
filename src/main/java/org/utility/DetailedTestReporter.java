package org.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.utility.NewSummaryReportGenerator.ModuleStats;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.utility.DetailedTestReporter;

public class DetailedTestReporter
{
	
	public static DetailedTestReporter detailedTestReporter;
	public static final Map<String, AtomicInteger> modulePassCount = new ConcurrentHashMap<>();
	public static final Map<String, AtomicInteger> moduleFailCount = new ConcurrentHashMap<>();
	public static final Map<String, AtomicInteger> moduleSkipCount = new ConcurrentHashMap<>();

	public enum ExecutionStatus
	{
			PASS, FAIL, SKIPPED
	}

	public enum StepStatus
	{
			PASS, FAIL, SKIPPED
	}

	private List<TestExecution> testExecutions;
	private String reportPath;
	private String projectName;
	private SimpleDateFormat dateFormat;
	private PerformanceMetrics performanceMetrics;

	public DetailedTestReporter(String projectName, String reportPath) {
		this.projectName = projectName;
		this.reportPath = reportPath;
		this.testExecutions = new ArrayList<>();
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.performanceMetrics = new PerformanceMetrics();
	}

	public void addTestExecution(String module, String scenarioId, String testCaseId, String shortDescription, ExecutionStatus status)
	{
		boolean exists = testExecutions.stream().anyMatch(e -> e.getTestCaseId().equals(testCaseId));

		if (exists)
		{
			System.out.println("Duplicate Test Case ID: " + testCaseId + " — Skipping entry.");
			return;
		}

		TestExecution execution = new TestExecution();
		execution.setModule(module);
		execution.setScenarioId(scenarioId);
		execution.setTestCaseId(testCaseId);
		execution.setShortDescription(shortDescription);
		execution.setStartTime(new Date()); // auto-start time
		execution.setStatus(status);
		execution.setSteps(new ArrayList<>());
		testExecutions.add(execution);
	}
	
	public static void createDetailReport()
	{
		detailedTestReporter = new DetailedTestReporter("Detail Test Suite", "test-output");
	}

	public static DetailedTestReporter getReport()
	{
		return detailedTestReporter;
	}

	public static void addStep(TestCase testCase, StepStatus status,WebDriver driver) {
	    boolean isDuplicate = false;
	    Optional<TestExecution> executionOpt = getReport().getTestExecutions().stream()
	            .filter(e -> e.getTestCaseId().equals(testCase.getTestCaseId()))
	            .findFirst();

	    TestExecution execution;
	    if (!executionOpt.isPresent()) {
	        execution = new TestExecution();
	        execution.setModule(testCase.getModuleName());
	        execution.setScenarioId(testCase.getExecutionId());
	        execution.setTestCaseId(testCase.getTestCaseId());
	        execution.setShortDescription(testCase.getDescription());
	        execution.setStartTime(new Date());
	        execution.setSteps(new ArrayList<>()); // Explicit initialization
	        execution.setStatus(ExecutionStatus.PASS);
	        execution.setTotalExpectedSteps(testCase.getTotalSteps());
	        getReport().getTestExecutions().add(execution);
	    } else {
	        execution = executionOpt.get();
	        // Ensure steps is never null for existing executions
	        if (execution.getSteps() == null) {
	            execution.setSteps(new ArrayList<>());
	        }
	    }

	    execution.incrementStepAttempts();

	    // Now safe to stream as steps is guaranteed non-null
	    isDuplicate = execution.getSteps().stream()
	            .anyMatch(step -> step.getAction().equals(testCase.getAction()) 
	                    && step.getExpectedResult().equals(testCase.getExpectedResult()));

	    if (!isDuplicate) {
	        getReport().addTestStep(
	            testCase.getTestCaseId(),
	            execution.getSteps().size() + 1,
	            testCase.getAction(),
	            testCase.getExpectedResult(),
	            testCase.getActualResult(),
	            status,
	            encryptScreenshot(driver)
	        );

	        if (status == StepStatus.FAIL) {
	            execution.setStatus(ExecutionStatus.FAIL);
	        }
	    }
	    
	    if (execution.getStepAttempts() == execution.getTotalExpectedSteps()) {
	        execution.setEndTime(new Date());

			// Update ModuleStats map
			ModuleStats stats = NewSummaryReportGenerator.moduleStats.computeIfAbsent(testCase.getModuleName(), m -> new ModuleStats());

			switch (execution.getStatus())
			{
			case PASS:
				stats.incrementPass();
				modulePassCount.computeIfAbsent(testCase.getModuleName(), k -> new AtomicInteger(0)).incrementAndGet();
				break;
			case FAIL:
				stats.incrementFail();
				moduleFailCount.computeIfAbsent(testCase.getModuleName(), k -> new AtomicInteger(0)).incrementAndGet();
				break;
			case SKIPPED:
				stats.incrementSkip();
				moduleSkipCount.computeIfAbsent(testCase.getModuleName(), k -> new AtomicInteger(0)).incrementAndGet();
				break;
			}
		}
	}
	
	public static String encryptScreenshot(WebDriver driver) {
	    try {
	        // Take screenshot as Base64
	        TakesScreenshot ts = (TakesScreenshot) driver;
	        String screenshotAs = ts.getScreenshotAs(OutputType.BASE64);

	        // Create HTML content to display the image immediately
	        String htmlContent = "<html><body style='margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;'>"
	                + "<img src='data:image/png;base64," + screenshotAs + "' style='max-width:100%;max-height:100%;'/>"
	                + "</body></html>";

	        // Save HTML file
	        String filePath = System.getProperty("java.io.tmpdir") + "screenshot.html";
	        try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
	            writer.write(htmlContent);
	        }

	        // Return file path (or open in browser directly if needed)
	        return "file:///" + filePath.replace("\\", "/");

	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	public List<TestExecution> getTestExecutions()
	{
		return testExecutions;
	}

	public void addTestStep(String testCaseId, String action, String expectedResult, String actualResult, StepStatus status, String screenshotPath)
	{
		addTestStep(testCaseId, -1, action, expectedResult, actualResult, status, screenshotPath);
	}

	public void addTestStep(String testCaseId, int stepNumber, String action, String expectedResult, String actualResult, StepStatus status, String screenshotPath)
	{
		for (TestExecution execution : testExecutions)
		{
			if (execution.getTestCaseId().equals(testCaseId))
			{
				TestStep step = new TestStep();
				step.setStepNo(stepNumber > 0 ? stepNumber : execution.getSteps().size() + 1);
				step.setAction(action);
				step.setExpectedResult(expectedResult);
				step.setActualResult(actualResult);
				step.setStatus(status);
				step.setScreenshotPath(screenshotPath);
				execution.getSteps().add(step);
				break;
			}
		}
	}

	public void generateReport()
	{
		try
		{
			File reportDir = new File(reportPath);
			if (!reportDir.exists())
			{
				reportDir.mkdirs();
			}

			String htmlContent = generateHTMLContent();
			File reportFile = new File(reportPath + "/Report.html");

			try (FileWriter writer = new FileWriter(reportFile))
			{
				writer.write(htmlContent);
			}

			System.out.println("Detailed test report generated successfully at: " + reportFile.getAbsolutePath());

		} catch (IOException e)
		{
			System.err.println("Error generating detailed test report: " + e.getMessage());
		}
	}

	private String generateHTMLContent()
	{
		StringBuilder html = new StringBuilder();

		// Calculate summary statistics
		int totalTests = testExecutions.size();
		int passedTests = (int) testExecutions.stream().filter(t -> t.getStatus() == ExecutionStatus.PASS).count();
		int failedTests = (int) testExecutions.stream().filter(t -> t.getStatus() == ExecutionStatus.FAIL).count();
		int skippedTests = (int) testExecutions.stream().filter(t -> t.getStatus() == ExecutionStatus.SKIPPED).count();

		long totalDuration = testExecutions.stream().mapToLong(t -> t.getEndTime().getTime() - t.getStartTime().getTime()).sum();

		// Calculate performance metrics
		double avgExecutionTime = performanceMetrics.getAverageExecutionTime();
		long minExecutionTime = performanceMetrics.getMinExecutionTime();
		long maxExecutionTime = performanceMetrics.getMaxExecutionTime();
		double passRate = performanceMetrics.getPassRate();

		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"en\">\n");
		html.append("<head>\n");
		html.append("    <meta charset=\"UTF-8\">\n");
		html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("    <title>").append(projectName).append(" - Detailed Test Report</title>\n");
		html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n");
		html.append("    <style>\n");
		html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
		html.append("        .container { max-width: 1600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
		html.append("        .header { text-align: center; margin-bottom: 30px; }\n");
		html.append("        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }\n");
		html.append("        .summary-card { padding: 20px; border-radius: 8px; text-align: center; color: white; }\n");
		html.append("        .total { background-color: #2196F3; }\n");
		html.append("        .passed { background-color: #4CAF50; }\n");
		html.append("        .failed { background-color: #f44336; }\n");
		html.append("        .skipped { background-color: #FF9800; }\n");
		html.append("        .summary-card h3 { margin: 0; font-size: 2em; }\n");
		html.append("        .summary-card p { margin: 5px 0; }\n");
		html.append("        .performance-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n");
		html.append("        .metric-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); border-left: 4px solid #2196F3; }\n");
		html.append("        .metric-card h4 { margin: 0 0 10px 0; color: #333; }\n");
		html.append("        .metric-card .value { font-size: 1.5em; font-weight: bold; color: #2196F3; }\n");
		html.append("        .charts-section { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px; }\n");
		html.append("        .chart-container { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n");
		html.append("        .search-filter { margin-bottom: 20px; }\n");
		html.append("        .search-filter input, .search-filter select { padding: 10px; margin: 5px; border: 1px solid #ddd; border-radius: 4px; }\n");
		html.append("        .search-filter button { padding: 10px 20px; background: #2196F3; color: white; border: none; border-radius: 4px; cursor: pointer; }\n");
		html.append("        .search-filter button:hover { background: #1976D2; }\n");
		html.append("        .export-buttons { margin-bottom: 20px; }\n");
		html.append("        .export-btn { padding: 8px 16px; margin: 5px; background: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer; }\n");
		html.append("        .export-btn:hover { background: #45a049; }\n");
		html.append("        .test-results { margin-top: 30px; }\n");
		html.append("        .test-table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
		html.append("        .test-table th, .test-table td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
		html.append("        .test-table th { background-color: #f2f2f2; font-weight: bold; }\n");
		html.append("        .test-table tr:nth-child(even) { background-color: #f9f9f9; }\n");
		html.append("        .test-table tr:hover { background-color: #f5f5f5; }\n");
		html.append("        .status-pass { color: #4CAF50; font-weight: bold; }\n");
		html.append("        .status-fail { color: #f44336; font-weight: bold; }\n");
		html.append("        .status-skip { color: #FF9800; font-weight: bold; }\n");
		html.append("        .details-link { color: #2196F3; text-decoration: none; }\n");
		html.append("        .details-link:hover { text-decoration: underline; }\n");
		html.append("        .step-details { display: none; margin-top: 20px; }\n");
		html.append("        .step-table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
		html.append("        .step-table th, .step-table td { border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 0.9em; }\n");
		html.append("        .step-table th { background-color: #e3f2fd; font-weight: bold; }\n");
		html.append("        .screenshot-link { color: #2196F3; text-decoration: none; }\n");
		html.append("        .screenshot-link:hover { text-decoration: underline; }\n");
		html.append("        .back-link { margin-top: 20px; }\n");
		html.append("        .back-link a { color: #2196F3; text-decoration: none; }\n");
		html.append("        .back-link a:hover { text-decoration: underline; }\n");
		html.append("        .summary-card:hover { transform: translateY(-2px); transition: transform 0.2s ease; }\n");
		html.append("        .test-row { transition: opacity 0.3s ease; }\n");
		html.append("        .test-row.hidden { display: none; }\n");
		html.append("        .summary-card.active { box-shadow: 0 4px 8px rgba(0,0,0,0.3); transform: translateY(-2px); }\n");
		html.append("        .dark-mode { background-color: #1a1a1a; color: #ffffff; }\n");
		html.append("        .dark-mode .container { background: #2d2d2d; }\n");
		html.append("        .dark-mode .metric-card { background: #3d3d3d; color: #ffffff; }\n");
		html.append("        .dark-mode .chart-container { background: #3d3d3d; color: #ffffff; }\n");
		html.append("        .theme-toggle { position: fixed; top: 20px; right: 20px; padding: 10px; background: #333; color: white; border: none; border-radius: 4px; cursor: pointer; }\n");
		html.append("    </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("    <div class=\"container\">\n");
		html.append("        <div class=\"header\">\n");
		html.append("            <h1>").append(projectName).append(" - Detailed Test Report</h1>\n");
		html.append("            <p>Generated on: ").append(dateFormat.format(new Date())).append("</p>\n");
		html.append("        </div>\n");

		// Summary section
		html.append("        <div class=\"summary\">\n");
		html.append("            <div class=\"summary-card total\" onclick=\"filterTests('all')\" style=\"cursor: pointer;\">\n");
		html.append("                <h3>").append(totalTests).append("</h3>\n");
		html.append("                <p>Total Tests</p>\n");
		html.append("            </div>\n");
		html.append("            <div class=\"summary-card passed\" onclick=\"filterTests('PASS')\" style=\"cursor: pointer;\">\n");
		html.append("                <h3>").append(passedTests).append("</h3>\n");
		html.append("                <p>Passed</p>\n");
		html.append("            </div>\n");
		html.append("            <div class=\"summary-card failed\" onclick=\"filterTests('FAIL')\" style=\"cursor: pointer;\">\n");
		html.append("                <h3>").append(failedTests).append("</h3>\n");
		html.append("                <p>Failed</p>\n");
		html.append("            </div>\n");
		html.append("            <div class=\"summary-card skipped\" onclick=\"filterTests('SKIPPED')\" style=\"cursor: pointer;\">\n");
		html.append("                <h3>").append(skippedTests).append("</h3>\n");
		html.append("                <p>Skipped</p>\n");
		html.append("            </div>\n");
		html.append("        </div>\n");

		// Search and Filter section
		html.append("        <div class=\"search-filter\">\n");
		html.append("            <input type=\"text\" id=\"searchInput\" placeholder=\"Search tests...\" onkeyup=\"searchTests()\">\n");
		html.append("            <select id=\"moduleFilter\" onchange=\"filterByModule()\">\n");
		html.append("                <option value=\"\">All Modules</option>\n");

		// Get unique modules
		List<String> modules = testExecutions.stream().map(TestExecution::getModule).distinct().collect(Collectors.toList());

		for (String module : modules)
		{
			html.append("                <option value=\"").append(module).append("\">").append(module).append("</option>\n");
		}

		html.append("            </select>\n");
		html.append("            <select id=\"statusFilter\" onchange=\"filterByStatus()\">\n");
		html.append("                <option value=\"\">All Statuses</option>\n");
		html.append("                <option value=\"PASS\">Passed</option>\n");
		html.append("                <option value=\"FAIL\">Failed</option>\n");
		html.append("                <option value=\"SKIPPED\">Skipped</option>\n");
		html.append("            </select>\n");
		html.append("            <button onclick=\"clearFilters()\">Clear Filters</button>\n");
		html.append("        </div>\n");

		// Export buttons
		html.append("        <div class=\"export-buttons\">\n");
		html.append("            <button class=\"export-btn\" onclick=\"exportToCSV()\">Export to CSV</button>\n");
		html.append("            <button class=\"export-btn\" onclick=\"exportToJSON()\">Export to JSON</button>\n");
		html.append("            <button class=\"export-btn\" onclick=\"printReport()\">Print Report</button>\n");
		html.append("        </div>\n");

		// Test Results Table
		html.append("        <div class=\"test-results\">\n");
		html.append("            <h2>Test Execution Summary <span id=\"filter-status\"></span></h2>\n");
		html.append("            <table class=\"test-table\">\n");
		html.append("                <thead>\n");
		html.append("                    <tr>\n");
		html.append("                        <th>Module</th>\n");
		html.append("                        <th>Scenario ID</th>\n");
		html.append("                        <th>Test Case ID</th>\n");
		html.append("                        <th>Short Description</th>\n");
		html.append("                        <th>Start Time</th>\n");
		html.append("                        <th>End Time</th>\n");
		html.append("                        <th>Duration (ms)</th>\n");
		html.append("                        <th>Execution Status</th>\n");
		html.append("                        <th>Details</th>\n");
		html.append("                    </tr>\n");
		html.append("                </thead>\n");
		html.append("                <tbody>\n");

		for (TestExecution execution : testExecutions)
		{
			long duration = execution.getEndTime().getTime() - execution.getStartTime().getTime();
			String statusClass = "status-" + execution.getStatus().toString().toLowerCase();

			html.append("                    <tr class=\"test-row\" data-status=\"").append(execution.getStatus().toString()).append("\" data-module=\"").append(execution.getModule()).append("\">\n");
			html.append("                        <td>").append(execution.getModule()).append("</td>\n");
			html.append("                        <td>").append(execution.getScenarioId()).append("</td>\n");
			html.append("                        <td>").append(execution.getTestCaseId()).append("</td>\n");
			html.append("                        <td>").append(execution.getShortDescription()).append("</td>\n");
			html.append("                        <td>").append(dateFormat.format(execution.getStartTime())).append("</td>\n");
			html.append("                        <td>").append(dateFormat.format(execution.getEndTime())).append("</td>\n");
			html.append("                        <td>").append(duration).append("</td>\n");
			html.append("                        <td class=\"").append(statusClass).append("\">").append(execution.getStatus()).append("</td>\n");
			html.append("                        <td>\n");
			html.append("                            <a href=\"#\" class=\"details-link\" onclick=\"showStepDetails('").append(execution.getTestCaseId()).append("')\">View Steps</a>\n");
			html.append("                        </td>\n");
			html.append("                    </tr>\n");

			// Step details section
			html.append("                    <tr id=\"steps-").append(execution.getTestCaseId()).append("\" class=\"step-details\">\n");
			html.append("                        <td colspan=\"9\">\n");
			html.append("                            <div class=\"back-link\"><a href=\"#\" onclick=\"hideStepDetails('").append(execution.getTestCaseId()).append("')\">Back to Summary</a></div>\n");
			html.append("                            <h3>Test Steps for ").append(execution.getTestCaseId()).append("</h3>\n");
			html.append("                            <table class=\"step-table\">\n");
			html.append("                                <thead>\n");
			html.append("                                    <tr>\n");
			html.append("                                        <th>Step No</th>\n");
			html.append("                                        <th>Action</th>\n");
			html.append("                                        <th>Expected Result</th>\n");
			html.append("                                        <th>Actual Result</th>\n");
			html.append("                                        <th>Status</th>\n");
			html.append("                                        <th>Screenshot</th>\n");
			html.append("                                    </tr>\n");
			html.append("                                </thead>\n");
			html.append("                                <tbody>\n");

			for (TestStep step : execution.getSteps())
			{
				String stepStatusClass = "status-" + step.getStatus().toString().toLowerCase();

				html.append("                                    <tr>\n");
				html.append("                                        <td>").append(step.getStepNo()).append("</td>\n");
				html.append("                                        <td>").append(step.getAction()).append("</td>\n");
				html.append("                                        <td>").append(step.getExpectedResult()).append("</td>\n");
				html.append("                                        <td>").append(step.getActualResult()).append("</td>\n");
				html.append("                                        <td class=\"").append(stepStatusClass).append("\">").append(step.getStatus()).append("</td>\n");
				html.append("                                        <td>\n");

				if (step.getScreenshotPath() != null && !step.getScreenshotPath().isEmpty())
				{
					html.append("                                            <a href=\"").append(step.getScreenshotPath()).append("\" class=\"screenshot-link\" target=\"_blank\">View Screenshot</a>\n");
				} else
				{
					html.append("                                            -");
				}

				html.append("                                        </td>\n");
				html.append("                                    </tr>\n");
			}

			html.append("                                </tbody>\n");
			html.append("                            </table>\n");
			html.append("                        </td>\n");
			html.append("                    </tr>\n");
		}

		html.append("                </tbody>\n");
		html.append("            </table>\n");
		html.append("        </div>\n");
		html.append("    </div>\n");

		// Enhanced JavaScript with all new features
		html.append("    <script>\n");
		html.append("        // Chart.js configuration\n");
		html.append("        const statusData = {\n");
		html.append("            labels: ['Passed', 'Failed', 'Skipped'],\n");
		html.append("            datasets: [{\n");
		html.append("                data: [").append(passedTests).append(", ").append(failedTests).append(", ").append(skippedTests).append("],\n");
		html.append("                backgroundColor: ['#4CAF50', '#f44336', '#FF9800'],\n");
		html.append("                borderWidth: 2,\n");
		html.append("                borderColor: '#fff'\n");
		html.append("            }]\n");
		html.append("        };\n");
		html.append("        \n");
		html.append("        const timeData = {\n");
		html.append("            labels: ['Fast (< 100ms)', 'Medium (100-500ms)', 'Slow (> 500ms)'],\n");
		html.append("            datasets: [{\n");
		html.append("                label: 'Execution Time Distribution',\n");
		html.append("                data: [").append(performanceMetrics.getFastTests()).append(", ").append(performanceMetrics.getMediumTests()).append(", ").append(performanceMetrics.getSlowTests()).append("],\n");
		html.append("                backgroundColor: ['#4CAF50', '#FF9800', '#f44336'],\n");
		html.append("                borderWidth: 1,\n");
		html.append("                borderColor: '#fff'\n");
		html.append("            }]\n");
		html.append("        };\n");
		html.append("        \n");
		html.append("        // Initialize charts\n");
		html.append("        const statusCtx = document.getElementById('statusChart').getContext('2d');\n");
		html.append("        new Chart(statusCtx, {\n");
		html.append("            type: 'doughnut',\n");
		html.append("            data: statusData,\n");
		html.append("            options: {\n");
		html.append("                responsive: true,\n");
		html.append("                plugins: {\n");
		html.append("                    legend: {\n");
		html.append("                        position: 'bottom'\n");
		html.append("                    }\n");
		html.append("                }\n");
		html.append("            }\n");
		html.append("        });\n");
		html.append("        \n");
		html.append("        const timeCtx = document.getElementById('timeChart').getContext('2d');\n");
		html.append("        new Chart(timeCtx, {\n");
		html.append("            type: 'bar',\n");
		html.append("            data: timeData,\n");
		html.append("            options: {\n");
		html.append("                responsive: true,\n");
		html.append("                scales: {\n");
		html.append("                    y: {\n");
		html.append("                        beginAtZero: true\n");
		html.append("                    }\n");
		html.append("                }\n");
		html.append("            }\n");
		html.append("        });\n");
		html.append("        \n");
		html.append("        // Theme toggle\n");
		html.append("        function toggleTheme() {\n");
		html.append("            document.body.classList.toggle('dark-mode');\n");
		html.append("            const btn = document.querySelector('.theme-toggle');\n");
		html.append("            btn.textContent = document.body.classList.contains('dark-mode') ? '☀️' : '🌙';\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        // Search functionality\n");
		html.append("        function searchTests() {\n");
		html.append("            const searchTerm = document.getElementById('searchInput').value.toLowerCase();\n");
		html.append("            const rows = document.querySelectorAll('.test-row');\n");
		html.append("            \n");
		html.append("            rows.forEach(row => {\n");
		html.append("                const text = row.textContent.toLowerCase();\n");
		html.append("                if (text.includes(searchTerm)) {\n");
		html.append("                    row.classList.remove('hidden');\n");
		html.append("                } else {\n");
		html.append("                    row.classList.add('hidden');\n");
		html.append("                }\n");
		html.append("            });\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        // Module filter\n");
		html.append("        function filterByModule() {\n");
		html.append("            const selectedModule = document.getElementById('moduleFilter').value;\n");
		html.append("            const rows = document.querySelectorAll('.test-row');\n");
		html.append("            \n");
		html.append("            rows.forEach(row => {\n");
		html.append("                const module = row.getAttribute('data-module');\n");
		html.append("                if (!selectedModule || module === selectedModule) {\n");
		html.append("                    row.classList.remove('hidden');\n");
		html.append("                } else {\n");
		html.append("                    row.classList.add('hidden');\n");
		html.append("                }\n");
		html.append("            });\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        // Status filter\n");
		html.append("        function filterByStatus() {\n");
		html.append("            const selectedStatus = document.getElementById('statusFilter').value;\n");
		html.append("            const rows = document.querySelectorAll('.test-row');\n");
		html.append("            \n");
		html.append("            rows.forEach(row => {\n");
		html.append("                const status = row.getAttribute('data-status');\n");
		html.append("                if (!selectedStatus || status === selectedStatus) {\n");
		html.append("                    row.classList.remove('hidden');\n");
		html.append("                } else {\n");
		html.append("                    row.classList.add('hidden');\n");
		html.append("                }\n");
		html.append("            });\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        // Clear all filters\n");
		html.append("        function clearFilters() {\n");
		html.append("            document.getElementById('searchInput').value = '';\n");
		html.append("            document.getElementById('moduleFilter').value = '';\n");
		html.append("            document.getElementById('statusFilter').value = '';\n");
		html.append("            \n");
		html.append("            const rows = document.querySelectorAll('.test-row');\n");
		html.append("            rows.forEach(row => row.classList.remove('hidden'));\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        // Export functions\n");
		html.append("        function exportToCSV() {\n");
		html.append("            const rows = document.querySelectorAll('.test-row:not(.hidden)');\n");
		html.append("            let csv = 'Module,Scenario ID,Test Case ID,Short Description,Start Time,End Time,Duration (ms),Execution Status\\n';\n");
		html.append("            \n");
		html.append("            rows.forEach(row => {\n");
		html.append("                const cells = row.querySelectorAll('td');\n");
		html.append("                const rowData = [];\n");
		html.append("                for (let i = 0; i < 8; i++) {\n");
		html.append("                    rowData.push('\"' + cells[i].textContent.trim() + '\"');\n");
		html.append("                }\n");
		html.append("                csv += rowData.join(',') + '\\n';\n");
		html.append("            });\n");
		html.append("            \n");
		html.append("            const blob = new Blob([csv], { type: 'text/csv' });\n");
		html.append("            const url = window.URL.createObjectURL(blob);\n");
		html.append("            const a = document.createElement('a');\n");
		html.append("            a.href = url;\n");
		html.append("            a.download = 'test-report.csv';\n");
		html.append("            a.click();\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        function exportToJSON() {\n");
		html.append("            const rows = document.querySelectorAll('.test-row:not(.hidden)');\n");
		html.append("            const data = [];\n");
		html.append("            \n");
		html.append("            rows.forEach(row => {\n");
		html.append("                const cells = row.querySelectorAll('td');\n");
		html.append("                data.push({\n");
		html.append("                    module: cells[0].textContent.trim(),\n");
		html.append("                    scenarioId: cells[1].textContent.trim(),\n");
		html.append("                    testCaseId: cells[2].textContent.trim(),\n");
		html.append("                    shortDescription: cells[3].textContent.trim(),\n");
		html.append("                    startTime: cells[4].textContent.trim(),\n");
		html.append("                    endTime: cells[5].textContent.trim(),\n");
		html.append("                    duration: cells[6].textContent.trim(),\n");
		html.append("                    executionStatus: cells[7].textContent.trim()\n");
		html.append("                });\n");
		html.append("            });\n");
		html.append("            \n");
		html.append("            const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });\n");
		html.append("            const url = window.URL.createObjectURL(blob);\n");
		html.append("            const a = document.createElement('a');\n");
		html.append("            a.href = url;\n");
		html.append("            a.download = 'test-report.json';\n");
		html.append("            a.click();\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        function printReport() {\n");
		html.append("            window.print();\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        function showStepDetails(testCaseId) {\n");
		html.append("            // Hide all step details first\n");
		html.append("            document.querySelectorAll('.step-details').forEach(function(element) {\n");
		html.append("                element.style.display = 'none';\n");
		html.append("            });\n");
		html.append("            \n");
		html.append("            // Show the selected step details\n");
		html.append("            const stepDetails = document.getElementById('steps-' + testCaseId);\n");
		html.append("            if (stepDetails) {\n");
		html.append("                stepDetails.style.display = 'table-row';\n");
		html.append("            }\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        function hideStepDetails(testCaseId) {\n");
		html.append("            const stepDetails = document.getElementById('steps-' + testCaseId);\n");
		html.append("            if (stepDetails) {\n");
		html.append("                stepDetails.style.display = 'none';\n");
		html.append("            }\n");
		html.append("        }\n");
		html.append("        \n");
		html.append("        function filterTests(status) {\n");
		html.append("            // Remove active class from all summary cards\n");
		html.append("            document.querySelectorAll('.summary-card').forEach(function(card) {\n");
		html.append("                card.classList.remove('active');\n");
		html.append("            });\n");
		html.append("            \n");
		html.append("            // Add active class to the clicked card\n");
		html.append("            const clickedCard = event.currentTarget;\n");
		html.append("            clickedCard.classList.add('active');\n");
		html.append("            \n");
		html.append("            const testRows = document.querySelectorAll('.test-row');\n");
		html.append("            \n");
		html.append("            testRows.forEach(function(row) {\n");
		html.append("                if (status === 'all') {\n");
		html.append("                    row.classList.remove('hidden');\n");
		html.append("                } else {\n");
		html.append("                    const rowStatus = row.getAttribute('data-status');\n");
		html.append("                    if (rowStatus === status) {\n");
		html.append("                        row.classList.remove('hidden');\n");
		html.append("                    } else {\n");
		html.append("                        row.classList.add('hidden');\n");
		html.append("                    }\n");
		html.append("                }\n");
		html.append("            });\n");
		html.append("            \n");
		html.append("            // Also hide all step details when filtering\n");
		html.append("            document.querySelectorAll('.step-details').forEach(function(element) {\n");
		html.append("                element.style.display = 'none';\n");
		html.append("            });\n");
		html.append("            \n");
		html.append("            // Update filter status text\n");
		html.append("            const filterStatus = document.getElementById('filter-status');\n");
		html.append("            if (status === 'all') {\n");
		html.append("                filterStatus.textContent = '';\n");
		html.append("            } else {\n");
		html.append("                filterStatus.textContent = '(Filtered by: ' + status + ')';\n");
		html.append("            }\n");
		html.append("        }\n");
		html.append("    </script>\n");
		html.append("</body>\n");
		html.append("</html>");

		return html.toString();
	}

	// Performance Metrics class
	public static class PerformanceMetrics
	{
		private List<Long> executionTimes;
		private Map<ExecutionStatus, Integer> statusCounts;

		public PerformanceMetrics() {
			this.executionTimes = new ArrayList<>();
			this.statusCounts = new HashMap<>();
		}

		public void addExecutionTime(long time)
		{
			executionTimes.add(time);
		}

		public void addTestExecution(ExecutionStatus status)
		{
			statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
		}

		public double getAverageExecutionTime()
		{
			if (executionTimes.isEmpty())
				return 0.0;
			return executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
		}

		public long getMinExecutionTime()
		{
			if (executionTimes.isEmpty())
				return 0;
			return executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
		}

		public long getMaxExecutionTime()
		{
			if (executionTimes.isEmpty())
				return 0;
			return executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
		}

		public double getPassRate()
		{
			int total = statusCounts.values().stream().mapToInt(Integer::intValue).sum();
			if (total == 0)
				return 0.0;
			int passed = statusCounts.getOrDefault(ExecutionStatus.PASS, 0);
			return (double) passed / total * 100;
		}

		public int getFastTests()
		{
			return (int) executionTimes.stream().filter(time -> time < 100).count();
		}

		public int getMediumTests()
		{
			return (int) executionTimes.stream().filter(time -> time >= 100 && time <= 500).count();
		}

		public int getSlowTests()
		{
			return (int) executionTimes.stream().filter(time -> time > 500).count();
		}
	}

	public static class TestExecution
	{
		private String module;
		private String scenarioId;
		private String testCaseId;
		private String shortDescription;
		private Date startTime;
		private Date endTime;
		private ExecutionStatus status;
		private List<TestStep> steps = new ArrayList<>();

		// New fields to track step counts
		private int totalExpectedSteps; // Total steps expected (from test case)
		private int stepAttempts; // Tracks how many steps were attempted (including duplicates)

		// Getters and setters (existing ones remain the same)
		public String getModule()
		{
			return module;
		}

		public void setModule(String module)
		{
			this.module = module;
		}

		public String getScenarioId()
		{
			return scenarioId;
		}

		public void setScenarioId(String scenarioId)
		{
			this.scenarioId = scenarioId;
		}

		public String getTestCaseId()
		{
			return testCaseId;
		}

		public void setTestCaseId(String testCaseId)
		{
			this.testCaseId = testCaseId;
		}

		public String getShortDescription()
		{
			return shortDescription;
		}

		public void setShortDescription(String shortDescription)
		{
			this.shortDescription = shortDescription;
		}

		public Date getStartTime()
		{
			return startTime;
		}

		public void setStartTime(Date startTime)
		{
			this.startTime = startTime;
		}

		public Date getEndTime()
		{
			return endTime;
		}

		public void setEndTime(Date endTime)
		{
			this.endTime = endTime;
		}

		public ExecutionStatus getStatus()
		{
			return status;
		}

		public void setStatus(ExecutionStatus status)
		{
			this.status = status;
		}

		public List<TestStep> getSteps()
		{
			return steps;
		}

		public void setSteps(List<TestStep> steps)
		{
			this.steps = steps;
		}

		// New getters and setters for step tracking
		public int getTotalExpectedSteps()
		{
			return totalExpectedSteps;
		}

		public void setTotalExpectedSteps(int totalExpectedSteps)
		{
			this.totalExpectedSteps = totalExpectedSteps;
		}

		public int getStepAttempts()
		{
			return stepAttempts;
		}

		public void incrementStepAttempts()
		{
			this.stepAttempts++;
		}
	}

	public static class TestStep
	{
		private int stepNo;
		private String action;
		private String expectedResult;
		private String actualResult;
		private StepStatus status;
		private String screenshotPath;

		// Getters and setters
		public int getStepNo()
		{
			return stepNo;
		}

		public void setStepNo(int stepNo)
		{
			this.stepNo = stepNo;
		}

		public String getAction()
		{
			return action;
		}

		public void setAction(String action)
		{
			this.action = action;
		}

		public String getExpectedResult()
		{
			return expectedResult;
		}

		public void setExpectedResult(String expectedResult)
		{
			this.expectedResult = expectedResult;
		}

		public String getActualResult()
		{
			return actualResult;
		}

		public void setActualResult(String actualResult)
		{
			this.actualResult = actualResult;
		}

		public StepStatus getStatus()
		{
			return status;
		}

		public void setStatus(StepStatus status)
		{
			this.status = status;
		}

		public String getScreenshotPath()
		{
			return screenshotPath;
		}

		public void setScreenshotPath(String screenshotPath)
		{
			this.screenshotPath = screenshotPath;
		}
	}

}
