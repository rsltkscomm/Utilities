package reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class NewSummaryReportGenerator
{

	private static String html = "";
	public static final Map<String, ModuleStats> moduleStats = new ConcurrentHashMap<>();


	private static String extractModuleName(String testName)
	{
		// Split on underscore and take the first part
		int underscoreIndex = testName.indexOf('_');
		return underscoreIndex > 0 ? testName.substring(0, underscoreIndex) : "Other";
	}

	public static void recordTestResult(String testName, String status)
	{
		String moduleName = extractModuleName(testName);

		moduleStats.compute(moduleName, (k, v) -> {
		    if (v == null) {
		        v = new ModuleStats();
		    }
		    switch (status.toUpperCase()) {
		        case "PASS": v.incrementPass(); break;
		        case "FAIL": v.incrementFail(); break;
		        case "SKIP":
		        case "SKIPPED": v.incrementSkip(); break;
		    }
		    return v;
		});
	}

	public static String getModuleDataJson() {
	    Map<String, ModuleStats> statsMap = new HashMap<>();

	    // Go through all completed scenarios
	    for (TestExecution exec : DetailedTestReporter.getReport().getTestExecutions()) {
	        statsMap.computeIfAbsent(exec.getModule(), m -> new ModuleStats());

	        ModuleStats stats = statsMap.get(exec.getModule());
	        switch (exec.getStatus()) {
	            case PASS: stats.incrementPass(); break;
	            case FAIL: stats.incrementFail(); break;
	            case SKIPPED: stats.incrementSkip(); break;
	        }
	    }

	    // Convert to JSON structure
	    List<Map<String, Object>> modules = new ArrayList<>();
	    for (Map.Entry<String, ModuleStats> entry : statsMap.entrySet()) {
	        Map<String, Object> module = new HashMap<>();
	        module.put("module", entry.getKey());
	        module.put("total", entry.getValue().getTotal());
	        module.put("passed", entry.getValue().getPassed());
	        module.put("failed", entry.getValue().getFailed());
	        module.put("skipped", entry.getValue().getSkipped());
	        modules.add(module);
	    }

	    return new Gson().toJson(modules);
	}

	public static void generateReport(int pass, int fail, int noRun, String duration, String startTime)
	{
		String customreport = System.getProperty("user.dir") + File.separator + "TestExecutionSummary.html";
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

	private static String percent(int count, int total)
	{
		return String.format("%.2f%%", (count * 100.0 / total));
	}

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
				String extension = path.substring(path.lastIndexOf(".") + 1);
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

	public static String getReportHtml(String productName, int pass, int fail, int noRun, int total, String duration,
			String startTime) {
		String detailedReportContent = DetailedTestReporter.generateHTMLContent();
		String moduleDataJson = getModuleDataJson();
		return String.format(
				"""
						<!DOCTYPE html>
						<html lang="en">
						<head>
						    <meta charset="UTF-8">
						    <meta name="viewport" content="width=device-width, initial-scale=1.0">
						    <title>Automation Test Summary Report</title>
						    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
						    <style>
						        /* --- Approved Summary Styles --- */
						        * { margin: 0; padding: 0; box-sizing: border-box; }
						        body {
						            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
						            background: #f5f5f5;
						            color: #333;
						        }
						        .header {
						            background: linear-gradient(135deg, white 0%%, #00006e 100%%);
						            color: white;
						            padding: 5px 0;
						            text-align: center;
						            position: relative;
						            box-shadow: 6px 10px 20px black;
						            border-radius:10px
						        }
						        .header-content {
						            max-width: 1400px;
						            margin: 0 auto;
						            padding: 0 20px;
						            display: flex;
						            align-items: center;
						            justify-content: flex-start;
						            gap: 50px;
						            margin-top:10px;
						        }
						        .header-text { text-align: center; }
						        .header h1 { font-size: 2em; margin-bottom: 5px; color: white; margin-left: 80px; }
						        .header p { font-size: 1.2em; opacity: 0.9; color: white; padding-left:110px;}
						        .logo-container { display: flex; align-items: center; gap: 15px; }
						        #resulticks-logo { height: 60px; width: 200px; object-fit: contain; margin-left: 30px; }
						        #resul-logo { height: 40px; width: 100px; object-fit: contain; margin-left: 90px; }
						        .environment-info {
						            background: #f8f9fa; border-bottom: 1px solid #ddd;
						            padding: 10px 0; font-size: 0.85em; color: #666; width: 1150px;
						        }
						        .environment-grid {
						            max-width: 1100px; margin: 40px 0px 0px 20px; padding: 20px;
						            display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
						            gap: 30px;
						        }
						        .env-label { font-weight: bold; color: #333; }
						        .container { max-width: 1400px; margin: 0 auto; padding: 20px; }
						        .stats-grid {
						            display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
						            gap: 20px; margin-bottom: 30px;
						        }
						        .stat-card {
						            background: white; padding: 5px; border-radius: 10px;
						            box-shadow: 6px 10px 20px black;
						            text-align: center; transition: transform 0.3s ease;
						        }
						        .stat-card:hover { transform: translateY(-5px); }
						        .stat-label { color: #666; font-size: 1.1em;font-weight:500 }
						        .passed { color: #28a745; box-shadow:0px } .failed { color: #dc3545; }
						        .skipped { color: #ffc107; } .total { color: #007bff; }
						        .charts-section {
						            background: white; border-radius: 10px; padding: 30px;
						            margin-bottom: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);
						        }
						        .charts-section h2 { color: black; text-align: left; }
						        .analytics-dashboard { display: grid; grid-template-columns: 0.4fr 1.6fr; gap: 30px; margin-top: 20px; }
						        .table-side { display: flex; flex-direction: column; }
						        .module-table {
						            width: 100%%; border-collapse: collapse; margin-top: 20px; background: white;
						            border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);
						            border: 1px solid #ddd;
						        }
						        .module-table th {
						            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
						            color: white; padding: 15px 12px; text-align: center;
						        }
						        .module-table td { padding: 12px;font-weight:600; text-align: center; border-bottom: 1px solid #eee; }
						        .module-name { font-weight: 600; color: #333; text-align: left; }
						        .footer {
						            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
						            color: white; text-align: center; padding: 20px; margin-top: 30px;
						        }
						        .footer-note { color: white; font-style: italic; }

						        /* --- Toggle Sections --- */
						        .detailed-section { display: none; }
						        .back-btn {
						            background: #764ba2; color: white; padding: 8px 15px;
						            border-radius: 5px; text-decoration: none;
						            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
						            display:inline-block; margin:20px 0;
						        }
						        .detailed-report-link {
						            position: relative; top: 60px; right: 40px;left:540px;
						            background: #fff; color: #764ba2;
						            padding: 0px; border-radius: 0px;
						            font-weight: 400;
						            font-style: italic;
						            text-decoration :underline;
						        }
						        .chart-container {
						            position: relative;
						            height: 300px;
						            width: 300px;
						            margin: 0 220px;
						        }
						        .chart-side>h3{
						            margin-bottom:30px;
						            text-align:center;
						        }
						        .stats-grid .stat-number {
                                    box-shadow: none !important;
                                     font-size: 2.5em; 
						            font-weight: bold; 
						            margin-bottom: 10px;
                                }
						    </style>
						</head>
						<body>
						    <div id="summary-section">
						        <div class="header">
						            <div class="header-content">
						                <div class="logo-container">
						                    <img id="resulticks-logo"
						                         src="https://www.resulticks.com/images/logos/resulticks-logo-blue.svg"
						                         alt="Resulticks Logo"/>
						                </div>
						                <div class="header-text">
						                    <h1>AUTOMATION - TEST SUMMARY REPORT</h1>
						                    <p>Comprehensive Test Execution Report with Analytics</p>
						                </div>
						                <img id="resul-logo"
						                     src="https://run19.resul.io/assets/resulticks-logo-white-391eec89.svg"
						                     alt="Resul Logo"/>
						            </div>
						            <a href="javascript:void(0)" class="detailed-report-link"
						               onclick="showDetailedReport()">Detailed Report</a>
						        </div>

						        <div class="environment-info">
						            <div class="environment-grid">
						                <div class="env-item"><span class="env-label">Environment:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Account:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Browser:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Username:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Release Version:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Requested By:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Machine User:</span> <span>%s</span></div>
						                <div class="env-item"><span class="env-label">Execution Date:</span> <span>%s</span></div>
						            </div>
						        </div>

						        <div class="container">
						            <div class="stats-grid">
						                <div class="stat-card"><div class="stat-number passed" box-shadow:0px>%d</div><div class="stat-label">Passed Tests</div></div>
						                <div class="stat-card"><div class="stat-number failed" box-shadow:0px>%d</div><div class="stat-label">Failed Tests</div></div>
						                <div class="stat-card"><div class="stat-number skipped" box-shadow:0px>%d</div><div class="stat-label">Skipped Tests</div></div>
						                <div class="stat-card"><div class="stat-number total" box-shadow:0px>%d</div><div class="stat-label">Total Tests</div></div>
						            </div>

						            <div class="charts-section">
						                <h2>📊 Test Analytics Dashboard</h2>
						                <div class="analytics-dashboard">
						                    <div class="table-side">
						                        <h3>📋 Module-wise Test Results</h3>
						                        <table class="module-table">
						                            <thead>
						                                <tr><th>Module</th><th>Total</th><th>Passed</th><th>Failed</th><th>Skipped</th><th>Success %%</th></tr>
						                            </thead>
						                            <tbody id="moduleTableBody"></tbody>
						                        </table>
						                    </div>
						                    <div class="chart-side">
						                        <h3>Test Summary Chart</h3>
						                        <div class="chart-container">
						                            <canvas id="mainChart"></canvas>
						                        </div>
						                    </div>
						                </div>
						            </div>
						        </div>

						        <div class="footer">
						            <div class="footer-note">
						                <i class="fas fa-envelope"></i> For any queries, please reach out to
						                <a href="mailto:qaautomation@resulticks.com" class="email-link">qaautomation@resulticks.com</a>.
						            </div>
						        </div>
						    </div>

						    <div id="detailed-section" class="detailed-section">
						        <a href="javascript:void(0)" class="back-btn" onclick="showSummary()">← Back to Summary</a>
						        %s
						    </div>
						    <script>
						        function showDetailedReport() {
						            document.getElementById("summary-section").style.display = "none";
						            document.getElementById("detailed-section").style.display = "block";
						            window.scrollTo(0,0);
						        }
						        function showSummary() {
						            document.getElementById("detailed-section").style.display = "none";
						            document.getElementById("summary-section").style.display = "block";
						            window.scrollTo(0,0);
						        }

						        const moduleData = %s;
						        // Initialize the chart
						        function initChart() {
						            try {
						                const ctx = document.getElementById('mainChart').getContext('2d');
						                new Chart(ctx, {
						                    type: 'doughnut',
						                    data: {
						                        labels: ['Passed', 'Failed', 'Skipped'],
						                        datasets: [{
						                            data: [%d, %d, %d],
						                            backgroundColor: ['#28a745', '#dc3545', '#ffc107'],
						                            borderWidth: 2,
						                            borderColor: '#fff'
						                        }]
						                    },
						                    options: {
						                        responsive: true,
						                        maintainAspectRatio: false,
						                        plugins: {
						                            legend: {
						                                position: 'bottom',
						                                labels: { font: { size: 14 } }
						                            },
						                            tooltip: {
						                                callbacks: {
						                                    label: function(context) {
						                                        const total = context.dataset.data.reduce((a, b) => a + b, 0);
						                                        const percentage = ((context.parsed / total) * 100).toFixed(1);
						                                        return `${context.label}: ${context.parsed} (${percentage}%%)`;
						                                    }
						                                }
						                            }
						                        }
						                    }
						                });
						            } catch (e) {
						                console.error("Chart initialization error:", e);
						            }
						        }
						        function populateModuleTable() {
						            const tableBody = document.getElementById('moduleTableBody');
						            tableBody.innerHTML = moduleData.map(m => {
						                const rate = ((m.passed/m.total)*100).toFixed(1);
						                return `<tr>
						                    <td class="module-name">${m.module}</td>
						                    <td>${m.total}</td>
						                    <td class="count-passed">${m.passed}</td>
						                    <td class="count-failed">${m.failed}</td>
						                    <td class="count-skipped">${m.skipped}</td>
						                    <td><div class="count-passed">${rate}%%</div></td>
						                </tr>`;
						            }).join('');
						        }
						        window.addEventListener('DOMContentLoaded',()=>{initChart(); populateModuleTable();});
						    </script>
						</body>
						</html>
						""",
				System.getProperty("Environment"), System.getProperty("Account"), System.getProperty("Browser"),
				System.getProperty("UserName"), System.getProperty("ReleaseVersion"), System.getProperty("user.name"),
				System.getProperty("user.name"), startTime, pass, fail, noRun, total, detailedReportContent,
				moduleDataJson, pass, fail, noRun);
	}

	private static String encodeFileToBase64(String filePath) {
	    try {
	        Path path = Paths.get(filePath);
	        if (!Files.exists(path)) {
	            System.err.println("Report file not found: " + path.toAbsolutePath());
	            return "";
	        }
	        byte[] fileContent = Files.readAllBytes(path);
	        return Base64.getEncoder().encodeToString(fileContent);
	    } catch (Exception e) {
	        System.err.println("Error encoding report file: " + e.getMessage());
	        return "";
	    }
	}

	static class ModuleStats {
	    private int passed;
	    private int failed;
	    private int skipped;

	    public void incrementPass() {
	        passed++;
	    }

	    public void incrementFail() {
	        failed++;
	    }

	    public void incrementSkip() {
	        skipped++;
	    }

	    public int getPassed() {
	        return passed;
	    }

	    public int getFailed() {
	        return failed;
	    }

	    public int getSkipped() {
	        return skipped;
	    }

	    public int getTotal() {
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
