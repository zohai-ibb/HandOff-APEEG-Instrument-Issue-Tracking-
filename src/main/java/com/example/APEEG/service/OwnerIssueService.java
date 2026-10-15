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
    private final StaffMemberService staffMemberService;

    public OwnerIssueService(IssueRecordRepository issueRecordRepository,
                             InstrumentRepository instrumentRepository,
                             ScientistListRepository scientistListRepository,
                             MailSchedulerService mailSchedulerService,
                             FileStorageService fileStorageService,
                             StaffMemberService staffMemberService) {
        this.issueRecordRepository = issueRecordRepository;
        this.instrumentRepository = instrumentRepository;
        this.scientistListRepository = scientistListRepository;
        this.mailSchedulerService = mailSchedulerService;
        this.fileStorageService = fileStorageService;
        this.staffMemberService = staffMemberService;
    }

    /**
     * Issues an instrument to a scientist contact saved in ScientistList.
     */
    @Transactional
    public IssueRecord issueToScientistFromList(String instrumentId,
                                                String scientistListId,
                                                Integer issueQuantity,
                                                String staffName,
                                                String staffEmail,
                                                LocalDate dueDate,
                                                String purpose,
                                                String conditionOut,
                                                MultipartFile photo) {

        Person authenticatedOwner = getAuthenticatedScientist();
        if (authenticatedOwner == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }

        if (instrumentId == null || instrumentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Instrument ID is required.");
        }

        Instrument masterInstrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + instrumentId));

        if (masterInstrument.getOwnerScientist() == null ||
                !masterInstrument.getOwnerScientist().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: You can only issue instruments from your own inventory.");
        }

        if (masterInstrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + masterInstrument.getStatus() + " and cannot be issued.");
        }

        int requestedQty = (issueQuantity != null && issueQuantity > 0) ? issueQuantity : 1;
        int currentMasterQty = masterInstrument.getQuantity() != null ? masterInstrument.getQuantity() : 1;

        if (requestedQty > currentMasterQty) {
            throw new IllegalArgumentException("Cannot issue " + requestedQty + " items. Only " + currentMasterQty + " available in inventory.");
        }

        if (scientistListId == null || scientistListId.trim().isEmpty()) {
            throw new IllegalArgumentException("Scientist contact ID is required.");
        }

        ScientistList targetScientist = scientistListRepository.findById(scientistListId)
                .orElseThrow(() -> new IllegalArgumentException("Scientist contact not found with ID: " + scientistListId));

        if (targetScientist.getOwnerUser() == null ||
                !targetScientist.getOwnerUser().getId().equals(authenticatedOwner.getId())) {
            throw new SecurityException("Forbidden: Access denied to this scientist contact.");
        }

        Instrument issuedInstrument;

        if (requestedQty == currentMasterQty) {
            masterInstrument.setStatus(Instrument.Status.ISSUED);
            issuedInstrument = instrumentRepository.save(masterInstrument);
        } else {
            masterInstrument.setQuantity(currentMasterQty - requestedQty);
            instrumentRepository.save(masterInstrument);

            issuedInstrument = new Instrument();
            issuedInstrument.setAssetId(masterInstrument.getAssetId() + "-" + System.currentTimeMillis() % 1000);
            issuedInstrument.setName(masterInstrument.getName());
            issuedInstrument.setMake(masterInstrument.getMake());
            issuedInstrument.setSerialNo(masterInstrument.getSerialNo());
            issuedInstrument.setQuantity(requestedQty);
            issuedInstrument.setLocation(masterInstrument.getLocation());
            issuedInstrument.setOwnerScientist(masterInstrument.getOwnerScientist());
            issuedInstrument.setCalibrationValidTo(masterInstrument.getCalibrationValidTo());
            issuedInstrument.setPurchaseDate(masterInstrument.getPurchaseDate());
            issuedInstrument.setPurchaseCost(masterInstrument.getPurchaseCost());
            issuedInstrument.setPhotoPath(masterInstrument.getPhotoPath());
            issuedInstrument.setQrPhoto(masterInstrument.getQrPhoto());
            issuedInstrument.setStatus(Instrument.Status.ISSUED);

            issuedInstrument = instrumentRepository.save(issuedInstrument);
        }

        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoPath = fileStorageService.saveFile(photo);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store condition photo: " + e.getMessage());
            }
        }

        Person borrowerSnapshot = new Person();
        borrowerSnapshot.setId(targetScientist.getId());
        borrowerSnapshot.setName(targetScientist.getName());
        borrowerSnapshot.setEmail(targetScientist.getEmail());
        borrowerSnapshot.setMobile(targetScientist.getMobile());
        borrowerSnapshot.setDepartment(targetScientist.getDepartment());
        borrowerSnapshot.setIsActive(true);

        IssueRecord record = new IssueRecord();
        record.setInstrument(issuedInstrument);
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