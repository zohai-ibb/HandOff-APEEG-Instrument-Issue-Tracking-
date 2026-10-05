package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.model.Person;
import com.example.APEEG.model.ScientistList;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import com.example.APEEG.repository.ScientistListRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Service
public class OwnerIssueService {

    private final IssueRecordRepository issueRecordRepository;
    private final InstrumentRepository instrumentRepository;
    private final ScientistListRepository scientistListRepository;
    private final MailSchedulerService mailSchedulerService;
    private final FileStorageService fileStorageService;

    public OwnerIssueService(IssueRecordRepository issueRecordRepository,
                             InstrumentRepository instrumentRepository,
                             ScientistListRepository scientistListRepository,
                             MailSchedulerService mailSchedulerService,
                             FileStorageService fileStorageService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.scientistListRepository = scientistListRepository;
        this.mailSchedulerService = mailSchedulerService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Issues an instrument to a scientist contact saved in ScientistList.
     */
    @Transactional
    public IssueRecord issueToScientistFromList(String instrumentId,
                                                String scientistListId,
                                                String staffName,
                                                String staffEmail,
                                                LocalDate dueDate,
                                                String purpose,
                                                String conditionOut,
                                                MultipartFile photo) {

        // 1. Authenticate Logged-In Instrument Owner
        Person authenticatedOwner = getAuthenticatedScientist();
        if (authenticatedOwner == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }

        // 2. Validate & Fetch Target Instrument
        if (instrumentId == null || instrumentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Instrument ID is required.");
        }

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + instrumentId));

        // 3. Verify Instrument Ownership
        if (instrument.getOwnerScientist() == null ||
                !instrument.getOwnerScientist().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: You can only issue instruments from your own inventory.");
        }

        // 4. Verify Instrument Availability
        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        // 5. Fetch Target Scientist Contact
        if (scientistListId == null || scientistListId.trim().isEmpty()) {
            throw new IllegalArgumentException("Scientist contact ID is required.");
        }

        ScientistList targetScientist = scientistListRepository.findById(scientistListId)
                .orElseThrow(() -> new IllegalArgumentException("Scientist contact not found with ID: " + scientistListId));

        if (targetScientist.getOwnerUser() == null ||
                !targetScientist.getOwnerUser().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: Access denied to this scientist contact.");
        }

        // 6. Save condition photo if uploaded
        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoPath = fileStorageService.saveFile(photo);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store condition photo: " + e.getMessage());
            }
        }

        // 7. Update Instrument Status to ISSUED
        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        // 8. Build Person Snapshot WITH Scientist Contact ID
        Person borrowerSnapshot = new Person();
        borrowerSnapshot.setId(targetScientist.getId()); // Sets the ID on the embedded object
        borrowerSnapshot.setName(targetScientist.getName());
        borrowerSnapshot.setEmail(targetScientist.getEmail());
        borrowerSnapshot.setMobile(targetScientist.getMobile());
        borrowerSnapshot.setDepartment(targetScientist.getDepartment());
        borrowerSnapshot.setIsActive(true);

        // 9. Construct and Persist IssueRecord
        IssueRecord record = new IssueRecord();
        record.setInstrument(instrument);
        record.setOwnerScientist(authenticatedOwner);
        record.setBorrowerScientist(borrowerSnapshot);
        record.setStaffName(staffName);
        record.setStaffEmail(staffEmail);
        record.setIssueDate(LocalDate.now());
        record.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(14));
        record.setPurpose(purpose);
        record.setConditionOut(conditionOut);
        record.setConditionPhotoPath(photoPath);
        record.setState(IssueRecord.State.OPEN);

        IssueRecord savedRecord = issueRecordRepository.save(record);

        // 10. Trigger transactional emails
        try {
            mailSchedulerService.sendIssueConfirmation(savedRecord);
        } catch (Exception e) {
            System.err.println("Failed to dispatch confirmation email: " + e.getMessage());
        }

        return savedRecord;
    }

    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}