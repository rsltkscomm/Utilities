package org.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class SummaryReportGenerator
{
	
	static String html = "";
	public static void generateReport(int pass, int fail, int noRun, String duration,String startTime)
	{
		String html = customReportHtml(pass, fail, noRun, duration,startTime);

		String value = System.getProperty("user.dir") + "\\TestExecutionSummary.html";
		System.out.println(value);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(value)))
		{
			writer.write(html);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		if (System.getProperty("isReportSend") != null && System.getProperty("isReportSend").equalsIgnoreCase("yes")) {
		    try {
		        Class.forName("org.utility.EmailSender");
		        EmailSender.sendEmail();
		    } catch (ClassNotFoundException e) {
		        System.err.println("EmailSender class not found - email functionality disabled");
		    }
		}
	}

	private static String percent(int count, int total)
	{
		return String.format("%.2f", (count * 100.0 / total)) + "%";
	}

	public static String customReportHtml(int pass, int fail, int noRun, String duration,String startTime)
	{
		String productName = System.getProperty("ProductName");
		int total = pass + fail + noRun;	
		html = getReportHtml(productName, pass, fail, noRun, total, duration,startTime);
		getCssAndJsPath("${JQUERY_JS}", "src/main/resources/js/jquery.min.js");
		getCssAndJsPath("${TABLESORTER_JS}", "/js/jquery.tablesorter.min.js");
		getCssAndJsPath("${BOOTSTRAP_CSS}", "/css/bootstrap.min.css");
		getCssAndJsPath("${CUCUMBER_CSS}", "/css/cucumber.css");
		getCssAndJsPath("${MOMENT_JS}", "/js/moment.min.js");
		getImageToBase64("{{logoImage}}", getProductLogo(productName));
		System.out.println(html);
		return html;
	}
	
	public static String getImageAsBase64(String pathInJar) {
	    try (InputStream is = CutsomHTMLReport.class.getResourceAsStream(pathInJar)) {
	        if (is == null) {
	            throw new FileNotFoundException("Image not found: " + pathInJar);
	        }
	        byte[] bytes = is.readAllBytes();
	        return Base64.getEncoder().encodeToString(bytes);
	    } catch (IOException e) {
	        e.printStackTrace();
	        return "";
	    }
	}
	
	public static String getProductLogoByUrl(String productName)
	{
		if (productName.equalsIgnoreCase("resul"))
		{
			return System.getProperty("resullogo");
		} else if (productName.equalsIgnoreCase("marketingstar"))
		{
			return System.getProperty("marketingstarlogo");
		} else if (productName.equalsIgnoreCase("smartdx"))
		{
			return System.getProperty("smartdxlogo");
		} else if (productName.equalsIgnoreCase("grape"))
		{
			return System.getProperty("grapelogo");
		}
		return "";
	}
	
	public static String getProductLogo(String productName)
	{
		if (productName.equalsIgnoreCase("resul"))
		{
			return "/images/resul.svg";
		} else if (productName.equalsIgnoreCase("marketingstar"))
		{
			return "/images/marketingstar.svg";
		} else if (productName.equalsIgnoreCase("smartdx"))
		{
			return "/images/smartdx.svg";
		} else if (productName.equalsIgnoreCase("grape"))
		{
			return "/images/grape.svg";
		}
		return "";
	}
	
	public static void getCssAndJsPath(String key, String resourcePath) {
	    String content;
	    URL url = SummaryReportGenerator.class.getResource(resourcePath);
	    if (url != null) {
	        System.out.println("URL: " + url);
	    } else {
	        System.out.println("Resource not found: " + resourcePath);
	    }

	    try (InputStream is = CutsomHTMLReport.class.getResourceAsStream(resourcePath)) {
	        if (is == null) {
	            throw new FileNotFoundException("Resource not found: " + resourcePath);
	        }

	        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
	        html = html.replace(key, content);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public static void getImageToBase64(String key,String path)
	{
		String base64 = getImageAsBase64(path);
		html = html.replace(key, "data:image/svg+xml;base64," + base64);
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
		// if (productName.toLowerCase().contains("marketing star"))
		// {
		// 	return "style=\"margin: 1px;padding-left: 5px;padding-bottom: 70px;height: 140px;width: 400px;\"";
		// }else {
		// 	return "style=\"style=\"margin: 5px;padding-left: 10px;padding-bottom: 30px;height: 80px;width: 300px;\"";
		// }
		return "";
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
				+ "          backgroundColor: '#00000000'\n"
				+ "        };\n"
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
				+"       <p>Environment : "+System.getProperty("environment")+" || Release Version: "+System.getProperty("version")+" || Browser: "+System.getProperty("browser")+" || Account: "+System.getProperty("Account")+" || Username: "+System.getProperty("UserName")+" || Requestor: "+System.getProperty("user.name")+",Date & time : "+startTime+"</p></h1>\n"
				+ "      <img id=\"logo\" "+getStyle(productName)+" src=\""+"{{logoImage}}"+"\" />\n"
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
				+ "                <td>"+System.getProperty("version")+"</td>\n"
				+ "              </tr>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Browser</th>\n"
				+ "                <td>"+System.getProperty("browser")+"</td>\n"
				+ "              </tr>\n"
				+ "              <tr class=\"info\">\n"
				+ "                <th>Environment</th>\n"
				+ "                <td>"+System.getProperty("environment")+"</td>\n"
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
