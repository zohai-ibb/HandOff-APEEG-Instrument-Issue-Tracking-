package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.MaintenanceRecord;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.MaintenanceRecordRepository;
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

    public MaintenanceRecord sendToMaintenance(MaintenanceRecord record) {
        Instrument instrument = instrumentRepository.findById(record.getInstrument().getId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found."));

        // Change instrument status to MAINTENANCE
        instrument.setStatus(Instrument.Status.MAINTENANCE);
        instrumentRepository.save(instrument);

        if (record.getOutDate() == null) {
            record.setOutDate(LocalDate.now());
        }

        return maintenanceRecordRepository.save(record);
    }

    public Optional<MaintenanceRecord> returnFromMaintenance(String id, String certificatePath) {
        return maintenanceRecordRepository.findById(id).map(record -> {
            record.setReturnedOn(LocalDate.now());
            if (certificatePath != null) {
                record.setCertificatePath(certificatePath);
            }

            // Restore instrument status to AVAILABLE
            Instrument instrument = record.getInstrument();
            if (instrument != null) {
                instrument.setStatus(Instrument.Status.AVAILABLE);
                instrumentRepository.save(instrument);
            }

            return maintenanceRecordRepository.save(record);
        });
    }
}