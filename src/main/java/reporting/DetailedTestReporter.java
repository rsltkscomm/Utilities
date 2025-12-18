package reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotType;

import base.DriverContext;
import base.DriverManager;
import reporting.NewSummaryReportGenerator.ModuleStats;

public class DetailedTestReporter
{
	private static final Object MUTEX = new Object();

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

	static List<TestExecution> testExecutions;
	private static String reportPath;
	private static String projectName;
	private static SimpleDateFormat dateFormat;
	private PerformanceMetrics performanceMetrics;

	public DetailedTestReporter(String projectName, String reportPath) {
		DetailedTestReporter.projectName = projectName;
		DetailedTestReporter.reportPath = reportPath;
        DetailedTestReporter.testExecutions = new CopyOnWriteArrayList<>();
		DetailedTestReporter.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.performanceMetrics = new PerformanceMetrics();
	}

	public void addTestExecution(String module, String scenarioId, String testCaseId, String shortDescription, ExecutionStatus status)
	{
		synchronized (MUTEX)
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
	}
	
	public static void createDetailReport()
	{
		detailedTestReporter = new DetailedTestReporter("Detail Test Suite", "test-output");
	}

	public static DetailedTestReporter getReport()
	{
		return detailedTestReporter;
	}
	
	public static void updateStep(
	        boolean status,
	        TestCase failConstant,
	        TestCase passConstant
	) {
	    DriverContext context = DriverManager.getContext();
	    String logPath = null;
	    File harFile = null;

	    Object automationDriver = context.getWebDriver();

	    if (!status) {

	        // 🔹 Selenium: capture browser logs
	        if (automationDriver instanceof org.openqa.selenium.WebDriver) {
	            try {
	                org.openqa.selenium.WebDriver webDriver =
	                        (org.openqa.selenium.WebDriver) automationDriver;

	                LogEntries logs = webDriver.manage()
	                        .logs()
	                        .get(LogType.BROWSER);

	                if (logs != null && !logs.getAll().isEmpty()) {
	                    logPath = saveBrowserLogs(
	                            logs,
	                            failConstant.getDescription()
	                    );
	                }
	            } catch (Exception e) {
	                System.out.println(
	                        "❌ Could not capture Selenium browser logs: "
	                                + e.getMessage()
	                );
	            }
	        }

	        // 🔹 Playwright: NO browser log API
	        // Console logs must be collected via listeners earlier

	        DetailedTestReporter.addStep(
	                failConstant,
	                StepStatus.FAIL,
	                context,
	                logPath,
	                harFile
	        );

	    } else {
	    	Page page = DriverManager.getContext().getPage();
	        DetailedTestReporter.addStep(
	                passConstant,
	                StepStatus.PASS,
	                page,
	                null,
	                null
	        );
	    }
	}

	
	/**
	 * Save browser console logs to a timestamped file
	 */
	private static String saveBrowserLogs(LogEntries logEntries, String stepName)
	{
	    try {
	        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	        String fileName = "browser_logs_" + stepName + "_" + timestamp + ".txt";
	        File file = new File("./logs/" + fileName);

	        StringBuilder content = new StringBuilder();
	        content.append("=====================================================================\n");
	        content.append("BROWSER CONSOLE LOGS (").append(stepName).append(")\n");
	        content.append("=====================================================================\n\n");

	        int count = 1;
	        for (LogEntry entry : logEntries) {
	            content.append(count++)
	                   .append(". [")
	                   .append(entry.getLevel())
	                   .append("] ")
	                   .append(new SimpleDateFormat("HH:mm:ss").format(new Date(entry.getTimestamp())))
	                   .append(" - ")
	                   .append(entry.getMessage())
	                   .append("\n");
	        }

	        try (FileWriter writer = new FileWriter(file)) {
	            writer.write(content.toString());
	        }

	        System.out.println("✅ Browser logs saved: " + file.getAbsolutePath());
	        return file.getAbsolutePath(); 
	    } catch (Exception e) {
	        System.out.println("❌ Failed to save browser logs: " + e.getMessage());
	        return null;
	    }
	}

