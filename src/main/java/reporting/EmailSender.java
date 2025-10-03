package reporting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.openxml4j.util.ZipSecureFile;

import constants.FrameworkConstants;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailSender {
	public static String zipPath;
	public static String FilePath;
	public static String ReportName = System.getProperty("reportFileName");
    public static void sendEmail(String filePaths,String fileNames) {
        String host = System.getProperty("host");
        String port = System.getProperty("port");
        String senderEmail = System.getProperty("senderEmail");
        String senderPassword = System.getProperty("senderPassword");
        String recipientEmails = System.getProperty("recipientEmails");
        String subject = System.getProperty("IsPageLoadReport").toLowerCase().equals("yes") ? System.getProperty("pageloadsubject"):System.getProperty("subject");

        Properties props = getSmtpProperties(host, port);
        Session session = createSession(props, senderEmail, senderPassword);
        session.setDebug(false);

        try {
            Message message = prepareMessage(session, senderEmail, recipientEmails, subject);
            Multipart multipart = new MimeMultipart("mixed");

         // Attach files
            String[] filePath = filePaths.split(",");
            String[] fileName = fileNames.split(",");
            String Onedrivepath=FrameworkConstants.ONEDRIVE_BASE_PATH+"\\DailyCheckListResults\\";
             zipPath = zipHtmlWithTimestamp(filePath[0], Onedrivepath);
           // FilePath="https://azureresulticks-my.sharepoint.com/personal/a_maheshanand_resulticks_com/_layouts/15/onedrive.aspx?id=%2Fpersonal%2Fa%5Fmaheshanand%5Fresulticks%5Fcom%2FDocuments%2FQA%20Reports%2FDailyCheckListReports&ga=1";
            FilePath="https://azureresulticks-my.sharepoint.com/:f:/g/personal/qaautomation_resulticks_com/Ev1Aog7jO5RAt_0Wrx5wJPkBsy0sQ47Tr2hTAfm0kiHUaw";

            
            if (ReportName.contains("Daily"))
			{
            	ReportName="Daily Checklist";
			}else if (ReportName.contains("Deploy")) {
            	ReportName="Deployment Checklist";

			}
            // Add HTML content
            addHtmlPart(multipart, getMailHtml());

            
            System.out.println(" Final ZIP stored at: " + zipPath);
            for (int i = 0; i < filePath.length; i++)
			{
            	 attachFile(multipart, filePath[i], fileName[i]);
			}
            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Email sent successfully to: " + recipientEmails);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties getSmtpProperties(String host, String port) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }

    private static Session createSession(Properties props, String email, String password) {
        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    private static Message prepareMessage(Session session, String from, String toList, String subject) throws Exception {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, parseRecipients(toList));
        message.setSubject(subject);
        return message;
    }

    private static InternetAddress[] parseRecipients(String emailList) {
        return Arrays.stream(emailList.split(","))
                .map(String::trim)
                .map(email -> {
                    try {
                        return new InternetAddress(email);
                    } catch (AddressException e) {
                        System.err.println("❌ Invalid email address: " + email);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(InternetAddress[]::new);
    }

    private static void addHtmlPart(Multipart multipart, String htmlContent) throws Exception {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html");
        multipart.addBodyPart(htmlPart);
    }

    private static void attachFile(Multipart multipart, String filePath, String fileName) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("❌ Attachment not found: " + filePath);
                return;
            }

            MimeBodyPart attachment = new MimeBodyPart();
            attachment.attachFile(file);
            attachment.setFileName(fileName);
            multipart.addBodyPart(attachment);

        } catch (Exception e) {
            System.err.println("❌ Failed to attach file: " + filePath + " - " + e.getMessage());
        }
    }

    private static String getMailHtml() {
        return """
               <!DOCTYPE html>
    <html>
    <head>
      <style>
        body {
          font-family: Arial, sans-serif;
        }
        .email-container {
          padding: 20px;
          margin: 0 auto;
          max-width: 700px;
        }
        .header-title {
          text-align: center;
          font-size: 20px;
          font-weight: bold;
          margin-bottom: 20px;
        }
        p {
          font-size: 14px;
          line-height: 1.6;
        }
       .footer {
  text-align: center;
  margin-top: 50px; /* increase spacing from content */
  font-size: 18px;
}
        a {
          font-weight: bold;
          text-decoration: underline;
        }
      </style>
    </head>
    <body>
      <table width="100%%" cellspacing="0" cellpadding="0" border="0">
        <tr>
          <td>
            <div class="email-container">
    """ +
              "<div class=\"header-title\">" + ReportName + " Report</div>\n" +
              "<p>Hello Team,</p>\n" +
              "<p>The " + ReportName + " Test Suite execution has been completed successfully.</p>\n" +
              "<p>\n" +
              "  Please find the attached HTML report for detailed results,<br/>\n" +
              "  You can also access the report directly via the following link:<br/><br/>\n" +
              "  <a href='" + FilePath + "'>[Execution Results : OneDrive Link]</a>\n" +
              "</p>\n" +
    """          
              <div class="footer">Regards,<br/>Automation Team</div>
            </div>
          </td>
        </tr>
      </table>
    </body>
    </html>
            """;
    }

    
    public static String zipHtmlWithTimestamp(String sourceFile, String oneDriveFolder) {
    	 // ⚡ Fix for Zip bomb detection
        ZipSecureFile.setMinInflateRatio(0.001);  // allow smaller compression ratios

        String timeStamp = new SimpleDateFormat("ddMMMyyyy_HHmmss").format(new Date());
        String zipFileName = "DailyCheckList_" + timeStamp + ".zip";
        String destZipFile = oneDriveFolder + File.separator + zipFileName;
        
        try (FileOutputStream fos = new FileOutputStream(destZipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(new File(sourceFile))) {

            ZipEntry zipEntry = new ZipEntry(new File(sourceFile).getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.closeEntry();

            return destZipFile; // ✅ Return the saved path

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}