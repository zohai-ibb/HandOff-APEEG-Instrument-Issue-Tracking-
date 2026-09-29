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

/**
 * Centralized Mail Management Service.
 * - Handles daily automated background jobs running at 09:00 AM IST.
 * - Provides transactional mail execution methods for real-time instrument issuing and returning.
 */
@Service
public class MailSchedulerService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final MailLogRepository mailLogRepository;
    private final EmailService emailService;

    public MailSchedulerService(IssueRecordRepository issueRecordRepository,
                                InstrumentRepository instrumentRepository,
                                MailLogRepository mailLogRepository,
                                EmailService emailService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.mailLogRepository = mailLogRepository;
        this.emailService = emailService;
    }

    /**
     * Daily Cron Task: Runs automatically every day at 09:00 AM IST.
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void runDailyReminderEngine() {
        LocalDate today = LocalDate.now();

        processAdvanceNotices(today);
        processOverdueReminders(today);
        processCalibrationWarnings(today);
    }

    /**
     * Instant Issue Confirmation Mail Service:
     * Called directly when an instrument is issued. Dispatches an email to ALL THREE stakeholders:
     * 1. Borrower Scientist
     * 2. Intermediary Staff
     * 3. Owner Scientist
     */
    public void sendIssueConfirmation(IssueRecord record) {
        if (record == null || record.getInstrument() == null) {
            return;
        }

        String recipients = buildRecipients(record, true); // Include Borrower, Staff, and Owner
        String subject = "[APEEG] Instrument Issued: " + record.getInstrument().getName() + " (" + record.getInstrument().getAssetId() + ")";
        String body = "The instrument " + record.getInstrument().getName() + " (" + record.getInstrument().getAssetId() + ") has been successfully issued."
                + "\n\nBorrower Scientist: " + (record.getBorrowerScientist() != null ? record.getBorrowerScientist().getName() : "N/A")
                + "\nOwning Scientist: " + (record.getOwnerScientist() != null ? record.getOwnerScientist().getName() : "N/A")
                + "\nIntermediary Staff: " + record.getStaffName() + " (" + record.getStaffEmail() + ")"
                + "\nIssue Date: " + record.getIssueDate()
                + "\nReturn Due Date: " + record.getDueDate()
                + "\nPurpose: " + (record.getPurpose() != null ? record.getPurpose() : "N/A")
                + "\nCondition at Checkout: " + (record.getConditionOut() != null ? record.getConditionOut() : "N/A");

        emailService.sendAndLogEmail(
                record,
                record.getInstrument(),
                MailLog.MailType.ISSUE,
                recipients,
                subject,
                body
        );
    }

    /**
     * Instant Return Confirmation Mail Service:
     * Called directly when an instrument is returned. Dispatches closure confirmation email to ALL THREE stakeholders:
     * 1. Borrower Scientist
     * 2. Intermediary Staff
     * 3. Owner Scientist
     */
    public void sendReturnConfirmation(IssueRecord record) {
        if (record == null) {
            return;
        }

        Instrument instrument = record.getInstrument();
        String recipients = buildRecipients(record, true); // Include Borrower, Staff, and Owner
        String subject = "[APEEG] Instrument Returned: " + (instrument != null ? instrument.getName() : "Equipment");
        String body = "The instrument loan transaction has been closed."
                + "\n\nReturned On: " + record.getActualReturnDate()
                + "\nCondition At Return: " + record.getConditionIn()
                + "\nHandled By Staff: " + record.getStaffName();

        emailService.sendAndLogEmail(
                record,
                instrument,
                MailLog.MailType.RETURN,
                recipients,
                subject,
                body
        );
    }

    /**
     * Advance Return Warnings: Dispatched 2 days prior to expected return date.
     */
    private void processAdvanceNotices(LocalDate today) {
        List<IssueRecord> openRecords = issueRecordRepository.findByState(IssueRecord.State.OPEN);

        for (IssueRecord record : openRecords) {
            if (record.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(today, record.getDueDate());

                if (daysUntilDue == 2) {
                    if (!isAlreadyMailedToday(record.getId(), MailLog.MailType.ADVANCE_NOTICE, today)) {
                        String recipients = buildRecipients(record, false);
                        String subject = "[APEEG Warning] Return Due in 2 Days: " + record.getInstrument().getName();
                        String body = "Reminder: Instrument " + record.getInstrument().getName()
                                + " (" + record.getInstrument().getAssetId() + ") is scheduled for return on "
                                + record.getDueDate() + ".\nHandled by Staff: " + record.getStaffName();

                        emailService.sendAndLogEmail(
                                record,
                                record.getInstrument(),
                                MailLog.MailType.ADVANCE_NOTICE,
                                recipients,
                                subject,
                                body
                        );
                    }
                }
            }
        }
    }

    /**
     * Daily Overdue Chasing: Dispatched every single day after due date passes until returned.
     */
    private void processOverdueReminders(LocalDate today) {
        List<IssueRecord> openRecords = issueRecordRepository.findByState(IssueRecord.State.OPEN);

        for (IssueRecord record : openRecords) {
            if (record.getDueDate() != null && record.getDueDate().isBefore(today)) {
                long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);

                if (!isAlreadyMailedToday(record.getId(), MailLog.MailType.OVERDUE, today)) {
                    String recipients = buildRecipients(record, true); // Copies Owner Scientist on overdue notices
                    String subject = "[APEEG OVERDUE] Immediate Action Required: " + record.getInstrument().getName();
                    String body = "OVERDUE NOTICE: The instrument " + record.getInstrument().getName()
                            + " (" + record.getInstrument().getAssetId() + ") is " + daysOverdue
                            + " day(s) overdue.\nExpected Return Date: " + record.getDueDate()
                            + "\nBorrowing Scientist: " + (record.getBorrowerScientist() != null ? record.getBorrowerScientist().getName() : "N/A")
                            + "\nIntermediary Staff: " + record.getStaffName() + " (" + record.getStaffEmail() + ")"
                            + "\n\nPlease return the item immediately to the lab.";

                    emailService.sendAndLogEmail(
                            record,
                            record.getInstrument(),
                            MailLog.MailType.OVERDUE,
                            recipients,
                            subject,
                            body
                    );
                }
            }
        }
    }

    /**
     * Re-Calibration Warnings: Dispatched when instrument calibration expires in 30 days.
     */
    private void processCalibrationWarnings(LocalDate today) {
        List<Instrument> instruments = instrumentRepository.findAll();

        for (Instrument instrument : instruments) {
            if (instrument.getCalibrationValidTo() != null) {
                long daysUntilCalibration = ChronoUnit.DAYS.between(today, instrument.getCalibrationValidTo());

                if (daysUntilCalibration == 30) {
                    if (!isInstrumentMailedToday(instrument.getId(), MailLog.MailType.CALIBRATION, today)) {
                        String ownerEmail = (instrument.getOwnerScientist() != null)
                                ? instrument.getOwnerScientist().getEmail()
                                : "";

                        String subject = "[APEEG Calibration] 30-Day Notice: " + instrument.getName();
                        String body = "Calibration Alert: Hardware asset " + instrument.getName()
                                + " (" + instrument.getAssetId() + ") calibration expires on "
                                + instrument.getCalibrationValidTo() + ". Please schedule servicing.";

                        emailService.sendAndLogEmail(
                                null, // No issue record associated with calibration alerts
                                instrument,
                                MailLog.MailType.CALIBRATION,
                                ownerEmail,
                                subject,
                                body
                        );
                    }
                }
            }
        }
    }

    /**
     * Idempotency Check: Verifies if an email of the given type was already logged today for an issue record.
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
     * Idempotency Check: Verifies if a calibration warning was already logged today for an instrument.
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
     * Helper: Constructs a comma-separated list of target recipient emails:
     * 1. Borrower Scientist Email
     * 2. Intermediary Staff Email
     * 3. Owner Scientist Email (if includeOwner is true)
     */
    private String buildRecipients(IssueRecord record, boolean includeOwner) {
        StringBuilder recipients = new StringBuilder();

        // 1. Borrower Scientist Email
        if (record.getBorrowerScientist() != null && record.getBorrowerScientist().getEmail() != null) {
            recipients.append(record.getBorrowerScientist().getEmail());
        }

        // 2. Intermediary Staff Email
        if (record.getStaffEmail() != null && !record.getStaffEmail().trim().isEmpty()) {
            if (recipients.length() > 0) recipients.append(", ");
            recipients.append(record.getStaffEmail());
        }

        // 3. Owner Scientist Email
        if (includeOwner && record.getOwnerScientist() != null && record.getOwnerScientist().getEmail() != null) {
            if (recipients.length() > 0) recipients.append(", ");
            recipients.append(record.getOwnerScientist().getEmail());
        }

        return recipients.toString();
    }
}