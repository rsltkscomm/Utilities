package reporting;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailSender {

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

            // Add HTML content
            addHtmlPart(multipart, getMailHtml());

            // Attach files
            String[] filePath = filePaths.split(",");
            String[] fileName = fileNames.split(",");
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
                .header-title {
                  text-align: center;
                  font-size: 24px;
                  font-weight: bold;
                  color: #ffffff;
                  margin-bottom: 20px;
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
              <table width="100%" cellspacing="0" cellpadding="0" border="0"
                style="background-image: url('https://www.go.resul.io/media/f5hgjx30/banner1.jpg?anchor=center&mode=crop&width=1920&height=1080&rnd=132285603734830000'); background-size: cover; background-repeat: no-repeat;">
                <tr>
                  <td>
                    <div class="email-container">
                      <div class="header-title">Automation Test Suite Report</div>
                      <p>Hello,</p>
                      <p>The Selenium Automation Test Suite has completed successfully.</p>
                      <p>Please find the attached HTML report for detailed results.</p>
                      <div class="footer">Regards,<br/>Automation Team</div>
                    </div>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """;
    }
}