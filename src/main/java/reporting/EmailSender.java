package reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import base.SuiteLifecycleListener;
import constants.FrameworkConstants;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.StepStatus;
import reporting.DetailedTestReporter.TestExecution;
import zephyrIntegration.DuplicateDefectChecker;

/**
 * EmailSender - builds and sends the HTML automation report, now with Jira bug-key lookup for top failures.
 */
public class EmailSender {

    // ──────────────────────────────
    // 🔹 GLOBAL VARIABLES
    // ──────────────────────────────
    public static String ReportName = System.getProperty("reportFileName");
    public static String CurrentDate, CurrentTime;
    public static String Environment, Project, Build, Date, Time;
    public static String Total, Passed, Failed, Skipped, PassRate;
    public static String StartTime, EndTime, TriggerType, Branch, ShortSHA;
    public static String CloudReportLink, LogsLink, DefectLink;
    public static String Failure1, Failure2, Failure3;
    public static String AppVersion, ApiVersion, Browser, DataSet, Infrastructure, OS;
    public static String DueDate, Env;
    public static String zipPath, FilePath;

    public static List<String> topFails = new ArrayList<>();

    // ──────────────────────────────
    // 🔹 JIRA LOOKUP SUPPORT
    // ──────────────────────────────
    // Cache to avoid repeated Jira calls for same failure text
    private static final ConcurrentMap<String, String> JIRA_LOOKUP_CACHE = new ConcurrentHashMap<>();
    // Lazy-initialized DuplicateDefectChecker (optional)
    private static volatile DuplicateDefectChecker jiraChecker = null;

    private static synchronized DuplicateDefectChecker getJiraChecker() {
        if (jiraChecker == null) {
            try {
                jiraChecker = new DuplicateDefectChecker(DuplicateDefectChecker.DuplicateStrategy.JIRA_QUERY);
            } catch (Throwable t) {
                System.err.println("⚠️  Failed to initialize DuplicateDefectChecker: " + t.getMessage());
                jiraChecker = null;
            }
        }
        return jiraChecker;
    }

    /**
     * Resolve bug key for a failure token or failure text:
     * 1) Try system property (existing behaviour)
     * 2) If not present, check in-memory cache
     * 3) Try DuplicateDefectChecker.findBugKeyByFailure(...) if available
     * 4) Fallback to direct Jira REST search
     *
     * Returns "N/A" when not found.
     */
 // Replace existing resolveBugKey(...) with this
    private static String resolveBugKey(String keyFromFailure, String fullFailureText) {
        try {
            // 1) System property token (preserve current behavior)
            if (keyFromFailure != null && !keyFromFailure.isBlank()) {
                String val = System.getProperty(keyFromFailure);
                if (val != null && !val.isBlank()) {
                    System.out.println("[JIRA-LOOKUP] Found system property for token '" + keyFromFailure + "' -> " + val);
                    return val;
                } else {
                    System.out.println("[JIRA-LOOKUP] No system property for token '" + keyFromFailure + "'");
                }
            }

            // 2) Nothing to search
            if (fullFailureText == null || fullFailureText.isBlank()) {
                System.out.println("[JIRA-LOOKUP] fullFailureText is empty -> returning N/A");
                return "N/A";
            }

            // 3) Cache check
            String cached = JIRA_LOOKUP_CACHE.get(fullFailureText);
            if (cached != null) {
                System.out.println("[JIRA-LOOKUP] Cache hit for failureText -> " + cached);
                return cached;
            }

            System.out.println("[JIRA-LOOKUP] Performing Jira search for: \"" + shortForLog(fullFailureText) + "\"");

            // 4) Try DuplicateDefectChecker (preferred)
            DuplicateDefectChecker checker = getJiraChecker();
            if (checker != null) {
                try {
                    // if your checker supports findBugKeyByFailure, this will call it
                    String found = null;
                    try {
                    	fullFailureText = fullFailureText.split("FailureReason:")[1].trim();
                        found = checker.findBugKeyByFailure(fullFailureText, 1);
                    } catch (Throwable ignore) { /* ignore if method signature differs */ }

                    if (found != null && !found.isBlank()) {
                        System.out.println("[JIRA-LOOKUP] DuplicateDefectChecker found: " + found);
                        JIRA_LOOKUP_CACHE.put(fullFailureText, found);
                        return found;
                    } else {
                        System.out.println("[JIRA-LOOKUP] DuplicateDefectChecker returned no results");
                    }
                } catch (Throwable e) {
                    System.err.println("[JIRA-LOOKUP] DuplicateDefectChecker exception: " + e.getMessage());
                }
            } else {
                System.out.println("[JIRA-LOOKUP] DuplicateDefectChecker is not available, falling back to direct REST search");
            }

            // 5) Fallback to direct Jira REST search
            String direct = searchJiraForFailure(fullFailureText);
            if (direct != null && !direct.isBlank()) {
                System.out.println("[JIRA-LOOKUP] searchJiraForFailure found: " + direct);
                JIRA_LOOKUP_CACHE.put(fullFailureText, direct);
                return direct;
            } else {
                System.out.println("[JIRA-LOOKUP] searchJiraForFailure returned nothing");
            }

        } catch (Throwable t) {
            System.err.println("[JIRA-LOOKUP] resolveBugKey error: " + t.getMessage());
        }
        JIRA_LOOKUP_CACHE.put(fullFailureText, "N/A");
        return "N/A";
    }

