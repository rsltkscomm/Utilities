package org.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

public class NewSummaryReportGenerator
{

	private static String html = "";
	private static final Map<String, ModuleStats> moduleStats = new ConcurrentHashMap<>();

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
			if (v == null)
			{
				v = new ModuleStats();
			}
			v.addTestResult(testName, status);
			return v;
		});
	}

	public static String getModuleDataJson()
	{
		List<Map<String, Object>> modules = new ArrayList<>();
		for (Map.Entry<String, ModuleStats> entry : moduleStats.entrySet())
		{
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

	public static void generateReport(int pass, int fail, int noRun, String duration, String startTime) {
	    String reportPath = System.getProperty("user.dir") + File.separator + "TestExecutionSummary.html";
	    
	    // 1. Generate and save the HTML report
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath))) {
	        String reportHtml = customReportHtml(pass, fail, noRun, duration, startTime);
	        writer.write(reportHtml);
	    } catch (Exception e) {
	        System.err.println("❌ Failed to generate HTML report: " + e.getMessage());
	        e.printStackTrace();
	        return; // Exit if report generation fails
	    }

	    // 2. Check if email should be sent
	    if (!"yes".equalsIgnoreCase(System.getProperty("isReportSend"))) {
	        return; // Skip email if not required
	    }

	    // 3. Prepare attachments (HTML + optional Excel)
	    List<String> filePaths = new ArrayList<>();
	    List<String> fileNames = new ArrayList<>();

	    // Add HTML report
	    File htmlFile = new File(reportPath);
	    if (htmlFile.exists()) {
	        filePaths.add(reportPath);
	        fileNames.add("TestExecutionSummary.html");
	    } else {
	        System.err.println("⚠️ HTML report not found: " + reportPath);
	        return; // Exit if HTML file missing
	    }

	    // Add Excel report if enabled
	    if ("yes".equalsIgnoreCase(System.getProperty("isExcelAttach"))) {
	        String excelFilePath = System.getProperty("user.dir") + File.separator + "TestSummary.xlsx";
	        File excelFile = new File(excelFilePath);
	        if (excelFile.exists()) {
	            filePaths.add(excelFilePath);
	            fileNames.add("TestSummary.xlsx");
	        } else {
	            System.err.println("⚠️ Excel file not found: " + excelFilePath);
	        }
	    }

	    // 4. Send email if there are attachments
	    if (!filePaths.isEmpty()) {
	        try {
	            String paths = String.join(",", filePaths);
	            String names = String.join(",", fileNames);
	            
	            System.out.println("Attachments:");
	            System.out.println("Paths: " + paths);
	            System.out.println("Names: " + names);
	            
	            EmailSender.sendEmail(paths, names);
	        } catch (Exception e) {
	            System.err.println("❌ Failed to send email: " + e.getMessage());
	            e.printStackTrace();
	        }
	    } else {
	        System.err.println("⚠️ No files available to attach.");
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

		System.out.println(html);

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

	public static String getReportHtml(String productName, int pass, int fail, int noRun, int total, String duration, String startTime)
	{
	    String base64Report = encodeFileToBase64("test-output/Report.html");
	    boolean isReportAvailable = !base64Report.isEmpty();
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
	                            * {
	                                margin: 0;
	                                padding: 0;
	                                box-sizing: border-box;
	                            }

	                            body {
	                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	                                background: #f5f5f5;
	                                color: #333;
	                            }

	                            .header {
	                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
	                    color: white;
	                    padding: 20px 0;
	                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
	                    text-align: center;
	                    position: relative;
	                }

	                .header-content {
	                    max-width: 1400px;
	                    margin: 0 auto;
	                    padding: 0 20px;
	                    display: flex;
	                    align-items: center;
	                    justify-content: flex-start;
	                    gap: 20px;
	                }

	                .header-text {
	                    text-align: left;
	                }

	                .header h1 {
	                    font-size: 2em;
	                    margin-bottom: 5px;
	                    color: white;
	                    margin-left: 50px;
	                }

	                .header p {
	                    font-size: 1.2em;
	                    opacity: 0.9;
	                    color: white;
	                    text-align: center;
	                    margin-bottom: 20px;
	                }

	                .logo-container {
	                    display: flex;
	                    align-items: center;
	                    gap: 15px;
	                }

	                .logo-divider {
	                    width: 1px;
	                    height: 60px;
	                    background-color: rgba(255,255,255,0.3);
	                }

	                #resulticks-logo {
	                    height: 60px;
	                    width: 200px;
	                    object-fit: contain;
	                }

	                #resul-logo {
	                    height: 40px;
	                    width: 100px;
	                    object-fit: contain;
	                }

	                            .environment-info {
	                                background: #f8f9fa;
	                                border-bottom: 1px solid #ddd;
	                                padding: 10px 0;
	                                font-size: 0.85em;
	                                color: #666;
	                            }

	                            .environment-grid {
	                                max-width: 1400px;
	                                margin: 0 auto;
	                                padding: 0 20px;
	                                display: grid;
	                                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
	                                gap: 23px;
	                            }

	                            .env-item {
	                                display: flex;
	                                align-items: center;
	                                gap: 5px;
	                            }

	                            .env-label {
	                                font-weight: bold;
	                                color: #333;
	                            }

	                            .container {
	                                max-width: 1400px;
	                                margin: 0 auto;
	                                padding: 20px;
	                            }

	                            .stats-grid {
	                                display: grid;
	                                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
	                                gap: 20px;
	                                margin-bottom: 30px;
	                            }

	                            .stat-card {
	                                background: white;
	                                padding: 5px;
	                                border-radius: 10px;
	                                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
	                                text-align: center;
	                                transition: transform 0.3s ease;
	                            }

	                            .stat-card:hover {
	                                transform: translateY(-5px);
	                            }

	                            .stat-number {
	                                font-size: 2.5em;
	                                font-weight: bold;
	                                margin-bottom: 10px;
	                            }

	                            .stat-label {
	                                color: #666;
	                                font-size: 1.1em;
	                            }

	                            .passed { color: #28a745; }
	                            .failed { color: #dc3545; }
	                            .skipped { color: #ffc107; }
	                            .total { color: #007bff; }

	                            .charts-section {
	                                background: white;
	                                border-radius: 10px;
	                                padding: 30px;
	                                margin-bottom: 30px;
	                                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
	                            }
	                            
	                            .charts-section h2{
                color: black;
                text-align: left;
            }

            .charts-section h3{
                color: black;
                 text-align: left;
                 margin-left: 10px;
            }

	                            .analytics-dashboard {
	                                display: grid;
	                                grid-template-columns: 0.4fr 1.6fr;
	                                gap: 30px;
	                                margin-top: 20px;
	                            }

	                            .chart-side {
	                                display: flex;
	                                flex-direction: column;
	                                align-items: center;
	                            }

	                            .table-side {
	                                display: flex;
	                                flex-direction: column;
	                            }

	                            .module-table {
	                                width: 100%%;
	                                border-collapse: collapse;
	                                margin-top: 20px;
	                                background: white;
	                                border-radius: 8px;
	                                overflow: hidden;
	                                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
	                                border: 1px solid #ddd;
	                            }

	                            .module-table th {
	                                background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
	                                color: white;
	                                padding: 15px 12px;
	                                text-align: center;
	                                font-weight: 600;
	                                font-size: 0.9em;
	                            }

	                            .module-table th:first-child {
	                                text-align: left;
	                            }

	                            .module-table td {
	                                padding: 12px;
	                                text-align: center;
	                                border-bottom: 1px solid #eee;
	                                font-size: 0.9em;
	                            }

	                            .module-table td:first-child {
	                                text-align: left;
	                            }

	                            .module-table tr:nth-child(even) {
	                                background: #f8f9fa;
	                            }

	                            .module-table tr:hover {
	                                background: #e3f2fd;
	                            }

	                            .module-name {
	                                font-weight: 600;
	                                color: #333;
	                                text-align: left;
	                            }

	                            .status-count {
	                                font-weight: bold;
	                            }

	                            .count-passed { color: #28a745; }
	                            .count-failed { color: #dc3545; }
	                            .count-skipped { color: #ffc107; }
	                            .count-total { color: #007bff; }

	                            .progress-bar {
	                                width: 100%%;
	                                height: 6px;
	                                background: #e9ecef;
	                                border-radius: 3px;
	                                overflow: hidden;
	                                margin-top: 5px;
	                            }

	                            .progress-fill {
	                                height: 100%%;
	                                background: linear-gradient(90deg, #28a745, #20c997);
	                                transition: width 0.3s ease;
	                            }

	                            .chart-container {
	                                background: #f8f9fa;
	                                border-radius: 8px;
	                                padding: 20px;
	                                text-align: center;
	                                min-height: 400px;
	                                display: flex;
	                                flex-direction: column;
	                                align-items: center;
	                                justify-content: center;
	                            }

	                            .chart-title {
	                                font-size: 1.5em;
	                                font-weight: bold;
	                                margin-bottom: 20px;
	                                color: #333;
	                            }

	                            .chart-canvas {
	                                max-width: 300px;
	                                max-height: 300px;
	                            }

	                            .footer {
	                                background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
	                                color: white;
	                                text-align: center;
	                                padding: 20px;
	                                margin-top: 30px;
	                            }
	                            
	                            .footer-note {
	                                color: white;
	                                font-style: italic;
	                                font-weight: 400;
	                            }
	                            
	                            .email-link {
	                                color: white;
	                                text-decoration: underline;
	                            }
	                            
	                            .email-link:hover {
	                                color: #f0f0f0;
	                            }

	                            @media (max-width: 768px) {
	                                .stats-grid {
	                                    grid-template-columns: repeat(2, 1fr);
	                                }

	                                .environment-grid {
	                                    grid-template-columns: 1fr;
	                                }

	                                .analytics-dashboard {
	                                    grid-template-columns: 1fr;
	                                    gap: 20px;
	                                }

	                                .module-table {
	                                    font-size: 0.8em;
	                                }

	                                .module-table th,
	                                .module-table td {
	                                    padding: 8px 6px;
	                                }
	                                
	                            }
	                        </style>
	                    </head>
	                    <body>
	                       <div class="header">
	                <div class="header-content">
	                    <div class="logo-container">
	                        <img id="resulticks-logo" src="https://www.resulticks.com/images/logos/resulticks-logo-blue.svg" alt="Resulticks Logo" />
	                        <div class="logo-divider"></div>
	                        <img id="resul-logo" src="https://run19.resul.io/assets/resulticks-logo-blue-bff3c259.svg" alt="Resul Logo" />
	                    </div>
	                    <div class="header-text">
	                        <h1>AUTOMATION - TEST SUMMARY REPORT</h1>
	                        <p>Comprehensive Test Execution Report with Analytics</p>
	                    </div>
	                </div>

	                        <div class="environment-info">
	                            <div class="environment-grid">
	                                <div class="env-item">
	                                    <span class="env-label">Environment:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Account:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Browser:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Username:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Release Version:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Requested By:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Machine User:</span>
	                                    <span>%s</span>
	                                </div>
	                                <div class="env-item">
	                                    <span class="env-label">Execution Date:</span>
	                                    <span>%s</span>
	                                </div>
	                            </div>
	                        </div>

	                        <div class="container">
	                            <!-- Statistics Overview -->
	                            <div class="stats-grid">
	                                <div class="stat-card">
	                                    <div class="stat-number passed">%d</div>
	                                    <div class="stat-label">Passed Tests</div>
	                                </div>
	                                <div class="stat-card">
	                                    <div class="stat-number failed">%d</div>
	                                    <div class="stat-label">Failed Tests</div>
	                                </div>
	                                <div class="stat-card">
	                                    <div class="stat-number skipped">%d</div>
	                                    <div class="stat-label">Skipped Tests</div>
	                                </div>
	                                <div class="stat-card">
	                                    <div class="stat-number total">%d</div>
	                                    <div class="stat-label">Total Tests</div>
	                                </div>
	                            </div>

	                            <!-- Pie Chart Section -->
	                            <div class="charts-section">
	                                <h2>📊 Test Analytics Dashboard</h2>

	                                <!-- Analytics Dashboard -->
	                                <div class="analytics-dashboard">
	                                    <!-- Table Side (Now Left) -->
	                                    <div class="table-side">
	                                        <h3>📋 Module-wise Test Results</h3>
	                                        <table class="module-table">
	                                            <thead>
	                                                <tr>
	                                                    <th>Module</th>
	                                                    <th>Total</th>
	                                                    <th>Passed</th>
	                                                    <th>Failed</th>
	                                                    <th>Skipped</th>
	                                                    <th>Success %%</th>
	                                                </tr>
	                                            </thead>
	                                            <tbody id="moduleTableBody">
	                                                <!-- Table data will be populated by JavaScript -->
	                                            </tbody>
	                                        </table>
	                                    </div>

	                                    <!-- Chart Side (Now Right) -->
	                                    <div class="chart-side">
	                                        <div class="chart-container">
	                                            <div class="chart-title">Test Execution Summary</div>
	                                            <canvas id="mainChart" class="chart-canvas" width="400" height="400"></canvas>
	                                        </div>
	                                    </div>
	                                </div>
	                            </div>
	                        </div>

	                        <div class="footer">
	                            <div class="footer-note">
	                                <i class="fas fa-envelope"></i> For any queries or support, please reach out to the Automation Testing Team at -
	                                <a href="mailto:qaautomation@resulticks.com" class="email-link">qaautomation@resulticks.com</a>.
	                            </div>
	                        </div>

	                        <script>
	                            // Module-wise test data
	                            const moduleData = %s;

	                            // Initialize the chart
	                            function initChart() {
	                                try {
	                                    const ctx = document.getElementById('mainChart').getContext('2d');
	                                    new Chart(ctx, {
	                                        type: 'pie',
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
	                                                    position: 'bottom'
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

	                            // Populate module table
	                            function populateModuleTable() {
	                                try {
	                                    const tableBody = document.getElementById('moduleTableBody');
	                                    tableBody.innerHTML = moduleData.map(module => {
	                                        const successRate = ((module.passed / module.total) * 100).toFixed(1);
	                                        return `
	                                            <tr>
	                                                <td class="module-name">${module.module}</td>
	                                                <td class="status-count count-total">${module.total}</td>
	                                                <td class="status-count count-passed">${module.passed}</td>
	                                                <td class="status-count count-failed">${module.failed}</td>
	                                                <td class="status-count count-skipped">${module.skipped}</td>
	                                                <td>
	                                                    <div class="status-count count-passed">${successRate}%%</div>
	                                                    <div class="progress-bar">
	                                                        <div class="progress-fill" style="width: ${successRate}%%"></div>
	                                                    </div>
	                                                </td>
	                                            </tr>
	                                        `;
	                                    }).join('');
	                                } catch (e) {
	                                    console.error("Table population error:", e);
	                                }
	                            }

	                            // Initialize the page when loaded
	                            window.addEventListener('DOMContentLoaded', () => {
	                                initChart();
	                                populateModuleTable();

	                                // If we have a comprehensive report, add the download button
	                                if (%b) {
	                                    const header = document.querySelector('.header');
	                                    const downloadBtn = document.createElement('a');
	                                    downloadBtn.href = '#';
	                                    downloadBtn.className = 'detailed-report-link';
	                                    downloadBtn.style.cssText = 'position:absolute;top:20px;right:30px;background:rgba(255,255,255,0.2);color:white;padding:10px 20px;text-decoration:none;border-radius:25px;font-weight:600;border:2px solid rgba(255,255,255,0.3);';
	                                    downloadBtn.innerHTML = '📄 Detailed Report';
	                                    downloadBtn.onclick = function(e) {
	                                        e.preventDefault();
	                                        const link = document.createElement('a');
	                                        link.href = 'data:text/html;base64,%s';
	                                        link.download = 'detailed_report.html';
	                                        link.click();
	                                    };
	                                    header.style.position = 'relative';
	                                    header.appendChild(downloadBtn);
	                                }
	                            });
	                        </script>
	                    </body>
	                    </html>
	                    """,
	            System.getProperty("Environment"), System.getProperty("Account"), System.getProperty("Browser"), System.getProperty("UserName"),
	            System.getProperty("ReleaseVersion"), System.getProperty("user.name"), System.getProperty("user.name"), startTime, pass, fail, noRun, total, moduleDataJson, pass, fail, noRun, isReportAvailable, base64Report);
	}

	private static String encodeFileToBase64(String filePath)
	{
		try
		{
			byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
			return Base64.getEncoder().encodeToString(fileContent);
		} catch (Exception e)
		{
			return "";
		}
	}

	static class ModuleStats
	{
		private final Set<String> allTests = new HashSet<>(); // Tracks all unique tests
		private final Set<String> passedTests = new HashSet<>();
		private final Set<String> failedTests = new HashSet<>();
		private final Set<String> skippedTests = new HashSet<>();

		public void addTestResult(String testName, String status)
		{
			allTests.add(testName); // Track all test names

			// First remove from all statuses to ensure no duplicates
			passedTests.remove(testName);
			failedTests.remove(testName);
			skippedTests.remove(testName);

			// Then add to the correct status
			switch (status.toUpperCase())
			{
			case "PASSED":
				passedTests.add(testName);
				break;
			case "FAILED":
				failedTests.add(testName);
				break;
			case "SKIPPED":
				// Only add to skipped if not already failed
				if (!failedTests.contains(testName))
				{
					skippedTests.add(testName);
				}
				break;
			}
		}

		// Getters for JSON serialization
		public int getTotal()
		{
			return allTests.size(); // Total unique tests
		}

		public int getPassed()
		{
			return passedTests.size();
		}

		public int getFailed()
		{
			return failedTests.size();
		}

		public int getSkipped()
		{
			return skippedTests.size();
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