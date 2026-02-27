package reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import org.json.JSONObject;
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

public class EmailSender
{
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
    private static final ConcurrentMap<String, String> JIRA_LOOKUP_CACHE = new ConcurrentHashMap<>();
    private static volatile DuplicateDefectChecker jiraChecker = null;
    private static final String GIT_EXE = "C:\\Program Files\\Git\\cmd\\git.exe";

    private static synchronized DuplicateDefectChecker getJiraChecker()
    {
        if (jiraChecker == null)
        {
            try
            {
                jiraChecker = new DuplicateDefectChecker(DuplicateDefectChecker.DuplicateStrategy.JIRA_QUERY);
            } catch (Throwable t)
            {
                System.err.println("⚠️  Failed to initialize DuplicateDefectChecker: " + t.getMessage());
                jiraChecker = null;
            }
        }
        return jiraChecker;
    }

    private static String resolveBugKey(String keyFromFailure, String fullFailureText)
    {
        try
        {
            if (keyFromFailure != null && !keyFromFailure.isBlank())
            {
                String val = System.getProperty(keyFromFailure);
                if (val != null && !val.isBlank())
                {
                    System.out.println("[JIRA-LOOKUP] Found system property for token '" + keyFromFailure + "' -> " + val);
                    return val;
                }
            }

            if (fullFailureText == null || fullFailureText.isBlank())
            {
                return "N/A";
            }

            String cached = JIRA_LOOKUP_CACHE.get(fullFailureText);
            if (cached != null)
            {
                return cached;
            }

            DuplicateDefectChecker checker = getJiraChecker();
            if (checker != null)
            {
                try
                {
                    String searchText = fullFailureText;
                    if (fullFailureText.contains("FailureReason:")) {
                        searchText = fullFailureText.split("FailureReason:")[1].trim();
                    }
                    String found = checker.findBugKeyByFailure(searchText, 1);
                    if (found != null && !found.isBlank())
                    {
                        JIRA_LOOKUP_CACHE.put(fullFailureText, found);
                        return found;
                    }
                } catch (Throwable e)
                {
                    System.err.println("[JIRA-LOOKUP] DuplicateDefectChecker exception: " + e.getMessage());
                }
            }

            String direct = searchJiraForFailure(fullFailureText);
            if (direct != null && !direct.isBlank())
            {
                JIRA_LOOKUP_CACHE.put(fullFailureText, direct);
                return direct;
            }

        } catch (Throwable t)
        {
            System.err.println("[JIRA-LOOKUP] resolveBugKey error: " + t.getMessage());
        }
        JIRA_LOOKUP_CACHE.put(fullFailureText, "N/A");
        return "N/A";
    }

