package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.MaintenanceRecord;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.MaintenanceRecordRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final InstrumentRepository instrumentRepository;

    public MaintenanceRecordService(MaintenanceRecordRepository maintenanceRecordRepository,
                                    InstrumentRepository instrumentRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.instrumentRepository = instrumentRepository;
    }

    public List<MaintenanceRecord> getAllMaintenanceRecords() {
        return maintenanceRecordRepository.findAll();
    }

    public List<MaintenanceRecord> getByInstrumentId(String instrumentId) {
        return maintenanceRecordRepository.findByInstrumentId(instrumentId);
    }

    /**
     * Dispatch an instrument to off-site vendor for repair or calibration.
     * SECURED: Only the Instrument OWNER can initiate maintenance.
     */
    public MaintenanceRecord sendToMaintenance(MaintenanceRecord record) {
        if (record.getInstrument() == null || record.getInstrument().getId() == null) {
            throw new IllegalArgumentException("Instrument ID must be provided.");
        }

        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found."));

        Person authenticatedScientist = getAuthenticatedScientist();

        // Check Ownership: Ensure the caller is the owner of the instrument
        if (authenticatedScientist != null && instrument.getOwnerScientist() != null) {
            if (!instrument.getOwnerScientist().getId().equals(authenticatedScientist.getId())) {
                throw new SecurityException("Forbidden: You are not the owner of this instrument and cannot send it to maintenance.");
            }
        }

        // Lock instrument status to MAINTENANCE
        instrument.setStatus(Instrument.Status.MAINTENANCE);
        instrumentRepository.save(instrument);

        if (record.getOutDate() == null) {
            record.setOutDate(LocalDate.now());
        }

        return maintenanceRecordRepository.save(record);
    }

    /**
     * Receive back instrument from vendor, attach certificate, and make item AVAILABLE.
     * SECURED: Only the Instrument OWNER can process the return from maintenance.
     */
    public Optional<MaintenanceRecord> returnFromMaintenance(String id, String certificatePath) {
        MaintenanceRecord record = maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found with ID: " + id));

        Person authenticatedScientist = getAuthenticatedScientist();

        Instrument instrument = record.getInstrument();

        // Check Ownership: Ensure the caller is the owner of the instrument
        if (authenticatedScientist != null && instrument != null && instrument.getOwnerScientist() != null) {
            if (!instrument.getOwnerScientist().getId().equals(authenticatedScientist.getId())) {
                throw new SecurityException("Forbidden: Only the owner scientist can process the return from maintenance.");
            }
        }

        record.setReturnedOn(LocalDate.now());
        if (certificatePath != null && !certificatePath.trim().isEmpty()) {
            record.setCertificatePath(certificatePath);
        }

        // Restore instrument status back to AVAILABLE
        if (instrument != null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
            instrumentRepository.save(instrument);
        }

        return Optional.of(maintenanceRecordRepository.save(record));
    }

    /**
     * Helper to extract the authenticated Person principal from SecurityContextHolder
     */
    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}