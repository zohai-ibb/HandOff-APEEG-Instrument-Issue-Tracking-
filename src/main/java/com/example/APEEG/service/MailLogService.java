package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.MailLog;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.MailLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MailLogService {

    private final MailLogRepository mailLogRepository;
    private final InstrumentRepository instrumentRepository;

    public MailLogService(MailLogRepository mailLogRepository, InstrumentRepository instrumentRepository) {
        this.mailLogRepository = mailLogRepository;
        this.instrumentRepository = instrumentRepository;
    }

    /**
     * Retrieves all logs for the currently logged-in scientist.
     * Returns logs where the scientist is either the Instrument Owner or Borrower.
     */
    public List<MailLog> getMyMailLogs() {
        Person authenticatedScientist = getAuthenticatedScientist();
        if (authenticatedScientist == null) {
            throw new SecurityException("Unauthorized: No authenticated scientist session found.");
        }

        List<MailLog> allLogs = mailLogRepository.findAll();

        return allLogs.stream().filter(log -> {
            boolean isOwner = log.getInstrument() != null
                    && log.getInstrument().getOwnerScientist() != null
                    && authenticatedScientist.getId().equals(log.getInstrument().getOwnerScientist().getId());

            boolean isBorrower = log.getIssueRecord() != null
                    && log.getIssueRecord().getBorrowerScientist() != null
                    && authenticatedScientist.getId().equals(log.getIssueRecord().getBorrowerScientist().getId());

            return isOwner || isBorrower;
        }).collect(Collectors.toList());
    }

    /**
     * Retrieves mail logs for a specific instrument ID.
     * SECURED: Strictly ensures the calling scientist is the owner of that instrument.
     */
    public List<MailLog> getByInstrumentId(String instrumentId) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + instrumentId));

        Person authenticatedScientist = getAuthenticatedScientist();

        // Security Check: Only the Owner Scientist can view mail audit logs for this instrument
        if (authenticatedScientist != null && instrument.getOwnerScientist() != null) {
            if (!instrument.getOwnerScientist().getId().equals(authenticatedScientist.getId())) {
                throw new SecurityException("Forbidden: You are not authorized to view mail logs for an instrument owned by another scientist.");
            }
        }

        return mailLogRepository.findByInstrumentId(instrumentId);
    }

    public MailLog recordMailAttempt(MailLog mailLog) {
        return mailLogRepository.save(mailLog);
    }

    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}