    private static String shortForLog(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }


    /**
     * Minimal Jira search fallback using REST API /rest/api/3/search
     * Returns the first issue key found or null if none.
     */
 // Replace existing searchJiraForFailure(...) with this verbose version
    private static String searchJiraForFailure(String failureText) {
        try {
            String jiraBase = System.getProperty("JIRA_BASE_URL");
            String email = System.getProperty("JIRA_EMAIL");
            String apiKey = System.getProperty("JIRA_API_KEY");
            String proj = System.getProperty("PROJECT_KEY");

            System.out.println("[JIRA-REST] Config: base=" + jiraBase + ", project=" + proj + ", user=" + (email != null ? email : "null"));

            if (jiraBase == null || email == null || apiKey == null) {
                System.err.println("[JIRA-REST] Missing Jira config (JIRA_BASE_URL/JIRA_EMAIL/JIRA_API_KEY). Aborting search.");
                return null;
            }

            // safe token
            String cleaned = failureText.replace("\\", "\\\\").replace("\"", "\\\"");
            if (cleaned.length() > 400) cleaned = cleaned.substring(0, 400);

            String jql = (proj != null && !proj.isBlank())
                    ? String.format("project = %s AND issuetype = Bug AND status not in (Closed, Resolved, Done) AND (summary ~ \"%s\" OR description ~ \"%s\") ORDER BY created DESC", proj, cleaned, cleaned)
                    : String.format("issuetype = Bug AND status not in (Closed, Resolved, Done) AND (summary ~ \"%s\" OR description ~ \"%s\") ORDER BY created DESC", cleaned, cleaned);

            System.out.println("[JIRA-REST] JQL: " + shortForLog(jql));

            String encoded = URLEncoder.encode(jql, StandardCharsets.UTF_8);
            String urlStr = jiraBase + "/rest/api/3/search?jql=" + encoded + "&maxResults=1&fields=key";

            System.out.println("[JIRA-REST] URL: " + urlStr);

            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(false);

            String auth = email + ":" + apiKey;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Accept", "application/json");

            int connTimeout = Integer.parseInt(System.getProperty("CONNECTION_TIMEOUT_MS", "10000"));
            int readTimeout = Integer.parseInt(System.getProperty("READ_TIMEOUT_MS", "10000"));
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);

            int code = conn.getResponseCode();
            String body = readHttpResponse(conn);

            System.out.println("[JIRA-REST] HTTP " + code + " - body (first 1000 chars): " + (body == null ? "null" : (body.length() <= 1000 ? body : body.substring(0, 1000) + "...")));

            if (code >= 200 && code < 300) {
                org.json.JSONObject json = new org.json.JSONObject(body);
                org.json.JSONArray issues = json.optJSONArray("issues");
                if (issues != null && issues.length() > 0) {
                    String key = issues.getJSONObject(0).getString("key");
                    System.out.println("[JIRA-REST] Found issue: " + key);
                    return key;
                } else {
                    System.out.println("[JIRA-REST] No issues found for JQL");
                }
            } else {
                System.err.println("[JIRA-REST] Non-success status: " + code);
            }

        } catch (Throwable t) {
            System.err.println("[JIRA-REST] error: " + t.getMessage());
        }
        return null;
    }


    private static String readHttpResponse(HttpsURLConnection conn) {
        try {
            InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }

    // ──────────────────────────────
    // 🔹 MAIN EMAIL SENDER
    // ──────────────────────────────
    public static void sendEmail(String filePaths, String fileNames) {
        try {
            // Load system properties
            String host = System.getProperty("host");
            String port = System.getProperty("port");
            String senderEmail = System.getProperty("senderEmail");
            String senderPassword = System.getProperty("senderPassword");
            String recipients = System.getProperty("recipientEmails");
            String subject = getEmailSubject();

            // Prepare data
            GetParameter();

            // Create mail session
            Session session = createMailSession(getSmtpProperties(host, port), senderEmail, senderPassword);

            Message message = prepareMessage(session, senderEmail, recipients, subject);
            Multipart multipart = new MimeMultipart("mixed");

            // Zip and attach reports
            handleReportAttachments(filePaths, fileNames, multipart);

            // Add email body (HTML)
            addHtmlPart(multipart, getMailHtml());

            // Send email
            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Email sent successfully to: " + recipients);

        } catch (SendFailedException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());

            // 🔹 Log invalid addresses
            if (e.getInvalidAddresses() != null) {
                System.err.println("🚫 Invalid Addresses:");
                for (Address addr : e.getInvalidAddresses()) {
                    System.err.println("   ➤ " + addr.toString());
                }
            }

            // 🔹 Log valid but unsent addresses (like mailbox full)
            if (e.getValidUnsentAddresses() != null) {
                System.err.println("⚠️ Valid but not sent (SMTP rejection):");
                for (Address addr : e.getValidUnsentAddresses()) {
                    System.err.println("   ➤ " + addr.toString());
                }
            }

            // 🔹 Log successfully sent ones
            if (e.getValidSentAddresses() != null) {
                System.out.println("✅ Successfully sent to:");
                for (Address addr : e.getValidSentAddresses()) {
                    System.out.println("   ➤ " + addr.toString());
                }
            }

            // 🔹 Get nested SMTP error info (specific failed recipient)
            Exception next = e.getNextException();
            if (next instanceof com.sun.mail.smtp.SMTPAddressFailedException smtpEx) {
                System.err.println("📧 Failed recipient: " + smtpEx.getAddress());
                System.err.println("📩 SMTP error code: " + smtpEx.getReturnCode());
                System.err.println("📜 Server message: " + smtpEx.getMessage());
            }

            e.printStackTrace();

        } catch (Exception e) {
            System.err.println("❌ General email failure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ──────────────────────────────
    // 🔹 SMTP & SESSION HANDLERS
    // ──────────────────────────────
    private static Properties getSmtpProperties(String host, String port) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }

    private static Session createMailSession(Properties props, String email, String password) {
        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    private static Message prepareMessage(Session session, String from, String toList, String subject) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, parseRecipients(toList));
        message.setSubject(subject);
        return message;
    }

    private static InternetAddress[] parseRecipients(String emailList) {
        return Arrays.stream(emailList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(email -> {
                    try {
                        return new InternetAddress(email);
                    } catch (AddressException e) {
                        System.err.println("❌ Invalid email: " + email);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(InternetAddress[]::new);
    }

    // ──────────────────────────────
    // 🔹 REPORT ATTACHMENT HANDLER
    // ──────────────────────────────
    private static void handleReportAttachments(String filePaths, String fileNames, Multipart multipart) throws Exception {
        String[] paths = filePaths.split(",");
        String[] names = fileNames.split(",");

        String sourceFile = paths[0];

        String targetFolder = "";

        if (ReportName.contains("Daily")) {
            targetFolder = FrameworkConstants.ONEDRIVE_BASE_PATH + "\\DailyCheckListResults\\";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eq7fuRascUlEk9jufCwOBeYByg5PbIo-dOjEf3mfTbKBJg?e=4e7gMT";

        } else if (ReportName.contains("Deploy")) {
            targetFolder = FrameworkConstants.ONEDRIVE_BASE_PATH + "\\DeploymentCheckListResults\\";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eq7fuRascUlEk9jufCwOBeYByg5PbIo-dOjEf3mfTbKBJg?e=4e7gMT";

        } else if (ReportName.contains("Regression")) {
            targetFolder = FrameworkConstants.ONEDRIVE_BASE_PATH + "\\RegressionExecution\\";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eqc9Vj5D0sNMr_rEREbfQgIB1CDqSqq6M-5noPgNHXaTOA?e=dwAkeT";
        }

        // Ensure folder exists
        File folder = new File(targetFolder);
        if (!folder.exists()) folder.mkdirs();

        // Build new file name inside OneDrive folder
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newFilePath = targetFolder + timestamp + "_" + new File(sourceFile).getName();

        // Copy the actual file into OneDrive
        Files.copy(Paths.get(sourceFile), Paths.get(newFilePath), StandardCopyOption.REPLACE_EXISTING);

     // ORIGINAL BEHAVIOR (UNCHANGED)
        FilePath = newFilePath;
        zipPath = newFilePath;

        // ──────────────────────────────
        // 🔹 NETLIFY ADDITION (ONLY NEW)
        // ──────────────────────────────
        File reportFile = new File(newFilePath);
        String reportDir = reportFile.getParent();

//        String netlifyUrl = publishToNetlify(reportDir);
//        if (netlifyUrl != null) {
//            FilePath = netlifyUrl + "/" + reportFile.getName();
//            System.out.println("netlify -> "+FilePath);
//        }
        
        String githubUrl = publishToGitHubRoot(newFilePath);
        if (githubUrl != null) {
            FilePath = githubUrl;
            System.out.println("GitHub Pages -> " + FilePath);
        }
        
        // Attach email files if needed
        boolean useCustomName = "yes".equalsIgnoreCase(System.getProperty("AttachMailFile", "no"));
        if (useCustomName) {
            for (int i = 0; i < paths.length; i++) {
                attachFile(multipart, paths[i], names[i]);
            }
        }

        System.out.println("📄 File stored in OneDrive path: " + newFilePath);
    }

    
    private static void attachFile(Multipart multipart, String filePath, String fileName) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("⚠️ Attachment not found: " + filePath);
                return;
            }
            MimeBodyPart attachment = new MimeBodyPart();
            attachment.attachFile(file);
            attachment.setFileName(fileName);
            multipart.addBodyPart(attachment);
        } catch (Exception e) {
            System.err.println("❌ Failed to attach " + filePath + ": " + e.getMessage());
        }
    }
    
    private static String publishToNetlify(String reportDir) {
        try {
            String userHome = System.getProperty("user.home");

            String nodePath = "C:\\Program Files\\nodejs\\node.exe";
            String netlifyCLIPath =
                    userHome + "\\AppData\\Roaming\\npm\\node_modules\\netlify-cli\\bin\\run.js";

            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "\"" + nodePath + "\" \"" + netlifyCLIPath + "\" deploy " +
                    "--create-site --dir=\"" + reportDir + "\" --prod --json"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[NETLIFY] " + line);
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Netlify exit code: " + exitCode);

            if (exitCode != 0) {
                return null;
            }

            // ---- JSON parsing (no external lib required) ----
            String json = output.toString();
            Pattern p = Pattern.compile("\"url\"\\s*:\\s*\"(https:[^\"]+)\"");
            Matcher m = p.matcher(json);

            if (m.find()) {
                return m.group(1);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─────────────

    // ──────────────────────────────
    // 🔹 ZIP CREATOR
    // ──────────────────────────────
    public static String zipHtmlWithTimestamp(String sourceFile, String oneDriveFolder) {
        ZipSecureFile.setMinInflateRatio(0.001);
        String zipFileName = null;
        String timeStamp = new SimpleDateFormat("ddMMMyyyy_HHmmss").format(new Date());
        if (ReportName.contains("Daily")) {
            zipFileName = "DailyCheckList_" + timeStamp + ".zip";
        } else if (ReportName.contains("Deploy")) {
            zipFileName = "DeploymentCheckList_" + timeStamp + ".zip";
        } else if (ReportName.contains("Regression")) {
            zipFileName = ReportName + "_" + timeStamp + ".zip";
        }
        String destZipFile = oneDriveFolder + File.separator + zipFileName;

        try (FileOutputStream fos = new FileOutputStream(destZipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFile)) {

            zipOut.putNextEntry(new ZipEntry(new File(sourceFile).getName()));
            fis.transferTo(zipOut);
            zipOut.closeEntry();
            return destZipFile;

        } catch (IOException e) {
            System.err.println("❌ Error creating ZIP: " + e.getMessage());
            return null;
        }
    }

    // ──────────────────────────────
    // 🔹 EMAIL HTML BUILDER
    // ──────────────────────────────
    private static void addHtmlPart(Multipart multipart, String htmlContent) throws MessagingException {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html");
        multipart.addBodyPart(htmlPart);
    }

    // ──────────────────────────────
    // 🔹 METADATA GATHERING
    // ──────────────────────────────
    public static void setDateTime() {
        String[] parts = NewCutsomHTMLReport.suiteStartTime.split(" ");
        CurrentDate = parts[0];
        CurrentTime = parts[1];
    }

    public static void GetParameter() {
        ReportName = System.getProperty("reportFileName");
        setDateTime();

        Environment = System.getProperty("Environment");
        Project = System.getProperty("Project");
        Build = System.getProperty("ReleaseVersion");
        Date = CurrentDate;
        Time = CurrentTime;

        List<Map<String, Object>> moduleData = NewSummaryReportGenerator.modules;
        int total = 0;
        int passed = 0;
        int failed = 0;
        int skipped = 0;

        for (Map<String, Object> module : moduleData) {
            total += Integer.parseInt(String.valueOf(module.get("total")));
            passed += Integer.parseInt(String.valueOf(module.get("passed")));
            failed += Integer.parseInt(String.valueOf(module.get("failed")));
            skipped += Integer.parseInt(String.valueOf(module.get("skipped")));
        }

        Total = String.valueOf(total);
        Passed = String.valueOf(passed);
        Failed = String.valueOf(failed);
        Skipped = String.valueOf(skipped);
        int totalTests = Integer.parseInt(Total);
        int passedTests = Integer.parseInt(Passed);
        PassRate = totalTests > 0 ? String.valueOf((passedTests * 100) / totalTests) : "0";

        StartTime = SuiteLifecycleListener.currentDate;
        EndTime = SuiteLifecycleListener.endDateTime;
        Browser = System.getProperty("Browser");
        Env = Environment;

        setTriggerAndGitInfo();
        setInfraAndOS();
        setDueDate();
        calculateTopFailures(DetailedTestReporter.testExecutions);
    }

    private static void setInfraAndOS() {
        WebDriver driver = null;
        String osName = System.getProperty("os.name").toLowerCase();
        OS = osName.contains("win") ? "Windows" :
                osName.contains("mac") ? "macOS" :
                        osName.contains("nux") ? "Linux" : "Unknown";

        Infrastructure = (driver instanceof RemoteWebDriver) ? "Grid" : "Local";
        String cloud = System.getenv("CLOUD_PROVIDER");
        if (cloud != null && !cloud.isEmpty()) Infrastructure = cloud;
    }

    private static void setDueDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        DueDate = new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(cal.getTime());
    }

    private static void setTriggerAndGitInfo() {
        // Trigger type
        String buildCause = System.getenv("BUILD_CAUSE");
        TriggerType = (buildCause != null && buildCause.contains("TIMERTRIGGER")) ? "Scheduled" : "On-Demand";

        // Branch and commit
        Branch = System.getenv("GIT_BRANCH");
        ShortSHA = System.getenv("GIT_COMMIT");

        // Fallback to git command if env variables not present
        if (Branch == null || ShortSHA == null) {
            try {
                Process p1 = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
                Branch = new BufferedReader(new InputStreamReader(p1.getInputStream())).readLine();

                Process p2 = Runtime.getRuntime().exec("git rev-parse --short HEAD");
                ShortSHA = new BufferedReader(new InputStreamReader(p2.getInputStream())).readLine();
            } catch (IOException e) {
                Branch = "Unknown";
                ShortSHA = "Unknown";
            }
        }
    }

    private static String execCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
    }

    private static void calculateTopFailures(List<TestExecution> executions) {
        // Collect counts per module + the reasons seen for that module
        Map<String, Integer> failureCounts = new HashMap<>();
        Map<String, List<String>> moduleFailTests = new HashMap<>();

        if (executions != null) {
            for (TestExecution t : executions) {
                if (t == null || t.getStatus() != ExecutionStatus.FAIL) continue;

                String module = t.getModule() != null ? t.getModule() : "Unknown";
                String failureReason = "Unknown";
                try {
                    var steps = t.getSteps();
                    if (steps != null && !steps.isEmpty()) {
                    	for(int i=0;i<steps.size();i++) {
                    		if(steps.get(i).getStatus()==StepStatus.FAIL) {
                    			String ar = steps.get(i) != null ? steps.get(i).getActualResult() : null;
                    			if (ar != null && !ar.isBlank()) {
                    				failureReason = ar;
                    				break;
                    			}
                    		}
                    	}
                    }
                } catch (Exception ignore) {
                    // Keep "Unknown"
                }

                failureCounts.merge(module, 1, Integer::sum);
                moduleFailTests.computeIfAbsent(module, k -> new ArrayList<>()).add(failureReason);
            }
        }

        // Sort modules by failure count desc, then by module name for stability
        List<String> modulesSorted = failureCounts.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    return (cmp != 0) ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
                })
                .map(Map.Entry::getKey)
                .toList();

        // Build the top 3 overall entries: "Module: X | FailureReason: Y"
        List<String> topFailsLocal = new ArrayList<>(3);
        for (String module : modulesSorted) {
            if (topFailsLocal.size() >= 3) break;

            // Deduplicate reasons per module while preserving order
            List<String> reasons = moduleFailTests.getOrDefault(module, List.of());
            LinkedHashSet<String> uniqueReasons = new LinkedHashSet<>(reasons);

            for (String reason : uniqueReasons) {
                topFailsLocal.add("Module: " + module + " | FailureReason: " + reason);
                if (topFailsLocal.size() >= 3) break;
            }
        }

        // Assign to your existing static fields
        Failure1 = topFailsLocal.size() > 0 ? topFailsLocal.get(0) : "N/A";
        Failure2 = topFailsLocal.size() > 1 ? topFailsLocal.get(1) : "N/A";
        Failure3 = topFailsLocal.size() > 2 ? topFailsLocal.get(2) : "N/A";
    }

    // ──────────────────────────────
    // 🔹 EMAIL SUBJECT BUILDER
    // ──────────────────────────────
    private static String getEmailSubject() {
        boolean isPageLoad = "yes".equalsIgnoreCase(System.getProperty("IsPageLoadReport"));
        return isPageLoad ? System.getProperty("pageloadsubject") : System.getProperty("subject");
    }
    
    private static String publishToGitHubRoot(String reportFilePath) {
        try {
            String token = System.getProperty("GITHUB_TOKEN");
            if (token == null || token.isBlank()) {
                System.err.println("❌ GITHUB_TOKEN not set");
                return null;
            }

            String repo = "rsltkscomm/Automation-Report";
            String pagesBaseUrl = "https://rsltkscomm.github.io/Automation-Report/";

            String tmpDir = System.getProperty("java.io.tmpdir")
                    + "/gh-pages-root-" + System.currentTimeMillis();

            // 1️⃣ Clone repo
            runGit(null,
                    "clone",
                    "https://" + token + "@github.com/" + repo + ".git",
                    tmpDir
            );

            // 2️⃣ Git identity
            runGit(tmpDir, "config", "user.name", "automation-bot");
            runGit(tmpDir, "config", "user.email", "automation@company.com");

            // 3️⃣ Timestamped report
            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String reportName = "report_" + timeStamp + ".html";

            // 4️⃣ Copy report FIRST
            Files.copy(
                    Paths.get(reportFilePath),
                    Paths.get(tmpDir, reportName),
                    StandardCopyOption.REPLACE_EXISTING
            );

            // 5️⃣ Commit & push
            runGit(tmpDir, "add", reportName);
            runGit(tmpDir, "commit", "-m", "Add report " + reportName);
            runGit(tmpDir, "push");

            return pagesBaseUrl + reportName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String GIT_EXE =
            "C:\\Program Files\\Git\\cmd\\git.exe";

    private static void runGit(String dir, String... cmd) throws Exception {

        String[] fullCmd = new String[cmd.length + 1];
        fullCmd[0] = GIT_EXE;
        System.arraycopy(cmd, 0, fullCmd, 1, cmd.length);

        ProcessBuilder pb = new ProcessBuilder(fullCmd);

        if (dir != null) {
            File d = new File(dir);
            if (!d.exists()) {
                throw new IOException("Git working directory does not exist: " + dir);
            }
            pb.directory(d);
        }

        pb.redirectErrorStream(true);

        Process p = pb.start();
        int exitCode = p.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                "Git command failed: " + String.join(" ", fullCmd)
            );
        }
    }

    // ──────────────────────────────
    // 🔹 EMAIL HTML TEMPLATE
    // ──────────────────────────────
    private static String getMailHtml() {

        // Helper: extract a reasonable System property key from a failure line.
        // Supports either "... Test: <key>" or "... FailureReason: <key>"
        java.util.function.Function<String, String> extractBugKey = (failure) -> {
            if (failure == null) return null;

            // Try "Test:"
            int idx = failure.indexOf("Test:");
            if (idx >= 0) {
                String after = failure.substring(idx + "Test:".length()).trim();
                return after.isEmpty() ? null : after;
            }

            // Try "FailureReason:"
            idx = failure.indexOf("FailureReason:");
            if (idx >= 0) {
                String after = failure.substring(idx + "FailureReason:".length()).trim();
                return after.isEmpty() ? null : after;
            }

            return null; // no recognizable token found
        };

        // Build the final list of failures to render (without mutating Failure1/2/3)
        java.util.List<String> validFails = java.util.stream.Stream.of(Failure1, Failure2, Failure3)
                .filter(f -> f != null && !f.trim().isEmpty() && !"N/A".equalsIgnoreCase(f.trim()))
                .map(f -> {
                    String key = extractBugKey.apply(f);
                    // Resolve bug id: prefer system property token, then Jira lookup if needed
                    String bugId = "N/A";

                    if (key != null && !key.isBlank()) {
                        String prop = System.getProperty(key);
                        if (prop != null && !prop.isBlank()) {
                            bugId = prop;
                        }
                    }

                    // If still N/A, attempt Jira lookup using the full failure text
                    if ("N/A".equalsIgnoreCase(bugId)) {
                        bugId = resolveBugKey(key, f);
                    }

                    return f + " - Bug ID : <b>" + (bugId == null ? "N/A" : bugId) + "</b>";
                })
                .collect(java.util.stream.Collectors.toList());

        String failuresSection = validFails.isEmpty() ? "" :
                "<h4 style='color:#34495e;margin-top:25px;'>Failures (Top Items)</h4><ol style='margin-left:25px;'>"
                        + validFails.stream().map(f -> "<li>" + f + "</li>").collect(java.util.stream.Collectors.joining())
                        + "</ol>";

        String reportName =
                ReportName != null && ReportName.toLowerCase().contains("daily") ? "Daily Checklist" :
                        ReportName != null && ReportName.toLowerCase().contains("postproduction") ? "Post Production Checklist" :
                                "Regression";

        // Main email HTML (unchanged layout/styles)
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 0;'>" +
                "  <table width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color: #f7f7f7; padding: 20px;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <div style='background-color: #ffffff; max-width: 700px; border-radius: 10px; padding: 30px; " +
                "box-shadow: 0 0 10px rgba(0,0,0,0.1); text-align: left;'>" +

                "          <div style='font-size: 20px; font-weight: bold; color: #2c3e50; margin-bottom: 20px; " +
                "text-align: center;'>" + reportName + " Automation Report</div>" +

                "          <p>Hi All,</p>" +
                "          <p>" + reportName + " has been successfully completed on <b>" + Environment + "</b> " +
                "Environment for <b>" + Project + "</b> (Build: <b>" + Build + "</b>) on <b>" + Date + " " + Time + " IST</b>.</p>" +

                "          <h4 style='color: #34495e; margin-top: 25px;'>Key Results</h4>" +
                "          <ul style='list-style-type: disc; margin-left: 25px;'>" +
                "            <li>Total test cases executed: <b>" + Total + "</b></li>" +
                "            <li>Passed: <b>" + Passed + "</b></li>" +
                "            <li>Failed: <b>" + Failed + "</b></li>" +
                "            <li>Skipped/Blocked: <b>" + Skipped + "</b></li>" +
                "            <li>Pass rate: <b>" + PassRate + "%</b></li>" +
                "            <li>Execution window: <b>" + StartTime + " : " + EndTime + " IST</b></li>" +
                "            <li>Trigger type: <b>" + TriggerType + "</b> | Branch: <b>" + Branch + "</b> | Commit: <b>" + ShortSHA + "</b></li>" +
                "          </ul>" +

                "          <h4 style='color: #34495e; margin-top: 25px;'>Quick Links</h4>" +
                "          <ul style='list-style-type: disc; margin-left: 25px;'>" +
                "            <li>Execution report: <a href='" + FilePath + "' style='color: #007bff;'>[Report Link]</a></li>" +
                "            <li>Logs / screenshots (if any): <a href='" + LogsLink + "' style='color: #007bff;'>[OneDrive Evidence Link]</a></li>" +
                "          </ul>" +

                failuresSection +

                "          <h4 style='color: #34495e; margin-top: 25px;'>Environment & Run Details</h4>" +
                "          <ul style='list-style-type: disc; margin-left: 25px;'>" +
                "            <li>Browser: <b>" + Browser + "</b></li>" +
                "            <li>Infrastructure: <b>" + Infrastructure + "</b> | OS: <b>" + OS + "</b></li>" +
                "          </ul>" +

                "          <h4 style='color: #34495e; margin-top: 25px;'>Next Actions</h4>" +
                "          <ul style='list-style-type: disc; margin-left: 25px;'>" +
                "            <li>Owners to review failing scenarios and update defect status by <b>" + DueDate + "</b>.</li>" +
                "            <li>Automation team will rerun impacted tests after fixes are deployed in <b>" + Env + "</b> Environment.</li>" +
                "          </ul>" +

                "          <div style='text-align: center; margin-top: 30px; font-size: 14px; color: #555;'>Thanks,<br/><b>QA Automation Team</b></div>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }
}
