package reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonReportMerger
{

	public static String mergeJsonReports(List<Path> jsonFiles)
	{

		try
		{
			Gson gson = new Gson();
			JsonArray allDetails = new JsonArray();
			Map<String, JsonObject> moduleMap = new LinkedHashMap<>();
			JsonObject meta = null;

			int passed = 0, failed = 0, skipped = 0;
			long totalDuration = 0;
			String startTime = "";

			for (Path path : jsonFiles)
			{

				JsonObject root = new JsonParser().parse(Files.readString(path)).getAsJsonObject();

				// ---------- META (take first) ----------
				if (meta == null && root.has("meta"))
				{
					meta = root.getAsJsonObject("meta");
				}

				// ---------- SUMMARY ----------
				JsonObject summary = root.getAsJsonObject("summary");
				passed += summary.get("passed").getAsInt();
				failed += summary.get("failed").getAsInt();
				skipped += summary.get("skipped").getAsInt();

				if (summary.get("durationMillis").isJsonPrimitive() && summary.get("durationMillis").getAsJsonPrimitive().isNumber())
				{
					totalDuration += summary.get("durationMillis").getAsLong();
				}

				startTime = summary.get("startTime").getAsString();

				// ---------- DETAILS ----------
				JsonArray details = root.getAsJsonArray("details");
				for (JsonElement d : details)
				{
					allDetails.add(d);
				}

				// ---------- MODULES ----------
				JsonArray modules = root.getAsJsonArray("modules");
				for (JsonElement m : modules)
				{
					JsonObject module = m.getAsJsonObject();
					String moduleName = module.get("module").getAsString();

					moduleMap.merge(moduleName, module, JsonReportMerger::mergeModule);
				}
			}

			// ---------- FINAL SUMMARY ----------
			JsonObject finalSummary = new JsonObject();
			finalSummary.addProperty("passed", passed);
			finalSummary.addProperty("failed", failed);
			finalSummary.addProperty("skipped", skipped);
			finalSummary.addProperty("total", passed + failed + skipped);
			finalSummary.addProperty("durationMillis", totalDuration);
			finalSummary.addProperty("startTime", startTime);

			// ---------- FINAL ROOT ----------
			JsonObject finalRoot = new JsonObject();
			finalRoot.add("summary", finalSummary);
			finalRoot.add("modules", new Gson().toJsonTree(moduleMap.values()));
			finalRoot.add("meta", meta != null ? meta : new JsonObject());
			finalRoot.add("details", allDetails);

			return new GsonBuilder().setPrettyPrinting().create().toJson(finalRoot);

		} catch (Exception e)
		{
			return null;
		}
	}

	// Merge module stats
	private static JsonObject mergeModule(JsonObject a, JsonObject b)
	{
		JsonObject merged = new JsonObject();
		merged.addProperty("module", a.get("module").getAsString());

		merged.addProperty("total", a.get("total").getAsInt() + b.get("total").getAsInt());
		merged.addProperty("passed", a.get("passed").getAsInt() + b.get("passed").getAsInt());
		merged.addProperty("failed", a.get("failed").getAsInt() + b.get("failed").getAsInt());
		merged.addProperty("skipped", a.get("skipped").getAsInt() + b.get("skipped").getAsInt());

		merged.addProperty("durationMillis", a.get("durationMillis").getAsLong() + b.get("durationMillis").getAsLong());

		return merged;
	}

	public static void generateCumulativeReport1(List<Path> files)
	{
		try
		{
			String mergedJson = JsonReportMerger.mergeJsonReports(files);
			NewSummaryReportGenerator.generateReportFromJson(mergedJson);
			Files.writeString(Paths.get("reports/json/merged-report.json"), mergedJson);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void generateCumulativeReport(String files)
	{
		try
		{
			List<Path> reportPaths = new LinkedList<Path>();
			String[] filePaths = files.split(",");
			for (int i = 0; i < filePaths.length; i++)
			{
				reportPaths.add(Paths.get(filePaths[i]));
			}
			String mergedJson = JsonReportMerger.mergeJsonReports(reportPaths);
			NewSummaryReportGenerator.generateReportFromJson(mergedJson);
			Files.writeString(Paths.get("reports/json/merged-report.json"), mergedJson);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
