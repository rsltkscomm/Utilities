package reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import reporting.DetailedTestReporter.TestExecution;
import seleniumUtils.DateUtils;

public class NewSummaryReportGenerator
{

	private static String html = "";
	public static final Map<String, ModuleStats> moduleStats = new ConcurrentHashMap<>();
	public static List<Map<String, Object>> modules;

	private static String extractModuleName(String testName)
	{
		// Split on underscore and take the first part
		int underscoreIndex = testName.indexOf('_');
		return underscoreIndex > 0 ? testName.substring(0, underscoreIndex) : "Other";
	}

	public static void recordTestResult(String testName, String status)
	{
		String moduleName = extractModuleName(testName);

		ModuleStats stats = moduleStats.get(moduleName);
		if (stats == null)
		{
			stats = new ModuleStats();
		}
		switch (status.toUpperCase())
		{
		case "PASS":
			stats.incrementPass();
			break;
		case "FAIL":
			stats.incrementFail();
			break;
		case "SKIP":
		case "SKIPPED":
			stats.incrementSkip();
			break;
		}
		moduleStats.put(moduleName, stats);
	}

	// Aggregated result types
	public static class AggregatedStats
	{
		public int totalPass;
		public int totalFail;
		public int totalSkip;
		public long totalDurationMillis;
		public Map<String, ModuleSummary> perModule = new HashMap<>();
	}

	public static class ModuleSummary
	{
		public int passed;
		public int failed;
		public int skipped;
		public long durationMillis;

		public int getTotal()
		{
			return passed + failed + skipped;
		}
	}

	public static AggregatedStats aggregateStats()
	{
		AggregatedStats agg = new AggregatedStats();
		if (DetailedTestReporter.getReport() == null || DetailedTestReporter.getReport().getTestExecutions() == null)
		{
			return agg;
		}
		for (TestExecution exec : DetailedTestReporter.getReport().getTestExecutions())
		{
			String moduleName = (exec.getModule() == null || exec.getModule().isBlank()) ? "Other" : exec.getModule();
			ModuleSummary mod = agg.perModule.computeIfAbsent(moduleName, k -> new ModuleSummary());
			switch (exec.getStatus())
			{
			case PASS:
				agg.totalPass++;
				mod.passed++;
				break;
			case FAIL:
				agg.totalFail++;
				mod.failed++;
				break;
			case SKIPPED:
				agg.totalSkip++;
				mod.skipped++;
				break;
			}
			try
			{
				if (exec.getStartTime() != null && exec.getEndTime() != null && exec.getEndTime().after(exec.getStartTime()))
				{
					long dur = exec.getEndTime().getTime() - exec.getStartTime().getTime();
					agg.totalDurationMillis += dur;
					mod.durationMillis += dur;
				}
			} catch (Exception ignored)
			{
			}
		}
		return agg;
	}

	public static String getModuleDataJson()
	{
		AggregatedStats agg = aggregateStats();
		modules = new ArrayList<>();
		for (Map.Entry<String, ModuleSummary> entry : agg.perModule.entrySet())
		{
			ModuleSummary ms = entry.getValue();
			Map<String, Object> module = new HashMap<>();
			module.put("module", entry.getKey());
			module.put("total", ms.getTotal());
			module.put("passed", ms.passed);
			module.put("failed", ms.failed);
			module.put("skipped", ms.skipped);
			module.put("durationMillis", ms.durationMillis);
			modules.add(module);
		}
		// Deterministic order by module name
		modules.sort((a, b) -> String.valueOf(a.get("module")).compareToIgnoreCase(String.valueOf(b.get("module"))));
		return new Gson().toJson(modules);
	}

