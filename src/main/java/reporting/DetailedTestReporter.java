package reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotType;

import base.DriverContext;
import base.DriverManager;

public class DetailedTestReporter {

    private static final Object MUTEX = new Object();

    private static DetailedTestReporter detailedTestReporter;

    public enum ExecutionStatus { PASS, FAIL, SKIPPED }
    public enum StepStatus { PASS, FAIL, SKIPPED }

    public static List<TestExecution> testExecutions = new CopyOnWriteArrayList<>();

    private static String reportPath;
    private static SimpleDateFormat dateFormat;

    /* ===============================
       LIFECYCLE
       =============================== */
    public DetailedTestReporter(String projectName, String reportPath) {
        DetailedTestReporter.reportPath = reportPath;
        DetailedTestReporter.testExecutions = new CopyOnWriteArrayList<>();
        DetailedTestReporter.dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static void createDetailReport() {
        detailedTestReporter =
                new DetailedTestReporter("Detail Test Suite", "test-output");
    }

    public static DetailedTestReporter getReport() {
        return detailedTestReporter;
    }

    /* ===============================
       SAFE ACCESS
       =============================== */
    public static List<TestExecution> getTestExecutionsSafe() {
        return testExecutions == null
                ? Collections.emptyList()
                : testExecutions;
    }

    public static List<TestExecution> getTestExecutions() {
        return testExecutions;
    }

    /* ===============================
       TEST EXECUTION TRACKING
       =============================== */
    public void addTestExecution(
            String module,
            String scenarioId,
            String testCaseId,
            String shortDescription,
            ExecutionStatus status
    ) {
        synchronized (MUTEX) {

            boolean exists = testExecutions.stream()
                    .anyMatch(e -> e.getTestCaseId().equals(testCaseId));

            if (exists) {
                System.out.println("⚠️ Duplicate Test Case ID skipped: " + testCaseId);
                return;
            }

            TestExecution execution = new TestExecution();
            execution.setModule(module);
            execution.setScenarioId(scenarioId);
            execution.setTestCaseId(testCaseId);
            execution.setShortDescription(shortDescription);
            execution.setStartTime(new Date());
            execution.setStatus(status);
            execution.setSteps(new ArrayList<>());

            testExecutions.add(execution);
        }
    }

    /* ===============================
       STEP HANDLING
       =============================== */
    public static void updateStep(
            boolean status,
            TestCase failConstant,
            TestCase passConstant
    ) {
        DriverContext context = DriverManager.getContext();
        Object driver = context.getWebDriver();
        String logPath = null;

        if (!status && driver instanceof WebDriver) {
            try {
                LogEntries logs =
                        ((WebDriver) driver).manage().logs().get(LogType.BROWSER);
                if (logs != null && !logs.getAll().isEmpty()) {
                    logPath = saveBrowserLogs(
                            logs,
                            failConstant.getDescription()
                    );
                }
            } catch (Exception ignored) {}
        }

        Page page = context.getPage();

        addStep(
                status ? passConstant : failConstant,
                status ? StepStatus.PASS : StepStatus.FAIL,
                page,
                logPath,
                null
        );
    }

    private static String saveBrowserLogs(
            LogEntries logEntries,
            String stepName
    ) {
        try {
            String ts =
                    new SimpleDateFormat("yyyyMMdd_HHmmss")
                            .format(new Date());

            File file =
                    new File("./logs/browser_logs_" + stepName + "_" + ts + ".txt");

            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                int i = 1;
                for (LogEntry entry : logEntries) {
                    writer.write(
                            i++ + ". [" + entry.getLevel() + "] "
                                    + new Date(entry.getTimestamp())
                                    + " - " + entry.getMessage() + "\n"
                    );
                }
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static void addStep(
            TestCase testCase,
            StepStatus status,
            Object context,
            String logFilePath,
            File harFile
    ) {
        synchronized (MUTEX) {

            Optional<TestExecution> opt =
                    getReport().getTestExecutions()
                            .stream()
                            .filter(e ->
                                    e.getTestCaseId()
                                            .equals(testCase.getTestCaseId()))
                            .findFirst();

            TestExecution execution = opt.orElseGet(() -> {
                TestExecution e = new TestExecution();
                e.setModule(testCase.getModuleName());
                e.setScenarioId(testCase.getExecutionId());
                e.setTestCaseId(testCase.getTestCaseId());
                e.setShortDescription(testCase.getDescription());
                e.setStartTime(new Date());
                e.setStatus(ExecutionStatus.PASS);
                e.setSteps(new ArrayList<>());
                getReport().getTestExecutions().add(e);
                return e;
            });

            boolean duplicate =
                    execution.getSteps().stream().anyMatch(s ->
                            Objects.equals(s.getAction(), testCase.getAction())
                                    && Objects.equals(
                                    s.getExpectedResult(),
                                    testCase.getExpectedResult())
                    );

            if (duplicate) return;

            TestStep step = new TestStep();
            step.setStepNo(execution.getSteps().size() + 1);
            step.setAction(testCase.getAction());
            step.setExpectedResult(testCase.getExpectedResult());
            step.setActualResult(testCase.getActualResult());
            step.setStatus(status);
            step.setScreenshotPath(encryptScreenshot(context));
            step.setLogFilePath(logFilePath);
            step.setHarFilePath(harFile);

            execution.getSteps().add(step);

            if (status == StepStatus.FAIL) {
                execution.setStatus(ExecutionStatus.FAIL);
            }

            execution.setEndTime(new Date());
        }
    }

    public static String encryptScreenshot(Object context) {
        try {
            if (context instanceof TakesScreenshot ts) {
                return "data:image/png;base64," +
                        ts.getScreenshotAs(OutputType.BASE64);
            }
            if (context instanceof Page page) {
                byte[] bytes =
                        page.screenshot(
                                new Page.ScreenshotOptions()
                                        .setType(ScreenshotType.PNG)
                        );
                return "data:image/png;base64," +
                        Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private static String escapeHtml(String text) {
	    if (text == null) return "";
	    return text.replace("&", "&amp;")
	              .replace("<", "&lt;")
	              .replace(">", "&gt;")
	              .replace("\"", "&quot;")
	              .replace("'", "&#39;");
	}
    
    private static String getAsString(com.google.gson.JsonObject o, String key) {
	    return o.has(key) && !o.get(key).isJsonNull()
	            ? o.get(key).getAsString()
	            : "";
	}
    
    private static String getStatusClass(String status) {
	    return switch (status) {
	        case "PASS" -> "status-pass";
	        case "FAIL" -> "status-fail";
	        case "SKIPPED" -> "status-skipped";
	        default -> "";
	    };
	}
    
    /* ===============================
       HTML FROM JSON (UNCHANGED)
       =============================== */
    public static String generateHTMLContentFromJson(String unifiedJson) {

	    JsonObject root =
	            new JsonParser().parse(unifiedJson).getAsJsonObject();

	    JsonObject meta = root.getAsJsonObject("meta");
	    JsonArray details = root.getAsJsonArray("details");

	    String environment = meta.get("environment").getAsString();
	    String browser = meta.get("browser").getAsString();
	    String release = meta.get("release").getAsString();
	    String executionDate = meta.get("executionDate").getAsString();

	    StringBuilder html = new StringBuilder();

	    // ================= HEADER =================
	    html.append("<div class=\"header\">")
	        .append("<img alt=\"Company Logo\" src=\"https://www.resulticks.com/images/logos/resulticks-logo-blue.svg\"/>")
	        .append("<h2>Detail Test Report</h2>")
	        .append("<img alt=\"Product Logo\" src=\"https://run19.resul.io/assets/resulticks-logo-white-391eec89.svg\"/>")
	        .append("</div>");

	    // ================= ENV RIBBON =================
	    html.append("<div class=\"environment-ribbon\">")
	        .append("<span><strong>Environment:</strong> ").append(escapeHtml(environment)).append("</span>")
	        .append("<span><strong>Browser:</strong> ").append(escapeHtml(browser)).append("</span>")
	        .append("<span><strong>Release:</strong> ").append(escapeHtml(release)).append("</span>")
	        .append("<span><strong>Execution Date:</strong> ").append(escapeHtml(executionDate)).append("</span>")
	        .append("</div>");

	    // ================= BACK LINK =================
	    html.append("<div style=\"text-align:right; padding:10px;\">")
	        .append("<a class=\"back-btn\" onclick=\"showSummaryReport()\">⬅ Back to Summary Report</a>")
	        .append("</div>");

	    // ================= TABLE + TOOLBAR =================
	    html.append("<div class=\"table-container\">")
	        .append("<h3>Test Case Results</h3>")

	        // Toolbar (search + filter)
	        .append("<div class=\"toolbar\">")
	        .append("<div style=\"display:flex; gap:15px; flex-wrap:wrap; margin:20px 0;\">")

	        .append("<div>")
	        .append("<label><b>Search:</b></label>")
	        .append("<input id=\"searchInput\" placeholder=\"Search test cases...\" type=\"text\"/>")
	        .append("</div>")

	        .append("<div>")
	        .append("<label><b>Filter by Status:</b></label>")
	        .append("<select id=\"statusFilter\">")
	        .append("<option value=\"\">All</option>")
	        .append("<option value=\"PASS\">Passed</option>")
	        .append("<option value=\"FAIL\">Failed</option>")
	        .append("<option value=\"SKIPPED\">Skipped</option>")
	        .append("</select>")
	        .append("</div>")

	        .append("</div></div>");

	    // ================= TABLE =================
	    html.append("<table id=\"testcaseTable\">")
	        .append("<thead><tr>")
	        .append("<th></th><th>Module</th><th>Test Case ID</th>")
	        .append("<th>Description</th><th>Status</th><th>Duration</th>")
	        .append("</tr></thead><tbody>");

	    int counter = 1;

	    for (JsonElement el : details) {
	        JsonObject test = el.getAsJsonObject();

	        String module = getAsString(test, "module");
	        String testCaseId = getAsString(test, "testCaseId");
	        String desc = getAsString(test, "description");
	        String status = getAsString(test, "status");

	        long durationMs = test.has("durationMillis")
	                ? test.get("durationMillis").getAsLong() : 0;

	        String duration = durationMs > 0 ? (durationMs / 1000) + "s" : "-";

	        String statusIcon =
	                "PASS".equals(status) ? "✅ Passed" :
	                "FAIL".equals(status) ? "❌ Failed" : "⚠️ Skipped";

	        // Main row
	        html.append("<tr data-test-id=\"tc").append(counter)
	            .append("\" data-expanded=\"false\" onclick=\"toggleDetails(this)\">")
	            .append("<td class=\"expand-icon\">+</td>")
	            .append("<td>").append(escapeHtml(module)).append("</td>")
	            .append("<td>").append(escapeHtml(testCaseId)).append("</td>")
	            .append("<td>").append(escapeHtml(desc)).append("</td>")
	            .append("<td class=\"").append(getStatusClass(status)).append("\">")
	            .append(statusIcon).append("</td>")
	            .append("<td>").append(duration).append("</td>")
	            .append("</tr>");

	        // Details row
	        html.append("<tr class=\"details-row\" id=\"tc")
	            .append(counter).append("-details\" style=\"display:none\">")
	            .append("<td colspan=\"6\">")
	            .append("<table class=\"step-table\">")
	            .append("<tr><th>Action</th><th>Expected Result</th>")
	            .append("<th>Actual Result</th><th>Status</th><th>Screenshot</th></tr>");

	        JsonArray steps = test.getAsJsonArray("steps");
	        for (JsonElement s : steps) {
	            JsonObject step = s.getAsJsonObject();

	            String stepStatus = getAsString(step, "status");
	            String icon =
	                    "PASS".equals(stepStatus) ? "✅" :
	                    "FAIL".equals(stepStatus) ? "❌" : "⚠️";

	            html.append("<tr>")
	                .append("<td>").append(escapeHtml(getAsString(step, "action"))).append("</td>")
	                .append("<td>").append(escapeHtml(getAsString(step, "expected"))).append("</td>")
	                .append("<td>").append(escapeHtml(getAsString(step, "actual"))).append("</td>")
	                .append("<td>").append(icon).append("</td>")
	                .append("<td>")
	                .append(step.has("screenshot") && !step.get("screenshot").isJsonNull()
	                        ? "<img class=\"screenshot\" src=\"" + step.get("screenshot").getAsString() + "\"/>"
	                        : "-")
	                .append("</td>")
	                .append("</tr>");
	        }

	        html.append("</table></td></tr>");
	        counter++;
	    }

	    html.append("</tbody></table></div>");

	    // ================= JS (same as old) =================
	    html.append("<script>\n")

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

    /* ===============================
       INNER MODELS
       =============================== */
    public static class TestExecution {
        private String module;
        private String scenarioId;
        private String testCaseId;
        private String shortDescription;
        private Date startTime;
        private Date endTime;
        private ExecutionStatus status;
        private List<TestStep> steps = new ArrayList<>();

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }

        public String getScenarioId() { return scenarioId; }
        public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }

        public String getTestCaseId() { return testCaseId; }
        public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }

        public String getShortDescription() { return shortDescription; }
        public void setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
        }

        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        public ExecutionStatus getStatus() { return status; }
        public void setStatus(ExecutionStatus status) { this.status = status; }

        public List<TestStep> getSteps() { return steps; }
        public void setSteps(List<TestStep> steps) { this.steps = steps; }
    }

    public static class TestStep {
        private int stepNo;
        private String action;
        private String expectedResult;
        private String actualResult;
        private StepStatus status;
        private String screenshotPath;
        private String logFilePath;
        private File harFilePath;

        public int getStepNo() { return stepNo; }
        public void setStepNo(int stepNo) { this.stepNo = stepNo; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) {
            this.expectedResult = expectedResult;
        }

        public String getActualResult() { return actualResult; }
        public void setActualResult(String actualResult) {
            this.actualResult = actualResult;
        }

        public StepStatus getStatus() { return status; }
        public void setStatus(StepStatus status) { this.status = status; }

        public String getScreenshotPath() { return screenshotPath; }
        public void setScreenshotPath(String screenshotPath) {
            this.screenshotPath = screenshotPath;
        }

        public String getLogFilePath() { return logFilePath; }
        public void setLogFilePath(String logFilePath) {
            this.logFilePath = logFilePath;
        }

        public File getHarFilePath() { return harFilePath; }
        public void setHarFilePath(File harFilePath) {
            this.harFilePath = harFilePath;
        }
    }
}
