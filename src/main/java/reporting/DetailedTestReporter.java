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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import reporting.NewSummaryReportGenerator.ModuleStats;

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

	static List<TestExecution> testExecutions;
	private static String reportPath;
	private static String projectName;
	private static SimpleDateFormat dateFormat;
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
	
	public static void updateStep(boolean status, TestCase failConstant, TestCase passConstant, WebDriver driver)
	{
		if (!status)
		{
			DetailedTestReporter.addStep(failConstant, StepStatus.FAIL, driver);
		} else
		{
			DetailedTestReporter.addStep(passConstant, StepStatus.PASS, driver);
		}
	}

	public static void addStep(TestCase testCase, StepStatus status, WebDriver driver) {
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

	    // Increment total expected steps only for new steps
	    if (!isDuplicate) {
	        execution.setTotalExpectedSteps(execution.getTotalExpectedSteps() + 1);
	    }

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
	    
	    // Mark execution as complete if this is the last step being added
	    if (!isDuplicate && execution.getSteps().size() >= execution.getTotalExpectedSteps()) {
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
	
	 public static String encryptScreenshot(WebDriver driver)
		{
			TakesScreenshot ts = (TakesScreenshot) driver;
			String screenshotAs = ts.getScreenshotAs(OutputType.BASE64);
			return "data:image/png;base64," + screenshotAs;
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

	static String generateHTMLContent() {
	    StringBuilder html = new StringBuilder();

	    int totalTests = testExecutions.size();
	    int passedTests = (int) testExecutions.stream().filter(t -> t.getStatus() == ExecutionStatus.PASS).count();
	    int failedTests = (int) testExecutions.stream().filter(t -> t.getStatus() == ExecutionStatus.FAIL).count();
	    int skippedTests = (int) testExecutions.stream().filter(t -> t.getStatus() == ExecutionStatus.SKIPPED).count();

	    // Get unique modules for filter dropdown
	    Set<String> modules = testExecutions.stream()
	        .map(TestExecution::getModule)
	        .collect(Collectors.toSet());

	    html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n")
	        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
	        .append("<title>").append(projectName).append(" - Test Report</title>\n")
	        .append("<style>\n")
	        /* Base styles */
	        .append("body { font-family: 'Segoe UI', Tahoma, sans-serif; background: #f4f6f8; margin: 0; padding: 20px; color: #333; transition: background 0.3s, color 0.3s; }\n")
	        .append(".dark-mode { background: #1e1e1e; color: #eaeaea; }\n")
	        .append(".container { background: #fff; border-radius: 10px; padding: 25px; max-width: 1600px; margin: auto; box-shadow: 0 4px 20px rgba(0,0,0,0.1); transition: background 0.3s; }\n")
	        .append(".dark-mode .container { background: #2b2b2b; }\n")
	        /* Header */
	        .append(".report-header { background: linear-gradient(3deg,white, darkblue); color: white; padding: 20px;box-shadow:6px 10px 20px black; border-radius: 8px; text-align: center; margin-bottom: 20px; }\n")
	        .append(".report-header h1 { margin: 0; font-size: 28px; }\n")
	        .append(".report-header p { margin: 5px 0 0; font-size: 14px; opacity: 0.9; }\n")
	        /* Summary cards */
	        .append(".summary { display: flex; gap: 20px; margin-bottom: 25px;margin-top: 50px; flex-wrap: wrap; justify-content: center; }\n")
	        .append(".summary-cards { flex: 1 1 150px; color: white; border-radius: 10px; text-align: center; cursor: pointer; transition: transform 0.2s ease, box-shadow 0.2s ease; }\n")
	        .append(".summary-cards:hover { transform: translateY(-4px); box-shadow:6px 10px 20px black }\n")
	        .append(".total { background: white;color:#007bff;font-size: 1.2em;font-weight: bold;padding:20px;box-shadow:6px 10px 20px black;}\n")
	        .append(".passed { background: white;color: #28a745;font-size: 1.2em;font-weight: bold;padding:20px;box-shadow:6px 10px 20px black;}\n")
	        .append(".failed { background: white;color: #dc3545;font-size: 1.2em;font-weight: bold;padding:20px;box-shadow:6px 10px 20px black;}\n")
	        .append(".skipped { background: white;color: #ffc107;font-size: 1.2em;font-weight: bold;padding:20px;box-shadow:6px 10px 20px black;}\n")
	        /* Filter controls */
	        .append(".filter-controls { display: flex; gap: 15px;margin-top: 50px; margin-bottom: 15px; flex-wrap: wrap; }\n")
	        .append(".filter-group { display: flex; align-items: center; gap: 8px; }\n")
	        .append(".filter-group label { font-weight: bold; }\n")
	        .append(".filter-group select, .filter-group input { padding: 8px 12px; border-radius: 5px; border: 1px solid #ccc; }\n")
	        .append(".action-buttons { display: flex; gap: 10px; margin-bottom: 30px;margin-top: 30px; }\n")
	        .append(".action-buttons button { padding: 8px 15px; border-radius: 5px; border: none; background: #007bff; color: white; cursor: pointer; }\n")
	        .append(".action-buttons button:hover { background: #0069d9; }\n")
	        /* Table */
	        .append(".test-table { width: 100%;box-shadow:6px 10px 20px black; border-collapse: separate; border-spacing: 0; border-radius: 8px; overflow: hidden; }\n")
	        .append(".test-table th, .test-table td { padding: 10px 12px; }\n")
	        .append(".test-table th { background: royalblue; position: sticky; top: 0;color:white }\n")
	        .append(".dark-mode .test-table th { background: #444; }\n")
	        .append(".test-table tr:nth-child(even) { background: #f9f9f9; }\n")
	        .append(".dark-mode .test-table tr:nth-child(even) { background: #333; }\n")
	        .append(".test-row:hover { background: #e3f2fd; cursor: pointer; }\n")
	        .append(".dark-mode .test-row:hover { background: #3a3a3a; }\n")
	        .append(".hidden { display: none; }\n")
	        /* Status colors */
	        .append(".status-pass { color: #28a745; font-weight: bold; }\n")
	        .append(".status-fail { color: #dc3545; font-weight: bold; }\n")
	        .append(".status-skipped { color: #ffc107; font-weight: bold; }\n")
	        /* Row background colors for PASS/FAIL */
	        .append(".row-pass { background: #C6F7BF !important; }\n")
	        .append(".row-fail { background: #FFB3B3 !important; }\n")
	        .append(".dark-mode .row-pass { background: #225522 !important; }\n")
	        .append(".dark-mode .row-fail { background: #552222 !important; }\n")
	        /* Search */
	        .append("#searchInput { padding: 8px 12px; width: 200px; border-radius: 5px; border: 1px solid #ccc; }\n")
	        /* Description cell ellipsis */
	        .append("td.description-cell { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 300px; }\n")
	        .append("td.description-cell:hover { white-space: normal; background: #ffffcc; position: relative; z-index: 1; }\n")
	        .append(".dark-mode td.description-cell:hover { background: #444; color: #fff; }\n")
	        /* Accordion step details */
	        .append(".accordion-content { display: none; overflow: hidden; background: #f4f4f4; }\n")
	        .append(".dark-mode .accordion-content { background: #2f2f2f; }\n")
	        /* Screenshot thumbnail */
	        .append(".screenshot-thumb { max-width: 80px; max-height: 60px; cursor: pointer; border-radius: 4px; border: 1px solid #ddd; transition: transform 0.2s; }\n")
	        .append(".screenshot-thumb:hover { transform: scale(1.05); box-shadow: 0 2px 8px rgba(0,0,0,0.2); }\n")
	        /* Modal */
	        .append(".modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.9); }\n")
	        .append(".modal-content { display: block; margin: auto; max-width: 90%; max-height: 90%; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); }\n")
	        .append(".close { position: absolute; top: 20px; right: 35px; color: white; font-size: 40px; font-weight: bold; cursor: pointer; }\n")
	        /* Ellipsis for accordion step detail cells */
	        .append(".accordion-content table.test-table td {\n")
	        .append("  white-space: nowrap;\n")
	        .append("  overflow: hidden;\n")
	        .append("  text-overflow: ellipsis;\n")
	        .append("  max-width: 200px;\n")
	        .append("}\n")
	        /* Optional: Hover to expand ellipsis text in accordion */
	        .append(".accordion-content table.test-table td:hover {\n")
	        .append("  white-space: normal;\n")
	        .append("  background: #ffffcc;\n")
	        .append("  position: relative;\n")
	        .append("  z-index: 1;\n")
	        .append("}\n")
	        .append(".dark-mode .accordion-content table.test-table td:hover {\n")
	        .append("  background: #444;\n")
	        .append("  color: #fff;\n")
	        .append("}\n")
	        .append("</style>\n</head>\n<body>\n")

	        .append("<div class=\"container\">\n")

	        // Header
	        .append("<div class='report-header'>")
	        .append("<h1>").append(projectName).append(" - Test Report</h1>")
	        .append("<p>Generated on: ").append(dateFormat.format(new java.util.Date())).append("</p>")
	        .append("</div>")

	        // Summary cards
	        .append("<div class=\"summary\">\n")
	        .append("<div class=\"summary-cards total\" onclick=\"filterTests('all')\"><h3>").append(totalTests).append("</h3><p>Total</p></div>\n")
	        .append("<div class=\"summary-cards passed\" onclick=\"filterTests('PASS')\"><h3>").append(passedTests).append("</h3><p>Passed</p></div>\n")
	        .append("<div class=\"summary-cards failed\" onclick=\"filterTests('FAIL')\"><h3>").append(failedTests).append("</h3><p>Failed</p></div>\n")
	        .append("<div class=\"summary-cards skipped\" onclick=\"filterTests('SKIPPED')\"><h3>").append(skippedTests).append("</h3><p>Skipped</p></div>\n")
	        .append("</div>\n")

	        // Filter controls
	        .append("<div class=\"filter-controls\">\n")
	        .append("<div class=\"filter-group\">\n")
	        .append("<label for=\"searchInput\">Search:</label>\n")
	        .append("<input type=\"text\" id=\"searchInput\" placeholder=\"Search...\" onkeyup=\"searchTests()\">\n")
	        .append("</div>\n")
	        .append("<div class=\"filter-group\">\n")
	        .append("<label for=\"moduleFilter\">Module:</label>\n")
	        .append("<select id=\"moduleFilter\" onchange=\"filterByModule()\">\n")
	        .append("<option value=\"\">All Modules</option>\n");
	    
	    // Add module options
	    for (String module : modules) {
	        html.append("<option value=\"").append(module).append("\">").append(module).append("</option>\n");
	    }
	    
	    html.append("</select>\n")
	        .append("</div>\n")
	        .append("<div class=\"filter-group\">\n")
	        .append("<label for=\"statusFilter\">Status:</label>\n")
	        .append("<select id=\"statusFilter\" onchange=\"filterByStatus()\">\n")
	        .append("<option value=\"\">All Statuses</option>\n")
	        .append("<option value=\"PASS\">Passed</option>\n")
	        .append("<option value=\"FAIL\">Failed</option>\n")
	        .append("<option value=\"SKIPPED\">Skipped</option>\n")
	        .append("</select>\n")
	        .append("</div>\n")
	        .append("<button onclick=\"clearFilters()\">Clear Filters</button>\n")
	        .append("</div>\n")

	        // Action buttons
	        .append("<div class=\"action-buttons\">\n")
	        .append("<button onclick=\"exportToCSV()\">Export to CSV</button>\n")
	        .append("<button onclick=\"exportToJSON()\">Export to JSON</button>\n")
	        .append("<button onclick=\"printReport()\">Print Report</button>\n")
	        .append("<button onclick=\"toggleDarkMode()\">Toggle Dark Mode</button>\n")
	        .append("</div>\n")

	        // Table start
	        .append("<table class=\"test-table\">\n<thead><tr><th>Module</th><th>Scenario ID</th><th>Test Case ID</th><th>Description</th><th>Start</th><th>End</th><th>Duration (ms)</th><th>Status</th></tr></thead><tbody>\n");

	    for (TestExecution exec : testExecutions) {
	        long duration = exec.getEndTime().getTime() - exec.getStartTime().getTime();
	        String statusClass = "status-" + exec.getStatus().toString().toLowerCase();
	        String rowColorClass = exec.getStatus() == ExecutionStatus.PASS ? "row-pass" :
	                               exec.getStatus() == ExecutionStatus.FAIL ? "row-fail" : "";

	        html.append("<tr class=\"test-row ").append(rowColorClass)
	            .append("\" data-status=\"").append(exec.getStatus())
	            .append("\" data-module=\"").append(exec.getModule())
	            .append("\" onclick=\"toggleAccordion(event, this)\">")
	            .append("<td>").append(exec.getModule()).append("</td>")
	            .append("<td>").append(exec.getScenarioId()).append("</td>")
	            .append("<td>").append(exec.getTestCaseId()).append("</td>")
	            .append("<td class='description-cell' title='").append(exec.getShortDescription()).append("'>").append(exec.getShortDescription()).append("</td>")
	            .append("<td>").append(dateFormat.format(exec.getStartTime())).append("</td>")
	            .append("<td>").append(dateFormat.format(exec.getEndTime())).append("</td>")
	            .append("<td>").append(duration).append("</td>")
	            .append("<td class=\"").append(statusClass).append("\">").append(exec.getStatus()).append("</td>")
	            .append("</tr>\n");

	        html.append("<tr class=\"accordion-content\"><td colspan=\"8\"><table class=\"test-table\">")
	            .append("<thead><tr><th>Step No</th><th>Action</th><th>Expected</th><th>Actual</th><th>Status</th><th>Screenshot</th></tr></thead><tbody>");

	        for (TestStep step : exec.getSteps()) {
	            String stepStatusClass = "status-" + step.getStatus().toString().toLowerCase();
	            String stepRowColorClass = step.getStatus() == StepStatus.PASS ? "row-pass" :
	                                       step.getStatus() == StepStatus.FAIL ? "row-fail" : "";

	            html.append("<tr class=\"").append(stepRowColorClass).append("\"><td>").append(step.getStepNo()).append("</td>")
	                .append("<td>").append(step.getAction()).append("</td>")
	                .append("<td>").append(step.getExpectedResult()).append("</td>")
	                .append("<td>").append(step.getActualResult()).append("</td>")
	                .append("<td class=\"").append(stepStatusClass).append("\">").append(step.getStatus()).append("</td>")
	                .append("<td>").append((step.getScreenshotPath() != null && !step.getScreenshotPath().isEmpty())
	                        ? "<img src='" + step.getScreenshotPath() + "' class='screenshot-thumb' onclick='openModal(this.src)'>"
	                        : "-").append("</td></tr>");
	        }
	        html.append("</tbody></table></td></tr>\n");
	    }

	    html.append("</tbody></table>\n</div>\n")
	        .append("<div id=\"screenshotModal\" class=\"modal\">\n")
	        .append("<span class=\"close\" onclick=\"closeModal()\">&times;</span>\n")
	        .append("<img class=\"modal-content\" id=\"modalImage\">\n")
	        .append("</div>\n")

	        .append("<script>\n")
	        // Search function
	        .append("function searchTests() {\n")
	        .append("  var term = document.getElementById('searchInput').value.toLowerCase();\n")
	        .append("  document.querySelectorAll('.test-row').forEach(r => {\n")
	        .append("    r.style.display = r.textContent.toLowerCase().includes(term) ? '' : 'none';\n")
	        .append("  });\n")
	        .append("  document.querySelectorAll('.accordion-content').forEach(a => a.style.display = 'none');\n")
	        .append("}\n")
	        
	        // Status filter function
	        .append("function filterTests(status) {\n")
	        .append("  document.querySelectorAll('.test-row').forEach((r, i) => {\n")
	        .append("    if (status === 'all' || r.getAttribute('data-status') === status) {\n")
	        .append("      r.style.display = '';\n")
	        .append("      document.querySelectorAll('.accordion-content')[i].style.display = 'none';\n")
	        .append("    } else {\n")
	        .append("      r.style.display = 'none';\n")
	        .append("      document.querySelectorAll('.accordion-content')[i].style.display = 'none';\n")
	        .append("    }\n")
	        .append("  });\n")
	        .append("}\n")
	        
	        // Module filter
	        .append("function filterByModule() {\n")
	        .append("    const selectedModule = document.getElementById('moduleFilter').value;\n")
	        .append("    const rows = document.querySelectorAll('.test-row');\n")
	        .append("    \n")
	        .append("    rows.forEach(row => {\n")
	        .append("        const module = row.getAttribute('data-module');\n")
	        .append("        if (!selectedModule || module === selectedModule) {\n")
	        .append("            row.classList.remove('hidden');\n")
	        .append("        } else {\n")
	        .append("            row.classList.add('hidden');\n")
	        .append("        }\n")
	        .append("    });\n")
	        .append("}\n")
	        .append("        \n")
	        // Status filter
	        .append("function filterByStatus() {\n")
	        .append("    const selectedStatus = document.getElementById('statusFilter').value;\n")
	        .append("    const rows = document.querySelectorAll('.test-row');\n")
	        .append("    \n")
	        .append("    rows.forEach(row => {\n")
	        .append("        const status = row.getAttribute('data-status');\n")
	        .append("        if (!selectedStatus || status === selectedStatus) {\n")
	        .append("            row.classList.remove('hidden');\n")
	        .append("        } else {\n")
	        .append("            row.classList.add('hidden');\n")
	        .append("        }\n")
	        .append("    });\n")
	        .append("}\n")
	        .append("        \n")
	        // Clear all filters
	        .append("function clearFilters() {\n")
	        .append("    document.getElementById('searchInput').value = '';\n")
	        .append("    document.getElementById('moduleFilter').value = '';\n")
	        .append("    document.getElementById('statusFilter').value = '';\n")
	        .append("    \n")
	        .append("    const rows = document.querySelectorAll('.test-row');\n")
	        .append("    rows.forEach(row => row.classList.remove('hidden'));\n")
	        .append("}\n")
	        .append("        \n")
	        // Export functions
	        .append("function exportToCSV() {\n")
	        .append("    const rows = document.querySelectorAll('.test-row:not(.hidden)');\n")
	        .append("    let csv = 'Module,Scenario ID,Test Case ID,Short Description,Start Time,End Time,Duration (ms),Execution Status\\n';\n")
	        .append("    \n")
	        .append("    rows.forEach(row => {\n")
	        .append("        const cells = row.querySelectorAll('td');\n")
	        .append("        const rowData = [];\n")
	        .append("        for (let i = 0; i < 8; i++) {\n")
	        .append("            rowData.push('\"' + cells[i].textContent.trim() + '\"');\n")
	        .append("        }\n")
	        .append("        csv += rowData.join(',') + '\\n';\n")
	        .append("    });\n")
	        .append("    \n")
	        .append("    const blob = new Blob([csv], { type: 'text/csv' });\n")
	        .append("    const url = window.URL.createObjectURL(blob);\n")
	        .append("    const a = document.createElement('a');\n")
	        .append("    a.href = url;\n")
	        .append("    a.download = 'test-report.csv';\n")
	        .append("    a.click();\n")
	        .append("}\n")
	        .append("        \n")
	        .append("function exportToJSON() {\n")
	        .append("    const rows = document.querySelectorAll('.test-row:not(.hidden)');\n")
	        .append("    const data = [];\n")
	        .append("    \n")
	        .append("    rows.forEach(row => {\n")
	        .append("        const cells = row.querySelectorAll('td');\n")
	        .append("        data.push({\n")
	        .append("            module: cells[0].textContent.trim(),\n")
	        .append("            scenarioId: cells[1].textContent.trim(),\n")
	        .append("            testCaseId: cells[2].textContent.trim(),\n")
	        .append("            shortDescription: cells[3].textContent.trim(),\n")
	        .append("            startTime: cells[4].textContent.trim(),\n")
	        .append("            endTime: cells[5].textContent.trim(),\n")
	        .append("            duration: cells[6].textContent.trim(),\n")
	        .append("            executionStatus: cells[7].textContent.trim()\n")
	        .append("        });\n")
	        .append("    });\n")
	        .append("    \n")
	        .append("    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });\n")
	        .append("    const url = window.URL.createObjectURL(blob);\n")
	        .append("    const a = document.createElement('a');\n")
	        .append("    a.href = url;\n")
	        .append("    a.download = 'test-report.json';\n")
	        .append("    a.click();\n")
	        .append("}\n")
	        .append("        \n")
	        .append("function printReport() {\n")
	        .append("    window.print();\n")
	        .append("}\n")
	        .append("        \n")
	        // Accordion toggle
	        .append("function toggleAccordion(e, row) {\n")
	        .append("  if (e.target.tagName === 'A' || e.target.tagName === 'IMG') return;\n")
	        .append("  var content = row.nextElementSibling;\n")
	        .append("  document.querySelectorAll('.accordion-content').forEach(c => {\n")
	        .append("    if (c !== content) { c.style.display = 'none'; }\n")
	        .append("  });\n")
	        .append("  content.style.display = (content.style.display === 'table-row') ? 'none' : 'table-row';\n")
	        .append("}\n")
	        
	        // Screenshot modal functions
	        .append("function openModal(src) {\n")
	        .append("  var modal = document.getElementById('screenshotModal');\n")
	        .append("  var modalImg = document.getElementById('modalImage');\n")
	        .append("  modal.style.display = 'block';\n")
	        .append("  modalImg.src = src;\n")
	        .append("  modal.onclick = function(event) {\n")
	        .append("    if (event.target === modal) {\n")
	        .append("      closeModal();\n")
	        .append("    }\n")
	        .append("  };\n")
	        .append("}\n")
	        .append("function closeModal() {\n")
	        .append("  document.getElementById('screenshotModal').style.display = 'none';\n")
	        .append("}\n")
	        
	        // Dark mode toggle
	        .append("function toggleDarkMode() {\n")
	        .append("  document.body.classList.toggle('dark-mode');\n")
	        .append("  localStorage.setItem('darkMode', document.body.classList.contains('dark-mode'));\n")
	        .append("}\n")
	        .append("window.onload = function() {\n")
	        .append("  if (localStorage.getItem('darkMode') === 'true') {\n")
	        .append("    document.body.classList.add('dark-mode');\n")
	        .append("  }\n")
	        .append("};\n")
	        .append("</script>\n</body></html>");

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