    private static String searchJiraForFailure(String failureText)
    {
        try
        {
            String jiraBase = System.getProperty("JIRA_BASE_URL");
            String email = System.getProperty("JIRA_EMAIL");
            String apiKey = System.getProperty("JIRA_API_KEY");
            String proj = System.getProperty("PROJECT_KEY");

            if (jiraBase == null || email == null || apiKey == null)
            {
                return null;
            }

            String cleaned = failureText.replace("\\", "\\\\").replace("\"", "\\\"");
            if (cleaned.length() > 400)
                cleaned = cleaned.substring(0, 400);

            String jql = (proj != null && !proj.isBlank()) ? 
                String.format("project = %s AND issuetype = Bug AND status not in (Closed, Resolved, Done) AND (summary ~ \"%s\" OR description ~ \"%s\") ORDER BY created DESC", proj, cleaned, cleaned)
                : String.format("issuetype = Bug AND status not in (Closed, Resolved, Done) AND (summary ~ \"%s\" OR description ~ \"%s\") ORDER BY created DESC", cleaned, cleaned);

            String encoded = URLEncoder.encode(jql, StandardCharsets.UTF_8);
            String urlStr = jiraBase + "/rest/api/3/search?jql=" + encoded + "&maxResults=1&fields=key";

            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String auth = email + ":" + apiKey;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Accept", "application/json");

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300)
            {
                String body = readHttpResponse(conn);
                org.json.JSONObject json = new org.json.JSONObject(body);
                org.json.JSONArray issues = json.optJSONArray("issues");
                if (issues != null && issues.length() > 0)
                {
                    return issues.getJSONObject(0).getString("key");
                }
            }

        } catch (Throwable t)
        {
            System.err.println("[JIRA-REST] error: " + t.getMessage());
        }
        return null;
    }

    private static String readHttpResponse(HttpsURLConnection conn)
    {
        try
        {
            InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
            {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                return sb.toString();
            }
        } catch (Exception e)
        {
            return "";
        }
    }

    public static void sendEmail(String filePaths, String fileNames)
    {
        try
        {
            System.out.println("📤 Sending email with attachments:");
            System.out.println("Paths: " + filePaths);
            System.out.println("Names: " + fileNames);
            
            String host = System.getProperty("host");
            String port = System.getProperty("port");
            String senderEmail = System.getProperty("senderEmail");
            String senderPassword = System.getProperty("senderPassword");
            String recipients = System.getProperty("recipientEmails");
            String subject = getEmailSubject();

            GetParameter();

            Session session = createMailSession(getSmtpProperties(host, port), senderEmail, senderPassword);
            Message message = prepareMessage(session, senderEmail, recipients, subject);
            Multipart multipart = new MimeMultipart("mixed");

            handleReportAttachments(filePaths, fileNames, multipart);
            addHtmlPart(multipart, getMailHtml());

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Email sent successfully to: " + recipients);

        } catch (SendFailedException e)
        {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            if (e.getInvalidAddresses() != null)
            {
                System.err.println("🚫 Invalid Addresses:");
                for (Address addr : e.getInvalidAddresses())
                {
                    System.err.println("   ➤ " + addr.toString());
                }
            }
            e.printStackTrace();
        } catch (Exception e)
        {
            System.err.println("❌ General email failure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties getSmtpProperties(String host, String port)
    {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }

    private static Session createMailSession(Properties props, String email, String password)
    {
        return Session.getInstance(props, new Authenticator()
        {
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    private static Message prepareMessage(Session session, String from, String toList, String subject) throws MessagingException
    {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, parseRecipients(toList));
        message.setSubject(subject);
        return message;
    }

    private static InternetAddress[] parseRecipients(String emailList)
    {
        return Arrays.stream(emailList.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(email -> {
            try
            {
                return new InternetAddress(email);
            } catch (AddressException e)
            {
                System.err.println("❌ Invalid email: " + email);
                return null;
            }
        }).filter(Objects::nonNull).toArray(InternetAddress[]::new);
    }

    private static void handleReportAttachments(String filePaths, String fileNames, Multipart multipart) throws Exception
    {
        String[] paths = filePaths.split(",");
        String[] names = fileNames.split(",");

        String sourceFile = paths[0];
        String targetFolder = "";

        if (ReportName.contains("Daily"))
        {
            targetFolder = FrameworkConstants.ONEDRIVE_BASE_PATH + "\\DailyCheckListResults\\";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eq7fuRascUlEk9jufCwOBeYByg5PbIo-dOjEf3mfTbKBJg?e=4e7gMT";
        } 
        else if (ReportName.contains("Deploy"))
        {
            targetFolder = FrameworkConstants.ONEDRIVE_BASE_PATH + "\\DeploymentCheckListResults\\";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eq7fuRascUlEk9jufCwOBeYByg5PbIo-dOjEf3mfTbKBJg?e=4e7gMT";
        } 
        else if (ReportName.contains("Regression"))
        {
            targetFolder = FrameworkConstants.ONEDRIVE_BASE_PATH + "\\RegressionExecution\\";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eqc9Vj5D0sNMr_rEREbfQgIB1CDqSqq6M-5noPgNHXaTOA?e=dwAkeT";
        }

        File folder = new File(targetFolder);
        if (!folder.exists())
            folder.mkdirs();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String newFilePath = targetFolder + timestamp + "_" + new File(sourceFile).getName();

        Files.copy(Paths.get(sourceFile), Paths.get(newFilePath), StandardCopyOption.REPLACE_EXISTING);

        FilePath = newFilePath;
        zipPath = newFilePath;

        // Publish to GitHub and wait for URL
        String githubUrl = publishToGitHubRoot(newFilePath);
        if (githubUrl != null) {
            FilePath = githubUrl;
            System.out.println("✅ GitHub Pages URL: " + FilePath);
        }

        boolean useCustomName = "yes".equalsIgnoreCase(System.getProperty("AttachMailFile", "no"));
        if (useCustomName)
        {
            for (int i = 0; i < paths.length; i++)
            {
                attachFile(multipart, paths[i], names[i]);
            }
        }

        System.out.println("📄 File stored in OneDrive path: " + newFilePath);
    }

    private static void attachFile(Multipart multipart, String filePath, String fileName)
    {
        try
        {
            File file = new File(filePath);
            if (!file.exists())
            {
                System.err.println("⚠️ Attachment not found: " + filePath);
                return;
            }
            MimeBodyPart attachment = new MimeBodyPart();
            attachment.attachFile(file);
            attachment.setFileName(fileName);
            multipart.addBodyPart(attachment);
        } catch (Exception e)
        {
            System.err.println("❌ Failed to attach " + filePath + ": " + e.getMessage());
        }
    }

    public static String zipHtmlWithTimestamp(String sourceFile, String oneDriveFolder)
    {
        ZipSecureFile.setMinInflateRatio(0.001);
        String zipFileName = null;
        String timeStamp = new SimpleDateFormat("ddMMMyyyy_HHmmss").format(new Date());
        if (ReportName.contains("Daily"))
        {
            zipFileName = "DailyCheckList_" + timeStamp + ".zip";
        } else if (ReportName.contains("Deploy"))
        {
            zipFileName = "DeploymentCheckList_" + timeStamp + ".zip";
        } else if (ReportName.contains("Regression"))
        {
            zipFileName = ReportName + "_" + timeStamp + ".zip";
        }
        String destZipFile = oneDriveFolder + File.separator + zipFileName;

        try (FileOutputStream fos = new FileOutputStream(destZipFile); 
             ZipOutputStream zipOut = new ZipOutputStream(fos); 
             FileInputStream fis = new FileInputStream(sourceFile))
        {
            zipOut.putNextEntry(new ZipEntry(new File(sourceFile).getName()));
            fis.transferTo(zipOut);
            zipOut.closeEntry();
            return destZipFile;
        } catch (IOException e)
        {
            System.err.println("❌ Error creating ZIP: " + e.getMessage());
            return null;
        }
    }

    private static void addHtmlPart(Multipart multipart, String htmlContent) throws MessagingException
    {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html");
        multipart.addBodyPart(htmlPart);
    }

    public static void setDateTime()
    {
        String[] parts = NewCutsomHTMLReport.suiteStartTime.split(" ");
        CurrentDate = parts[0];
        CurrentTime = parts[1];
    }

    public static void GetParameter()
    {
        ReportName = System.getProperty("SuiteName");
        setDateTime();

        Environment = System.getProperty("Environment");
        Project = System.getProperty("Project");
        Build = System.getProperty("ReleaseVersion");
        Date = CurrentDate;
        Time = CurrentTime;

        NewSummaryReportGenerator.AggregatedStats agg = NewSummaryReportGenerator.aggregateStats();

        Total = String.valueOf(agg.totalSkip + agg.totalPass + agg.totalFail);
        Passed = String.valueOf(agg.totalPass);
        Failed = String.valueOf(agg.totalFail);
        Skipped = String.valueOf(agg.totalSkip);
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

    private static void setInfraAndOS()
    {
        WebDriver driver = null;
        String osName = System.getProperty("os.name").toLowerCase();
        OS = osName.contains("win") ? "Windows" : osName.contains("mac") ? "macOS" : osName.contains("nux") ? "Linux" : "Unknown";

        Infrastructure = (driver instanceof RemoteWebDriver) ? "Grid" : "Local";
        String cloud = System.getenv("CLOUD_PROVIDER");
        if (cloud != null && !cloud.isEmpty())
            Infrastructure = cloud;
    }

    private static void setDueDate()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        DueDate = new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(cal.getTime());
    }

    private static void setTriggerAndGitInfo()
    {
        String buildCause = System.getenv("BUILD_CAUSE");
        TriggerType = (buildCause != null && buildCause.contains("TIMERTRIGGER")) ? "Scheduled" : "On-Demand";

        Branch = System.getenv("GIT_BRANCH");
        ShortSHA = System.getenv("GIT_COMMIT");

        if (Branch == null || ShortSHA == null)
        {
            try
            {
                Process p1 = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
                Branch = new BufferedReader(new InputStreamReader(p1.getInputStream())).readLine();

                Process p2 = Runtime.getRuntime().exec("git rev-parse --short HEAD");
                ShortSHA = new BufferedReader(new InputStreamReader(p2.getInputStream())).readLine();
            } catch (IOException e)
            {
                Branch = "Unknown";
                ShortSHA = "Unknown";
            }
        }
    }

    private static void calculateTopFailures(List<TestExecution> executions)
    {
        Map<String, Integer> failureCounts = new HashMap<>();
        Map<String, List<String>> moduleFailTests = new HashMap<>();

        if (executions != null)
        {
            for (TestExecution t : executions)
            {
                if (t == null || t.getStatus() != ExecutionStatus.FAIL)
                    continue;

                String module = t.getModule() != null ? t.getModule() : "Unknown";
                String failureReason = "Unknown";
                try
                {
                    var steps = t.getSteps();
                    if (steps != null && !steps.isEmpty())
                    {
                        for (int i = 0; i < steps.size(); i++)
                        {
                            if (steps.get(i).getStatus() == StepStatus.FAIL)
                            {
                                String ar = steps.get(i) != null ? steps.get(i).getActualResult() : null;
                                if (ar != null && !ar.isBlank())
                                {
                                    failureReason = ar;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {}

                failureCounts.merge(module, 1, Integer::sum);
                moduleFailTests.computeIfAbsent(module, k -> new ArrayList<>()).add(failureReason);
            }
        }

        List<String> modulesSorted = failureCounts.entrySet().stream()
            .sorted((a, b) -> {
                int cmp = Integer.compare(b.getValue(), a.getValue());
                return (cmp != 0) ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
            })
            .map(Map.Entry::getKey)
            .toList();

        List<String> topFailsLocal = new ArrayList<>(3);
        for (String module : modulesSorted)
        {
            if (topFailsLocal.size() >= 3)
                break;

            List<String> reasons = moduleFailTests.getOrDefault(module, List.of());
            LinkedHashSet<String> uniqueReasons = new LinkedHashSet<>(reasons);

            for (String reason : uniqueReasons)
            {
                topFailsLocal.add("Module: " + module + " | FailureReason: " + reason);
                if (topFailsLocal.size() >= 3)
                    break;
            }
        }

        Failure1 = topFailsLocal.size() > 0 ? topFailsLocal.get(0) : "N/A";
        Failure2 = topFailsLocal.size() > 1 ? topFailsLocal.get(1) : "N/A";
        Failure3 = topFailsLocal.size() > 2 ? topFailsLocal.get(2) : "N/A";
    }

    private static String getEmailSubject()
    {
        boolean isPageLoad = "yes".equalsIgnoreCase(System.getProperty("IsPageLoadReport"));
        return isPageLoad ? System.getProperty("pageloadsubject") : System.getProperty("subject");
    }

    private static String publishToGitHubRoot(String reportFilePath)
    {
        try
        {
            String token = System.getProperty("GITHUB_TOKEN");
            if (token == null || token.isBlank())
            {
                System.err.println("❌ GITHUB_TOKEN not set");
                return null;
            }

            String repo = "rsltkscomm/Automation-Report";
            String pagesBaseUrl = "https://rsltkscomm.github.io/Automation-Report/";
            
            // Use a SINGLE persistent directory
            String repoDir = "C:/automation/github-pages/repo";
            File repoDirectory = new File(repoDir);
            
            // Check if repo exists and is valid
            boolean repoExists = repoDirectory.exists() && new File(repoDir, ".git").exists();
            
            if (!repoExists) {
                // First time - clone the entire repo (only happens once)
                System.out.println("📦 First run: Cloning repository (this is a one-time operation)...");
                if (repoDirectory.exists()) {
                    deleteDirectory(repoDirectory);
                }
                String cloneUrl = "https://" + token + "@github.com/" + repo + ".git";
                runGit("C:/automation/github-pages", "clone", cloneUrl, "repo");
                
                // After clone, check which branch exists and standardize to 'main'
                String defaultBranch = detectDefaultBranch(repoDir);
                System.out.println("📌 Default branch detected: " + defaultBranch);
                
                if (!"main".equals(defaultBranch)) {
                    // Rename the branch to main
                    runGit(repoDir, "branch", "-m", defaultBranch, "main");
                    // Push the renamed branch and delete the old one
                    runGit(repoDir, "push", "origin", "main");
                    runGit(repoDir, "push", "origin", "--delete", defaultBranch);
                    // Set upstream
                    runGit(repoDir, "branch", "--set-upstream-to=origin/main", "main");
                }
            } else {
                // Subsequent runs - just pull latest changes from main
                System.out.println("🔄 Updating existing repository...");
                
                // Clean any stale locks
                cleanupGitLocks(repoDir);
                
                // Ensure we're on main branch
                try {
                    runGit(repoDir, "checkout", "main");
                } catch (Exception e) {
                    // If main doesn't exist locally, create it from origin/main
                    try {
                        runGit(repoDir, "fetch", "origin");
                        runGit(repoDir, "checkout", "-b", "main", "origin/main");
                    } catch (Exception ex) {
                        System.out.println("⚠️ Could not switch to main: " + ex.getMessage());
                    }
                }
                
                // Pull latest changes from main
                try {
                    runGit(repoDir, "fetch", "origin");
                    runGit(repoDir, "reset", "--hard", "origin/main");
                } catch (Exception e) {
                    System.out.println("⚠️ Pull failed: " + e.getMessage());
                }
            }

            // Configure git
            runGit(repoDir, "config", "user.name", "automation-bot");
            runGit(repoDir, "config", "user.email", "automation@company.com");

            // Copy the new report
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String reportName = "report_" + timeStamp + ".html";
            Files.copy(Paths.get(reportFilePath), Paths.get(repoDir, reportName), StandardCopyOption.REPLACE_EXISTING);

            // Update index.html with link to new report
            updateIndexHtml(repoDir, reportName, timeStamp);

            // Add and commit (only new files)
            runGit(repoDir, "add", reportName);
            runGit(repoDir, "add", "index.html");
            
            // Check if there are changes
            ProcessBuilder statusPb = new ProcessBuilder(GIT_EXE, "status", "--porcelain");
            statusPb.directory(repoDirectory);
            Process statusProcess = statusPb.start();
            String status = new String(statusProcess.getInputStream().readAllBytes());
            
            if (!status.isEmpty()) {
                runGit(repoDir, "commit", "-m", "Add report " + timeStamp);
                
                // Push to main only
                System.out.println("📤 Pushing to GitHub (main branch)...");
                runGit(repoDir, "push", "origin", "main");
            } else {
                System.out.println("⚠️ No changes to commit");
            }

            // Verify the branch is set correctly in GitHub Pages
            verifyGithubPagesBranch(repo, token);

            return pagesBaseUrl + reportName;

        } catch (Exception e)
        {
            System.err.println("❌ GitHub publish failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String detectDefaultBranch(String repoDir) throws Exception {
        try {
            // Try to get current branch
            ProcessBuilder pb = new ProcessBuilder(GIT_EXE, "branch", "--show-current");
            pb.directory(new File(repoDir));
            Process p = pb.start();
            String branch = new String(p.getInputStream().readAllBytes()).trim();
            if (!branch.isEmpty()) return branch;
        } catch (Exception e) {
            // Ignore
        }
        
        // If that fails, try to get from remote
        try {
            ProcessBuilder pb = new ProcessBuilder(GIT_EXE, "ls-remote", "--symref", "origin", "HEAD");
            pb.directory(new File(repoDir));
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            // Parse output to find default branch
            Pattern pattern = Pattern.compile("ref: refs/heads/(\\S+)\\s+HEAD");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return "master"; // Default fallback
    }

    private static void verifyGithubPagesBranch(String repo, String token) {
        try {
            // This is optional - just logs the current Pages settings
            String apiUrl = "https://api.github.com/repos/" + repo + "/pages";
            
            URL url = new URL(apiUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "token " + token);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readHttpResponse(conn);
                System.out.println("📌 GitHub Pages is configured on: " + response);
            } else {
                System.out.println("⚠️ Could not verify GitHub Pages configuration");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error verifying GitHub Pages: " + e.getMessage());
        }
    }

    private static void updateIndexHtml(String repoDir, String newReport, String timestamp) throws IOException {
        File indexFile = new File(repoDir, "index.html");
        List<String> lines;
        
        if (indexFile.exists()) {
            lines = Files.readAllLines(indexFile.toPath());
            
            // Check if we need to add "Latest" tag to previous latest
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("(Latest)")) {
                    lines.set(i, lines.get(i).replace(" (Latest)", ""));
                    break;
                }
            }
        } else {
            lines = new ArrayList<>();
            lines.add("<!DOCTYPE html>");
            lines.add("<html>");
            lines.add("<head><title>Automation Reports</title>");
            lines.add("<style>");
            lines.add("body { font-family: Arial, sans-serif; margin: 20px; }");
            lines.add("h1 { color: #333; }");
            lines.add("ul { list-style-type: none; padding: 0; }");
            lines.add("li { margin: 10px 0; }");
            lines.add("a { color: #0066cc; text-decoration: none; }");
            lines.add("a:hover { text-decoration: underline; }");
            lines.add(".latest { font-weight: bold; color: #28a745; }");
            lines.add("</style>");
            lines.add("</head>");
            lines.add("<body>");
            lines.add("<h1>Automation Test Reports</h1>");
            lines.add("<ul>");
            lines.add("</ul>");
            lines.add("</body>");
            lines.add("</html>");
        }
        
        // Find the <ul> tag and insert new link at top with "Latest" tag
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("<ul>")) {
                String newLink = "  <li><a href='" + newReport + "' class='latest'>🚀 Report " + timestamp + " (Latest)</a></li>";
                lines.add(i + 1, newLink);
                break;
            }
        }
        
        Files.write(indexFile.toPath(), lines);
    }

    private static void cleanupGitLocks(String repoDir) {
        try {
            File gitDir = new File(repoDir, ".git");
            if (!gitDir.exists()) return;
            
            Files.walk(gitDir.toPath())
                .filter(p -> p.toString().endsWith(".lock"))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        } catch (Exception e) {
            // Ignore
        }
    }

    private static String uploadViaGitHubAPI(String reportFilePath) throws Exception {
        String token = System.getProperty("GITHUB_TOKEN");
        String repo = "rsltkscomm/Automation-Report";
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String reportName = "report_" + timeStamp + ".html";
        
        // Read file content
        byte[] content = Files.readAllBytes(Paths.get(reportFilePath));
        String encodedContent = Base64.getEncoder().encodeToString(content);
        
        // Create API request
        String apiUrl = "https://api.github.com/repos/" + repo + "/contents/" + reportName;
        
        JSONObject body = new JSONObject();
        body.put("message", "Add report " + timeStamp);
        body.put("content", encodedContent);
        body.put("branch", "main");
        
        URL url = new URL(apiUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes());
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            System.out.println("✅ File uploaded via API");
            return "https://rsltkscomm.github.io/Automation-Report/" + reportName;
        } else {
            String response = readHttpResponse(conn);
            System.err.println("❌ API upload failed: " + responseCode + " - " + response);
            return null;
        }
    }

    private static void cleanupOldRuns() {
        try {
            File baseDir = new File("C:/automation/github-pages");
            if (!baseDir.exists()) return;
            
            File[] runs = baseDir.listFiles((dir, name) -> name.startsWith("run_"));
            if (runs != null && runs.length > 5) {
                Arrays.sort(runs, (a, b) -> b.getName().compareTo(a.getName())); // newest first
                
                // Keep newest 5, delete rest
                for (int i = 5; i < runs.length; i++) {
                    deleteDirectory(runs[i]);
                    System.out.println("🧹 Cleaned up old run: " + runs[i].getName());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Cleanup failed: " + e.getMessage());
        }
    }

    private static void deleteDirectory(File directory) {
        try {
            if (directory.exists()) {
                Files.walk(directory.toPath())
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void runGit(String dir, String... cmd) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(GIT_EXE);
        command.addAll(Arrays.asList(cmd));

        System.out.println("[GIT] " + String.join(" ", cmd));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);

        Process p = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[GIT] " + line);
                output.append(line).append("\n");
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed: " + String.join(" ", cmd) + "\nOutput: " + output.toString());
        }
    }

    private static String getMailHtml()
    {
        String executionReportLink = "";
        if (FilePath != null && FilePath.startsWith("https://rsltkscomm.github.io/Automation-Report/")) {
            executionReportLink = "<li>Execution report: <a href='" + FilePath + "' style='color: #007bff;'>[Report Link]</a></li>";
        }

        List<String> validFails = new ArrayList<>();
        for (String failure : Arrays.asList(Failure1, Failure2, Failure3)) {
            if (failure != null && !failure.trim().isEmpty() && !"N/A".equalsIgnoreCase(failure.trim())) {
                String key = null;
                int idx = failure.indexOf("Test:");
                if (idx >= 0) {
                    key = failure.substring(idx + "Test:".length()).trim();
                } else {
                    idx = failure.indexOf("FailureReason:");
                    if (idx >= 0) {
                        key = failure.substring(idx + "FailureReason:".length()).trim();
                    }
                }
                
                String bugId = "N/A";
                if (key != null && !key.isBlank()) {
                    String prop = System.getProperty(key);
                    if (prop != null && !prop.isBlank()) {
                        bugId = prop;
                    }
                }
                
                if ("N/A".equalsIgnoreCase(bugId)) {
                    bugId = resolveBugKey(key, failure);
                }
                
                validFails.add(failure + " - Bug ID : <b>" + (bugId == null ? "N/A" : bugId) + "</b>");
            }
        }

        String failuresSection = validFails.isEmpty() ? ""
            : "<h4 style='color:#34495e;margin-top:25px;'>Failures (Top Items)</h4><ol style='margin-left:25px;'>" 
            + validFails.stream().map(f -> "<li>" + f + "</li>").collect(java.util.stream.Collectors.joining()) 
            + "</ol>";

        String reportName = ReportName != null && ReportName.toLowerCase().contains("daily") ? "Daily Checklist" 
            : ReportName != null && ReportName.toLowerCase().contains("postproduction") ? "Post Production Checklist" 
            : "Regression";

        return "<!DOCTYPE html>" 
            + "<html>" 
            + "<body style='font-family: Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 0;'>"
            + "  <table width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color: #f7f7f7; padding: 20px;'>" 
            + "    <tr>" 
            + "      <td align='center'>"
            + "        <div style='background-color: #ffffff; max-width: 700px; border-radius: 10px; padding: 30px; box-shadow: 0 0 10px rgba(0,0,0,0.1); text-align: left;'>"
            + "          <div style='font-size: 20px; font-weight: bold; color: #2c3e50; margin-bottom: 20px; text-align: center;'>" 
            + reportName + " Automation Report</div>"
            + "          <p>Hi All,</p>" 
            + "          <p>" + reportName + " has been successfully completed on <b>" + Environment + "</b> " 
            + "Environment for <b>" + Project + "</b> (Build: <b>" + Build + "</b>) on <b>" + Date + " " + Time + " IST</b>.</p>"
            + "          <h4 style='color: #34495e; margin-top: 25px;'>Key Results</h4>" 
            + "          <ul style='list-style-type: disc; margin-left: 25px;'>" 
            + "            <li>Total test cases executed: <b>" + Total + "</b></li>"
            + "            <li>Passed: <b>" + Passed + "</b></li>" 
            + "            <li>Failed: <b>" + Failed + "</b></li>" 
            + "            <li>Skipped/Blocked: <b>" + Skipped + "</b></li>" 
            + "            <li>Pass rate: <b>" + PassRate + "%</b></li>" 
            + "            <li>Execution window: <b>" + StartTime + " : " + EndTime + " IST</b></li>" 
            + "            <li>Trigger type: <b>" + TriggerType + "</b> | Branch: <b>" + Branch + "</b> | Commit: <b>" + ShortSHA + "</b></li>" 
            + "          </ul>"
            + "          <h4 style='color: #34495e; margin-top: 25px;'>Quick Links</h4>" 
            + "          <ul style='list-style-type: disc; margin-left: 25px;'>" 
            + executionReportLink 
            + "            <li>Logs / screenshots: <a href='" + LogsLink + "' style='color: #007bff;'>[OneDrive Link]</a></li>" 
            + "          </ul>"
            + failuresSection
            + "          <h4 style='color: #34495e; margin-top: 25px;'>Environment & Run Details</h4>" 
            + "          <ul style='list-style-type: disc; margin-left: 25px;'>" 
            + "            <li>Browser: <b>" + Browser + "</b></li>"
            + "            <li>Infrastructure: <b>" + Infrastructure + "</b> | OS: <b>" + OS + "</b></li>" 
            + "          </ul>"
            + "          <h4 style='color: #34495e; margin-top: 25px;'>Next Actions</h4>" 
            + "          <ul style='list-style-type: disc; margin-left: 25px;'>" 
            + "            <li>Owners to review failing scenarios and update defect status by <b>" + DueDate + "</b>.</li>" 
            + "            <li>Automation team will rerun impacted tests after fixes are deployed in <b>" + Env + "</b> Environment.</li>" 
            + "          </ul>"
            + "          <div style='text-align: center; margin-top: 30px; font-size: 14px; color: #555;'>Thanks,<br/><b>QA Automation Team</b></div>" 
            + "        </div>" 
            + "      </td>" 
            + "    </tr>" 
            + "  </table>" 
            + "</body>" 
            + "</html>";
    }
}