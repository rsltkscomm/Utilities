package org.utility;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class EmailSender {

    public static void sendEmail() {
        String host = System.getProperty("host");
        String port = System.getProperty("port");
        String senderEmail = System.getProperty("senderEmail");
        String senderPassword = System.getProperty("senderPassword");
        String recipientEmails = System.getProperty("recipientEmails");

        String htmlFilePath = System.getProperty("user.dir") + "\\TestExecutionSummary.html";
        String excelFilePath = System.getProperty("ResulExcelPath");
        String subject = System.getProperty("subject");
        String htmlContent = getMailHtml();

        Properties props = setSmtpProperties(host, port);
        Session session = createSession(props, senderEmail, senderPassword);
        session.setDebug(true);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, getRecipientList(recipientEmails));
            message.setSubject(subject);

            // Create a multipart email with HTML + attachment
            MimeMultipart multipart = new MimeMultipart("mixed");

            // HTML part as inline content
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html");
            multipart.addBodyPart(htmlPart);

              // or wherever your Excel file is
             attachFileToEmail(multipart, excelFilePath, "TestSummary.xlsx");

            // HTML report as attachment
            attachFileToEmail(multipart, htmlFilePath, "TestExecutionSummary.html");

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("✅ Email with embedded HTML and attachment sent successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send email.");
        }
    }

    private static Properties setSmtpProperties(String host, String port) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }

    private static Session createSession(Properties props, String senderEmail, String senderPassword) {
        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });
    }

    private static InternetAddress[] getRecipientList(String recipientEmails) {
        return Arrays.stream(recipientEmails.split(","))
                .map(String::trim)
                .map(email -> {
                    try {
                        return new InternetAddress(email);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(InternetAddress[]::new);
    }

    private static void attachFileToEmail(Multipart multipart, String filePath, String filename) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.attachFile(file);
                attachment.setFileName(filename);
                multipart.addBodyPart(attachment);
            } else {
                System.err.println("❌ File not found: " + filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                    margin: 0;
                    padding: 0;
                    background-color: #e9ecef;
                  }
                  .email-container {
                    background-color: rgba(0, 0, 0, 0);
                    padding: 30px;
                    margin: 30px auto;
                    border-radius: 12px;
                    box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
                    max-width: 70%;
                    border: 1px solid #ddd;
                  }
                  .header-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 10px 20px;
                    border-radius: 8px;
                  }
                  .header-title {
                    flex-grow: 1;
                    text-align: center;
                    font-size: 24px;
                    font-weight: bold;
                    color: #ffffff;
                  }
                  #companylogo, #productlogo {
                    height: 30px;
                    width: 160px;
                    background-color: white;
                    padding: 4px;
                    border-radius: 10px;
                  }
                  p {
                    font-size: 16px;
                    color: #ffffff;
                    line-height: 1.6;
                  }
                  .footer {
                    text-align: center;
                    margin-top: 30px;
                    font-size: 12px;
                    color: #ffffff;
                  }
                </style>
              </head>
              <body>
                <table width="100%" height="100%" cellspacing="0" cellpadding="0" border="0"
                  style="background-image: url('https://www.go.resul.io/media/f5hgjx30/banner1.jpg?anchor=center&mode=crop&width=1920&height=1080&rnd=132285603734830000'); background-size: cover; background-repeat: no-repeat;">
                  <tr>
                    <td>
                      <div class="email-container">
                        <div class="header-row">
                          <div class="header-title">Automation Test Suite Report</div>
                        </div>
                        <p>Hello,</p>
                        <p>The Selenium Automation Test Suite has completed successfully.</p>
                        <p>Please find the attached HTML report for detailed results.</p>
                        <div class="footer">Regards,<br />Automation Team</div>
                      </div>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """;
    }
}
