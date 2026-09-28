package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.model.MailLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

/**
 * Service responsible for creating MIME email messages, sending them via SMTP,
 * and saving delivery outcomes to the mail_logs MongoDB collection for auditing.
 */
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final MailLogService mailLogService;

    @Value("${apeeg.mail.from-address:apeeg-noreply@cbri.res.in}")
    private String fromAddress;

    public EmailService(JavaMailSender javaMailSender, MailLogService mailLogService) {
        this.javaMailSender = javaMailSender;
        this.mailLogService = mailLogService;
    }

    /**
     * Sends an email via SMTP and records an immutable log entry in MongoDB.
     *
     * @param issueRecord Associated issue record (null for calibration alerts)
     * @param instrument  Associated hardware instrument
     * @param type        Category of mail (ISSUE, OVERDUE, ADVANCE_NOTICE, CALIBRATION)
     * @param recipients  Comma-separated target email addresses
     * @param subject     Email subject
     * @param body        Plain text body
     */
    public void sendAndLogEmail(IssueRecord issueRecord,
                                Instrument instrument,
                                MailLog.MailType type,
                                String recipients,
                                String subject,
                                String body) {

        // 1. Prepare Audit Log Entry
        MailLog log = new MailLog();
        log.setIssueRecord(issueRecord);
        log.setInstrument(instrument);
        log.setType(type);
        log.setRecipients(recipients);
        log.setSubject(subject);
        log.setBody(body);
        log.setSentAt(LocalDateTime.now());

        // 2. Validate Recipients
        if (recipients == null || recipients.trim().isEmpty()) {
            log.setDeliveryStatus(MailLog.DeliveryStatus.FAILED);
            log.setError("Recipient address list is empty.");
            mailLogService.recordMailAttempt(log);
            return;
        }

        // 3. Attempt Delivery via SMTP
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(recipients.split(",\\s*"));
            helper.setSubject(subject);
            helper.setText(body, false);

            javaMailSender.send(message);

            // Stamp Delivery Success
            log.setDeliveryStatus(MailLog.DeliveryStatus.SENT);
            log.setError(null);

        } catch (Exception ex) {
            // Stamp Delivery Failure (Prevents rolling back Spring database transactions)
            log.setDeliveryStatus(MailLog.DeliveryStatus.FAILED);
            log.setError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }

        // 4. Save attempt row into mail_logs collection
        mailLogService.recordMailAttempt(log);
    }
}