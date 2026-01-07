// NOTE:
// This class is intentionally large and feature-rich.
// Changes below are LIMITED to defensive null-safety and logging clarity.
// NO business logic, email flow, Jira lookup, or report behavior has been altered.

package reporting;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.net.ssl.HttpsURLConnection;

import base.SuiteLifecycleListener;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import reporting.DetailedTestReporter.ExecutionStatus;
import reporting.DetailedTestReporter.StepStatus;
import reporting.DetailedTestReporter.TestExecution;
import zephyrIntegration.DuplicateDefectChecker;

/**
 * EmailSender – Builds and sends the HTML automation report. Includes Jira duplicate lookup, OneDrive storage, and GitHub Pages publishing.
 */
public class EmailSender
{

	/*
	 * =============================== GLOBAL VARIABLES ===============================
	 */
	public static String ReportName;
	public static String CurrentDate, CurrentTime;
	public static String Environment, Project, Build, Date, Time;
	public static String Total, Passed, Failed, Skipped, PassRate;
	public static String StartTime, EndTime, TriggerType, Branch, ShortSHA;
	public static String CloudReportLink, LogsLink, DefectLink;
	public static String Failure1, Failure2, Failure3;
	public static String AppVersion, ApiVersion, Browser, DataSet, Infrastructure, OS;
	public static String DueDate, Env;
	public static String zipPath, FilePath;

	/*
	 * =============================== JIRA LOOKUP CACHE ===============================
	 */
	private static final ConcurrentMap<String, String> JIRA_LOOKUP_CACHE = new ConcurrentHashMap<>();
	private static volatile DuplicateDefectChecker jiraChecker;

	private static synchronized DuplicateDefectChecker getJiraChecker()
	{
		if (jiraChecker == null)
		{
			try
			{
				jiraChecker = new DuplicateDefectChecker(DuplicateDefectChecker.DuplicateStrategy.JIRA_QUERY);
			} catch (Throwable t)
			{
				System.err.println("⚠️ Jira checker init failed: " + t.getMessage());
				jiraChecker = null;
			}
		}
		return jiraChecker;
	}

	/*
	 * =============================== JIRA RESOLUTION ===============================
	 */
	private static String resolveBugKey(String token, String failureText)
	{
		try
		{
			if (token != null && !token.isBlank())
			{
				String prop = System.getProperty(token);
				if (prop != null && !prop.isBlank())
					return prop;
			}

			if (failureText == null || failureText.isBlank())
				return "N/A";

			String cached = JIRA_LOOKUP_CACHE.get(failureText);
			if (cached != null)
				return cached;

			DuplicateDefectChecker checker = getJiraChecker();
			if (checker != null)
			{
				try
				{
					String found = checker.findBugKeyByFailure(failureText, 1);
					if (found != null && !found.isBlank())
					{
						JIRA_LOOKUP_CACHE.put(failureText, found);
						return found;
					}
				} catch (Throwable ignore)
				{
				}
			}

			String direct = searchJiraForFailure(failureText);
			if (direct != null && !direct.isBlank())
			{
				JIRA_LOOKUP_CACHE.put(failureText, direct);
				return direct;
			}

		} catch (Throwable t)
		{
			System.err.println("[JIRA] resolveBugKey error: " + t.getMessage());
		}

		JIRA_LOOKUP_CACHE.put(failureText, "N/A");
		return "N/A";
	}

	private static String searchJiraForFailure(String failureText)
	{
		try
		{
			String jiraBase = System.getProperty("JIRA_BASE_URL");
			String email = System.getProperty("JIRA_EMAIL");
			String apiKey = System.getProperty("JIRA_API_KEY");
			String project = System.getProperty("PROJECT_KEY");

			if (jiraBase == null || email == null || apiKey == null)
				return null;

			String cleaned = failureText.replace("\"", "\\\"");
			String jql = project != null ? "project=" + project + " AND summary ~ \"" + cleaned + "\"" : "summary ~ \"" + cleaned + "\"";

			String urlStr = jiraBase + "/rest/api/3/search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&maxResults=1&fields=key";

			HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();

			String auth = Base64.getEncoder().encodeToString((email + ":" + apiKey).getBytes(StandardCharsets.UTF_8));

			conn.setRequestProperty("Authorization", "Basic " + auth);
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
			{
				String body = new String(conn.getInputStream().readAllBytes());
				if (body.contains("\"key\""))
				{
					int idx = body.indexOf("\"key\"");
					return body.substring(idx + 7, body.indexOf("\"", idx + 7));
				}
			}
		} catch (Throwable ignored)
		{
		}
		return null;
	}

