package reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class SummaryReportGenerator
{

	private static String html = "";

	public static void generateReport(int pass, int fail, int noRun, String duration, String startTime)
	{
		String reportHtml = customReportHtml(pass, fail, noRun, duration, startTime);
		String reportPath = System.getProperty("user.dir") + File.separator + "TestExecutionSummary.html";

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath)))
		{
			writer.write(reportHtml);
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			Class.forName("org.utility.EmailSender");
			if ("yes".equalsIgnoreCase(System.getProperty("isReportSend")))
			{
				try
				{
					String htmlFilePath = reportPath;
					String excelFilePath = System.getProperty("user.dir") + File.separator + "TestSummary.xlsx";

					File htmlFile = new File(htmlFilePath);
					File excelFile = new File(excelFilePath);

					StringBuilder filePaths = new StringBuilder();
					StringBuilder fileNames = new StringBuilder();

					if (htmlFile.exists())
					{
						filePaths.append(htmlFilePath);
						fileNames.append("TestExecutionSummary.html");
					} else
					{
						System.err.println("⚠️ HTML Report not found: " + htmlFilePath);
					}

					if (excelFile.exists())
					{
						if (filePaths.length() > 0)
						{
							filePaths.append(",");
							fileNames.append(",");
						}
						filePaths.append(excelFilePath);
						fileNames.append("TestSummary.xlsx");
					} else
					{
						System.err.println("⚠️ Excel file not found: " + excelFilePath);
					}

					if (filePaths.length() > 0)
					{
						EmailSender.sendEmail(filePaths.toString(), fileNames.toString());
						System.out.println(filePaths.toString());
						System.out.println(fileNames.toString());
					} else
					{
						System.err.println("⚠️ No files available to send.");
					}

				} catch (Exception e)
				{
					System.err.println("❌ Error sending email: " + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e)
		{
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

		replaceResourceContent("${JQUERY_JS}", "/js/jquery.min.js");
		replaceResourceContent("${TABLESORTER_JS}", "/js/jquery.tablesorter.min.js");
		replaceResourceContent("${BOOTSTRAP_CSS}", "/css/bootstrap.min.css");
		replaceResourceContent("${CUCUMBER_CSS}", "/css/cucumber.css");
		replaceResourceContent("${MOMENT_JS}", "/js/moment.min.js");

		replaceImageWithBase64("{{logoImage}}", getProductLogo(productName));

		return html;
	}

	private static void replaceResourceContent(String key, String resourcePath)
	{
		try (InputStream is = SummaryReportGenerator.class.getResourceAsStream(resourcePath))
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

	private static void replaceImageWithBase64(String key, String path)
	{
		try (InputStream is = SummaryReportGenerator.class.getResourceAsStream(path))
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
		return "all".equalsIgnoreCase(suiteName) ? "All module" : suiteName;
	}

	public static String getReportHtml(String productName, int pass, int fail, int noRun, int total, String duration, String startTime)
	{

		String base64Report = encodeFileToBase64("test-output/SingleReport.html");
		boolean isReportAvailable = !base64Report.isEmpty();

		return """
				<!DOCTYPE html>
				<html>
				<head>
				    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
				    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css" />
				     <script>${JQUERY_JS}</script>
				            <script>${TABLESORTER_JS}</script>
				            <style>${BOOTSTRAP_CSS}</style>
				            <style>${CUCUMBER_CSS}</style>
				            <script>${MOMENT_JS}</script>

				   <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
				            <script type="text/javascript">
				                google.charts.load("current", { packages: ["corechart"] });
				                google.charts.setOnLoadCallback(drawChart);
				                function drawChart() {
				                    var data = google.visualization.arrayToDataTable([
				                        ["Status", "Count"],
				                        ["Pass", %d],
				                        ["Fail", %d],
				                        ["Skip", %d]
				                    ]);
				                    var options = {
				                        title: "Test Execution Summary Chart",
				                        chartArea: { width: "150%%", top: 60, left: 100 },
				                        pieHole: 0.4,
				                        backgroundColor: '#00000000',
				                        slices: {
				                            0: { color: 'green' },
				                            1: { color: 'red' },
				                            2: { color: 'yellow' }
				                        }
				                    };
				                    var chart = new google.visualization.PieChart(document.getElementById("piechart"));
				                    chart.draw(data, options);
				                }
				            </script>

				            <script>
				                $(document).ready(function () {
				                    $("#tablesorter").tablesorter({
				                        textAttribute: "data-value",
				                        selectorHeaders: "> thead tr:not(.dont-sort) th",
				                        sortStable: true
				                    });
				                    %s
				                });

				                function downloadComprehensiveReport() {
				                    const link = document.createElement('a');
				                    link.href = 'data:text/html;base64,' + '%s';
				                    link.download = 'SingleReport.html';
				                    link.click();
				                }
				            </script>

				            <title>Automation Reports - Features Overview</title>
				        </head>

				        <body>
				            <div id="header">
				                <img id="resultickslogo" src="https://www.resulticks.com/images/logos/resulticks-logo-blue.svg" />
				                <div style="width: 1px; height: 48px; background-color: grey; margin-top: 20px;"></div>
				                <img id="logo" src="{{logoImage}}" />
				                <h1>AUTOMATION - TEST SUMMARY REPORT
				                <p>
				                    Environment: %s ||
				                    Release Version: %s ||
				                    Browser: %s ||
				                    Account: %s ||
				                    Username: %s ||
				                    Requestor: %s ||
				                    Date & time: %s
				                </p>
				                </h1>
				                <div>
				                    <button id="comprehensive-report-btn" onclick="downloadComprehensiveReport()" style="display: none;">
				                        Download Comprehensive Report <i class="fas fa-arrow-down"></i>
				                    </button>
				                </div>
				            </div>

				            <div class="container-fluid" id="report">
				                <div class="row">
				                    <div class="col-md-10 col-md-offset-1">
				                        <table id="tablesorter" class="stats-table table-hover">
				                            <thead>
				                                <tr class="header dont-sort">
				                                    <th></th>
				                                    <th colspan="8">Status</th>
				                                </tr>
				                                <tr>
				                                    <th>Module</th>
				                                    <th class="passed">Passed</th>
				                                    <th class="passed">Passed %%</th>
				                                    <th class="failed">Failed</th>
				                                    <th class="failed">Failed %%</th>
				                                    <th class="skipped">Skipped</th>
				                                    <th class="skipped">Skipped %%</th>
				                                    <th class="total">Total</th>
				                                    <th>Duration</th>
				                                </tr>
				                            </thead>
				                            <tbody>
				                                <tr>
				                                    <td class="tagname" style="text-align: center;">%s</td>
				                                    <td class="passed">%d</td>
				                                    <td class="passed">%s</td>
				                                    <td class="failed">%d</td>
				                                    <td class="failed">%s</td>
				                                    <td class="skipped">%d</td>
				                                    <td class="skipped">%s</td>
				                                    <td class="total">%d</td>
				                                    <td class="duration" style="text-align: center;">%s</td>
				                                </tr>
				                            </tbody>
				                        </table>
				                    </div>
				                </div>
				            </div>

				            <div id="report-lead" class="container-fluid">
				                <div class="col-md-10 col-md-offset-1">
				                    <h2>Specifications & Statistics</h2>
				                    <p>The following graphs show passing and failing statistics</p>
				                </div>
				            </div>

				            <div>
				                <div id="footer">
				                    <div class="col-md-3 col-md-offset-2">
				                        <table class="table table-bordered" id="classifications">
				                            <tbody>
				                                <tr class="info">
				                                    <th>Test</th>
				                                    <td>Functional Testing</td>
				                                </tr>
				                                <tr class="info">
				                                    <th>Version</th>
				                                    <td>%s</td>
				                                </tr>
				                                <tr class="info">
				                                    <th>Browser</th>
				                                    <td>%s</td>
				                                </tr>
				                                <tr class="info">
				                                    <th>Environment</th>
				                                    <td>%s</td>
				                                </tr>
				                                <tr class="info">
				                                    <th>Requested by</th>
				                                    <td>%s</td>
				                                </tr>
				                            </tbody>
				                        </table>
				                    </div>
				                    <div id="piechart" style="width: 500px; height: 300px;"></div>
				                </div>
				            </div>
				        </body>
				        </html>
				        """.formatted(pass, fail, noRun, isReportAvailable ? "$('#comprehensive-report-btn').show();" : "", base64Report, System.getProperty("Environment"), System.getProperty("ReleaseVersion"), System.getProperty("Browser"),
				System.getProperty("Account"), System.getProperty("UserName"), System.getProperty("user.name"), startTime, getModuleName(), pass, percent(pass, total), fail, percent(fail, total), noRun, percent(noRun, total), total, duration,
				System.getProperty("ReleaseVersion"), System.getProperty("Browser"), System.getProperty("Environment"), System.getProperty("user.name"));
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

}
