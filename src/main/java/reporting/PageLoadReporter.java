package reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;


public class PageLoadReporter
{

	public static List<Map<String, Object>> parsePerformanceData(String text)
	{
		List<Map<String, Object>> result = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return result;
		}

		String[] lines = text.split("\n");
		List<String> versions = new ArrayList<>();
		int dataStartLine = -1;

		// First pass: Find all versions and where data starts
		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i].trim();

			// Find all versions in the line
			if (line.contains("Version -"))
			{
				String[] versionParts = line.split("Version -");
				for (int j = 1; j < versionParts.length; j++)
				{
					String version = versionParts[j].split("\\|")[0].trim();
					versions.add(version);
				}
			}

			// Find where the actual data starts
			if (line.contains("Time(Seconds)"))
			{
				dataStartLine = i + 1;
				break;
			}
		}

		if (versions.isEmpty() || dataStartLine == -1)
		{
			return result;
		}

		// Second pass: Parse the data lines
		for (int i = dataStartLine; i < lines.length; i++)
		{
			String line = lines[i].trim();
			if (line.isEmpty())
			{
				continue;
			}

			// Split transaction name and time values
			String[] parts = line.split("\t");
			if (parts.length >= versions.size() + 1)
			{ // At least transaction + time values
				try
				{
					Map<String, Object> record = new LinkedHashMap<>();
					record.put("Transaction", parts[0].trim());

					// Add all available time values
					for (int j = 0; j < versions.size(); j++)
					{
						if (j + 1 < parts.length)
						{
							record.put(versions.get(0), Double.parseDouble(parts[parts.length - 1].trim()));
						}
					}

					result.add(record);
				} catch (NumberFormatException e)
				{
					// Skip lines with invalid numbers
					continue;
				}
			}
		}

		return result;
	}

	private static Map<String, List<String>> extractAllMetadata(String input)
	{
		Map<String, List<String>> metadata = new LinkedHashMap<>();

		// Initialize lists for each metadata type
		metadata.put("Version", new ArrayList<>());
		metadata.put("Account", new ArrayList<>());
		metadata.put("UserName", new ArrayList<>());
		metadata.put("CDP_Volume", new ArrayList<>());

		// Patterns for extraction
		Pattern versionAccountPattern = Pattern.compile("Version\\s*-\\s*(\\S+)\\s*\\|\\s*Account\\s*-\\s*(\\S+)");
		Pattern userPattern = Pattern.compile("UserName\\s*-\\s*(\\S+)");
		Pattern volumePattern = Pattern.compile("CDP\\s+volume\\s*-\\s*([\\d,]*)");

		// Process each line
		for (String line : input.split("\\r?\\n"))
		{
			line = line.trim();
			if (line.isEmpty())
			{
				continue;
			}

			// Split by tabs if present
			String[] parts = line.contains("\t") ? line.split("\t") : new String[] { line };

			for (String part : parts)
			{
				part = part.trim();
				if (part.isEmpty())
				{
					continue;
				}

				// Extract Version and Account
				Matcher vaMatcher = versionAccountPattern.matcher(part);
				if (vaMatcher.find())
				{
					metadata.get("Version").add(vaMatcher.group(1).trim());
					metadata.get("Account").add(vaMatcher.group(2).trim());
				}

				// Extract UserName
				Matcher userMatcher = userPattern.matcher(part);
				if (userMatcher.find())
				{
					metadata.get("UserName").add(userMatcher.group(1).trim());
				}

				// Extract CDP Volume (handles empty values)
				Matcher volumeMatcher = volumePattern.matcher(part);
				if (volumeMatcher.find())
				{
					String volume = volumeMatcher.group(1).replace("-", "").trim();
					metadata.get("CDP_Volume").add(volume.isEmpty() ? "N/A" : volume);
				}
			}
		}

		// Ensure all lists have the same size by filling with "N/A"
		int maxSize = metadata.values().stream().mapToInt(List::size).max().orElse(0);

		for (List<String> list : metadata.values())
		{
			while (list.size() < maxSize)
			{
				list.add("N/A");
			}
		}

		// Set system properties
		if (!metadata.get("Version").isEmpty())
		{
			System.setProperty("pl_accountname", String.join(",", metadata.get("Account")));
			System.setProperty("pl_username", String.join(",", metadata.get("UserName")));
			System.setProperty("pl_cdpvolume", String.join(":", metadata.get("CDP_Volume")));
		}

		return metadata;
	}

	static class VersionInfo
	{
		String version;
		String account;
		String userName;
		String cdpVolume;

		VersionInfo(String version, String account) {
			this.version = version;
			this.account = account;
		}
	}

	public static String readExcelToString(String filePath) throws Exception
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			try (Workbook workbook = new XSSFWorkbook(new File(filePath)))
			{
				Sheet sheet = workbook.getSheetAt(0); // get first sheet

				for (Row row : sheet)
				{
					for (Cell cell : row)
					{
						switch (cell.getCellType())
						{
						case STRING:
							sb.append(cell.getStringCellValue()).append("\t");
							break;
						case NUMERIC:
							sb.append(cell.getNumericCellValue()).append("\t");
							break;
						default:
							sb.append("\t");
						}
					}
					sb.append("\n");
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return sb.toString();
	}

	public static List<Map<String, Object>> transformData(List<Map<String, Object>> originalData)
	{
		List<Map<String, Object>> transformed = new ArrayList<>();

		for (Map<String, Object> item : originalData)
		{
			Map<String, Object> newItem = new LinkedHashMap<>();

			// Extract transaction name
			String transactionName = (String) item.get("Transaction");
			newItem.put("name", transactionName != null ? transactionName : "N/A");

			// Extract and transform version times
			Map<String, Double> times = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : item.entrySet())
			{
				if (!entry.getKey().equals("Transaction"))
				{
					String version = "V" + entry.getKey(); // e.g. "4.8.8" → "V488"
					try
					{
						double time = Double.parseDouble(entry.getValue().toString());
						times.put(version, time);
					} catch (NumberFormatException e)
					{
						times.put(version, 0.0); // or handle error as needed
					}
				}
			}
			newItem.put("times", times);
			transformed.add(newItem);
		}

		return transformed;
	}

	public static void createPageLoaderReport(String filePath)
	{
		try
		{
			String excelData = readExcelToString(filePath);
			extractAllMetadata(excelData);
			List<Map<String, Object>> pageloadPerformanceData = parsePerformanceData(excelData);
			List<Map<String, Object>> transformData = transformData(pageloadPerformanceData);
			ObjectMapper mapper = new ObjectMapper();
			String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformData);
			System.out.println("const transactionData = " + jsonOutput + ";");
			String escapedJson = jsonOutput.replace("</", "<\\/");
			String reportHtml = pageLoadTimerHtml("const transactionData = " + escapedJson + ";");
			String reportPath = System.getProperty("user.dir") + File.separator + "TestReport.html";
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath)))
			{
				writer.write(reportHtml);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void getFormat(long number)
	{
		DecimalFormat indianFormat = new DecimalFormat("#,##,##,##,##0");
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
		symbols.setGroupingSeparator(',');
		indianFormat.setDecimalFormatSymbols(symbols);

		String formattedNumber = indianFormat.format(number);
		System.out.println("Indian Format: " + formattedNumber);
	}

	private static String escapeHtml(String input)
	{
		if (input == null)
		{
			return "";
		}
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#039;");
	}

	public static String pageLoadTimerHtml(String jsonOutput) {
	    String environment = System.getProperty("Environment", "N/A");
	    String[] accounts = System.getProperty("pl_accountname", "N/A N/A").split(",");
	    String[] username = System.getProperty("pl_username", "N/A N/A").split(",");
	    String[] volumes = System.getProperty("pl_cdpvolume", "N/A N/A").split(":");
	    String slatarget = "3.0 Seconds";

	    // Determine how many versions we have (minimum length of all arrays)
	    int numVersions = Math.min(Math.min(accounts.length, username.length), volumes.length);

	    // Build the versionData array dynamically
	    StringBuilder versionDataBuilder = new StringBuilder();
	    versionDataBuilder.append("const versionData = [");
	    for (int i = 0; i < numVersions; i++) {
	        if (i > 0) {
	            versionDataBuilder.append(",");
	        }
	        versionDataBuilder.append(String.format(
	            "{ account: \"%s\", UserName: \"%s\", volume: \"%s\" }", 
	            escapeHtml(i < accounts.length ? accounts[i] : "N/A"), 
	            escapeHtml(i < username.length ? username[i] : "N/A"),
	            escapeHtml(i < volumes.length ? volumes[i] : "N/A")
	        ));
	    }
	    versionDataBuilder.append("];");
	    String versionDataScript = versionDataBuilder.toString();

	    String metaInfo = "<b>Environment : " + escapeHtml(environment) + " || SLA Target : " + slatarget + "</b>";

	    String htmlString = "<!DOCTYPE html>\n" + 
	        "<html lang=\"en\">\n" + 
	        "  <head>\n" + 
	        "    <meta charset=\"UTF-8\" />\n" + 
	        "    <title>Pageload Time Report</title>\n" + 
	        "    <link\n" + 
	        "      href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600&display=swap\"\n" + 
	        "      rel=\"stylesheet\"\n" + 
	        "    />\n" + 
	        "    <style>\n" + 
	        "      :root {\n" + 
	        "        --primary: #1a73e8;\n" + 
	        "        --primary-light: #e8f0fe;\n" + 
	        "        --primary-lighter: #f5f9ff;\n" + 
	        "        --secondary: #0d47a1;\n" + 
	        "        --success: #34a853;\n" + 
	        "        --warning: #f9ab00;\n" + 
	        "        --danger: #ea4335;\n" + 
	        "        --dark: #202124;\n" + 
	        "        --darker: #3c4043;\n" + 
	        "        --light: #ffffff;\n" + 
	        "        --border-radius: 10px;\n" + 
	        "        --box-shadow: 0 4px 12px rgba(26, 115, 232, 0.1);\n" + 
	        "        --transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      body {\n" + 
	        "        font-family: \"Poppins\", sans-serif;\n" + 
	        "        background-color: #f8fafd;\n" + 
	        "        color: var(--darker);\n" + 
	        "        margin: 0;\n" + 
	        "        padding: 0;\n" + 
	        "        line-height: 1.6;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .container {\n" + 
	        "        max-width: 1000px;\n" + 
	        "        margin: 40px auto;\n" + 
	        "        background: var(--light);\n" + 
	        "        border-radius: var(--border-radius);\n" + 
	        "        box-shadow: var(--box-shadow);\n" + 
	        "        padding: 50px;\n" + 
	        "        position: relative;\n" + 
	        "        overflow: hidden;\n" + 
	        "        border: 1px solid rgba(26, 115, 232, 0.1);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .container::before {\n" + 
	        "        content: \"\";\n" + 
	        "        position: absolute;\n" + 
	        "        top: 0;\n" + 
	        "        left: 0;\n" + 
	        "        width: 6px;\n" + 
	        "        height: 100%;\n" + 
	        "        background: linear-gradient(\n" + 
	        "          to bottom,\n" + 
	        "          var(--primary),\n" + 
	        "          var(--secondary)\n" + 
	        "        );\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      h2 {\n" + 
	        "        text-align: center;\n" + 
	        "        margin: 0 30px;\n" + 
	        "        font-size: 32px;\n" + 
	        "        font-weight: 200;\n" + 
	        "        color: #00006e;\n" + 
	        "        position: relative;\n" + 
	        "        padding-bottom: 15px;\n" + 
	        "        padding-left: 1px;\n" + 
	        "        padding-top: 20px;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      #header {\n" + 
	        "        display: flex;\n" + 
	        "        justify-content: space-between;\n" + 
	        "        align-items: center;\n" + 
	        "        border: 1px solid black;\n" + 
	        "        border-radius: 8px;\n" + 
	        "        box-shadow: 6px 10px 20px black;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      #header > img[id=\"companylogo\"] {\n" + 
	        "        height: 45px;\n" + 
	        "        width: 200px;\n" + 
	        "        transition: var(--transition);\n" + 
	        "        padding-left: 20px;\n" + 
	        "        filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      #header > img[id=\"productlogo\"] {\n" + 
	        "        height: 45px;\n" + 
	        "        width: 200px;\n" + 
	        "        transition: var(--transition);\n" + 
	        "        filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      #header > img:hover {\n" + 
	        "        transform: scale(1.05);\n" + 
	        "        filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15));\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      #header p {\n" + 
	        "        margin: 9px auto;\n" + 
	        "        font-size: 27%;\n" + 
	        "        text-align: center;\n" + 
	        "        color: black;\n" + 
	        "        max-width: 940px;\n" + 
	        "        width: 110%;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .meta-info {\n" + 
	        "        display: grid;\n" + 
	        "        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));\n" + 
	        "        gap: 25px;\n" + 
	        "        margin-bottom: 40px;\n" + 
	        "        padding: 25px;\n" + 
	        "        background: var(--primary-lighter);\n" + 
	        "        border-radius: var(--border-radius);\n" + 
	        "        border: 1px solid rgba(26, 115, 232, 0.1);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .meta-info div {\n" + 
	        "        display: flex;\n" + 
	        "        flex-direction: column;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .meta-info label {\n" + 
	        "        font-weight: 500;\n" + 
	        "        margin-bottom: 8px;\n" + 
	        "        color: var(--primary);\n" + 
	        "        font-size: 14px;\n" + 
	        "        letter-spacing: 0.5px;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .meta-info span {\n" + 
	        "        font-weight: 600;\n" + 
	        "        color: var(--dark);\n" + 
	        "        font-size: 18px;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      table {\n" + 
	        "        width: 100%;\n" + 
	        "        border-collapse: separate;\n" + 
	        "        border-spacing: 0;\n" + 
	        "        margin-top: 30px;\n" + 
	        "        overflow: hidden;\n" + 
	        "        background: var(--light);\n" + 
	        "        border-radius: var(--border-radius);\n" + 
	        "        box-shadow: 6px 10px 20px black;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      th {\n" + 
	        "        background: linear-gradient(to right, var(--primary), var(--secondary));\n" + 
	        "        color: white;\n" + 
	        "        padding: 18px;\n" + 
	        "        font-size: 16px;\n" + 
	        "        text-align: center;\n" + 
	        "        font-weight: 500;\n" + 
	        "        position: relative;\n" + 
	        "        letter-spacing: 0.5px;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      th:first-child {\n" + 
	        "        border-top-left-radius: var(--border-radius);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      th:last-child {\n" + 
	        "        border-top-right-radius: var(--border-radius);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      td {\n" + 
	        "        padding: 16px;\n" + 
	        "        text-align: center;\n" + 
	        "        border-bottom: 1px solid rgba(26, 115, 232, 0.05);\n" + 
	        "        font-size: 15px;\n" + 
	        "        transition: var(--transition);\n" + 
	        "        color: var(--darker);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      tr:nth-child(even) {\n" + 
	        "        background-color: var(--primary-light);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      tr:hover td {\n" + 
	        "        background-color: rgba(26, 115, 232, 0.08);\n" + 
	        "        transform: translateX(5px);\n" + 
	        "        box-shadow: 0 2px 8px rgba(26, 115, 232, 0.1);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .footer-note {\n" + 
	        "        margin-top: 40px;\n" + 
	        "        text-align: center;\n" + 
	        "        font-size: 16px;\n" + 
	        "        color: #00006e;\n" + 
	        "        padding-top: 25px;\n" + 
	        "        padding-left: 20px;\n" + 
	        "        border-top: 1px solid rgba(26, 115, 232, 0.1);\n" + 
	        "        font-style: italic;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .transaction-name {\n" + 
	        "        display: flex;\n" + 
	        "        flex-direction: column;\n" + 
	        "        align-items: flex-start;\n" + 
	        "        padding-left: 20px;\n" + 
	        "        text-align: left;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .transaction-id {\n" + 
	        "        font-size: 13px;\n" + 
	        "        color: #5f6368;\n" + 
	        "        margin-bottom: 2px;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .transaction-title {\n" + 
	        "        font-weight: 500;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .response-time {\n" + 
	        "        font-weight: 600;\n" + 
	        "        display: inline-block;\n" + 
	        "        min-width: 60px;\n" + 
	        "        padding: 4px 8px;\n" + 
	        "        border-radius: 4px;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .response-good {\n" + 
	        "        background-color: rgba(52, 168, 83, 0.1);\n" + 
	        "        color: var(--success);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .response-poor {\n" + 
	        "        background-color: rgba(234, 67, 53, 0.1);\n" + 
	        "        color: var(--danger);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .trend-indicator {\n" + 
	        "        font-size: 26px;\n" + 
	        "        margin-left: 5px;\n" + 
	        "        font-weight: 600;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .trend-up {\n" + 
	        "        color: var(--danger);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .trend-down {\n" + 
	        "        color: var(--success);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .version-header {\n" + 
	        "        display: flex;\n" + 
	        "        flex-direction: column;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .version-title {\n" + 
	        "        font-weight: 500;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      .version-subtitle {\n" + 
	        "        font-size: 11px;\n" + 
	        "        margin-top: 4px;\n" + 
	        "        opacity: 0.8;\n" + 
	        "        white-space: normal;\n" + 
	        "        line-height: 1.4;\n" + 
	        "        color:white;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      @keyframes fadeIn {\n" + 
	        "        from {\n" + 
	        "          opacity: 0;\n" + 
	        "          transform: translateY(15px);\n" + 
	        "        }\n" + 
	        "        to {\n" + 
	        "          opacity: 1;\n" + 
	        "          transform: translateY(0);\n" + 
	        "        }\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      tbody tr {\n" + 
	        "        animation: fadeIn 0.6s ease forwards;\n" + 
	        "        opacity: 0;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      tbody tr:nth-child(1) {\n" + 
	        "        animation-delay: 0.1s;\n" + 
	        "      }\n" + 
	        "      tbody tr:nth-child(2) {\n" + 
	        "        animation-delay: 0.2s;\n" + 
	        "      }\n" + 
	        "      tbody tr:nth-child(3) {\n" + 
	        "        animation-delay: 0.3s;\n" + 
	        "      }\n" + 
	        "      tbody tr:nth-child(4) {\n" + 
	        "        animation-delay: 0.4s;\n" + 
	        "      }\n" + 
	        "      tbody tr:nth-child(5) {\n" + 
	        "        animation-delay: 0.5s;\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      @media (max-width: 768px) {\n" + 
	        "        .container {\n" + 
	        "          padding: 25px;\n" + 
	        "          margin: 20px;\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        #header {\n" + 
	        "          flex-direction: column;\n" + 
	        "          gap: 20px;\n" + 
	        "          text-align: center;\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        .meta-info {\n" + 
	        "          grid-template-columns: 1fr;\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        h2 {\n" + 
	        "          font-size: 26px;\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        p[id=\"subheader\"] {\n" + 
	        "          height: 20px;\n" + 
	        "          width: 20px;\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        .transaction-name {\n" + 
	        "          padding-left: 10px;\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        .version-col:nth-child(n + 3) {\n" + 
	        "          display: none;\n" + 
	        "        }\n" + 
	        "      }\n" + 
	        "    </style>\n" + 
	        "  </head>\n" + 
	        "  <body>\n" + 
	        "    <div class=\"container\">\n" + 
	        "      <div id=\"header\">\n" + 
	        "        <img\n" + 
	        "          id=\"companylogo\"\n" + 
	        "          src=\"https://www.resulticks.com/images/logos/resulticks-logo-blue.svg\"\n" + 
	        "          alt=\"companylogo\"\n" + 
	        "        />\n" + 
	        "        <h2>\n" + 
	        "          Pageload Performance Report\n" + 
	        "          <p id=\"subheader\">\n" + 
	        "            " + metaInfo + "\n" + 
	        "            <br>\n" + 
	        "             <b>Note : </b>Report generated on <span id=\"report-date\"></span> All values are measured in seconds. Green = within SLA (≤3.0s), Red = exceeds SLA\n" + 
	        "          </p>\n" + 
	        "        </h2>\n" + 
	        "        <img\n" + 
	        "          id=\"productlogo\"\n" + 
	        "          src=\"https://www.resulticks.com/images/home/resul-logo.svg\"\n" + 
	        "          alt=\"productlogo\"\n" + 
	        "        />\n" + 
	        "      </div>\n" + 
	        "\n" + 
	        "      <table>\n" + 
	        "        <thead id=\"table-header\">\n" + 
	        "          <!-- JavaScript will insert version-specific headers here -->\n" + 
	        "        </thead>\n" + 
	        "        \n" + 
	        "        <tbody id=\"table-body\">\n" + 
	        "          <!-- JavaScript will insert rows here -->\n" + 
	        "        </tbody>\n" + 
	        "      </table>\n" + 
	        "      <div class=\"footer-note\">\n" + 
	        "         <b>For any queries or support, Please reach out to the Automation Testing Team at 'qaautomation@resulticks.com'.\n" + 
	        "        </div>\n" + 
	        "    </div>\n" + 
	        "\n" + 
	        "    <script>\n" + 
	        "      // HTML escaping function\n" + 
	        "      function escapeHtml(unsafe) {\n" + 
	        "        if (!unsafe) return '';\n" + 
	        "        return unsafe.toString()\n" + 
	        "          .replace(/&/g, \"&amp;\")\n" + 
	        "          .replace(/</g, \"&lt;\")\n" + 
	        "          .replace(/>/g, \"&gt;\")\n" + 
	        "          .replace(/\"/g, \"&quot;\")\n" + 
	        "          .replace(/'/g, \"&#039;\");\n" + 
	        "      }\n" + 
	        "      \n" + 
	        "      " + jsonOutput + "\n" + 
	        "      " + versionDataScript + "\n" + 
	        "\n" + 
	        "      const tableHeader = document.getElementById(\"table-header\");\n" + 
	        "      const tableBody = document.getElementById(\"table-body\");\n" + 
	        "      const sla = 3.0;\n" + 
	        "\n" + 
	        "      // Format current date\n" + 
	        "      const now = new Date();\n" + 
	        "      document.getElementById(\"report-date\").textContent = now.toLocaleDateString('en-US', {\n" + 
	        "        year: 'numeric',\n" + 
	        "        month: 'long',\n" + 
	        "        day: 'numeric',\n" + 
	        "        hour: '2-digit',\n" + 
	        "        minute: '2-digit',\n" + 
	        "        second: '2-digit'\n" + 
	        "      });\n" + 
	        "\n" + 
	        "      // Function to get all unique versions from the data in original order\n" + 
	        "      function getVersionsFromData() {\n" + 
	        "        if (!transactionData || transactionData.length === 0) return [];\n" + 
	        "\n" + 
	        "        // Get the order from the first transaction's times object\n" + 
	        "        const firstTransaction = transactionData[0];\n" + 
	        "        return firstTransaction.times ? Object.keys(firstTransaction.times) : [];\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      // Function to determine trend based on last two columns\n" + 
	        "      function getTrend(times, versions) {\n" + 
	        "        if (!times || versions.length < 2) return null;\n" + 
	        "\n" + 
	        "        const lastVersion = versions[versions.length - 1];  // Right-most column\n" + 
	        "        const secondLastVersion = versions[versions.length - 2];  // Second from right\n" + 
	        "\n" + 
	        "        if (!times[lastVersion] || !times[secondLastVersion]) return null;\n" + 
	        "\n" + 
	        "        return times[lastVersion] > times[secondLastVersion] ? 'up' : 'down';\n" + 
	        "      }\n" + 
	        "      \n" + 
	        "      // Function to get version-specific metadata (like CDP Volume)\n" + 
	        "      function getVersionMetadata(index) {\n" + 
	        "        if (index >= 0 && index < versionData.length) {\n" + 
	        "          return `Account: ${versionData[index].account}<br>UserName: ${versionData[index].UserName}<br>CDP Volume: ${versionData[index].volume}<br>Time(seconds)`;\n" + 
	        "        }\n" + 
	        "        return 'Account: N/A UserName: N/A CDP Volume: N/A Time(seconds)';\n" + 
	        "      }\n" + 
	        "      \n" + 
	        "      // Generate table header\n" + 
	        "      function generateTableHeader(versions) {\n" + 
	        "        if (!tableHeader) return;\n" + 
	        "        \n" + 
	        "        tableHeader.innerHTML = '';\n" + 
	        "        const headerRow = document.createElement(\"tr\");\n" + 
	        "\n" + 
	        "        // Transaction column\n" + 
	        "        const transactionHeader = document.createElement(\"th\");\n" + 
	        "        transactionHeader.textContent = \"Transaction\";\n" + 
	        "        headerRow.appendChild(transactionHeader);\n" + 
	        "\n" + 
	        "        // Version columns in original order with metadata\n" + 
	        "        versions.forEach((version, index) => {\n" + 
	        "          const versionHeader = document.createElement(\"th\");\n" + 
	        "          versionHeader.className = \"version-col\";\n" + 
	        "          \n" + 
	        "          const versionContainer = document.createElement(\"div\");\n" + 
	        "          versionContainer.className = \"version-header\";\n" + 
	        "          \n" + 
	        "          const versionTitle = document.createElement(\"div\");\n" + 
	        "          versionTitle.className = \"version-title\";\n" + 
	        "          versionTitle.textContent = version;\n" + 
	        "          \n" + 
	        "          const versionSubtitle = document.createElement(\"div\");\n" + 
	        "          versionSubtitle.className = \"version-subtitle\";\n" + 
	        "          versionSubtitle.innerHTML = getVersionMetadata(index);\n" + 
	        "          \n" + 
	        "          versionContainer.appendChild(versionTitle);\n" + 
	        "          versionContainer.appendChild(versionSubtitle);\n" + 
	        "          versionHeader.appendChild(versionContainer);\n" + 
	        "          headerRow.appendChild(versionHeader);\n" + 
	        "        });\n" + 
	        "\n" + 
	        "        // Trend column (only if we have at least 2 versions)\n" + 
	        "        if (versions.length >= 2) {\n" + 
	        "          const trendHeader = document.createElement(\"th\");\n" + 
	        "          trendHeader.textContent = \"Trend \";\n" + 
	        "          headerRow.appendChild(trendHeader);\n" + 
	        "        }\n" + 
	        "\n" + 
	        "        tableHeader.appendChild(headerRow);\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      // Generate table rows\n" + 
	        "      function generateTableRows(versions) {\n" + 
	        "        if (!tableBody || !transactionData) return;\n" + 
	        "        \n" + 
	        "        tableBody.innerHTML = '';\n" + 
	        "        \n" + 
	        "        transactionData.forEach((txn) => {\n" + 
	        "          if (!txn || !txn.name || !txn.times) return;\n" + 
	        "          \n" + 
	        "          const row = document.createElement(\"tr\");\n" + 
	        "          const trend = versions.length >= 2 ? getTrend(txn.times, versions) : null;\n" + 
	        "\n" + 
	        "          // Transaction name cell (now showing only the name)\n" + 
	        "          const nameCell = document.createElement(\"td\");\n" + 
	        "          nameCell.innerHTML = `\n" + 
	        "            <div class=\"transaction-name\">\n" + 
	        "              <span class=\"transaction-title\">${escapeHtml(txn.name)}</span>\n" + 
	        "            </div>\n" + 
	        "          `;\n" + 
	        "          row.appendChild(nameCell);\n" + 
	        "\n" + 
	        "          // Version data cells in original order\n" + 
	        "          versions.forEach(version => {\n" + 
	        "            const timeCell = document.createElement(\"td\");\n" + 
	        "            timeCell.className = \"version-col\";\n" + 
	        "\n" + 
	        "            if (txn.times[version] !== undefined && txn.times[version] !== null) {\n" + 
	        "              const time = parseFloat(txn.times[version]);\n" + 
	        "              const responseClass = time <= sla ? \"response-good\" : \"response-poor\";\n" + 
	        "              timeCell.innerHTML = `<span class=\"response-time ${responseClass}\">${time.toFixed(2)}</span>`;\n" + 
	        "            } else {\n" + 
	        "              timeCell.innerHTML = '<span class=\"response-time\">-</span>';\n" + 
	        "            }\n" + 
	        "\n" + 
	        "            row.appendChild(timeCell);\n" + 
	        "          });\n" + 
	        "\n" + 
	        "          // Trend cell (based on last two columns)\n" + 
	        "          if (trend !== null) {\n" + 
	        "            const trendCell = document.createElement(\"td\");\n" + 
	        "            trendCell.innerHTML = `<span class=\"trend-indicator trend-${trend}\">${trend === 'up' ? '↑' : '↓'}</span>`;\n" + 
	        "            row.appendChild(trendCell);\n" + 
	        "          }\n" + 
	        "\n" + 
	        "          tableBody.appendChild(row);\n" + 
	        "        });\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      // Initialize the table\n" + 
	        "      function initializeTable() {\n" + 
	        "        try {\n" + 
	        "          const versions = getVersionsFromData();\n" + 
	        "          generateTableHeader(versions);\n" + 
	        "          generateTableRows(versions);\n" + 
	        "        } catch (error) {\n" + 
	        "          console.error(\"Error initializing table:\", error);\n" + 
	        "        }\n" + 
	        "      }\n" + 
	        "\n" + 
	        "      // Initialize on load\n" + 
	        "      document.addEventListener('DOMContentLoaded', initializeTable);\n" + 
	        "    </script>\n" + 
	        "  </body>\n" + 
	        "</html>";

	    return htmlString;
	}

}