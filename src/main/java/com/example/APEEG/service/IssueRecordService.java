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

/**
 * Service managing equipment lending operations.
 * Implements safeguards against self-borrowing, peer impersonation, and unauthorized returns.
 */
@Service
public class IssueRecordService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;

    public IssueRecordService(IssueRecordRepository issueRecordRepository, InstrumentRepository instrumentRepository) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
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

    /**
     * Direct Checkout: Validates ownership, availability, and automatically injects logged-in scientist.
     */
    public IssueRecord createDirectIssue(IssueRecord record) {
        if (record.getInstrument() == null || record.getInstrument().getId() == null) {
            throw new IllegalArgumentException("Associated instrument ID must be provided.");
        }

        // Fetch target instrument from database
        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + record.getInstrument().getId()));

        // Check 1: Instrument Availability
        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        // Extract currently authenticated scientist from JWT Context
        Person loggedInScientist = getAuthenticatedScientist();

        // Check 2: Prevent Impersonation — Lock Borrower Scientist to Authenticated Principal
        if (loggedInScientist != null) {
            record.setBorrowerScientist(loggedInScientist);
        } else if (record.getBorrowerScientist() == null) {
            throw new IllegalArgumentException("Borrower scientist details are missing.");
        }

        // Check 3: Prevent Self-Borrowing
        if (instrument.getOwnerScientist() != null && loggedInScientist != null) {
            if (instrument.getOwnerScientist().getId().equals(loggedInScientist.getId())) {
                throw new IllegalArgumentException("You cannot borrow an instrument that you already own.");
            }
        }

        // Ensure ownerScientist is attached from instrument snapshot
        if (record.getOwnerScientist() == null) {
            record.setOwnerScientist(instrument.getOwnerScientist());
        }

        // Mutate instrument status
        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        // Stamp defaults
        record.setState(IssueRecord.State.OPEN);
        if (record.getIssueDate() == null) {
            record.setIssueDate(LocalDate.now());
        }

        return issueRecordRepository.save(record);
    }

    public Optional<IssueRecord> returnInstrument(String issueRecordId, String conditionIn) {
        IssueRecord record = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found with ID: " + issueRecordId));

        // 1. Extract currently authenticated scientist from JWT token
        Person loggedInScientist = getAuthenticatedScientist();

        if (loggedInScientist == null) {
            throw new SecurityException("Unauthorized: No authenticated scientist session found.");
        }

        // 2. Strict Stakeholder Authorization Verification
        boolean isBorrower = record.getBorrowerScientist() != null
                && loggedInScientist.getId().equals(record.getBorrowerScientist().getId());

        boolean isOwner = record.getOwnerScientist() != null
                && loggedInScientist.getId().equals(record.getOwnerScientist().getId());

        // 3. Reject request if the caller is neither the borrower nor the owner
        if (!isBorrower && !isOwner) {
            throw new SecurityException("Forbidden: Only the borrowing scientist or the owner scientist can process this return.");
        }

        // 4. Update Issue Record
        record.setConditionIn(conditionIn != null && !conditionIn.trim().isEmpty() ? conditionIn : "Returned intact");
        record.setActualReturnDate(LocalDate.now());
        record.setState(IssueRecord.State.RETURNED);

        // 5. Restore Instrument Status back to AVAILABLE
        Instrument instrument = record.getInstrument();
        if (instrument != null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
            instrumentRepository.save(instrument);
        }

        return Optional.of(issueRecordRepository.save(record));
    }

    /**
     * Helper to retrieve authenticated Person principal from Spring Security Context
     */
    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}