package org.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class SummaryReportGenerator
{

	public static void generateReport(int pass, int fail, int noRun, String duration)
	{
		String html = customReportHtml(pass, fail, noRun, duration);

		String value = System.getProperty("user.dir") + "\\TestExecutionSummary.html";
		System.out.println(value);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(value)))
		{
			writer.write(html);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static String percent(int count, int total)
	{
		return String.format("%.2f", (count * 100.0 / total)) + "%";
	}

	public static String customReportHtml(int pass, int fail, int noRun, String duration)
	{
		String productName = System.getProperty("ProductName");
		String logo = getProductLogo(productName);
		int total = pass + fail + noRun;
		String html = "<!DOCTYPE html>\n" + "<html>\n" + "  <head>\n" + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" + "\n" + "    <script src=\"" + CutsomHTMLReport.customPath()
				+ "\\src\\main\\resources\\js\\jquery.min.js" + "\"></script>\n" + "    <script src=\"" + CutsomHTMLReport.customPath() + "\\src\\main\\resources\\js\\jquery.tablesorter.min.js" + "\"></script>\n" + "\n"
				+ "    <link rel=\"stylesheet\" href=\"" + CutsomHTMLReport.customPath() + "\\src\\main\\resources\\css\\bootstrap.min.css" + "\" type=\"text/css\" />\n" + "    <link rel=\"stylesheet\" href=\"" + CutsomHTMLReport.customPath()
				+ "\\src\\main\\resources\\css\\cucumber.css" + "\" type=\"text/css\" />\n" + "<!-- Google Charts Loader -->\n" + "<script\n" + "  type=\"text/javascript\"\n" + "  src=\"https://www.gstatic.com/charts/loader.js\"\n" + "></script>\n"
				+ "<script type=\"text/javascript\">\n" + "  // Load the 'corechart' package for PieChart\n" + "  google.charts.load(\"current\", { packages: [\"corechart\"] });\n" + "\n"
				+ "  // Set a callback function to draw the chart after the Google Charts library is loaded\n" + "  google.charts.setOnLoadCallback(drawChart);\n" + "\n" + "  // Function to draw the chart\n" + "  function drawChart() {\n"
				+ "    // Create data for the chart\n" + "    var data = google.visualization.arrayToDataTable([\n" + "      [\"Status\", \"Count\"],\n" + "      [\"Pass\", "+pass+"],\n" + "      [\"Fail\", "+fail+"],\n" + "      [\"Skip\", "+noRun+"]\n" + "    ]);\n"
				+ "\n" + "    // Chart options\n" + "    var options = {\n" + "      title: \"Test Execution Summary Chart\",\n" + "      chartArea: { width: \"150%\",top:60,left:100},\n"
				+ "      pieHole: 0.4, // <<< This line makes it a Donut Chart!\n" + "      backgroundColor: '#00000000',\n" + "    };\n" + "\n" + "    // Create the Pie Chart and draw it in the HTML element with ID 'piechart'\n"
				+ "    var chart = new google.visualization.PieChart(\n" + "      document.getElementById(\"piechart\")\n" + "    );\n" + "    chart.draw(data, options);\n" + "  }\n" + "</script>\n" + "    <script>\n"
				+ "        $(document).ready(function () {\n" + "          $(\"#tablesorter\").tablesorter({\n" + "            textAttribute: \"data-value\",\n" + "            selectorHeaders: \"> thead tr:not(.dont-sort) th\",\n"
				+ "            sortStable: true,\n" + "          });\n" + "        });\n" + "      </script>\n" + "  \n" + "      <title>Cucumber Reports - Features Overview</title>\n" + "  </head>\n" + "\n" + "  <body>\n"
				+ "    <div id = \"header\">\n" + "        <img id = \"resultickslogo\" src='https://www.resulticks.com/images/logos/resulticks-logo-blue.svg'></img>\n" + "        <h1>TEST SUMMARY REPORT</h1>\n" + "        <img id = \"logo\" "+getStyle(productName)+" src='"
				+ logo + "'></img>\n" + "    </div>\n" + "       <div class=\"container-fluid\" id=\"report\">\n" + "       <div class=\"row\">\n" + "         <div class=\"col-md-10 col-md-offset-1\">\n"
				+ "       <table id=\"tablesorter\" class=\"stats-table table-hover\">\n" + "\n" + "        <thead>\n" + "          <tr class=\"header dont-sort\">\n" + "              <th></th>\n" + "            <th colspan=\"8\">Status</th>\n"
				+ "          </tr>\n" + "          <tr>\n" + "            <th>Module</th>\n" + "            <th class=\"passed\">Passed</th>\n" + "            <th class=\"passed\">Passed percentage</th>\n"
				+ "            <th class=\"failed\">Failed</th>\n" + "            <th class=\"failed\">Failed percentage</th>\n" + "            <th class=\"skipped\">Skipped</th>\n" + "            <th class=\"skipped\">Skipped percentage</th>\n"
				+ "            <th class=\"total\">Total</th>\n" + "            <th>Duration</th>\n" + "          </tr>\n" + "        </thead>\n" + "          <tbody>\n" + "              <tr>\n"
				+ "                <td class=\"tagname\" style=\"text-align: center;\">" + getModuleName() + "</td>\n" + "                <td class=\"passed\">" + pass + "</td>\n" + "                <td class=\"passed\" >" + percent(pass, total)
				+ "</td>\n" + "                <td class=\"failed\">" + fail + "</td>\n" + "                <td class=\"failed\">" + percent(fail, total) + "</td>\n" + "                <td class=\"skipped\">" + noRun + "</td>\n"
				+ "                <td class=\"skipped\">" + percent(noRun, total) + "</td>\n" + "                <td class=\"total\">" + total + "</td>\n"
				+ "                <td class=\"duration\" data-value=\"8243950600\" style=\"text-align: center;\">" + duration + "</td>\n" + "              </tr>\n" + "        </table>\n" + "      </div>\n" + "    </div>\n" + "        </div>\n"
				+ "        \n" + "      <div id=\"report-lead\" class=\"container-fluid\">\n" + "        <div class=\"col-md-10 col-md-offset-1\">\n" + "          <h2>Specifications & Statistics</h2>\n" + "          <p>\n"
				+ "            The following graphs show passing and failing statistics\n" + "          </p>\n" + "        </div>\n" + "      </div>\n" + "      <div>\n" + "        <div id=\"footer\">\n"
				+ "      <div class=\"col-md-3 col-md-offset-2\">\n" + "        <table class=\"table table-bordered\" id=\"classifications\">\n" + "          <tbody>\n" + "            <tr class=\"info\">\n" + "              <th>Test</th>\n"
				+ "              <td>Functional Testing</td>\n" + "            </tr>\n" + "            <tr class=\"info\">\n" + "              <th>Version</th>\n" + "              <td>" + System.getProperty("version") + "</td>\n"
				+ "            </tr>\n" + "            <tr class=\"info\">\n" + "                <th>Browser</th>\n" + "                <td>" + System.getProperty("browser") + "</td>\n" + "              </tr>\n"
				+ "              <tr class=\"info\">\n" + "                <th>Environment</th>\n" + "                <td>" + System.getProperty("environment") + "</td>\n" + "              </tr>\n" + "              <tr class=\"info\">\n"
				+ "                <th>Requested by</th>\n" + "                <td>Automation Team</td>\n" + "              </tr>\n" + "          </tbody>\n" + "        </table>\n" + "      </div>\n"
				+ "    <div id=\"piechart\" style=\"width: 500px; height: 300px;\"></div>\n" + "  </div>\n" + "  </div>\n" + "  </body>\n" + "</html>\n" + "";
		System.out.println(html);
		return html;
	}

	public static String getProductLogo(String productName)
	{
		if (productName.equalsIgnoreCase("resul"))
		{
			return CutsomHTMLReport.customPath() + "\\src\\main\\resources\\images\\resul.svg";
		} else if (productName.equalsIgnoreCase("marketing star"))
		{
			return CutsomHTMLReport.customPath() + "\\src\\main\\resources\\images\\marketingstar.svg";
		} else if (productName.equalsIgnoreCase("smartdx"))
		{
			return CutsomHTMLReport.customPath() + "\\src\\main\\resources\\images\\smartdx.svg";
		} else if (productName.equalsIgnoreCase("grape"))
		{
			return CutsomHTMLReport.customPath() + "\\src\\main\\resources\\images\\grape.svg";
		}
		return "";
	}

	public static String getPropertyByKey(String propertyFilePath, String key)
	{
		Properties properties = new Properties();
		try
		{
			FileInputStream inputStream = new FileInputStream(propertyFilePath);
			properties.load(inputStream);
			inputStream.close();
			return properties.getProperty(key);
		} catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static String detectFilePath(String path)
	{
		path = getNormalizedPath(path);
		return path;
	}

	public static String getNormalizedPath(String pathString)
	{
		return pathString.replace("/", File.separator).replace("\\", File.separator);
	}

	public static String getModuleName()
	{
		String testName = System.getProperty("SuiteName");
		if (testName.toLowerCase().contains("audience"))
		{
			return "Audience";
		} else if (testName.toLowerCase().contains("communication"))
		{
			return "Communication";
		} else if (testName.toLowerCase().contains("analytics"))
		{
			return "Analytics";
		} else if (testName.toLowerCase().contains("preferences"))
		{
			return "Preferences";
		} else if (testName.toLowerCase().contains("accountsetup"))
		{
			return "Account Setup";
		} else if (testName.toLowerCase().contains("daily"))
		{
			return "Daily Checklist";
		} else if (testName.toLowerCase().contains("deployment"))
		{
			return "Deployment Checklist";
		} else
		{
			return "All module";
		}
	}

	public static String getStyle(String productName)
	{
		if (productName.toLowerCase().contains("marketing star"))
		{
			return "style=\"margin: 5px;padding-left: 250px;padding-bottom: 30px;height: 90px;width: 400px;\"";
		}
		return "";
	}

}
