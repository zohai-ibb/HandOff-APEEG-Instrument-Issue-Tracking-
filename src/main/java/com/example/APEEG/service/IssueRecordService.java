package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class IssueRecordService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final MailSchedulerService mailSchedulerService;

    public IssueRecordService(IssueRecordRepository issueRecordRepository,
                              InstrumentRepository instrumentRepository,
                              MailSchedulerService mailSchedulerService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.mailSchedulerService = mailSchedulerService;
    }

    public List<IssueRecord> getAllRecords() {
        return issueRecordRepository.findAll();
    }

    public Optional<IssueRecord> getById(String id) {
        return issueRecordRepository.findById(id);
    }

    public List<IssueRecord> getByBorrowerScientistId(String borrowerId) {
        return issueRecordRepository.findByBorrowerScientistId(borrowerId);
    }

    public List<IssueRecord> getByOwnerScientistId(String ownerId) {
        return issueRecordRepository.findByOwnerScientistId(ownerId);
    }

    public List<IssueRecord> getByState(IssueRecord.State state) {
        return issueRecordRepository.findByState(state);
    }

    public IssueRecord createDirectIssue(IssueRecord record) {
        if (record.getInstrument() == null || record.getInstrument().getId() == null) {
            throw new IllegalArgumentException("Associated instrument ID must be provided.");
        }

        // 1. Fetch target instrument from database (Fully populated with names, etc.)
        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + record.getInstrument().getId()));

        // 2. Validate availability
        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        // 3. Authenticate Borrower Scientist from JWT context
        Person loggedInScientist = getAuthenticatedScientist();

        if (loggedInScientist != null) {
            record.setBorrowerScientist(loggedInScientist);
        } else if (record.getBorrowerScientist() == null) {
            throw new IllegalArgumentException("Borrower scientist details are missing.");
        }

        // 4. Prevent Self-Borrowing
        if (instrument.getOwnerScientist() != null && loggedInScientist != null) {
            if (instrument.getOwnerScientist().getId().equals(loggedInScientist.getId())) {
                throw new IllegalArgumentException("You cannot borrow an instrument that you already own.");
            }
        }

        // --- FIX 1: Attach the fully loaded Instrument back to the record ---
        // This ensures the email service has access to the instrument's name and assetId
        record.setInstrument(instrument);

        // --- FIX 2: Attach the fully loaded Owner Scientist unconditionally ---
        // The JSON payload only contains the ID, so the name is null.
        // We overwrite it with the fully loaded owner from the fetched instrument.
        record.setOwnerScientist(instrument.getOwnerScientist());

        // 5. Update Instrument Status to ISSUED
        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        // 6. Stamp Record defaults and save
        record.setState(IssueRecord.State.OPEN);
        if (record.getIssueDate() == null) {
            record.setIssueDate(LocalDate.now());
        }

        IssueRecord savedRecord = issueRecordRepository.save(record);

        // 7. Trigger mail confirmation via MailSchedulerService
        mailSchedulerService.sendIssueConfirmation(savedRecord);

        return savedRecord;
    }

    public Optional<IssueRecord> returnInstrument(String issueRecordId, String conditionIn) {
        IssueRecord record = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found with ID: " + issueRecordId));

        Person loggedInScientist = getAuthenticatedScientist();

        if (loggedInScientist == null) {
            throw new SecurityException("Unauthorized: No authenticated scientist session found.");
        }

        boolean isBorrower = record.getBorrowerScientist() != null
                && loggedInScientist.getId().equals(record.getBorrowerScientist().getId());

        boolean isOwner = record.getOwnerScientist() != null
                && loggedInScientist.getId().equals(record.getOwnerScientist().getId());

        if (!isBorrower && !isOwner) {
            throw new SecurityException("Forbidden: Only the borrowing scientist or the owner scientist can process this return.");
        }

        record.setConditionIn(conditionIn != null && !conditionIn.trim().isEmpty() ? conditionIn : "Returned intact");
        record.setActualReturnDate(LocalDate.now());
        record.setState(IssueRecord.State.RETURNED);

        Instrument instrument = record.getInstrument();
        if (instrument != null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
            instrumentRepository.save(instrument);
        }

        IssueRecord updatedRecord = issueRecordRepository.save(record);

        // Trigger return confirmation email via MailSchedulerService
        mailSchedulerService.sendReturnConfirmation(updatedRecord);

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