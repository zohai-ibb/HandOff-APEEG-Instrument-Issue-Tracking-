package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Direct Issue Logic:
     * Checks if instrument is AVAILABLE, updates status to ISSUED, and saves IssueRecord as OPEN.
     */
    public IssueRecord createDirectIssue(IssueRecord record) {
        if (record.getInstrument() == null || record.getInstrument().getId() == null) {
            throw new IllegalArgumentException("Associated instrument ID must be provided.");
        }

        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + record.getInstrument().getId()));

        if (instrument.getStatus() != Instrument.Status.AVAILABLE) {
            throw new IllegalStateException("Instrument is currently " + instrument.getStatus() + " and cannot be issued.");
        }

        // 1. Mutate instrument status
        instrument.setStatus(Instrument.Status.ISSUED);
        instrumentRepository.save(instrument);

        // 2. Prepare and save issue record
        record.setState(IssueRecord.State.OPEN);
        if (record.getIssueDate() == null) {
            record.setIssueDate(LocalDate.now());
        }

        return issueRecordRepository.save(record);
    }

    /**
     * Return Logic:
     * Closes the active checkout record and resets instrument status to AVAILABLE.
     */
    public Optional<IssueRecord> returnInstrument(String issueRecordId, String conditionIn) {
        return issueRecordRepository.findById(issueRecordId).map(record -> {
            record.setConditionIn(conditionIn);
            record.setActualReturnDate(LocalDate.now());
            record.setState(IssueRecord.State.RETURNED);

            // Restore instrument back to AVAILABLE
            Instrument instrument = record.getInstrument();
            if (instrument != null) {
                instrument.setStatus(Instrument.Status.AVAILABLE);
                instrumentRepository.save(instrument);
            }

            return issueRecordRepository.save(record);
        });
    }
}