package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import com.example.APEEG.repository.PersonRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service dedicated to managing instrument issuing workflows initiated
 * directly by the Instrument Owner for another registered Scientist in the application.
 */
@Service
public class OwnerIssueService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final PersonRepository personRepository;
    private final MailSchedulerService mailSchedulerService;

    public OwnerIssueService(IssueRecordRepository issueRecordRepository,
                             InstrumentRepository instrumentRepository,
                             PersonRepository personRepository,
                             MailSchedulerService mailSchedulerService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.personRepository = personRepository;
        this.mailSchedulerService = mailSchedulerService;
    }

    /**
     * Issues an instrument directly to another registered scientist in the system.
     * Initiated strictly by the logged-in Owner Scientist.
     *
     * @param instrumentId         Target instrument ID to be issued
     * @param borrowerScientistId  Selected registered borrower scientist ID
     * @param staffName            Intermediary staff member name
     * @param staffEmail           Intermediary staff member email
     * @param dueDate              Target return due date
     * @param purpose              Purpose of the loan/borrowing
     * @param conditionOut         Equipment physical condition at issuing time
     * @return Saved IssueRecord document in MongoDB
     */
    @Transactional
    public IssueRecord issueToInternalScientist(String instrumentId,
                                                String borrowerScientistId,
                                                String staffName,
                                                String staffEmail,
                                                LocalDate dueDate,
                                                String purpose,
                                                String conditionOut) {

        // 1. Authenticate Logged-In Owner Scientist from JWT Context
        Person authenticatedOwner = getAuthenticatedScientist();
        if (authenticatedOwner == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }

        // 2. Validate & Fetch Target Instrument from Database
        if (instrumentId == null || instrumentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Instrument ID is required.");
        }

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + instrumentId));

        // 3. Ownership Guard: Ensure caller actually owns this instrument
        if (instrument.getOwnerScientist() == null ||
                !instrument.getOwnerScientist().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: You can only issue instruments that belong to your inventory.");
        }

        // 4. Availability Guard: Confirm instrument status is AVAILABLE
        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        // 5. Validate & Fetch Borrower Scientist from Registered Users ('persons' collection)
        if (borrowerScientistId == null || borrowerScientistId.trim().isEmpty()) {
            throw new IllegalArgumentException("Borrower Scientist ID is required.");
        }

        Person borrowerScientist = personRepository.findById(borrowerScientistId)
                .orElseThrow(() -> new IllegalArgumentException("Borrower scientist not found with ID: " + borrowerScientistId));

        // 6. Prevent Self-Issuing
        if (borrowerScientist.getId().equals(authenticatedOwner.getId())) {
            throw new IllegalArgumentException("You cannot issue an instrument to yourself.");
        }

        // 7. Update Instrument Status to ISSUED
        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        // 8. Construct and Persist the IssueRecord Document
        IssueRecord record = new IssueRecord();
        record.setInstrument(instrument);
        record.setOwnerScientist(authenticatedOwner);
        record.setBorrowerScientist(borrowerScientist); // Hydrated DB entity (contains full name & email)
        record.setStaffName(staffName);
        record.setStaffEmail(staffEmail);
        record.setIssueDate(LocalDate.now());
        record.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(14));
        record.setPurpose(purpose);
        record.setConditionOut(conditionOut);
        record.setState(IssueRecord.State.OPEN);

        IssueRecord savedRecord = issueRecordRepository.save(record);

        // 9. Dispatch Automated Email Notification
        try {
            mailSchedulerService.sendIssueConfirmation(savedRecord);
        } catch (Exception e) {
            System.err.println("Failed to dispatch issue confirmation email: " + e.getMessage());
        }

        return savedRecord;
    }

    /**
     * Helper to retrieve currently authenticated Person principal from JWT SecurityContext
     */
    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}