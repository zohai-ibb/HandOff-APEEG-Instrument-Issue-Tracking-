package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.model.MailLog;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IssueRecordService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final MailSchedulerService mailSchedulerService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    public IssueRecordService(IssueRecordRepository issueRecordRepository,
                              InstrumentRepository instrumentRepository,
                              MailSchedulerService mailSchedulerService,
                              FileStorageService fileStorageService,
                              EmailService emailService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.mailSchedulerService = mailSchedulerService;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
    }

    public List<IssueRecord> getMyIssueRecords() {
        Person loggedInScientist = getAuthenticatedScientist();
        if (loggedInScientist == null) {
            throw new SecurityException("Unauthorized: No authenticated scientist session found.");
        }

        String loggedInId = loggedInScientist.getId();
        List<IssueRecord> allRecords = issueRecordRepository.findAll();

        return allRecords.stream().filter(record -> {
            boolean isBorrower = record.getBorrowerScientist() != null
                    && loggedInId.equals(record.getBorrowerScientist().getId());

            boolean isOwner = record.getOwnerScientist() != null
                    && loggedInId.equals(record.getOwnerScientist().getId());

            return isBorrower || isOwner;
        }).collect(Collectors.toList());
    }

    public Optional<IssueRecord> getByIdSecured(String id) {
        Optional<IssueRecord> recordOpt = issueRecordRepository.findById(id);

        if (recordOpt.isPresent()) {
            IssueRecord record = recordOpt.get();
            Person loggedInScientist = getAuthenticatedScientist();

            if (loggedInScientist != null) {
                boolean isBorrower = record.getBorrowerScientist() != null
                        && loggedInScientist.getId().equals(record.getBorrowerScientist().getId());

                boolean isOwner = record.getOwnerScientist() != null
                        && loggedInScientist.getId().equals(record.getOwnerScientist().getId());

                if (!isBorrower && !isOwner) {
                    throw new SecurityException("Forbidden: You are not authorized to view this issue record.");
                }
            }
        }

        return recordOpt;
    }

    public List<IssueRecord> getByBorrowerScientistId(String borrowerId) {
        Person loggedInScientist = getAuthenticatedScientist();
        if (loggedInScientist != null && !loggedInScientist.getId().equals(borrowerId)) {
            throw new SecurityException("Forbidden: You cannot view borrowing records of another scientist.");
        }
        return issueRecordRepository.findByBorrowerScientistId(borrowerId);
    }

    public List<IssueRecord> getByOwnerScientistId(String ownerId) {
        Person loggedInScientist = getAuthenticatedScientist();
        if (loggedInScientist != null && !loggedInScientist.getId().equals(ownerId)) {
            throw new SecurityException("Forbidden: You cannot view loan records of instruments owned by another scientist.");
        }
        return issueRecordRepository.findByOwnerScientistId(ownerId);
    }

    @Transactional
    public IssueRecord createDirectIssue(IssueRecord record) {
        if (record.getInstrument() == null || record.getInstrument().getId() == null) {
            throw new IllegalArgumentException("Associated instrument ID must be provided.");
        }

        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + record.getInstrument().getId()));

        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        Person loggedInScientist = getAuthenticatedScientist();

        if (loggedInScientist != null) {
            record.setBorrowerScientist(loggedInScientist);
        } else if (record.getBorrowerScientist() == null) {
            throw new IllegalArgumentException("Borrower scientist details are missing.");
        }

        if (instrument.getOwnerScientist() != null && loggedInScientist != null) {
            if (instrument.getOwnerScientist().getId().equals(loggedInScientist.getId())) {
                throw new IllegalArgumentException("You cannot borrow an instrument that you already own.");
            }
        }

        record.setInstrument(instrument);
        record.setOwnerScientist(instrument.getOwnerScientist());

        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        record.setState(IssueRecord.State.OPEN);
        if (record.getIssueDate() == null) {
            record.setIssueDate(LocalDate.now());
        }

        IssueRecord savedRecord = issueRecordRepository.save(record);

        try {
            mailSchedulerService.sendIssueConfirmation(savedRecord);
        } catch (Exception e) {
            System.err.println("Failed to send issue confirmation email: " + e.getMessage());
        }

        return savedRecord;
    }

    @Transactional
    public Optional<IssueRecord> returnInstrument(String issueRecordId, String conditionIn, MultipartFile photo) {
        // 1. Fetch Issue Record
        IssueRecord record = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found with ID: " + issueRecordId));

        if (record.getState() == IssueRecord.State.RETURNED) {
            throw new IllegalStateException("This instrument loan is already marked as RETURNED.");
        }

        // 2. Validate Stakeholder Authentication
        Person authenticatedUser = getAuthenticatedScientist();
        if (authenticatedUser == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }

        boolean isOwner = record.getOwnerScientist() != null
                && record.getOwnerScientist().getId().equals(authenticatedUser.getId());

        boolean isBorrower = record.getBorrowerScientist() != null
                && authenticatedUser.getId().equals(record.getBorrowerScientist().getId());

        if (!isOwner && !isBorrower) {
            throw new SecurityException("Forbidden: Only the borrowing scientist or owner scientist can process this return.");
        }

        // 3. Resolve Binary Condition State ("GOOD" vs "BAD")
        String resolvedCondition = "GOOD";
        if (conditionIn != null && conditionIn.trim().equalsIgnoreCase("BAD")) {
            resolvedCondition = "BAD";
        }

        // 4. Save Return Photo if attached
        if (photo != null && !photo.isEmpty()) {
            try {
                String photoPath = fileStorageService.saveFile(photo);
                record.setConditionInPhotoPath(photoPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store return condition photo: " + e.getMessage(), e);
            }
        }

        // 5. Update Issue Record closure fields
        record.setConditionIn(resolvedCondition);
        record.setActualReturnDate(LocalDate.now());
        record.setState(IssueRecord.State.RETURNED);

        // 6. Handle Re-merging Inventory Quantity
        Instrument issuedInstrument = record.getInstrument();

        if (issuedInstrument != null) {
            int returnedQty = issuedInstrument.getQuantity() != null ? issuedInstrument.getQuantity() : 1;

            // Check if this instrument was created as a split child item (e.g. assetId ends with timestamp)
            if (issuedInstrument.getAssetId() != null && issuedInstrument.getAssetId().contains("-")) {
                String baseAssetId = issuedInstrument.getAssetId().split("-")[0];

                // Locate master inventory instrument by base asset ID
                Optional<Instrument> masterOpt = instrumentRepository.findByAssetId(baseAssetId);

                if (masterOpt.isPresent()) {
                    Instrument master = masterOpt.get();

                    // Restore master quantity
                    int existingMasterQty = master.getQuantity() != null ? master.getQuantity() : 0;
                    master.setQuantity(existingMasterQty + returnedQty);

                    // Update master status if returning in BAD condition
                    if ("BAD".equals(resolvedCondition)) {
                        master.setStatus(Instrument.Status.MAINTENANCE);
                    } else if (master.getStatus() == Instrument.Status.ISSUED) {
                        master.setStatus(Instrument.Status.AVAILABLE);
                    }

                    instrumentRepository.save(master);

                    // Link record to master before deleting child entry
                    record.setInstrument(master);
                    instrumentRepository.delete(issuedInstrument);
                } else {
                    // Fallback: Restore child status if master is not found
                    updateSingleInstrumentStatus(issuedInstrument, resolvedCondition);
                }
            } else {
                // Full checkout return (No split occurred)
                updateSingleInstrumentStatus(issuedInstrument, resolvedCondition);
            }
        }

        IssueRecord updatedRecord = issueRecordRepository.save(record);

        // 7. Dispatch Return Email Notification
        try {
            mailSchedulerService.sendReturnConfirmation(updatedRecord);
        } catch (Exception e) {
            System.err.println("Failed to send return confirmation email: " + e.getMessage());
        }

        return Optional.of(updatedRecord);
    }

    private void updateSingleInstrumentStatus(Instrument instrument, String condition) {
        if ("BAD".equals(condition)) {
            instrument.setStatus(Instrument.Status.MAINTENANCE);
        } else {
            instrument.setStatus(Instrument.Status.AVAILABLE);
        }
        instrumentRepository.save(instrument);
    }

    /**
     * Reissues an active instrument loan by updating its due date to a future date.
     */
    @Transactional
    public Optional<IssueRecord> reissueInstrument(String issueRecordId, String newDueDateStr) {
        // 1. Fetch existing issue record
        IssueRecord record = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found with ID: " + issueRecordId));

        if (record.getState() == IssueRecord.State.RETURNED) {
            throw new IllegalStateException("Cannot reissue an instrument that has already been returned.");
        }

        // 2. Stakeholder Authorization Safeguard
        Person authenticatedUser = getAuthenticatedScientist();
        if (authenticatedUser != null) {
            boolean isOwner = record.getOwnerScientist() != null
                    && authenticatedUser.getId().equals(record.getOwnerScientist().getId());

            boolean isBorrower = record.getBorrowerScientist() != null
                    && authenticatedUser.getId().equals(record.getBorrowerScientist().getId());

            if (!isOwner && !isBorrower) {
                throw new SecurityException("Forbidden: Only the borrowing scientist or owner scientist can reissue this instrument.");
            }
        }

        // 3. Parse and validate new due date
        LocalDate newDueDate = LocalDate.parse(newDueDateStr);
        if (newDueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("New due date must be today or a future date.");
        }

        // 4. Update the due date
        record.setDueDate(newDueDate);

        IssueRecord savedRecord = issueRecordRepository.save(record);

        // 5. Optional: Send Reissue Confirmation Email
        try {
            if (mailSchedulerService != null) {
                // mailSchedulerService.sendReissueConfirmation(savedRecord);
            }
        } catch (Exception e) {
            System.err.println("Failed to send reissue notification email: " + e.getMessage());
        }

        return Optional.of(savedRecord);
    }

    /**
     * Send Manual Reminder: Dispatches an instant overdue/reminder email to stakeholders.
     */
    public void sendManualReminder(String recordId) {
        IssueRecord record = issueRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found with ID: " + recordId));

        if (record.getState() == IssueRecord.State.RETURNED) {
            throw new IllegalStateException("Cannot send reminder for an instrument that has already been returned.");
        }

        // Construct reminder email details
        Instrument instrument = record.getInstrument();
        String instrumentName = instrument != null ? instrument.getName() : "Instrument";
        String assetId = instrument != null ? instrument.getAssetId() : "N/A";

        String subject = "[APEEG REMINDER] Return Notice: " + instrumentName;
        String body = "REMINDER: The instrument " + instrumentName + " (" + assetId + ") is due/overdue."
                + "\nExpected Return Date: " + record.getDueDate()
                + "\nBorrower: " + (record.getBorrowerScientist() != null ? record.getBorrowerScientist().getName() : "N/A")
                + "\nStaff Handler: " + record.getStaffName()
                + "\n\nPlease arrange for its return to the laboratory.";

        String recipients = buildRecipients(record);

        emailService.sendAndLogEmail(
                record,
                instrument,
                MailLog.MailType.OVERDUE,
                recipients,
                subject,
                body
        );
    }

    private String buildRecipients(IssueRecord record) {
        StringBuilder recipients = new StringBuilder();
        if (record.getBorrowerScientist() != null && record.getBorrowerScientist().getEmail() != null) {
            recipients.append(record.getBorrowerScientist().getEmail());
        }
        if (record.getStaffEmail() != null && !record.getStaffEmail().trim().isEmpty()) {
            if (recipients.length() > 0) recipients.append(", ");
            recipients.append(record.getStaffEmail());
        }
        if (record.getOwnerScientist() != null && record.getOwnerScientist().getEmail() != null) {
            if (recipients.length() > 0) recipients.append(", ");
            recipients.append(record.getOwnerScientist().getEmail());
        }
        return recipients.toString();
    }


    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}