package com.ledger.config;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Sends OTP to EACH registering user's own email.
 * Only the SMTP SENDER is configured once in application.properties
 * (platform mail account). Users never touch the backend.
 */
@Service
public class MailService {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.from:}")
    private String from;

    private volatile String lastError = "";

    public String getLastError() {
        return lastError;
    }

    public boolean isConfigured() {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            lastError = "SMTP not set. Put Gmail + App Password in application.properties (one-time platform setup).";
            return false;
        }
        String u = username.trim().toLowerCase();
        String pw = password.trim();
        if (u.contains("your_email") || u.contains("your_gmail") || u.contains("example.com")
                || u.startsWith("paste")
                || pw.contains("YOUR_APP") || pw.contains("YOUR_16") || pw.contains("APP_PASSWORD")
                || pw.equalsIgnoreCase("password")) {
            lastError = "Still using placeholder SMTP values. Set real Gmail and 16-char App Password in application.properties.";
            return false;
        }
        return true;
    }

    /**
     * Send OTP/code TO the given user email (any student or admin).
     * @return true if SMTP accepted the message
     */
    public boolean sendOtp(String toEmail, String otpOrCode, String purpose) {
        lastError = "";
        System.out.println("[OTP] " + purpose + " for " + toEmail + " => " + otpOrCode);

        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            lastError = "Invalid recipient email";
            return false;
        }
        if (!isConfigured()) {
            System.err.println("[MAIL] Not configured: " + lastError);
            return false;
        }

        try {
            JavaMailSender sender = buildSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            String fromAddr = (from != null && !from.isBlank()
                    && !from.toLowerCase().contains("your_")) ? from : username;
            helper.setFrom(fromAddr);
            helper.setTo(toEmail.trim());
            helper.setSubject("Ledger LMS – " + purpose);
            helper.setText(
                    "Hello,\n\n"
                            + "Your Ledger LMS " + purpose + " is:\n\n"
                            + "    " + otpOrCode + "\n\n"
                            + "Valid for 10 minutes.\n"
                            + "If you did not request this, ignore this email.\n\n"
                            + "— Ledger LMS",
                    false
            );

            sender.send(message);
            System.out.println("[MAIL] Sent OK to " + toEmail);
            lastError = "";
            return true;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.toLowerCase().contains("auth") || msg.toLowerCase().contains("535")
                    || msg.toLowerCase().contains("username and password")) {
                lastError = "Authentication failed. Use Gmail App Password (not normal password). "
                        + "Enable 2-Step Verification → https://myaccount.google.com/apppasswords → "
                        + "paste 16-char code into spring.mail.password. Username must be the same Gmail.";
            } else {
                lastError = msg;
            }
            System.err.println("[MAIL] FAILED to " + toEmail + ": " + lastError);
            e.printStackTrace();
            return false;
        }
    }

    private JavaMailSender buildSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username.trim());
        // Gmail app passwords may be pasted with spaces — remove spaces
        sender.setPassword(password.replace(" ", "").trim());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        return sender;
    }
}
