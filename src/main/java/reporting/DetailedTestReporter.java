package reporting;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import base.DriverManager;
import reporting.NewSummaryReportGenerator.ModuleStats;

public class DetailedTestReporter
{
	private static final Object MUTEX = new Object();
//	private static String escapeHtml(String value)
//	{
//		if (value == null)
//			return "";
//		StringBuilder out = new StringBuilder(Math.max(16, value.length()));
//		for (int i = 0; i < value.length(); i++)
//		{
//			char c = value.charAt(i);
//			switch (c)
//			{
//				case '<': out.append("&lt;"); break;
//				case '>': out.append("&gt;"); break;
//				case '"': out.append("&quot;"); break;
//				case '\'': out.append("&#39;"); break;
//				case '&': out.append("&amp;"); break;
//				default: out.append(c);
//			}
//		}
//		return out.toString();
//	}
	
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
	
	public static void updateStep(boolean status, TestCase failConstant, TestCase passConstant)
	{
		if (!status)
		{
			DetailedTestReporter.addStep(failConstant, StepStatus.FAIL, DriverManager.getDriver());
		} else
		{
			DetailedTestReporter.addStep(passConstant, StepStatus.PASS, DriverManager.getDriver());
		}
	}

    public static void addStep(TestCase testCase, StepStatus status, WebDriver driver) {
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
                    .anyMatch(step -> step.getAction().equals(testCase.getAction()) 
                            && step.getExpectedResult().equals(testCase.getExpectedResult()));

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
                    encryptScreenshot(driver)
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
	
     public static String encryptScreenshot(WebDriver driver)
        {
            if (driver == null) {
                return null;
            }
            try {
                if (driver instanceof TakesScreenshot) {
                    TakesScreenshot ts = (TakesScreenshot) driver;
                    String screenshotAs = ts.getScreenshotAs(OutputType.BASE64);
                    return "data:image/png;base64," + screenshotAs;
                }
            } catch (Exception e) {
                // ignore screenshot errors and proceed without image
            }
            return null;
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
	        .append("<tr><th></th><th>Module</th><th>Test Case ID</th><th>Description</th><th>Status</th><th>Duration</th></tr>\n");

	    // Generate test case rows
	    int testCaseCounter = 1;
	    for (TestExecution exec : testExecutions) {
	        String statusIcon = exec.getStatus() == ExecutionStatus.PASS ? "✅ Passed" :
	                          exec.getStatus() == ExecutionStatus.FAIL ? "❌ Failed" : "⚠️ Skipped";
	        
	        // Calculate duration
	        String duration = "-";
	        if (exec.getStartTime() != null && exec.getEndTime() != null && exec.getEndTime().after(exec.getStartTime())) {
	            long durationMs = exec.getEndTime().getTime() - exec.getStartTime().getTime();
	            long seconds = durationMs / 1000;
	            duration = seconds + "s";
	        }

	        html.append("<tr onclick=\"toggleDetails('tc").append(testCaseCounter).append("', this)\" style=\"cursor:pointer;\">")
	            .append("<td>+</td>")
	            .append("<td>").append(escapeHtml(exec.getModule())).append("</td>")
	            .append("<td>").append(escapeHtml(exec.getTestCaseId())).append("</td>")
	            .append("<td>").append(escapeHtml(exec.getShortDescription())).append("</td>")
	            .append("<td class=\"").append(getStatusClass(exec.getStatus())).append("\">").append(statusIcon).append("</td>")
	            .append("<td>").append(duration).append("</td>")
	            .append("</tr>\n")
	            .append("<tr class=\"details\" id=\"tc").append(testCaseCounter).append("\"><td colspan=\"6\">\n")
	            .append("<table class=\"step-table\">\n")
	            .append("<tr><th>Action</th><th>Expected Result</th><th>Actual Result</th><th>Status</th><th>Screenshot</th></tr>\n");

	        // Generate step rows
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

	    html.append("</table>\n")
	        .append("</div>\n")
	        .append("<script>\n")
	        .append("function toggleDetails(id, row) {\n")
	        .append("  var details = document.getElementById(id);\n")
	        .append("  if (details.style.display === 'table-row') {\n")
	        .append("    details.style.display = 'none';\n")
	        .append("    row.cells[0].innerText = '+';\n")
	        .append("  } else {\n")
	        .append("    details.style.display = 'table-row';\n")
	        .append("    row.cells[0].innerText = '-';\n")
	        .append("  }\n")
	        .append("}\n")
	        .append("// Lightbox functionality\n")
	        .append("var modal = document.getElementById(\"lightboxModal\");\n")
	        .append("var modalImg = document.getElementById(\"lightboxImage\");\n")
	        .append("var closeBtn = document.getElementById(\"closeModal\");\n")
	        .append("document.querySelectorAll('.screenshot').forEach(function(img) {\n")
	        .append("  img.onclick = function() {\n")
	        .append("    modal.style.display = \"block\";\n")
	        .append("    modalImg.src = this.src;\n")
	        .append("  }\n")
	        .append("});\n")
	        .append("closeBtn.onclick = function() {\n")
	        .append("  modal.style.display = \"none\";\n")
	        .append("}\n")
	        .append("modal.onclick = function(e) {\n")
	        .append("  if (e.target == modal) {\n")
	        .append("    modal.style.display = \"none\";\n")
	        .append("  }\n")
	        .append("}\n")
	        .append("// Search function\n")
	        .append("function searchTests() {\n")
	        .append("  let input = document.getElementById(\"searchInput\").value.toLowerCase();\n")
	        .append("  let rows = document.querySelectorAll(\"#testcaseTable tr\");\n")
	        .append("  for (let i = 1; i < rows.length; i++) {\n")
	        .append("    let row = rows[i];\n")
	        .append("    if (row.classList.contains('details')) continue;\n")
	        .append("    let text = row.innerText.toLowerCase();\n")
	        .append("    row.style.display = text.includes(input) ? \"\" : \"none\";\n")
	        .append("    // Hide corresponding detail rows\n")
	        .append("    if (i + 1 < rows.length && rows[i + 1].classList.contains('details')) {\n")
	        .append("      rows[i + 1].style.display = text.includes(input) ? \"\" : \"none\";\n")
	        .append("    }\n")
	        .append("  }\n")
	        .append("}\n")
	        .append("// Filter function\n")
	        .append("document.getElementById(\"statusFilter\").addEventListener(\"change\", function() {\n")
	        .append("  let filter = this.value;\n")
	        .append("  let rows = document.querySelectorAll(\"#testcaseTable tr\");\n")
	        .append("  for (let i = 1; i < rows.length; i++) {\n")
	        .append("    let row = rows[i];\n")
	        .append("    if (row.classList.contains('details')) continue;\n")
	        .append("    let statusCell = row.cells[4];\n")
	        .append("    let status = statusCell.textContent.includes('Passed') ? 'PASS' : \n")
	        .append("                 statusCell.textContent.includes('Failed') ? 'FAIL' : 'SKIPPED';\n")
	        .append("    if (!filter || status === filter) {\n")
	        .append("      row.style.display = \"\";\n")
	        .append("      // Show corresponding detail row\n")
	        .append("      if (i + 1 < rows.length && rows[i + 1].classList.contains('details')) {\n")
	        .append("        rows[i + 1].style.display = \"\";\n")
	        .append("      }\n")
	        .append("    } else {\n")
	        .append("      row.style.display = \"none\";\n")
	        .append("      // Hide corresponding detail row\n")
	        .append("      if (i + 1 < rows.length && rows[i + 1].classList.contains('details')) {\n")
	        .append("        rows[i + 1].style.display = \"none\";\n")
	        .append("      }\n")
	        .append("    }\n")
	        .append("  }\n")
	        .append("});\n")
	        .append("document.getElementById(\"searchInput\").addEventListener(\"keyup\", searchTests);\n")
	        .append("// Export functions\n")
	        .append("function exportToCSV() {\n")
	        .append("  let visibleRows = [];\n")
	        .append("  let rows = document.querySelectorAll(\"#testcaseTable tr\");\n")
	        .append("  for (let i = 1; i < rows.length; i++) {\n")
	        .append("    if (!rows[i].classList.contains('details') && rows[i].style.display !== 'none') {\n")
	        .append("      visibleRows.push(rows[i]);\n")
	        .append("    }\n")
	        .append("  }\n")
	        .append("  let csv = [];\n")
	        .append("  visibleRows.forEach(r => {\n")
	        .append("    let cols = r.querySelectorAll(\"td\");\n")
	        .append("    // Skip the first column (expand/collapse icon)\n")
	        .append("    let rowData = [];\n")
	        .append("    for (let i = 1; i < cols.length; i++) {\n")
	        .append("      rowData.push('\"' + cols[i].innerText + '\"');\n")
	        .append("    }\n")
	        .append("    csv.push(rowData.join(\",\"));\n")
	        .append("  });\n")
	        .append("  let blob = new Blob([csv.join(\"\\n\")], { type: \"text/csv\" });\n")
	        .append("  let a = document.createElement(\"a\");\n")
	        .append("  a.href = URL.createObjectURL(blob);\n")
	        .append("  a.download = \"detailed_report.csv\";\n")
	        .append("  a.click();\n")
	        .append("}\n")
	        .append("function exportToJSON() {\n")
	        .append("  let visibleRows = [];\n")
	        .append("  let rows = document.querySelectorAll(\"#testcaseTable tr\");\n")
	        .append("  for (let i = 1; i < rows.length; i++) {\n")
	        .append("    if (!rows[i].classList.contains('details') && rows[i].style.display !== 'none') {\n")
	        .append("      visibleRows.push(rows[i]);\n")
	        .append("    }\n")
	        .append("  }\n")
	        .append("  let data = [];\n")
	        .append("  visibleRows.forEach(r => {\n")
	        .append("    let cols = r.querySelectorAll(\"td\");\n")
	        .append("    data.push({\n")
	        .append("      module: cols[1].innerText,\n")
	        .append("      testCaseId: cols[2].innerText,\n")
	        .append("      description: cols[3].innerText,\n")
	        .append("      status: cols[4].innerText,\n")
	        .append("      duration: cols[5].innerText\n")
	        .append("    });\n")
	        .append("  });\n")
	        .append("  let blob = new Blob([JSON.stringify(data, null, 2)], { type: \"application/json\" });\n")
	        .append("  let a = document.createElement(\"a\");\n")
	        .append("  a.href = URL.createObjectURL(blob);\n")
	        .append("  a.download = \"detailed_report.json\";\n")
	        .append("  a.click();\n")
	        .append("}\n")
	        .append("function exportToPDF() {\n")
	        .append("  var element = document.getElementById('detailed-section');\n")
	        .append("  html2pdf().from(element).save(\"detailed_report.pdf\");\n")
	        .append("}\n")
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
