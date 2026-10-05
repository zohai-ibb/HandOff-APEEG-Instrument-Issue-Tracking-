package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
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

    public IssueRecordService(IssueRecordRepository issueRecordRepository,
                              InstrumentRepository instrumentRepository,
                              MailSchedulerService mailSchedulerService,
                              FileStorageService fileStorageService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.mailSchedulerService = mailSchedulerService;
        this.fileStorageService = fileStorageService;
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

    /**
     * Processes an instrument return.
     * Enforces conditionIn as "GOOD" or "BAD".
     * Updates Instrument.status to AVAILABLE if GOOD, or MAINTENANCE if BAD.
     */
    @Transactional
    public Optional<IssueRecord> returnInstrument(String issueRecordId, String conditionIn, MultipartFile photo) {
        // 1. Fetch IssueRecord
        IssueRecord record = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found with ID: " + issueRecordId));

        if (record.getState() == IssueRecord.State.RETURNED) {
            throw new IllegalStateException("This instrument loan is already marked as RETURNED.");
        }

        // 2. Security / Stakeholder Check
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

        // 3. Validate & Resolve Binary Condition State ("GOOD" vs "BAD")
        String resolvedCondition = "GOOD"; // Default baseline
        if (conditionIn != null && conditionIn.trim().equalsIgnoreCase("BAD")) {
            resolvedCondition = "BAD";
        }

        // 4. Save Return Photo if provided
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

        // 6. Update Instrument Status based on condition state
        Instrument instrument = record.getInstrument();
        if (instrument != null) {
            if ("BAD".equals(resolvedCondition)) {
                // Damaged/faulty instrument goes directly under MAINTENANCE
                instrument.setStatus(Instrument.Status.MAINTENANCE);
            } else {
                // Good condition instrument restores to AVAILABLE
                instrument.setStatus(Instrument.Status.AVAILABLE);
            }
            instrumentRepository.save(instrument);
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

    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}