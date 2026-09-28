package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public Optional<Instrument> getByAssetId(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return Optional.empty();
        }
        return instrumentRepository.findByAssetId(assetId.trim());
    }

    public List<Instrument> getByOwnerScientistId(String ownerId) {
        return instrumentRepository.findByOwnerScientistId(ownerId);
    }

    public List<Instrument> getByStatus(Instrument.Status status) {
        return instrumentRepository.findByStatus(status);
    }

    /**
     * Creates a new instrument in the inventory.
     * SECURED: Automatically sets the authenticated user as the ownerScientist.
     */
    public Instrument createInstrument(Instrument instrument) {
        if (instrument.getAssetId() != null &&
                instrumentRepository.findByAssetId(instrument.getAssetId()).isPresent()) {
            throw new IllegalArgumentException("Asset ID " + instrument.getAssetId() + " is already registered.");
        }

        // 1. EXTRACT AUTHENTICATED SCIENTIST FROM JWT SECURITY CONTEXT
        Person authenticatedScientist = getAuthenticatedScientist();

        if (authenticatedScientist == null) {
            throw new SecurityException("Unauthorized: No authenticated scientist found in session context.");
        }

        // 2. OVERWRITE/ENFORCE OWNERSHIP: Always lock ownership to the token holder
        instrument.setOwnerScientist(authenticatedScientist);

        // 3. Set default status if missing
        if (instrument.getStatus() == null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
        }

        return instrumentRepository.save(instrument);
    }

    public Instrument updateInstrument(String id, Instrument details) {
        Instrument existing = instrumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + id));

        Person loggedInScientist = getAuthenticatedScientist();

        // Enforce Ownership Verification for Updates
        if (loggedInScientist != null && existing.getOwnerScientist() != null) {
            if (!existing.getOwnerScientist().getId().equals(loggedInScientist.getId())) {
                throw new SecurityException("Forbidden: You are not the owner of this instrument and cannot update it.");
            }
        }

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
        existing.setPhotoPath(details.getPhotoPath());
        existing.setManualPath(details.getManualPath());

        return instrumentRepository.save(existing);
    }

    public void deleteInstrument(String id) {
        Instrument existing = instrumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + id));

        Person loggedInScientist = getAuthenticatedScientist();

        // Enforce Ownership Verification for Deletions
        if (loggedInScientist != null && existing.getOwnerScientist() != null) {
            if (!existing.getOwnerScientist().getId().equals(loggedInScientist.getId())) {
                throw new SecurityException("Forbidden: You are not the owner of this instrument and cannot delete it.");
            }
        }

        instrumentRepository.deleteById(id);
    }

    /**
     * Helper method to retrieve the authenticated scientist from SecurityContextHolder
     */
    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}