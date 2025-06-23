package org.utility;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailSender {

    public static void sendEmail(String filePaths, String fileNames) {
        String host = System.getProperty("host");
        String port = System.getProperty("port");
        String senderEmail = System.getProperty("senderEmail");
        String senderPassword = System.getProperty("senderPassword");
        String recipientEmails = System.getProperty("recipientEmails");
        String subject = System.getProperty("subject");

        Properties props = getSmtpProperties(host, port);
        Session session = createSession(props, senderEmail, senderPassword);
        session.setDebug(false);

        try {
            Message message = prepareMessage(session, senderEmail, recipientEmails, subject);
            Multipart multipart = new MimeMultipart("mixed");

            String[] filePathArray = filePaths.split(",");
            String[] fileNameArray = fileNames.split(",");

            // Embed first file (HTML Report) as body content if it exists
            String htmlReportContent = readFileContent(filePathArray[0]);
            addHtmlPart(multipart, htmlReportContent);

            // Attach all provided files
            for (int i = 0; i < filePathArray.length; i++) {
                attachFile(multipart, filePathArray[i], fileNameArray[i]);
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Email sent successfully.");
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
        htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
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

    private static String readFileContent(String path) {
        try {
            return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("❌ Failed to read HTML file: " + e.getMessage());
            return "<p>Unable to load report content.</p>";
        }
    }
}