    public static void addStep(TestCase testCase, StepStatus status, Object context,String logFilePath,File harFile) {
        synchronized (MUTEX) {
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
                execution.setSteps(new ArrayList<>());
                execution.setStatus(ExecutionStatus.PASS);
                // Remove hardcoded total expected steps - calculate dynamically
                execution.setTotalExpectedSteps(0); 
                getReport().getTestExecutions().add(execution);
            } else {
                execution = executionOpt.get();
                if (execution.getSteps() == null) {
                    execution.setSteps(new ArrayList<>());
                }
            }

            // Determine duplicate first based on meaningful identity
            isDuplicate = execution.getSteps().stream()
                    .anyMatch(step -> safeEquals(step.getAction(), testCase.getAction()) 
                            && safeEquals(step.getExpectedResult(), testCase.getExpectedResult()));

            // Increment total expected steps only for new steps
            if (!isDuplicate) {
                execution.setTotalExpectedSteps(execution.getTotalExpectedSteps() + 1);
            }

            if (!isDuplicate) {
                getReport().addTestStep(
                    testCase.getTestCaseId(),
                    execution.getSteps().size() + 1,
                    testCase.getAction(),
                    testCase.getExpectedResult(),
                    testCase.getActualResult(),
                    status,
                    encryptScreenshot(context),
                    logFilePath,
                    harFile
                    
                );

                if (status == StepStatus.FAIL) {
                    execution.setStatus(ExecutionStatus.FAIL);
                }
            }
            
            // Mark execution as complete if explicitly equal and at least one step exists
            if (!isDuplicate && execution.getTotalExpectedSteps() > 0 && execution.getSteps().size() == execution.getTotalExpectedSteps()) {
                execution.setEndTime(new Date());

                // Update ModuleStats map
                ModuleStats stats = NewSummaryReportGenerator.moduleStats.computeIfAbsent(
                    testCase.getModuleName(), m -> new ModuleStats());

                switch (execution.getStatus()) {
                    case PASS:
                        stats.incrementPass();
                        modulePassCount.computeIfAbsent(testCase.getModuleName(), 
                            k -> new AtomicInteger(0)).incrementAndGet();
                        break;
                    case FAIL:
                        stats.incrementFail();
                        moduleFailCount.computeIfAbsent(testCase.getModuleName(), 
                            k -> new AtomicInteger(0)).incrementAndGet();
                        break;
                    case SKIPPED:
                        stats.incrementSkip();
                        moduleSkipCount.computeIfAbsent(testCase.getModuleName(), 
                            k -> new AtomicInteger(0)).incrementAndGet();
                        break;
                }
            }
        }
	}
	
    public static String encryptScreenshot(Object context) {
        if (context == null) {
            return null;
        }

        try {
            // Selenium
            if (context instanceof TakesScreenshot) {
                TakesScreenshot ts = (TakesScreenshot) context;
                return "data:image/png;base64,"
                        + ts.getScreenshotAs(OutputType.BASE64);
            }

            // Playwright
            if (context instanceof Page) {
                Page page = (Page) context;

                byte[] bytes = page.screenshot(
                        new Page.ScreenshotOptions()
                                .setType(ScreenshotType.PNG)
                );

                return "data:image/png;base64,"
                        + Base64.getEncoder().encodeToString(bytes);
            }

        } catch (Exception e) {
            // Optional: log debug info
        }

        return null;
    }


    public static List<TestExecution> getTestExecutions()
	{
		return testExecutions;
	}

	public void addTestStep(String testCaseId, String action, String expectedResult, String actualResult, StepStatus status, String screenshotPath,String filePath,File harFilePath)
	{
		addTestStep(testCaseId, -1, action, expectedResult, actualResult, status, screenshotPath,filePath,harFilePath);
	}

	public void addTestStep(String testCaseId, int stepNumber, String action, String expectedResult, String actualResult, StepStatus status, String screenshotPath,String filePath,File harFilePath)
	{
		synchronized (MUTEX)
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
					step.setLogFilePath(filePath);
					step.setHarFilePath(harFilePath);
					execution.getSteps().add(step);
					break;
				}
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

	static String generateHTMLContent() {
	    StringBuilder html = new StringBuilder();
	    java.text.SimpleDateFormat localDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	    html.append("<div class=\"header\">\n")
	        .append("<img alt=\"Company Logo\" src=\"https://www.resulticks.com/images/logos/resulticks-logo-blue.svg\"/>\n")
	        .append("<h2>Detail Test Report</h2>\n")
	        .append("<img alt=\"Product Logo\" src=\"https://run19.resul.io/assets/resulticks-logo-white-391eec89.svg\"/>\n")
	        .append("</div>\n")
	        .append("<div class=\"environment-ribbon\">\n")
	        .append("<span><strong>Environment:</strong> ").append(getSystemProperty("Environment", "Not Specified")).append("</span>\n")
	        .append("<span><strong>Browser:</strong> ").append(getSystemProperty("Browser", "Not Specified")).append("</span>\n")
	        .append("<span><strong>Release:</strong> ").append(getSystemProperty("ReleaseVersion", "Not Specified")).append("</span>\n")
	        .append("<span><strong>Execution Date:</strong> ").append(localDateFormat.format(new java.util.Date())).append("</span>\n")
	        .append("</div>\n")
	        .append("<div style=\"text-align:right; padding:10px;\">\n")
	        .append("<a class=\"back-btn\" onclick=\"showSummaryReport()\">⬅ Back to Summary Report</a>\n")
	        .append("</div>\n")
	        .append("<div class=\"table-container\">\n")
	        .append("<h3>Test Case Results</h3>\n")
	        .append("<div class=\"toolbar\">\n")
	        .append("<div style=\"display:flex; gap:15px; flex-wrap:wrap; margin:20px 0;\">\n")
	        .append("<div>\n")
	        .append("<label for=\"searchInput\"><b>Search:</b></label>\n")
	        .append("<input id=\"searchInput\" placeholder=\"Search test cases...\" type=\"text\"/>\n")
	        .append("</div>\n")
	        .append("<div>\n")
	        .append("<label for=\"statusFilter\"><b>Filter by Status:</b></label>\n")
	        .append("<select id=\"statusFilter\">\n")
	        .append("<option value=\"\">All</option>\n")
	        .append("<option value=\"PASS\">Passed</option>\n")
	        .append("<option value=\"FAIL\">Failed</option>\n")
	        .append("<option value=\"SKIPPED\">Skipped</option>\n")
	        .append("</select>\n")
	        .append("</div>\n")
	        .append("<div>\n")
	        .append("<button onclick=\"exportToCSV()\" title=\"Export CSV\"><span>📄</span></button>\n")
	        .append("<button onclick=\"exportToJSON()\" title=\"Export JSON\"><span>🗂️</span></button>\n")
	        .append("<button onclick=\"exportToPDF()\" title=\"Export PDF\"><span>📕</span></button>\n")
	        .append("<button onclick=\"window.print()\" title=\"Print Report\"><span>🖨️</span></button>\n")
	        .append("</div>\n")
	        .append("</div>\n")
	        .append("</div>\n")
	        .append("<table id=\"testcaseTable\">\n")
	        .append("<thead><tr><th></th><th>Module</th><th>Test Case ID</th><th>Description</th><th>Status</th><th>Duration</th></tr></thead>\n")
	        .append("<tbody>\n");

	    // Generate test case rows
	    int testCaseCounter = 1;
	    for (TestExecution exec : testExecutions) {
	        String statusIcon = exec.getStatus() == ExecutionStatus.PASS ? "✅ Passed" :
	                             exec.getStatus() == ExecutionStatus.FAIL ? "❌ Failed" : "⚠️ Skipped";

	        String duration = "-";
	        if (exec.getStartTime() != null && exec.getEndTime() != null && exec.getEndTime().after(exec.getStartTime())) {
	            long durationMs = exec.getEndTime().getTime() - exec.getStartTime().getTime();
	            long seconds = durationMs / 1000;
	            duration = seconds + "s";
	        }

	        html.append("<tr data-test-id=\"tc").append(testCaseCounter).append("\" data-expanded=\"false\" onclick=\"toggleDetails(this)\" style=\"cursor:pointer;\">")
	            .append("<td class=\"expand-icon\">+</td>")
	            .append("<td>").append(escapeHtml(exec.getModule())).append("</td>")
	            .append("<td>").append(escapeHtml(exec.getTestCaseId())).append("</td>")
	            .append("<td>").append(escapeHtml(exec.getShortDescription())).append("</td>")
	            .append("<td class=\"").append(getStatusClass(exec.getStatus())).append("\">").append(statusIcon).append("</td>")
	            .append("<td>").append(duration).append("</td>")
	            .append("</tr>\n")
	            .append("<tr class=\"details-row\" id=\"tc").append(testCaseCounter).append("-details\" style=\"display:none;\">")
	            .append("<td colspan=\"6\">\n")
	            .append("<table class=\"step-table\">\n")
	            .append("<tr><th>Action</th><th>Expected Result</th><th>Actual Result</th><th>Status</th><th>Screenshot</th></tr>\n");

	        for (TestStep step : exec.getSteps()) {
	            String stepStatusIcon = step.getStatus() == StepStatus.PASS ? "✅" :
	                                     step.getStatus() == StepStatus.FAIL ? "❌" : "⚠️";

	            html.append("<tr>")
	                .append("<td>").append(escapeHtml(step.getAction())).append("</td>")
	                .append("<td>").append(escapeHtml(step.getExpectedResult())).append("</td>")
	                .append("<td>").append(escapeHtml(step.getActualResult())).append("</td>")
	                .append("<td>").append(stepStatusIcon).append("</td>")
	                .append("<td>").append((step.getScreenshotPath() != null && !step.getScreenshotPath().isEmpty())
	                        ? "<img class=\"screenshot\" src=\"" + step.getScreenshotPath() + "\"/>"
	                        : "-").append("</td></tr>\n");
	        }

	        html.append("</table>\n")
	            .append("</td></tr>\n");

	        testCaseCounter++;
	    }

	    html.append("</tbody>\n")
	        .append("</table>\n")
	        .append("</div>\n")
	        .append("<script>\n")

	        // toggle expand/collapse
	        .append("function toggleDetails(row) {\n")
	        .append("  if (!row) return;\n")
	        .append("  var testId = row.getAttribute('data-test-id');\n")
	        .append("  var detailsRow = document.getElementById(testId + '-details');\n")
	        .append("  if (!detailsRow) return;\n")
	        .append("  var expanded = row.getAttribute('data-expanded') === 'true';\n")
	        .append("  var expandIcon = row.querySelector('.expand-icon');\n")
	        .append("  if (expanded) {\n")
	        .append("    detailsRow.style.display = 'none';\n")
	        .append("    row.setAttribute('data-expanded', 'false');\n")
	        .append("    if (expandIcon) expandIcon.textContent = '+';\n")
	        .append("  } else {\n")
	        .append("    detailsRow.style.display = 'table-row';\n")
	        .append("    row.setAttribute('data-expanded', 'true');\n")
	        .append("    if (expandIcon) expandIcon.textContent = '-';\n")
	        .append("  }\n")
	        .append("}\n")

	        // FIXED: applySearchAndFilter ensures details rows follow main rows
	        .append("function applySearchAndFilter() {\n")
	        .append("  var searchText = document.getElementById('searchInput').value.toLowerCase().trim();\n")
	        .append("  var filterValue = document.getElementById('statusFilter').value;\n")
	        .append("  var mainRows = document.querySelectorAll('#testcaseTable tbody tr:not(.details-row)');\n")
	        .append("  mainRows.forEach(function(row) {\n")
	        .append("    var rowText = row.textContent.toLowerCase();\n")
	        .append("    var statusText = row.cells[4] ? row.cells[4].textContent.toLowerCase() : '';\n")
	        .append("    var rowStatus = statusText.includes('pass') ? 'PASS' : statusText.includes('fail') ? 'FAIL' : statusText.includes('skip') ? 'SKIPPED' : '';\n")
	        .append("    var matchesSearch = !searchText || rowText.includes(searchText);\n")
	        .append("    var matchesFilter = !filterValue || rowStatus === filterValue;\n")
	        .append("    var shouldShow = matchesSearch && matchesFilter;\n")
	        .append("    var testId = row.getAttribute('data-test-id');\n")
	        .append("    var detailsRow = document.getElementById(testId + '-details');\n")
	        .append("    if (shouldShow) {\n")
	        .append("      row.style.display = '';\n")
	        .append("      if (detailsRow) {\n")
	        .append("        var expanded = row.getAttribute('data-expanded') === 'true';\n")
	        .append("        detailsRow.style.display = expanded ? 'table-row' : 'none';\n")
	        .append("      }\n")
	        .append("    } else {\n")
	        .append("      row.style.display = 'none';\n")
	        .append("      if (detailsRow) detailsRow.style.display = 'none';\n")
	        .append("      if (row.querySelector('.expand-icon')) {\n")
	        .append("        row.querySelector('.expand-icon').textContent = '+';\n")
	        .append("        row.setAttribute('data-expanded', 'false');\n")
	        .append("      }\n")
	        .append("    }\n")
	        .append("  });\n")
	        .append("}\n")

	        // event listeners
	        .append("document.addEventListener('DOMContentLoaded', function() {\n")
	        .append("  var searchInput = document.getElementById('searchInput');\n")
	        .append("  var statusFilter = document.getElementById('statusFilter');\n")
	        .append("  if (searchInput) searchInput.addEventListener('input', applySearchAndFilter);\n")
	        .append("  if (statusFilter) statusFilter.addEventListener('change', applySearchAndFilter);\n")
	        .append("});\n")

	        .append("</script>\n");

	    return html.toString();
	}




	
	// Helper method to get status class
	private static String getStatusClass(ExecutionStatus status) {
	    switch (status) {
	        case PASS: return "status-pass";
	        case FAIL: return "status-fail";
	        case SKIPPED: return "status-skipped";
	        default: return "";
	    }
	}

	// Helper method to escape HTML
	private static String escapeHtml(String text) {
	    if (text == null) return "";
	    return text.replace("&", "&amp;")
	              .replace("<", "&lt;")
	              .replace(">", "&gt;")
	              .replace("\"", "&quot;")
	              .replace("'", "&#39;");
	}

	// Helper method to get system property with fallback
	private static String getSystemProperty(String key, String defaultValue) {
	    try {
	        return System.getProperty(key, defaultValue);
	    } catch (Exception e) {
	        return defaultValue;
	    }
	}

	// Utility to safely compare strings (null-safe)
	private static boolean safeEquals(String a, String b) {
	    if (a == null && b == null) return true;
	    if (a == null || b == null) return false;
	    return a.equals(b);
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
		private String logFilePath;
		private File harFilePath;

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
		
		public String getLogFilePath() {
			return logFilePath;
		}
		
		public void setLogFilePath(String logFilePath) {
			this.logFilePath = logFilePath;
		}
		
		public File getHarFilePath() {
			return harFilePath;
		}
		
		public void setHarFilePath(File harFilePath) {
			this.harFilePath = harFilePath;
		}
	}

}
