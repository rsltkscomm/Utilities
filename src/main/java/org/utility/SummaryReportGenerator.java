package org.utility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SummaryReportGenerator {

	private static String html = "";

	public static void generateReport(int pass, int fail, int noRun, String duration, String startTime) {
		String reportHtml = customReportHtml(pass, fail, noRun, duration, startTime);
		String filePath = System.getProperty("user.dir") + File.separator + "TestExecutionSummary.html";

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(reportHtml);
			if ("yes".equalsIgnoreCase(System.getProperty("isReportSend")))
			{
				try
				{
					Class.forName("org.utility.EmailSender");
					String htmlFilePath = System.getProperty("user.dir") + "\\TestExecutionSummary.html";
					String excelFilePath = System.getProperty("user.dir") +"\\TestSummary.xlsx";
					if (new File(excelFilePath).exists())
					{
						EmailSender.sendEmail(excelFilePath, "TestSummary.xlsx");
					}
					EmailSender.sendEmail(htmlFilePath, "TestExecutionSummary.html");
				} catch (ClassNotFoundException e)
				{
					System.err.println("EmailSender class not found - email functionality disabled");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String percent(int count, int total) {
		return String.format("%.2f%%", (count * 100.0 / total));
	}

	public static String customReportHtml(int pass, int fail, int noRun, String duration, String startTime) {
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

	private static void replaceResourceContent(String key, String resourcePath) {
		try (InputStream is = SummaryReportGenerator.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				System.err.println("Resource not found: " + resourcePath);
				return;
			}
			String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			html = html.replace(key, content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void replaceImageWithBase64(String key, String path) {
		try (InputStream is = SummaryReportGenerator.class.getResourceAsStream(path)) {
			if (is != null) {
				byte[] bytes = is.readAllBytes();
				String base64 = Base64.getEncoder().encodeToString(bytes);
				html = html.replace(key, "data:image/svg+xml;base64," + base64);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getProductLogo(String productName) {
		switch (productName.toLowerCase()) {
			case "resul": return "/images/resul.svg";
			case "marketingstar": return "/images/marketingstar.svg";
			case "smartdx": return "/images/smartdx.svg";
			case "grape": return "/images/grape.svg";
			default: return "";
		}
	}

	public static String getModuleName() {
		String suiteName = System.getProperty("SuiteName");
		return "all".equalsIgnoreCase(suiteName) ? "All module" : suiteName;
	}

	public static String getReportHtml(String productName,int pass,int fail,int noRun,int total,String duration,String startTime)
	{
		return "<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "  <head>\n"
				+ "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n"
				+ "\n"
				+ "   <script>\n"
				+ "      // === jquery.min.js ===\n"
				+ "      ${JQUERY_JS}\n"
				+ "    </script>\n"
				+ "	<script>\n"
				+ "      // === jquery.tablesorter.min.js ===\n"
				+ "      ${TABLESORTER_JS}\n"
				+ "    </script>\n"
				+ "	\n"
				+ "	<style>\n"
				+ "      /* === bootstrap.min.css === */\n"
				+ "      ${BOOTSTRAP_CSS}\n"
				+ "    </style>\n"
				+ "\n"
				+ "   <style>\n"
				+ "      /* === cucumber.css === */\n"
				+ "      ${CUCUMBER_CSS}\n"
				+ "    </style>\n"
				+ "	\n"
				+ "	<script>\n"
				+ "      // === moment.min.js ===\n"
				+ "      ${MOMENT_JS}\n"
				+ "    </script>\n"
				+ "\n"
				+ "    <!-- Google Charts Loader -->\n"
				+ "    <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n"
				+ "    <script type=\"text/javascript\">\n"
				+ "      // Load the 'corechart' package for PieChart\n"
				+ "      google.charts.load(\"current\", { packages: [\"corechart\"] });\n"
				+ "\n"
				+ "      // Set a callback function to draw the chart after the Google Charts library is loaded\n"
				+ "      google.charts.setOnLoadCallback(drawChart);\n"
				+ "\n"
				+ "      // Function to draw the chart\n"
				+ "      function drawChart() {\n"
				+ "        // Create data for the chart\n"
				+ "        var data = google.visualization.arrayToDataTable([\n"
				+ "          [\"Status\", \"Count\"],\n"
				+ "          [\"Pass\", "+pass+"],\n"
				+ "          [\"Fail\", "+fail+"],\n"
				+ "          [\"Skip\", "+noRun+"]\n"
				+ "        ]);\n"
				+ "\n"
				+ "        // Chart options\n"
				+ "        var options = {\n"
				+ "          title: \"Test Execution Summary Chart\",\n"
				+ "          chartArea: { width: \"150%\", top: 60, left: 100 },\n"
				+ "          pieHole: 0.4,\n"
				+ "          backgroundColor: '#00000000',\n"
				+ "          slices: { \n"
				+ "          0: { color: 'green' },\n"
				+ "          1: { color: 'red' }, \n"
				+ "          2: { color: 'yellow' } \n"
				+ "          },      \n"
				+ "         };\n"
				+ "\n"
				+ "        var chart = new google.visualization.PieChart(\n"
				+ "          document.getElementById(\"piechart\")\n"
				+ "        );\n"
				+ "        chart.draw(data, options);\n"
				+ "      }\n"
				+ "    </script>\n"
				+ "\n"
				+ "    <script>\n"
				+ "      $(document).ready(function () {\n"
				+ "        $(\"#tablesorter\").tablesorter({\n"
				+ "          textAttribute: \"data-value\",\n"
				+ "          selectorHeaders: \"> thead tr:not(.dont-sort) th\",\n"
				+ "          sortStable: true\n"
				+ "        });\n"
				+ "      });\n"
				+ "    </script>\n"
				+ "\n"
				+ "    <title>Automation Reports - Features Overview</title>\n"
				+ "  </head>\n"
				+ "\n"
				+ "  <body>\n"
				+ "    <div id=\"header\">\n"
				+ "      <img id=\"resultickslogo\" src=\"https://www.resulticks.com/images/logos/resulticks-logo-blue.svg\" />\n"
				+ "      <h1>AUTOMATION - TEST SUMMARY REPORT"
				+"       <p>Environment : "+System.getProperty("Environment")+" || Release Version: "+System.getProperty("ReleaseVersion")+" || Browser: "+System.getProperty("Browser")+" || Account: "+System.getProperty("Account")+" || Username: "+System.getProperty("UserName")+" || Requestor: "+System.getProperty("user.name")+" || Date & time : "+startTime+"</p></h1>\n"
				+ "      <img id=\"logo\" src=\""+"{{logoImage}}"+"\" />\n"
				+ "    </div>\n"
				+ "\n"
				+ "    <div class=\"container-fluid\" id=\"report\">\n"
				+ "      <div class=\"row\">\n"
				+ "        <div class=\"col-md-10 col-md-offset-1\">\n"
				+ "          <table id=\"tablesorter\" class=\"stats-table table-hover\">\n"
				+ "            <thead>\n"
				+ "              <tr class=\"header dont-sort\">\n"
				+ "                <th></th>\n"
				+ "                <th colspan=\"8\">Status</th>\n"
				+ "              </tr>\n"
				+ "              <tr>\n"
				+ "                <th>Module</th>\n"
				+ "                <th class=\"passed\">Passed</th>\n"
				+ "                <th class=\"passed\">Passed %</th>\n"
				+ "                <th class=\"failed\">Failed</th>\n"
				+ "                <th class=\"failed\">Failed %</th>\n"
				+ "                <th class=\"skipped\">Skipped</th>\n"
				+ "                <th class=\"skipped\">Skipped %</th>\n"
				+ "                <th class=\"total\">Total</th>\n"
				+ "                <th>Duration</th>\n"
				+ "              </tr>\n"
				+ "            </thead>\n"
				+ "            <tbody>\n"
				+ "              <tr>\n"
				+ "                <td class=\"tagname\" style=\"text-align: center;\">"+getModuleName()+"</td>\n"
				+ "                <td class=\"passed\">"+pass+"</td>\n"
				+ "                <td class=\"passed\">"+percent(pass, total)+"</td>\n"
				+ "                <td class=\"failed\">"+fail+"</td>\n"
				+ "                <td class=\"failed\">"+percent(fail, total)+"</td>\n"
				+ "                <td class=\"skipped\">"+noRun+"</td>\n"
				+ "                <td class=\"skipped\">"+percent(noRun, total)+"</td>\n"
				+ "                <td class=\"total\">"+total+"</td>\n"
				+ "                <td class=\"duration\" data-value=\"8243950600\" style=\"text-align: center;\">"+duration+"</td>\n"
				+ "              </tr>\n"
				+ "            </tbody>\n"
				+ "          </table>\n"
				+ "        </div>\n"
				+ "      </div>\n"
				+ "    </div>\n"
				+ "\n"
				+ "    <div id=\"report-lead\" class=\"container-fluid\">\n"
				+ "      <div class=\"col-md-10 col-md-offset-1\">\n"
				+ "        <h2>Specifications & Statistics</h2>\n"
				+ "        <p>The following graphs show passing and failing statistics</p>\n"
				+ "      </div>\n"
				+ "    </div>\n"
				+ "\n"
				+ "    <div>\n"
				+ "      <div id=\"footer\">\n"
				+ "        <div class=\"col-md-3 col-md-offset-2\">\n"
				+ "          <table class=\"table table-bordered\" id=\"classifications\">\n"
				+ "            <tbody>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Test</th>\n"
				+ "                <td>Functional Testing</td>\n"
				+ "              </tr>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Version</th>\n"
				+ "                <td>"+System.getProperty("ReleaseVersion")+"</td>\n"
				+ "              </tr>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Browser</th>\n"
				+ "                <td>"+System.getProperty("Browser")+"</td>\n"
				+ "              </tr>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Environment</th>\n"
				+ "                <td>"+System.getProperty("Environment")+"</td>\n"
				+ "              </tr>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Requested by</th>\n"
				+ "                <td>"+System.getProperty("user.name")+"</td>\n"
				+ "              </tr>\n"
				+ "            </tbody>\n"
				+ "          </table>\n"
				+ "        </div>\n"
				+ "        <div id=\"piechart\" style=\"width: 500px; height: 300px;\"></div>\n"
				+ "      </div>\n"
				+ "    </div>\n"
				+ "  </body>\n"
				+ "</html>";
	}
}
