package reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Custom SmartUI HTML report generator for stakeholder-friendly visual summaries.
 */
public class SmartUICustomReport {
    public static class Entry {
        public String suiteName;
        public String testName;
        public String baselinePath;
        public String actualPath;
        public String diffPath;
        public String screenshotStrategy;
        public String comparisonMethod;
        public double diffPercent;
        public double tolerance;
        public boolean passed;
        public long timestamp;
    }

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    public static void clear() {
        ENTRIES.clear();
    }

    public static void add(String suiteName, String testName,
                           String baselinePath, String actualPath, String diffPath,
                           String screenshotStrategy, String comparisonMethod,
                           double diffPercent, double tolerance, boolean passed) {
        Entry e = new Entry();
        e.suiteName = suiteName;
        e.testName = testName;
        e.baselinePath = baselinePath;
        e.actualPath = actualPath;
        e.diffPath = diffPath;
        e.screenshotStrategy = screenshotStrategy;
        e.comparisonMethod = comparisonMethod;
        e.diffPercent = diffPercent;
        e.tolerance = tolerance;
        e.passed = passed;
        e.timestamp = System.currentTimeMillis();
        ENTRIES.add(e);
    }

    public static String writeHtmlReport(String suiteName) {
        try {
            String dir = "target/smartui-report";
            new File(dir).mkdirs();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String out = dir + "/SmartUI_Report_" + timestamp + ".html";
            
            System.setProperty("smartUIComparisonReportPath", out);

            List<Entry> snapshot = new ArrayList<>(ENTRIES);
            Collections.sort(snapshot, (a, b) -> Long.compare(a.timestamp, b.timestamp));

            try (FileWriter fw = new FileWriter(out, false)) {
                fw.write("<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\">\n");
                fw.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
                fw.write("<title>SmartUI Visual Report - " + escape(suiteName) + "</title>\n");
                fw.write("<style>\n");
                fw.write("body{font-family:Segoe UI,Arial,sans-serif;margin:0;background:#f6f7fb;color:#222;}\n");
                fw.write("header{background:#1e293b;color:#fff;padding:16px 24px;}\n");
                fw.write(".meta{font-size:12px;opacity:.85} .container{padding:16px 24px;}\n");
                fw.write(".card{background:#fff;border-radius:10px;box-shadow:0 6px 18px rgba(0,0,0,.08);margin:14px 0;padding:16px;}\n");
                fw.write(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;}\n");
                fw.write(".tag{display:inline-block;border-radius:999px;padding:4px 10px;font-size:12px;margin-right:6px;}\n");
                fw.write(".pass{background:#e6ffed;color:#036b26;border:1px solid #9ae6b4;}\n");
                fw.write(".fail{background:#ffe6e6;color:#9b1c1c;border:1px solid #feb2b2;}\n");
                fw.write(".imgs{display:flex;gap:10px;flex-wrap:wrap;} .imgs img{max-width:100%;border-radius:6px;border:1px solid #e2e8f0;}\n");
                fw.write(".title{font-weight:600;margin:0 0 6px 0;} .small{font-size:12px;color:#475569;}\n");
                fw.write(".pill{font-size:12px;background:#eef2ff;color:#3730a3;border:1px solid #c7d2fe;border-radius:999px;padding:3px 8px;margin-right:6px;}\n");
                fw.write("details summary{cursor:pointer;font-weight:600;margin:8px 0;}\n");
                fw.write("footer{font-size:12px;color:#475569;padding:12px 24px;}\n");
                fw.write("</style></head><body>\n");
                fw.write("<header><h2 style=\"margin:0\">SmartUI Visual Report</h2><div class=\"meta\">Suite: "
                        + escape(suiteName) + " • Generated: " + timestamp + "</div></header>\n");
                fw.write("<div class=container>\n");

                int pass=0, fail=0;
                for (Entry e : snapshot) { if (e.passed) pass++; else fail++; }
                fw.write("<div class=card><span class=\"tag pass\">Passed: " + pass + "</span>"
                        + "<span class=\"tag fail\">Failed: " + fail + "</span>"
                        + "<span class=\"pill\">Total: " + snapshot.size() + "</span></div>\n");

                for (Entry e : snapshot) {
                    fw.write("<div class=card>\n");
                    fw.write("<div class=title>" + escape(e.testName) + "</div>\n");
                    fw.write("<div class=small>Diff: " + fmt(e.diffPercent) + "% • Tolerance: " + fmt(e.tolerance)
                            + "% • Method: " + escape(nullToEmpty(e.comparisonMethod))
                            + " • Strategy: " + escape(nullToEmpty(e.screenshotStrategy)) + "</div>\n");
                    fw.write("<div style=\"margin:8px 0\"><span class=\"tag " + (e.passed?"pass":"fail") + "\">"
                            + (e.passed?"PASSED":"FAILED") + "</span></div>\n");
                    fw.write("<details><summary>Images</summary><div class=imgs>\n");
                    if (exists(e.baselinePath)) fw.write(img("Baseline",encodeImageToBase64(e.baselinePath)));
                    if (exists(e.actualPath)) fw.write(img("Actual", encodeImageToBase64(e.actualPath)));
                    if (exists(e.diffPath)) fw.write(img("Difference", encodeImageToBase64(e.diffPath)));
                    fw.write("</div></details>\n");
                    fw.write("</div>\n");
                }

                fw.write("</div><footer>SmartUI Custom Report • © " + new SimpleDateFormat("yyyy").format(new Date()) + "</footer></body></html>");
            }

            return out;
        } catch (IOException ex) {
            return null;
        }
    }

    private static boolean exists(String path) {
        return path != null && new File(path).exists() && new File(path).length() > 0;
    }

    private static String img(String label, String path) {
        String safe = path.replace("\\", "/");
        return "<div><div class=small>" + escape(label) + "</div><img src=\"" + escape(safe) + "\" loading=\"lazy\"></div>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String fmt(double d) { return String.format("%.2f", d); }
    
 // Convert image file to Base64 string
 	public static String encodeImageToBase64(String imagePath)
 	{
 		try
 		{
 			File file = new File(imagePath);
 			byte[] imageBytes = Files.readAllBytes(file.toPath());
 			return "data:image/png;base64,"+Base64.getEncoder().encodeToString(imageBytes);
 		} catch (Exception e)
 		{
 			imagePath = null;
 		}
 		return imagePath;

 	}
}


