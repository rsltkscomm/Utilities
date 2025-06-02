package org.utility;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class SummaryReportGenerator {

	private static String html = "";

	public static void generateReport(int pass, int fail, int noRun, String duration, String startTime) {
		String reportHtml = customReportHtml(pass, fail, noRun, duration, startTime);
		String filePath = System.getProperty("user.dir") + File.separator + "TestExecutionSummary.html";

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(reportHtml);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if ("yes".equalsIgnoreCase(System.getProperty("isReportSend"))) {
			try {
				Class.forName("org.utility.EmailSender");
				EmailSender.sendEmail();
			} catch (ClassNotFoundException e) {
				System.err.println("EmailSender class not found - email functionality disabled");
			}
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

	private static String getReportHtml(String productName, int pass, int fail, int noRun, int total, String duration, String startTime) {
		// For brevity, return a placeholder. Insert template logic or load from HTML file here.
		return "<html><head><title>Summary</title></head><body><h1>Sample Report for " + productName + "</h1></body></html>";
	}
}
