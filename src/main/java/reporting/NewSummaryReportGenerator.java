package reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import reporting.DetailedTestReporter.TestExecution;
import seleniumUtils.DateUtils;

public class NewSummaryReportGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String html = "";

    /* =========================================================
       JSON AGGREGATION (SINGLE SOURCE OF TRUTH)
       ========================================================= */
    public static String generateReportJson(
            int pass,
            int fail,
            int skip,
            String durationMillis,
            String startTime
    ) {
        Map<String, Object> root = new LinkedHashMap<>();
        int total = pass + fail + skip;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("passed", pass);
        summary.put("failed", fail);
        summary.put("skipped", skip);
        summary.put("total", total);
        summary.put("durationMillis", durationMillis);
        summary.put("startTime", startTime);

        root.put("summary", summary);
        root.put("modules", aggregateModulesFromExecutions());
        root.put("meta", buildMetaJson());
        root.put("details", buildDetailedReportJson());

        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
                .toJson(root);
    }

    /* =========================================================
       MODULE AGGREGATION (EXECUTION BASED)
       ========================================================= */
    private static List<Map<String, Object>> aggregateModulesFromExecutions() {
        AggregatedStats agg = aggregateStats();
        List<Map<String, Object>> modules = new ArrayList<>();

        for (Map.Entry<String, ModuleSummary> e : agg.perModule.entrySet()) {
            ModuleSummary ms = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("module", e.getKey().toUpperCase());
            m.put("total", ms.getTotal());
            m.put("passed", ms.passed);
            m.put("failed", ms.failed);
            m.put("skipped", ms.skipped);
            m.put("durationMillis", ms.durationMillis);
            modules.add(m);
        }

        modules.sort(Comparator.comparing(o -> o.get("module").toString()));
        return modules;
    }

    /* =========================================================
       HTML FROM JSON (UNCHANGED)
       ========================================================= */
    public static void generateReportFromJson(String json) {
        writeJsonToFile(json);

        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonObject summary = root.getAsJsonObject("summary");

        int pass = summary.get("passed").getAsInt();
        int fail = summary.get("failed").getAsInt();
        int skip = summary.get("skipped").getAsInt();
        String duration = summary.get("durationMillis").getAsString();
        String startTime = summary.get("startTime").getAsString();

        String htmlString = customReportHtml(pass, fail, skip, duration, startTime, json);
        String reportPath = writeHtml(htmlString);
        
        if (!"yes".equalsIgnoreCase(System.getProperty("isReportSend")))
		{
			return;
		}

		// 3. Prepare attachments
		List<String> filePaths = new ArrayList<>();
		List<String> fileNames = new ArrayList<>();

		// Add HTML report
		File htmlFile = new File(reportPath);
		if (htmlFile.exists())
		{
			filePaths.add(reportPath);
			fileNames.add(System.getProperty("IsPageLoadReport").toLowerCase().equals("yes") ? "PageloadReport.html" : "TestExecutionSummary.html");
		} else
		{
			System.err.println("⚠️ HTML report not found: " + reportPath);
			return;
		}

		// Add Excel report if required
		if ("yes".equalsIgnoreCase(System.getProperty("isExcelAttach")))
		{
			String excelFilePath = System.getProperty("user.dir") + File.separator + "TestSummary.xlsx";
			File excelFile = new File(excelFilePath);
			if (excelFile.exists())
			{
				filePaths.add(excelFilePath);
				fileNames.add("TestSummary.xlsx");
			} else
			{
				System.err.println("⚠️ Excel file not found: " + excelFilePath);
			}
		}

		// 4. Send email with attachments (only once)
		try
		{
			if (!filePaths.isEmpty())
			{
				String paths = String.join(",", filePaths);
				String names = String.join(",", fileNames);

				System.out.println("📤 Sending email with attachments:");
				System.out.println("Paths: " + paths);
				System.out.println("Names: " + names);

				EmailSender.sendEmail(paths, names);
			} else
			{
				System.err.println("⚠️ No files available to attach.");
			}
		} catch (Exception e)
		{
			System.err.println("❌ Failed to send email: " + e.getMessage());
			e.printStackTrace();
		}
    }

    /* =========================================================
       AGGREGATION CORE
       ========================================================= */
    static class AggregatedStats {
        int totalPass;
        int totalFail;
        int totalSkip;
        long totalDurationMillis;
        Map<String, ModuleSummary> perModule = new HashMap<>();
    }

    static class ModuleSummary {
        int passed;
        int failed;
        int skipped;
        long durationMillis;

        int getTotal() {
            return passed + failed + skipped;
        }
    }

    public static AggregatedStats aggregateStats() {
        AggregatedStats agg = new AggregatedStats();

        if (DetailedTestReporter.getReport() == null ||
            DetailedTestReporter.getReport().getTestExecutions() == null) {
            return agg;
        }

        for (TestExecution exec : DetailedTestReporter.getReport().getTestExecutions()) {
            String module = (exec.getModule() == null || exec.getModule().isBlank())
                    ? "Other" : exec.getModule();

            ModuleSummary mod = agg.perModule.computeIfAbsent(module, k -> new ModuleSummary());

            switch (exec.getStatus()) {
                case PASS -> { agg.totalPass++; mod.passed++; }
                case FAIL -> { agg.totalFail++; mod.failed++; }
                case SKIPPED -> { agg.totalSkip++; mod.skipped++; }
            }

            if (exec.getStartTime() != null && exec.getEndTime() != null &&
                exec.getEndTime().after(exec.getStartTime())) {
                long dur = exec.getEndTime().getTime() - exec.getStartTime().getTime();
                agg.totalDurationMillis += dur;
                mod.durationMillis += dur;
            }
        }
        return agg;
    }

    /* =========================================================
       MODULE JSON FOR UI (UNCHANGED)
       ========================================================= */
    public static String getModuleDataJson(String unifiedJson) {
    	JsonObject root = new JsonParser()
    	        .parse(unifiedJson)
    	        .getAsJsonObject();
        if (root.has("modules")) {
            return root.getAsJsonArray("modules").toString();
        }
        return "[]";
    }

    /* =========================================================
       DETAILS JSON (UNCHANGED)
       ========================================================= */
    private static List<Map<String, Object>> buildDetailedReportJson() {

	    List<Map<String, Object>> details = new ArrayList<>();

	    for (DetailedTestReporter.TestExecution exec :
	            DetailedTestReporter.getTestExecutionsSafe()) {

	        Map<String, Object> test = new LinkedHashMap<>();

	        // ================= BASIC TEST INFO =================
	        test.put("module", safe(exec.getModule()));
	        test.put("scenarioId", safe(exec.getScenarioId()));
	        test.put("testCaseId", safe(exec.getTestCaseId()));
	        test.put("description", safe(exec.getShortDescription()));
	        test.put("status", exec.getStatus() != null ? exec.getStatus().name() : "SKIPPED");

	        String startTime = formatDate(exec.getStartTime());
	        String endTime   = formatDate(exec.getEndTime());

	        test.put("startTime", startTime);
	        test.put("endTime", endTime);

	        // ================= DURATION =================
	        long durationMillis = 0;
	        if (exec.getStartTime() != null && exec.getEndTime() != null) {
	            durationMillis = exec.getEndTime().getTime() - exec.getStartTime().getTime();
	        }
	        test.put("durationMillis", durationMillis);

	        // ================= STEPS =================
	        List<Map<String, Object>> steps = new ArrayList<>();

	        for (DetailedTestReporter.TestStep step : exec.getSteps()) {

	            Map<String, Object> s = new LinkedHashMap<>();
	            s.put("stepNo", step.getStepNo());
	            s.put("action", safe(step.getAction()));
	            s.put("expected", safe(step.getExpectedResult()));
	            s.put("actual", safe(step.getActualResult()));
	            s.put("status", step.getStatus() != null ? step.getStatus().name() : "SKIPPED");

	            // Optional artifacts
	            s.put("screenshot",
	                    step.getScreenshotPath() != null && !step.getScreenshotPath().isBlank()
	                            ? step.getScreenshotPath()
	                            : null);

	            s.put("logFilePath",
	                    step.getLogFilePath() != null && !step.getLogFilePath().isBlank()
	                            ? step.getLogFilePath()
	                            : null);

	            steps.add(s);
	        }

	        test.put("steps", steps);
	        details.add(test);
	    }

	    return details;
	}

    /* =========================================================
       META
       ========================================================= */
    private static Map<String, Object> buildMetaJson() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("environment", System.getProperty("Environment", "NA"));
        meta.put("browser", System.getProperty("Browser", "NA"));
        meta.put("release", System.getProperty("ReleaseVersion", "NA"));
        meta.put("executionDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        meta.put("generatedBy", System.getProperty("user.name", "automation"));
        return meta;
    }

    /* =========================================================
       UTILS
       ========================================================= */
    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static String formatDate(Date d) {
        return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private static void writeJsonToFile(String json) {
        try {
            String fileName = "TestSummary_" +
                    new SimpleDateFormat("ddMMMyyyy_HHmmss").format(new Date()) + ".json";

            String dir = System.getProperty("user.dir") + File.separator + "reports" + File.separator + "json";
            new File(dir).mkdirs();

            try (FileWriter writer = new FileWriter(new File(dir, fileName))) {
                writer.write(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void writeTextFile(String content)
	{
    	try {
            String fileName = "checkbuild.txt";

            String dir = System.getProperty("user.dir") + File.separator;
            new File(dir).mkdirs();

            try (FileWriter writer = new FileWriter(new File(dir, fileName))) {
                writer.write(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    private static String writeHtml(String reportHtml) {
        try {
            String fileName = System.getProperty("reportFileName", "TestSummary") + "_"
                    + DateUtils.getCurrentDate("ddMMMyyyy_HHmmss") + ".html";

            String baseDir = System.getProperty("user.dir")
                    + File.separator + "src"
                    + File.separator + "test"
                    + File.separator + "resources"
                    + File.separator + "DetailedReports";

            new File(baseDir).mkdirs();

            File reportFile = new File(baseDir, fileName);
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(reportFile), StandardCharsets.UTF_8))) {
                writer.write(reportHtml);
            }
            return reportFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* =========================================================
       HTML BUILDER (YOUR EXISTING HTML — UNCHANGED)
       ========================================================= */
    public static String customReportHtml(int pass, int fail, int skip, String duration, String startTime, String json) {
        String productName = System.getProperty("ProductName");
        int total = pass + fail + skip;
        html = getReportHtml(productName, pass, fail, skip, total, duration, startTime, json);
        return html;
    }
    
    private static String formatMillisAsHMS(String millisString) {
        try {
            long ms = Long.parseLong(millisString);
            if (ms <= 0) return "-";

            long totalSeconds = ms / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } catch (Exception e) {
            return "-";
        }
    }

    
    public static String getReportHtml(String productName, int pass, int fail, int noRun, int total, String durationMillis, String startTime,String json) {
	    String detailedReportContent = DetailedTestReporter.generateHTMLContentFromJson(json);
	    java.util.function.Function<String, String> injectBackLink = (html) -> {
	        if (html == null || html.isEmpty()) return "";
	        String backLink =
	            "<div style='text-align:right; padding:10px;'>"
	          +   "<a class='back-btn' href='#' "
	          +      "onclick='if (window.parent && typeof window.parent.showSummaryReport === \"function\") { "
	          +                "window.parent.showSummaryReport(); "
	          +              "} return false;' "
	          +      "style='"
	          +        "display:inline-block;"
	          +        "background:#ffffff;"
	          +        "color:#0066cc;"
	          +        "padding:8px 14px;"
	          +        "border-radius:6px;"
	          +        "text-decoration:none;"
	          +        "box-shadow:0 2px 10px rgba(0,0,0,0.2);"
	          +        "cursor:pointer;"
	          +        "font-size:14px !important;"
	          +        "line-height:1 !important;"
	          +      "'>"
	          +      "<span style=\"font-size:14px !important; vertical-align:middle;\">⬅</span>"
	          +      "<span style=\"margin-left:6px; vertical-align:middle; font-weight:600;\">Back to Summary Report</span>"
	          +   "</a>"
	          + "</div>";
	        String lower = html.toLowerCase();
	        int idx = lower.indexOf("</header>");
	        if (idx >= 0) {
	            return html.substring(0, idx + 9) + backLink + html.substring(idx + 9);
	        }
	        return backLink + html;
	    };

	    String smartUIHtmlString = "";
	    String smartUiReportPath = System.getProperty("smartUIComparisonReportPath");
	    boolean hasSmartUIReport = false;
	    try {
	        if (smartUiReportPath != null && !smartUiReportPath.isBlank()) {
	            java.nio.file.Path p = java.nio.file.Paths.get(smartUiReportPath);
	            if (java.nio.file.Files.exists(p)) {
	                smartUIHtmlString = java.nio.file.Files.readString(p, java.nio.charset.StandardCharsets.UTF_8);
	                smartUIHtmlString = injectBackLink.apply(smartUIHtmlString);
	                hasSmartUIReport = true;
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        smartUIHtmlString = "";
	    }

	    String performanceHtmlString = "";
	    String performanceReport = System.getProperty("performanceReportPath");
	    boolean hasPerformanceReport = false;
	    try {
	        if (performanceReport != null && !performanceReport.isBlank()) {
	            java.nio.file.Path p = java.nio.file.Paths.get(performanceReport);
	            if (java.nio.file.Files.exists(p)) {
	                performanceHtmlString = java.nio.file.Files.readString(p, java.nio.charset.StandardCharsets.UTF_8);
	                performanceHtmlString = injectBackLink.apply(performanceHtmlString);
	                hasPerformanceReport = true;
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        performanceHtmlString = "";
	    }

	    String moduleDataJson = getModuleDataJson(json); // must be valid JSON; ensure your impl returns "[]"
	    if (moduleDataJson == null || moduleDataJson.isBlank()) {
	        moduleDataJson = "[]";
	    }

	    String overallDurationFormatted = formatMillisAsHMS(durationMillis);
	    String reportTitle = System.getProperty("reportTitle");

	    // Optional environment props (kept for future use)
	    String environment = System.getProperty("Environment", "Not Specified");
	    String account = System.getProperty("Account", "Not Specified");
	    String browser = System.getProperty("Browser", "Not Specified");
	    String username = System.getProperty("UserName", "Not Specified");
	    String releaseVersion = System.getProperty("ReleaseVersion", "Not Specified");
	    String requestedBy = System.getProperty("RequestedBy", System.getProperty("user.name", "Not Specified"));
	    String machineUser = System.getProperty("user.name", "Not Specified");

	    // SLA percentage (currently same as success rate; adjust if your SLA differs)
	    double slaPercentage = total > 0 ? ((double) pass / total) * 100 : 0;
	    String slaFormatted = String.format("%.0f%%", slaPercentage);
	    
	 // Overall pass rate
	    double passRate = total > 0 ? ((double) pass / total) * 100 : 0;

	    // SLA threshold
	    double slaThreshold = 90.0;

	    // Console output
	    System.out.printf("Overall Pass Rate: %.2f%%%n", passRate);
	    
	    if (passRate >= slaThreshold) {
	    	writeTextFile("SUCCESS build");
	    } else {
	    	writeTextFile("FAILURE build");
	    }

	    return String.format("""
	            <!DOCTYPE html>
	            <html lang="en">
	            <head>
	            <meta charset="utf-8"/>
	            <title>Automation Test Summary Report</title>
	            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
	            <script src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js"></script>
	            <style>
	            body { font-family: 'Segoe UI', sans-serif; background:#f8f9fb; margin:0; color:#333; }
	            .header { display:flex; justify-content:space-between; align-items:center; background:linear-gradient(90deg,#002b6b,#0052cc); color:white; padding:10px 20px; }
	            .header img { height:40px; }
	            .summary-band { background:#0052cc; color:white; display:flex; justify-content:center; gap:30px; padding:8px; font-weight:600; }
	            .summary-band div { display:flex; align-items:center; gap:5px; }
	            .main { display:flex; gap:20px; padding:20px; }
	            .chart-container { flex:1; display:flex; justify-content:center; align-items:center; }
	            .table-container { flex:1; background:white; padding:15px; border-radius:8px; box-shadow:0 2px 6px rgba(0,0,0,0.1); }
	            .table-container table { width:100%%; border-collapse:collapse; }
	            .table-container th, .table-container td { padding:8px; border-bottom:1px solid #eee; text-align:left; }
	            .table-container th { background:#002b6b; color:white; }
	            .footer { text-align:center; font-size:0.8em; padding:10px; background:#f1f1f1; margin-top:20px; }
	            
	            /* Detailed Report Styles */
	            .detailed-section { display: none; }
	            #smartui-section { display: none; }
	            /* Ensure performance section is hidden by default so refresh always starts on summary */
	            #performance-section { display: none; }

	            .step-table { width:90%%; margin:10px auto; border-collapse:collapse; font-size:0.85em; }
	            .step-table th, .step-table td { border:1px solid #ccc; padding:5px; text-align:left; }
	            .step-table th { background:#f1f1f1; color:black; }
	            .screenshot { width:100px; cursor:pointer; }
	            .details { display:none; }
	            .environment-ribbon { background:#f4f6f8; padding:10px; font-size:0.9em; border-bottom:1px solid #ddd; display:flex; justify-content:space-around; }
	            .toolbar input, .toolbar select, .toolbar button { padding:6px 10px; border-radius:5px; border:1px solid #ccc; }
	            .toolbar button { background:#007bff; color:white; border:none; cursor:pointer; }
	            .toolbar button:hover { background:#0056b3; }
	            
	            /* Modal Styles */
	            .modal {
	              display: none;
	              position: fixed;
	              z-index: 9999;
	              padding-top: 60px;
	              left: 0;
	              top: 0;
	              width: 100%%;
	              height: 100%%;
	              overflow: auto;
	              background-color: rgba(0,0,0,0.9);
	            }
	            .modal-content {
	              display: block;
	              margin: auto;
	              max-width: 80%%;
	              max-height: 80%%;
	            }
	            #closeModal {
	              position: absolute;
	              top: 20px;
	              right: 35px;
	              color: #fff;
	              font-size: 30px;
	              font-weight: bold;
	              cursor: pointer;
	            }

	            table tr[onclick]:hover {
	              background-color: #f0f8ff !important;
	              transition: background-color 0.3s ease;
	            }

	            .chart-container canvas {
	              max-width: 300px !important;
	              max-height: 300px !important;
	            }
	            
	            .detailed-report-link {
	                text-align:right; 
	                padding:10px;
	                display: block;
	            }
	            .detailed-report-link a {
	                color: #0052cc;
	                text-decoration: none;
	                font-weight: 600;
	                cursor: pointer;
	                padding-right: 47px;
	            }
	            .detailed-report-link a:hover {
	                text-decoration: underline;
	            }
	            
	            .smartui-report-link {
	                text-align:right; 
	                padding-right:10px;
	                padding-top:4px;
	                display: %s;
	            }
	            .smartui-report-link a {
	                color: #0052cc;
	                text-decoration: none;
	                font-weight: 600;
	                cursor: pointer;
	            }
	            .smartui-report-link a:hover {
	                text-decoration: underline;
	            }
	            
	            .performance-report-link {
	                text-align:right; 
	                padding-top:12px;
	                padding-right:23px;
	                display: %s;
	            }
	            .performance-report-link a {
	                color: #0052cc;
	                text-decoration: none;
	                font-weight: 600;
	                cursor: pointer;
	            }
	            .performance-report-link a:hover {
	                text-decoration: underline;
	            }
	            
	            .back-btn {
	                background: white; 
	                color: blue; 
	                padding: 8px 15px;
	                border-radius: 5px; 
	                text-decoration: none;
	                box-shadow: 0 2px 10px rgba(0,0,0,0.2);
	                display:inline-block; 
	                margin:20px 0;
	                cursor: pointer;
	            }
	            
	            .status-pass { color: #28a745; font-weight: bold; }
	            .status-fail { color: #dc3545; font-weight: bold; }
	            .status-skipped { color: #ffc107; font-weight: bold; }

	            /* Iframes to isolate embedded reports */
	            .embedded-report-frame {
	                width: 100%%;
	                border: 0;
	                display: block;
	                min-height: 600px; /* fallback height */
	            }
	            
	            .sla-pass {
  color: #28a745;
  font-weight: bold;
}

.sla-fail {
  color: #dc3545;
  font-weight: bold;
}
	            
	            </style>
	            </head>
	            <body>
	            <!-- Summary Report Section -->
	            <div id="summary-section">
	                <div class="header">
	                <img alt="Company Logo" src="https://www.resulticks.com/images/logos/resulticks-logo-blue.svg"/>
	                <h2>%s</h2>
	                <img alt="Product Logo" src="https://run19.resul.io/assets/resulticks-logo-white-391eec89.svg"/>
	                </div>
	                <div class="summary-band">
	                <div>✅ Passed: %d</div>
	                <div>❌ Failed: %d</div>
	                <div>⚠️ Skipped: %d</div>
	                <div>📊 Total: %d</div>
	                <div>⏱️ Duration: %s</div>
	                <div>🎯 Pass Rate: %s</div>
	                <div>🎯 SLA: 90%%</div>
	                </div>
	                <div class="detailed-report-link">
	                <a onclick="showDetailedReport()">📑 Detailed Report</a>
	                </div>
	                %s
	                %s
	                <div class="main">
	                <div class="chart-container">
	                    <canvas id="chart1"></canvas>
	                </div>
	                <div class="table-container">
	                <h3>Module-wise Results</h3>
	                <table>
	                <tr><th>Module</th><th>Total</th><th>Passed</th><th>Failed</th><th>Skipped</th><th>Duration</th><th>SLA %%</th><th>Pass %%</th></tr>
	                <tbody id="moduleTableBody"></tbody>
	                </table>
	                </div>
	                </div>
	                <div class="footer">Report generated on <span id="current-datetime">%s</span> | Contact: <a href="mailto:qaautomation@resulticks.com">qaautomation@resulticks.com</a></div>
	            </div>
	            
	            <!-- Detailed Report Section -->
	            <div id="detailed-section" class="detailed-section">
	                %s
	            </div>
	            
	            <!-- SmartUI report isolated in iframe -->
	            <div id="smartui-section" class="smartui-section">
	                <iframe id="smartui-frame" class="embedded-report-frame"></iframe>
	            </div>
	            
	            <!-- Performance report isolated in iframe -->
	            <div id="performance-section" class="performance-section">
	                <iframe id="performance-frame" class="embedded-report-frame"></iframe>
	            </div>
	            
	            <!-- Lightbox Modal -->
	            <div class="modal" id="lightboxModal">
	                <span id="closeModal">✖</span>
	                <img class="modal-content" id="lightboxImage"/>
	            </div>
	            
	            <script>
	            // Navigation functions
	            function showDetailedReport() {
	                document.getElementById("summary-section").style.display = "none";
	                document.getElementById("detailed-section").style.display = "block";
	                document.getElementById("smartui-section").style.display = "none";
	                document.getElementById("performance-section").style.display = "none";
	                window.scrollTo(0,0);
	            }
	            
	            function smartuiReport() {
	                document.getElementById("summary-section").style.display = "none";
	                document.getElementById("detailed-section").style.display = "none";
	                document.getElementById("smartui-section").style.display = "block";
	                document.getElementById("performance-section").style.display = "none";
	                window.scrollTo(0,0);
	            }
	            
	            function performanceReport() {
	                document.getElementById("summary-section").style.display = "none";
	                document.getElementById("detailed-section").style.display = "none";
	                document.getElementById("smartui-section").style.display = "none";
	                document.getElementById("performance-section").style.display = "block";
	                window.scrollTo(0,0);
	            }
	            
	            function showSummaryReport() {
	                document.getElementById("detailed-section").style.display = "none";
	                document.getElementById("summary-section").style.display = "block";
	                document.getElementById("smartui-section").style.display = "none";
	                document.getElementById("performance-section").style.display = "none";
	                window.scrollTo(0,0);
	            }

	            // Ensure we always start on summary after a hard refresh
	            (function ensureStartOnSummary(){
	                document.addEventListener('DOMContentLoaded', function(){
	                    showSummaryReport();
	                });
	            })();

	            // Utility: sanitize IDs consistently
	            const sanitizeId = s => String(s ?? '')
	              .toLowerCase()
	              .replace(/\\s+/g, '-')
	              .replace(/[^a-z0-9\\-_:.]/g, '');

	            // Initialize main chart
	            new Chart(document.getElementById('chart1'), {
	              type: 'doughnut',
	              data: { labels:['Passed','Failed','Skipped'], datasets:[{ data:[%d,%d,%d], backgroundColor:['#28a745','#dc3545','#ffc107']}] },
	              options: { plugins:{ legend:{ position:'bottom'} } }
	            });

	            // Safer JSON injection/parsing for module data
	            const moduleDataRaw = `%s`;
	            let moduleData = [];
	            try { moduleData = JSON.parse(moduleDataRaw); } catch (e) { moduleData = []; }

	            function formatDuration(ms) {
	                if (!ms || ms <= 0) return '-';
	                const totalSeconds = Math.floor(ms / 1000);
	                const hours = Math.floor(totalSeconds / 3600);
	                const minutes = Math.floor((totalSeconds %% 3600) / 60);
	                const seconds = totalSeconds %% 60;
	                const pad = (n) => n.toString().padStart(2, '0');
	                return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
	            }
	            
	            function populateModuleTable() {
	                const tableBody = document.getElementById('moduleTableBody');
	                if (!moduleData || !Array.isArray(moduleData) || moduleData.length === 0) {
	                    tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #666;">No module data available</td></tr>';
	                    return;
	                }
	                
	                tableBody.innerHTML = moduleData.map(m => {
	                    const successRate = m.total > 0 ? ((m.passed/m.total)*100).toFixed(0) : '0';
	                    const slaRate = 90; // TODO: compute real SLA if different
	                    const moduleName = m.module || 'Unknown Module';
	                    const anchorId = 'module-' + sanitizeId(moduleName);
	                    
	                    return `<tr onclick="showDetailedReport(); setTimeout(() => { const el = document.getElementById('${anchorId}'); if (el) el.scrollIntoView({behavior:'smooth'}); }, 100);" style="cursor:pointer;">
	                        <td>${moduleName}</td>
	                        <td>${m.total || 0}</td>
	                        <td>${m.passed || 0}</td>
	                        <td>${m.failed || 0}</td>
	                        <td>${m.skipped || 0}</td>
	                        <td>${formatDuration(m.durationMillis || 0)}</td>
	                        <td class="${slaRate >= 90 ? 'sla-pass' : 'sla-fail'}">${slaRate}%%</td>
	                        <td class="${successRate >= slaRate ? 'sla-pass' : 'sla-fail'}">${successRate}%%</td>
                            </td>
	                    </tr>`;
	                }).join('');
	            }

	            // ===== Embed external reports in sandboxed iframes so their original CSS is preserved =====
	            const HAS_SMARTUI = %s;
	            const HAS_PERFORMANCE = %s;

	            // Raw HTML strings (with back link injected on the Java side)
	            const SMARTUI_HTML = `%s`;
	            const PERFORMANCE_HTML = `%s`;

	            function setIframeHtml(iframeId, html) {
	                const frame = document.getElementById(iframeId);
	                if (!frame) return;
	                function resize() {
	                    try {
	                        const doc = frame.contentDocument || frame.contentWindow.document;
	                        if (!doc) return;
	                        const h = Math.max(
	                          doc.documentElement.scrollHeight || 0,
	                          doc.body ? doc.body.scrollHeight : 0
	                        );
	                        if (h) frame.style.height = (h + 20) + 'px';
	                    } catch (e) { /* ignore */ }
	                }
	                frame.addEventListener('load', resize);
	                frame.srcdoc = html || '<!doctype html><html><body><div style="padding:16px;color:#666;">No report content</div></body></html>';
	            }

	            document.addEventListener("DOMContentLoaded", () => {
	              const now = new Date();
	              document.getElementById("current-datetime").textContent = now.toLocaleString();
	              populateModuleTable();

	              if (HAS_SMARTUI) setIframeHtml('smartui-frame', SMARTUI_HTML);
	              if (HAS_PERFORMANCE) setIframeHtml('performance-frame', PERFORMANCE_HTML);
	            });
	            </script>
	            </body>
	            </html>
	            """,
	            // CSS display properties
	            hasSmartUIReport ? "block" : "none",
	            hasPerformanceReport ? "block" : "none",
	            // Main content
	            reportTitle, pass, fail, noRun, total, overallDurationFormatted, slaFormatted,
	            // Conditional report links
	            hasSmartUIReport ? "<div class='smartui-report-link'><a onclick='smartuiReport()'>📑 UI Comparison Report</a></div>" : "",
	            hasPerformanceReport ? "<div class='performance-report-link'><a onclick='performanceReport()'>📑 Performance Report</a></div>" : "",
	            startTime,
	            // Detailed report stays inline
	            detailedReportContent,
	            // Chart data
	            pass, fail, noRun,
	            // Module data (escape backslashes and backticks)
	            moduleDataJson.replace("\\", "\\\\").replace("`", "\\`"),
	            // Flags for iframes
	            String.valueOf(hasSmartUIReport), String.valueOf(hasPerformanceReport),
	            // SmartUI & Performance HTML passed into JS (escape backslashes and backticks)
	            smartUIHtmlString.replace("\\", "\\\\").replace("`", "\\`"),
	            performanceHtmlString.replace("\\", "\\\\").replace("`", "\\`")
	    );
	}
}