	public static void generateReport(int pass, int fail, int noRun, String duration, String startTime)
	{
		zephyrIntegration.JiraZephyrClient.zephyrUpdater();;
		String reportFileName = System.getProperty("reportFileName") + "_" + DateUtils.getCurrentDate("ddMMMyyyy")+".html";
		String customreport = System.getProperty("user.dir") + File.separator + reportFileName;
		String pageloadReportPath = System.getProperty("user.dir") + File.separator + "TestReport.html";
		String reportPath = System.getProperty("IsPageLoadReport").toLowerCase().equals("yes") ? pageloadReportPath : customreport;

		// 1. Generate and save the HTML report
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath)))
		{
			String reportHtml = customReportHtml(pass, fail, noRun, duration, startTime);
			writer.write(reportHtml);
		} catch (Exception e)
		{
			System.err.println("❌ Failed to generate HTML report: " + e.getMessage());
			e.printStackTrace();
			return; // Stop further execution if report generation fails
		}

		// 2. Check if email should be sent
		if (!"yes".equalsIgnoreCase(System.getProperty("isReportSend")))
		{
			return; // Exit if email is not required
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
			return; // Stop if HTML report is missing
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

	// removed unused percent() helper

	public static String customReportHtml(int pass, int fail, int noRun, String duration, String startTime)
	{
		String productName = System.getProperty("ProductName");
		int total = pass + fail + noRun;
		html = getReportHtml(productName, pass, fail, noRun, total, duration, startTime);

		replaceResourceContent("${CHART_JS}", "https://cdn.jsdelivr.net/npm/chart.js");
		replaceResourceContent("${FONT_AWESOME}", "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css");

		replaceImageWithBase64("{{logoImage}}", getProductLogo(productName));
		replaceImageWithBase64("{{detailedReportIcon}}", "/images/report-icon.svg");

		return html;
	}

	private static void replaceResourceContent(String key, String resourcePath)
	{
		if (resourcePath.startsWith("http"))
		{
			// For CDN resources, just replace the placeholder with the CDN link
			String linkTag = resourcePath.endsWith(".css") ? String.format("<link rel=\"stylesheet\" href=\"%s\" />", resourcePath) : String.format("<script src=\"%s\"></script>", resourcePath);
			html = html.replace(key, linkTag);
		} else
		{
			try (InputStream is = NewSummaryReportGenerator.class.getResourceAsStream(resourcePath))
			{
				if (is == null)
				{
					System.err.println("⚠️ Resource not found: " + resourcePath);
					return;
				}
				String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				html = html.replace(key, content);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private static void replaceImageWithBase64(String key, String path)
	{
		if (path.isEmpty())
		{
			return;
		}

		try (InputStream is = NewSummaryReportGenerator.class.getResourceAsStream(path))
		{
			if (is != null)
			{
				byte[] bytes = is.readAllBytes();
				String base64 = Base64.getEncoder().encodeToString(bytes);
				html = html.replace(key, "data:image/svg+xml;base64," + base64);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static String getProductLogo(String productName)
	{
		if (productName == null)
		{
			return "";
		}

		switch (productName.toLowerCase())
		{
		case "resul":
			return "/images/resul.svg";
		case "marketingstar":
			return "/images/marketingstar.svg";
		case "smartdx":
			return "/images/smartdx.svg";
		case "grape":
			return "/images/grape.svg";
		default:
			return "";
		}
	}

	public static String getModuleName()
	{
		String suiteName = System.getProperty("SuiteName");
		return "all".equalsIgnoreCase(suiteName) ? "All Modules" : suiteName;
	}

	public static String getReportHtml(String productName, int pass, int fail, int noRun, int total, String duration, String startTime) {
	    String detailedReportContent = DetailedTestReporter.generateHTMLContent();
	    String moduleDataJson = getModuleDataJson();
	    String overallDurationFormatted = formatMillisAsHMS(duration);
	    String reportTitle = System.getProperty("reportTitle");
	    
	    // Get environment properties with fallbacks
	    String environment = getSystemProperty("Environment", "Not Specified");
	    String account = getSystemProperty("Account", "Not Specified");
	    String browser = getSystemProperty("Browser", "Not Specified");
	    String username = getSystemProperty("UserName", "Not Specified");
	    String releaseVersion = getSystemProperty("ReleaseVersion", "Not Specified");
	    String requestedBy = getSystemProperty("user.name", "Not Specified");
	    String machineUser = getSystemProperty("user.name", "Not Specified");
	    
	    // Calculate SLA percentage
	    double slaPercentage = total > 0 ? ((double) pass / total) * 100 : 0;
	    String slaFormatted = String.format("%.0f%%", slaPercentage);
	    
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
	            }
	            .detailed-report-link a:hover {
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
	                <div>🎯 SLA: %s</div>
	                </div>
	                <div class="detailed-report-link">
	                <a onclick="showDetailedReport()">📑 Open Detailed Report</a>
	                </div>
	                <div class="main">
	                <div class="chart-container">
	                    <canvas id="chart1"></canvas>
	                </div>
	                <div class="table-container">
	                <h3>Module-wise Results</h3>
	                <table>
	                <tr><th>Module</th><th>Total</th><th>Passed</th><th>Failed</th><th>Skipped</th><th>Duration</th><th>SLA %%</th><th>Success %%</th></tr>
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
	                window.scrollTo(0,0);
	            }
	            
	            function showSummaryReport() {
	                document.getElementById("detailed-section").style.display = "none";
	                document.getElementById("summary-section").style.display = "block";
	                window.scrollTo(0,0);
	            }
	            
	            // Initialize main chart
	            new Chart(document.getElementById('chart1'), {
	             type: 'doughnut',
	             data: { labels:['Passed','Failed','Skipped'], datasets:[{ data:[%d,%d,%d], backgroundColor:['#28a745','#dc3545','#ffc107']}] },
	             options: { plugins:{ legend:{ position:'bottom'} } }
	            });
	            
	            const moduleData = %s;
	            
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
	                if (!moduleData || !Array.isArray(moduleData)) {
	                    tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #666;">No module data available</td></tr>';
	                    return;
	                }
	                
	                tableBody.innerHTML = moduleData.map(m => {
	                    const successRate = m.total > 0 ? ((m.passed/m.total)*100).toFixed(0) : '0';
	                    const slaRate = m.total > 0 ? ((m.passed/m.total)*100).toFixed(0) : '0';
	                    const moduleName = m.module || 'Unknown Module';
	                    
	                    return `<tr onclick="showDetailedReport(); setTimeout(() => document.getElementById('module-${moduleName.toLowerCase()}')?.scrollIntoView(), 100);" style="cursor:pointer;">
	                        <td>${moduleName}</td>
	                        <td>${m.total || 0}</td>
	                        <td>${m.passed || 0}</td>
	                        <td>${m.failed || 0}</td>
	                        <td>${m.skipped || 0}</td>
	                        <td>${formatDuration(m.durationMillis || 0)}</td>
	                        <td>${slaRate}%%</td>
	                        <td>${successRate}%%</td>
	                    </tr>`;
	                }).join('');
	            }
	            
	            // Set current datetime
	            document.addEventListener("DOMContentLoaded", () => {
	              const now = new Date();
	              document.getElementById("current-datetime").textContent = now.toLocaleString();
	              populateModuleTable();
	            });
	            </script>
	            </body>
	            </html>
	            """,
	            reportTitle,pass, fail, noRun, total, overallDurationFormatted, slaFormatted,
	            startTime, detailedReportContent, pass, fail, noRun, moduleDataJson);
	}

	// Helper method to safely get system properties with fallback
	private static String getSystemProperty(String key, String defaultValue) {
	    try {
	        String value = System.getProperty(key);
	        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
	    } catch (Exception e) {
	        return defaultValue;
	    }
	}

	private static String formatMillisAsHMS(String millisString)
	{
		try
		{
			long ms = Long.parseLong(millisString);
			if (ms <= 0)
				return "-";
			long totalSeconds = ms / 1000;
			long hours = totalSeconds / 3600;
			long minutes = (totalSeconds % 3600) / 60;
			long seconds = totalSeconds % 60;
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} catch (Exception e)
		{
			return "-";
		}
	}

	// removed unused encodeFileToBase64()

	static class ModuleStats
	{
		private int passed;
		private int failed;
		private int skipped;

		public void incrementPass()
		{
			passed++;
		}

		public void incrementFail()
		{
			failed++;
		}

		public void incrementSkip()
		{
			skipped++;
		}

		public int getPassed()
		{
			return passed;
		}

		public int getFailed()
		{
			return failed;
		}

		public int getSkipped()
		{
			return skipped;
		}

		public int getTotal()
		{
			return passed + failed + skipped;
		}
	}

	// The filterCount method remains the same for overall filtering
	public void filterCount(List<String> passMethod, List<String> failMethod, List<String> noRunMethod)
	{
		Set<String> passSet = new HashSet<>(passMethod);
		Set<String> failSet = new HashSet<>(failMethod);

		Iterator<String> iterator = noRunMethod.iterator();
		while (iterator.hasNext())
		{
			String method = iterator.next();
			if (passSet.contains(method) || failSet.contains(method))
			{
				iterator.remove();
			}
		}
	}
}