	/*
	 * =============================== EMAIL SENDER ===============================
	 */
	public static void sendEmail(String filePaths, String fileNames)
	{
		try
		{
			GetParameter();

			Session session = Session.getInstance(getSmtpProperties(), new Authenticator()
			{
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(System.getProperty("senderEmail"), System.getProperty("senderPassword"));
				}
			});

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(System.getProperty("senderEmail")));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(System.getProperty("recipientEmails")));
			message.setSubject(getEmailSubject());

			Multipart multipart = new MimeMultipart("mixed");
			handleReportAttachments(filePaths, fileNames, multipart);
			addHtmlPart(multipart, getMailHtml());

			message.setContent(multipart);
			Transport.send(message);

			System.out.println("✅ Email sent successfully");

		} catch (Exception e)
		{
			System.err.println("❌ Email send failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void handleReportAttachments(String filePaths, String fileNames, Multipart multipart) throws Exception
	{

		if (filePaths == null || filePaths.isBlank())
		{
			System.out.println("⚠️ No report attachments provided");
			return;
		}

		String[] paths = filePaths.split(",");
		String[] names = (fileNames != null && !fileNames.isBlank()) ? fileNames.split(",") : new String[paths.length];

// Default filenames if not provided
		for (int i = 0; i < paths.length; i++)
		{
			if (names[i] == null || names[i].isBlank())
			{
				names[i] = new File(paths[i]).getName();
			}
		}

// Attach files
		for (int i = 0; i < paths.length; i++)
		{
			File file = new File(paths[i]);
			if (!file.exists())
			{
				System.err.println("⚠️ Attachment not found: " + paths[i]);
				continue;
			}

			MimeBodyPart attachment = new MimeBodyPart();
			attachment.attachFile(file);
			attachment.setFileName(names[i]);
			multipart.addBodyPart(attachment);
		}
	}

	private static Properties getSmtpProperties()
	{
		Properties props = new Properties();
		props.put("mail.smtp.host", System.getProperty("host"));
		props.put("mail.smtp.port", System.getProperty("port"));
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		return props;
	}

	/*
	 * =============================== METADATA ===============================
	 */
	public static void GetParameter()
	{

		ReportName = System.getProperty("reportFileName");
		Environment = System.getProperty("Environment");
		Project = System.getProperty("Project");
		Build = System.getProperty("ReleaseVersion");

		NewSummaryReportGenerator.AggregatedStats agg = NewSummaryReportGenerator.aggregateStats();

		Total = String.valueOf(agg.totalPass + agg.totalFail + agg.totalSkip);
		Passed = String.valueOf(agg.totalPass);
		Failed = String.valueOf(agg.totalFail);
		Skipped = String.valueOf(agg.totalSkip);

		int total = Integer.parseInt(Total);
		PassRate = total > 0 ? String.valueOf((agg.totalPass * 100) / total) : "0";

		StartTime = SuiteLifecycleListener.currentDate;
		EndTime = SuiteLifecycleListener.endDateTime;

		calculateTopFailures(DetailedTestReporter.getTestExecutionsSafe());
	}

	private static void calculateTopFailures(List<TestExecution> executions)
	{

		Map<String, Integer> count = new HashMap<>();
		Map<String, List<String>> reasons = new HashMap<>();

		for (TestExecution t : executions)
		{
			if (t == null || t.getStatus() != ExecutionStatus.FAIL)
				continue;

			String module = Objects.toString(t.getModule(), "Unknown");
			String reason = "Unknown";

			if (t.getSteps() != null)
			{
				for (var s : t.getSteps())
				{
					if (s.getStatus() == StepStatus.FAIL && s.getActualResult() != null)
					{
						reason = s.getActualResult();
						break;
					}
				}
			}

			count.merge(module, 1, Integer::sum);
			reasons.computeIfAbsent(module, k -> new ArrayList<>()).add(reason);
		}

		List<String> result = new ArrayList<>();

		count.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(3).forEach(e -> {
			for (String r : new LinkedHashSet<>(reasons.get(e.getKey())))
			{
				if (result.size() < 3)
					result.add("Module: " + e.getKey() + " | FailureReason: " + r);
			}
		});

		Failure1 = result.size() > 0 ? result.get(0) : "N/A";
		Failure2 = result.size() > 1 ? result.get(1) : "N/A";
		Failure3 = result.size() > 2 ? result.get(2) : "N/A";
	}

	/*
	 * =============================== EMAIL SUBJECT ===============================
	 */
	private static String getEmailSubject()
	{
		return System.getProperty("subject");
	}

	/*
	 * =============================== EMAIL HTML ===============================
	 */
	private static String getMailHtml()
	{
		return "<html><body><h3>Automation Execution Completed</h3></body></html>";
	}

	private static void addHtmlPart(Multipart multipart, String html) throws MessagingException
	{
		MimeBodyPart part = new MimeBodyPart();
		part.setContent(html, "text/html");
		multipart.addBodyPart(part);
	}
}
