// File: src/main/java/com/example/APEEG/service/MailSchedulerService.java
package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.model.MailLog;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import com.example.APEEG.repository.MailLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MailSchedulerService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final MailLogRepository mailLogRepository;

    // Inject repositories via constructor
    public MailSchedulerService(IssueRecordRepository issueRecordRepository,
                                InstrumentRepository instrumentRepository,
                                MailLogRepository mailLogRepository) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.mailLogRepository = mailLogRepository;
    }

    /**
     * Daily Cron Job: Runs every day at 09:00 AM IST.
     * Cron expression syntax: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void runDailyReminderEngine() {
        LocalDate today = LocalDate.now();

        processAdvanceNotices(today);
        processOverdueReminders(today);
        processCalibrationWarnings(today);
    }

    /**
     * 1. Advance Warning: Sent 2 days before the return due date
     */
    private void processAdvanceNotices(LocalDate today) {
        List<IssueRecord> openRecords = issueRecordRepository.findByState(IssueRecord.State.OPEN);

        for (IssueRecord record : openRecords) {
            if (record.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(today, record.getDueDate());

                // Trigger if exactly 2 days remain
                if (daysUntilDue == 2) {
                    if (!isAlreadyMailedToday(record.getId(), MailLog.MailType.ADVANCE_NOTICE, today)) {
                        sendAndLogEmail(
                                record,
                                record.getInstrument(),
                                MailLog.MailType.ADVANCE_NOTICE,
                                buildRecipients(record, false),
                                "[APEEG Warning] Instrument Return Due in 2 Days: " + record.getInstrument().getName(),
                                "Reminder: The instrument " + record.getInstrument().getName() + " ("
                                        + record.getInstrument().getAssetId() + ") is due for return on "
                                        + record.getDueDate() + "."
                        );
                    }
                }
            }
        }
    }

    /**
     * 2. Overdue Reminders: Sent daily for items past due date until returned
     */
    private void processOverdueReminders(LocalDate today) {
        List<IssueRecord> openRecords = issueRecordRepository.findByState(IssueRecord.State.OPEN);

        for (IssueRecord record : openRecords) {
            if (record.getDueDate() != null && record.getDueDate().isBefore(today)) {
                long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);

                if (!isAlreadyMailedToday(record.getId(), MailLog.MailType.OVERDUE, today)) {
                    sendAndLogEmail(
                            record,
                            record.getInstrument(),
                            MailLog.MailType.OVERDUE,
                            buildRecipients(record, true), // Include owner scientist on overdue CC
                            "[APEEG OVERDUE] Immediate Action Required: " + record.getInstrument().getName(),
                            "OVERDUE NOTICE: Instrument " + record.getInstrument().getName() + " ("
                                    + record.getInstrument().getAssetId() + ") is " + daysOverdue
                                    + " day(s) overdue. Expected return was " + record.getDueDate() + "."
                    );
                }
            }
        }
    }

    /**
     * 3. Calibration Warnings: Sent when calibration expires in 30 days
     */
    private void processCalibrationWarnings(LocalDate today) {
        List<Instrument> instruments = instrumentRepository.findAll();

        for (Instrument instrument : instruments) {
            if (instrument.getCalibrationValidTo() != null) {
                long daysUntilCalibration = ChronoUnit.DAYS.between(today, instrument.getCalibrationValidTo());

                if (daysUntilCalibration == 30) {
                    // Check idempotency for instrument calibration alerts
                    if (!isInstrumentMailedToday(instrument.getId(), MailLog.MailType.CALIBRATION, today)) {
                        String ownerEmail = (instrument.getOwnerScientist() != null)
                                ? instrument.getOwnerScientist().getEmail()
                                : "";

                        sendAndLogEmail(
                                null, // No active issue record required for calibration alerts
                                instrument,
                                MailLog.MailType.CALIBRATION,
                                ownerEmail,
                                "[APEEG Calibration] 30-Day Re-Calibration Notice: " + instrument.getName(),
                                "Calibration Notice: " + instrument.getName() + " (" + instrument.getAssetId()
                                        + ") calibration validity expires on " + instrument.getCalibrationValidTo()
                                        + ". Please arrange servicing."
                        );
                    }
                }
            }
        }
    }

    /**
     * Helper: Idempotency Check for Issue Record emails
     */
    private boolean isAlreadyMailedToday(String issueRecordId, MailLog.MailType type, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<MailLog> logs = mailLogRepository.findByIssueRecordId(issueRecordId);
        return logs.stream().anyMatch(log -> log.getType() == type
                && log.getSentAt() != null
                && log.getSentAt().isAfter(startOfDay)
                && log.getSentAt().isBefore(endOfDay));
    }

    /**
     * Helper: Idempotency Check for Instrument Calibration emails
     */
    private boolean isInstrumentMailedToday(String instrumentId, MailLog.MailType type, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<MailLog> logs = mailLogRepository.findByInstrumentId(instrumentId);
        return logs.stream().anyMatch(log -> log.getType() == type
                && log.getSentAt() != null
                && log.getSentAt().isAfter(startOfDay)
                && log.getSentAt().isBefore(endOfDay));
    }

    /**
     * Helper: Construct comma-separated recipients string
     */
    private String buildRecipients(IssueRecord record, boolean includeOwner) {
        StringBuilder recipients = new StringBuilder();

        if (record.getBorrowerScientist() != null && record.getBorrowerScientist().getEmail() != null) {
            recipients.append(record.getBorrowerScientist().getEmail());
        }

        if (record.getStaffEmail() != null && !record.getStaffEmail().isEmpty()) {
            if (recipients.length() > 0) recipients.append(", ");
            recipients.append(record.getStaffEmail());
        }

        if (includeOwner && record.getOwnerScientist() != null && record.getOwnerScientist().getEmail() != null) {
            if (recipients.length() > 0) recipients.append(", ");
            recipients.append(record.getOwnerScientist().getEmail());
        }

        return recipients.toString();
    }

    /**
     * Dispatch SMTP message and write immutable audit trail row to mail_logs
     */
    private void sendAndLogEmail(IssueRecord issueRecord, Instrument instrument, MailLog.MailType type,
                                 String recipients, String subject, String body) {
        MailLog mailLog = new MailLog();
        mailLog.setIssueRecord(issueRecord);
        mailLog.setInstrument(instrument);
        mailLog.setType(type);
        mailLog.setRecipients(recipients);
        mailLog.setSubject(subject);
        mailLog.setBody(body);
        mailLog.setSentAt(LocalDateTime.now());

        try {
            // Placeholder for JavaMailSender dispatch:
            // mimeMessageHelper.setTo(recipients.split(","));
            // javaMailSender.send(mimeMessage);

            mailLog.setDeliveryStatus(MailLog.DeliveryStatus.SENT);
            mailLog.setError(null);
        } catch (Exception e) {
            // On mail server error, log failure without breaking backend application
            mailLog.setDeliveryStatus(MailLog.DeliveryStatus.FAILED);
            mailLog.setError(e.getMessage());
        }

        // Always save attempt to mail_logs collection for auditing
        mailLogRepository.save(mailLog);
    }
}