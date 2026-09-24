package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.repository.InstrumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public List<Instrument> getAllInstruments() {
        return instrumentRepository.findAll();
    }

    public Optional<Instrument> getInstrumentById(String id) {
        return instrumentRepository.findById(id);
    }

    /**
     * Look up an instrument by scanning its physical QR code (Asset ID)
     * @param assetId e.g. "CBRI/APEEG/0121"
     */
    public Optional<Instrument> getByAssetId(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return Optional.empty();
        }
        return instrumentRepository.findByAssetId(assetId.trim());
    }

    public List<Instrument> getByName(String name) {
        return instrumentRepository.findByName(name);
    }

    public List<Instrument> getByOwnerScientistId(String ownerId) {
        return instrumentRepository.findByOwnerScientistId(ownerId);
    }

    public List<Instrument> getByStatus(Instrument.Status status) {
        return instrumentRepository.findByStatus(status);
    }

    public Instrument createInstrument(Instrument instrument) {
        if (instrumentRepository.findByAssetId(instrument.getAssetId()).isPresent()) {
            throw new IllegalArgumentException("Asset ID " + instrument.getAssetId() + " is already registered.");
        }
        if (instrument.getStatus() == null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
        }
        return instrumentRepository.save(instrument);
    }

    public Optional<Instrument> updateInstrument(String id, Instrument details) {
        return instrumentRepository.findById(id).map(existing -> {
            existing.setName(details.getName());
            existing.setMake(details.getMake());
            existing.setSerialNo(details.getSerialNo());
            existing.setQuantity(details.getQuantity());
            existing.setLocation(details.getLocation());
            existing.setStatus(details.getStatus());
            existing.setCalibrationValidTo(details.getCalibrationValidTo());
            existing.setPurchaseDate(details.getPurchaseDate());
            existing.setPurchaseCost(details.getPurchaseCost());
            existing.setAccessories(details.getAccessories());
            if (details.getOwnerScientist() != null) {
                existing.setOwnerScientist(details.getOwnerScientist());
            }
            return instrumentRepository.save(existing);
        });
    }

    public boolean deleteInstrument(String id) {
        if (instrumentRepository.existsById(id)) {
            instrumentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}