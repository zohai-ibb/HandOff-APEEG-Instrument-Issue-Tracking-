package com.example.APEEG.service;

import com.example.APEEG.model.ExternalIssueRecord;
import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.MailLog;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.ExternalIssueRecordRepository;
import com.example.APEEG.repository.InstrumentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing equipment checkout transactions for non-registered external scientists.
 * Verifies owner authentication, mutates instrument availability, and dispatches email notifications.
 */
@Service
public class ExternalIssueService {

    private final ExternalIssueRecordRepository externalIssueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final EmailService emailService;

    public ExternalIssueService(ExternalIssueRecordRepository externalIssueRecordRepository,
                                InstrumentRepository instrumentRepository,
                                EmailService emailService) {
        this.externalIssueRecordRepository = externalIssueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.emailService = emailService;
    }

    public List<ExternalIssueRecord> getAllRecords() {
        return externalIssueRecordRepository.findAll();
    }

    public Optional<ExternalIssueRecord> getById(String id) {
        return externalIssueRecordRepository.findById(id);
    }

    public List<ExternalIssueRecord> getByOwnerScientistId(String ownerId) {
        return externalIssueRecordRepository.findByOwnerScientistId(ownerId);
    }

    /**
     * Issue an instrument to an external non-app scientist.
     * Authenticates owner identity, checks availability, locks instrument status,
     * and sends emails to Owner, External Scientist, and Staff[cite: 3, 4].
     */
    @Transactional
    public ExternalIssueRecord createExternalIssue(ExternalIssueRecord record) {
        // 1. Input payload validations
        if (record.getInstrument() == null || record.getInstrument().getId() == null) {
            throw new IllegalArgumentException("Associated instrument ID must be provided.");
        }
        if (record.getExternalBorrowerName() == null || record.getExternalBorrowerName().trim().isEmpty()) {
            throw new IllegalArgumentException("External borrower scientist name is required.");
        }
        if (record.getExternalBorrowerEmail() == null || record.getExternalBorrowerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("External borrower scientist email is required.");
        }

        // 2. Fetch instrument entity from database
        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + record.getInstrument().getId()));

        // 3. Confirm availability
        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        // 4. Authenticate logged-in owner scientist
        Person authenticatedOwner = getAuthenticatedScientist();
        if (authenticatedOwner == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }

        // 5. Verify that the authenticated scientist owns this instrument
        if (instrument.getOwnerScientist() != null
                && !instrument.getOwnerScientist().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: Only the owner scientist can issue this instrument to an external borrower.");
        }

        // 6. Set relationship and date attributes
        record.setOwnerScientist(authenticatedOwner);
        record.setInstrument(instrument);
        record.setState(ExternalIssueRecord.State.OPEN);
        if (record.getIssueDate() == null) {
            record.setIssueDate(LocalDate.now());
        }
        if (record.getDueDate() == null) {
            record.setDueDate(LocalDate.now().plusDays(14)); // Default 14-day loan duration
        }

        // 7. Update instrument status to ISSUED
        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        // 8. Save external loan record to MongoDB
        ExternalIssueRecord savedRecord = externalIssueRecordRepository.save(record);

        // 9. Dispatch notification email to Owner, External Scientist, and Intermediary Staff
        sendExternalIssueNotification(savedRecord, instrument, authenticatedOwner);

        return savedRecord;
    }

    /**
     * Process return of an externally issued instrument.
     * Restricted strictly to the instrument's owner scientist.
     */
    @Transactional
    public Optional<ExternalIssueRecord> returnExternalInstrument(String recordId, String conditionIn) {
        ExternalIssueRecord record = externalIssueRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("External issue record not found with ID: " + recordId));

        if (record.getState() == ExternalIssueRecord.State.RETURNED) {
            throw new IllegalStateException("Instrument is already marked as RETURNED.");
        }

        // Authenticate owner
        Person authenticatedOwner = getAuthenticatedScientist();
        if (authenticatedOwner == null) {
            throw new SecurityException("Unauthorized: Session not authenticated.");
        }

        // Confirm caller is the owner
        if (record.getOwnerScientist() != null
                && !record.getOwnerScientist().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: Only the owner scientist can confirm the return of an externally issued asset.");
        }

        // Update record
        record.setConditionIn(conditionIn != null && !conditionIn.trim().isEmpty() ? conditionIn : "Returned intact");
        record.setActualReturnDate(LocalDate.now());
        record.setState(ExternalIssueRecord.State.RETURNED);

        // Reset instrument status to AVAILABLE
        Instrument instrument = record.getInstrument();
        if (instrument != null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
            instrumentRepository.save(instrument);
        }

        ExternalIssueRecord updatedRecord = externalIssueRecordRepository.save(record);

        // Dispatch return confirmation email
        sendExternalReturnNotification(updatedRecord);

        return Optional.of(updatedRecord);
    }

    /**
     * Email helper for external checkouts
     */
    private void sendExternalIssueNotification(ExternalIssueRecord record, Instrument instrument, Person owner) {
        try {
            String recipientList = buildRecipientList(
                    owner.getEmail(),
                    record.getExternalBorrowerEmail(),
                    record.getStaffEmail()
            );

            if (!recipientList.isEmpty()) {
                String subject = "[APEEG] External Instrument Loan: " + instrument.getName() + " (" + instrument.getAssetId() + ")";
                String body = String.format(
                        "Dear Scientists & Staff,\n\n" +
                                "An instrument checkout has been created for an external borrower:\n\n" +
                                "• Asset ID: %s\n" +
                                "• Instrument: %s\n" +
                                "• Owning Scientist: %s\n" +
                                "• External Borrower: %s (%s)\n" +
                                "• Intermediary Staff: %s (%s)\n" +
                                "• Issue Date: %s\n" +
                                "• Due Date: %s\n" +
                                "• Purpose: %s\n\n" +
                                "Regards,\nCSIR-CBRI APEEG Inventory System",
                        instrument.getAssetId(),
                        instrument.getName(),
                        owner.getName(),
                        record.getExternalBorrowerName(),
                        record.getExternalBorrowerEmail(),
                        record.getStaffName() != null ? record.getStaffName() : "N/A",
                        record.getStaffEmail() != null ? record.getStaffEmail() : "N/A",
                        record.getIssueDate(),
                        record.getDueDate(),
                        record.getPurpose() != null ? record.getPurpose() : "N/A"
                );

                emailService.sendAndLogEmail(null, instrument, MailLog.MailType.ISSUE, recipientList, subject, body);
            }
        } catch (Exception e) {
            System.err.println("Failed to send external issue notification: " + e.getMessage());
        }
    }

    /**
     * Email helper for external returns
     */
    private void sendExternalReturnNotification(ExternalIssueRecord record) {
        try {
            Instrument instrument = record.getInstrument();
            String ownerEmail = record.getOwnerScientist() != null ? record.getOwnerScientist().getEmail() : null;

            String recipientList = buildRecipientList(
                    ownerEmail,
                    record.getExternalBorrowerEmail(),
                    record.getStaffEmail()
            );

            if (!recipientList.isEmpty()) {
                String subject = "[APEEG] External Instrument Returned: " + (instrument != null ? instrument.getName() : "Asset");
                String body = String.format(
                        "Dear Scientists & Staff,\n\n" +
                                "The external loan transaction has been closed and the instrument checked back into inventory:\n\n" +
                                "• Instrument: %s\n" +
                                "• External Borrower: %s\n" +
                                "• Actual Return Date: %s\n" +
                                "• Condition In: %s\n\n" +
                                "The instrument status is now reset to AVAILABLE.\n\n" +
                                "Regards,\nCSIR-CBRI APEEG Inventory System",
                        instrument != null ? instrument.getName() : "N/A",
                        record.getExternalBorrowerName(),
                        record.getActualReturnDate(),
                        record.getConditionIn()
                );

                emailService.sendAndLogEmail(null, instrument, MailLog.MailType.RETURN, recipientList, subject, body);
            }
        } catch (Exception e) {
            System.err.println("Failed to send external return notification: " + e.getMessage());
        }
    }

    private String buildRecipientList(String ownerEmail, String borrowerEmail, String staffEmail) {
        StringBuilder sb = new StringBuilder();
        if (ownerEmail != null && !ownerEmail.trim().isEmpty()) sb.append(ownerEmail.trim()).append(",");
        if (borrowerEmail != null && !borrowerEmail.trim().isEmpty()) sb.append(borrowerEmail.trim()).append(",");
        if (staffEmail != null && !staffEmail.trim().isEmpty()) sb.append(staffEmail.trim()).append(",");
        return sb.toString().replaceAll(",$", "");
    }

    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}