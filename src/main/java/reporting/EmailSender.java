package reporting;

import base.BaseTest;
import constants.FrameworkConstants;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.TestExecution;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
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

        if (ReportName.contains("Daily")) {
            zipPath = zipHtmlWithTimestamp(paths[0], FrameworkConstants.ONEDRIVE_BASE_PATH + "\\DailyCheckListResults\\");
            FilePath = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/qaautomation_resulticks_com/Ev1Aog7jO5RAt_0Wrx5wJPkBsy0sQ47Tr2hTAfm0kiHUaw";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eq7fuRascUlEk9jufCwOBeYByg5PbIo-dOjEf3mfTbKBJg?e=4e7gMT";
        } else if (ReportName.contains("Deploy")) {
            zipPath = zipHtmlWithTimestamp(paths[0], FrameworkConstants.ONEDRIVE_BASE_PATH + "\\DeploymentCheckListResults\\");
            FilePath = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/qaautomation_resulticks_com/ElTgyT1WS9lDvvhRMHlnL4ABWvmHGIYvYUu4QR0GkDQTmw?e=fkj5xP";
            LogsLink = "https://azureresulticks-my.sharepoint.com/:f:/g/personal/a_maheshanand_resulticks_com/Eq7fuRascUlEk9jufCwOBeYByg5PbIo-dOjEf3mfTbKBJg?e=4e7gMT";
        }

        for (int i = 0; i < paths.length; i++) {
            attachFile(multipart, paths[i], names[i]);
        }

        System.out.println("📦 Final ZIP stored at: " + zipPath);
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

    // ──────────────────────────────
    // 🔹 ZIP CREATOR
    // ──────────────────────────────
    public static String zipHtmlWithTimestamp(String sourceFile, String oneDriveFolder) {
        ZipSecureFile.setMinInflateRatio(0.001);
        String timeStamp = new SimpleDateFormat("ddMMMyyyy_HHmmss").format(new Date());
        String zipFileName = "DailyCheckList_" + timeStamp + ".zip";
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
        String[] parts = NewCutsomHTMLReport.dateTime.split(" ");
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

        StartTime = BaseTest.currentDate;
        EndTime = BaseTest.EndDateTime;
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

    private static String execCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
    }

    private static void calculateTopFailures(List<TestExecution> executions) {
        Map<String, Integer> failureCounts = new LinkedHashMap<>();
        Map<String, List<String>> moduleFailTests = new LinkedHashMap<>();

        for (TestExecution t : executions) {
            if (t.getStatus() == ExecutionStatus.FAIL) {
                String module = t.getModule();
                String testName = t.getShortDescription();

                failureCounts.put(module, failureCounts.getOrDefault(module, 0) + 1);
                moduleFailTests.computeIfAbsent(module, k -> new ArrayList<>()).add(testName);
            }
        }

        failureCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .forEach(entry -> {
                    String module = entry.getKey();
                    moduleFailTests.get(module).stream()
                            .limit(3 - topFails.size())
                            .forEach(test -> topFails.add("Module: " + module + " | Test: " + test));
                });

        Failure1 = topFails.size() > 0 ? topFails.get(0) : "N/A";
        Failure2 = topFails.size() > 1 ? topFails.get(1) : "N/A";
        Failure3 = topFails.size() > 2 ? topFails.get(2) : "N/A";
    }

    // ──────────────────────────────
    // 🔹 EMAIL SUBJECT BUILDER
    // ──────────────────────────────
    private static String getEmailSubject() {
        boolean isPageLoad = "yes".equalsIgnoreCase(System.getProperty("IsPageLoadReport"));
        return isPageLoad ? System.getProperty("pageloadsubject") : System.getProperty("subject");
    }

    // ──────────────────────────────
    // 🔹 EMAIL HTML TEMPLATE
    // ──────────────────────────────
    private static String getMailHtml() {
        List<String> validFails = Arrays.asList(Failure1, Failure2, Failure3)
                .stream().filter(f -> f != null && !"N/A".equals(f) && !f.isEmpty())
                .toList();

        String failuresSection = validFails.isEmpty() ? "" :
                "<h4 style='color:#34495e;margin-top:25px;'>Failures (Top Items)</h4><ol style='margin-left:25px;'>"
                        + validFails.stream().map(f -> "<li>" + f + "</li>").collect(Collectors.joining())
                        + "</ol>";

        String reportName = ReportName.toLowerCase().contains("daily") ? "Daily Checklist" :
                            ReportName.toLowerCase().contains("postproduction") ? "Post Production Checklist" :
                            "Regression";

        // Main email HTML
	    return "<!DOCTYPE html>" +
	            "<html>" +
	            "<body style='font-family: Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 0;'>" +
	            "  <table width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color: #f7f7f7; padding: 20px;'>" +
	            "    <tr>" +
	            "      <td align='center'>" +
	            "        <div style='background-color: #ffffff; max-width: 700px; border-radius: 10px; padding: 30px; " +
	            "box-shadow: 0 0 10px rgba(0,0,0,0.1); text-align: left;'>" +

	            "          <div style='font-size: 20px; font-weight: bold; color: #2c3e50; margin-bottom: 20px; " +
	            "text-align: center;'>"+reportName+" Automation Report</div>" +

	            "          <p>Hi All,</p>" +
	            "          <p>"+reportName+" has been successfully completed on <b>" + Environment + "</b> " +
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
	            "            <li>Execution report: <a href='" + FilePath + "' style='color: #007bff;'>[OneDrive Report Link]</a></li>" +
	            "            <li>Logs / screenshots (if any): <a href='" + LogsLink + "' style='color: #007bff;'>[OneDrive Evidence Link]</a></li>" +
	            "          </ul>" +

	            failuresSection.toString() + // Only included if failures exist

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
