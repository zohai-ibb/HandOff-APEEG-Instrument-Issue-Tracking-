package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.Person;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.service.FileStorageService;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final FileStorageService fileStorageService;

    public InstrumentService(InstrumentRepository instrumentRepository,
                             FileStorageService fileStorageService) {
        this.instrumentRepository = instrumentRepository;
        this.fileStorageService = fileStorageService;
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

        Person authenticatedScientist = getAuthenticatedScientist();

        if (authenticatedScientist == null) {
            throw new SecurityException("Unauthorized: No authenticated scientist found in session context.");
        }

        instrument.setOwnerScientist(authenticatedScientist);

        if (instrument.getStatus() == null) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
        }

        return instrumentRepository.save(instrument);
    }

    public Instrument updateInstrument(String id, Instrument details) {
        Instrument existing = instrumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + id));

        Person loggedInScientist = getAuthenticatedScientist();

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

        if (loggedInScientist != null && existing.getOwnerScientist() != null) {
            if (!existing.getOwnerScientist().getId().equals(loggedInScientist.getId())) {
                throw new SecurityException("Forbidden: You are not the owner of this instrument and cannot delete it.");
            }
        }

        instrumentRepository.deleteById(id);
    }

    /**
     * Creates an instrument record with photo, purchase date, and cost details.
     */
    public Instrument createInstrumentWithPhoto(
            String assetId,
            String name,
            String make,
            String serialNo,
            String location,
            LocalDate calibrationValidTo,
            LocalDate purchaseDate,
            BigDecimal purchaseCost,
            String status,
            Integer quantity,
            MultipartFile photo) throws IOException {

        if (assetId != null && instrumentRepository.findByAssetId(assetId).isPresent()) {
            throw new IllegalArgumentException("Asset ID '" + assetId + "' is already registered.");
        }

        Person authenticatedOwner = getAuthenticatedScientist();

        Instrument instrument = new Instrument();
        instrument.setAssetId(assetId);
        instrument.setName(name);
        instrument.setMake(make);
        instrument.setSerialNo(serialNo);
        instrument.setLocation(location);
        instrument.setCalibrationValidTo(calibrationValidTo);
        instrument.setPurchaseDate(purchaseDate);
        instrument.setPurchaseCost(purchaseCost);
        instrument.setQuantity(quantity != null ? quantity : 1);
        instrument.setOwnerScientist(authenticatedOwner);

        try {
            instrument.setStatus(status != null ? Instrument.Status.valueOf(status.toUpperCase()) : Instrument.Status.AVAILABLE);
        } catch (IllegalArgumentException e) {
            instrument.setStatus(Instrument.Status.AVAILABLE);
        }

        if (photo != null && !photo.isEmpty()) {
            String photoPath = fileStorageService.saveFile(photo);
            instrument.setPhotoPath(photoPath);
        }

        return instrumentRepository.save(instrument);
    }

    public Instrument updateInstrumentWithPhoto(
            String id,
            String assetId,
            String name,
            String make,
            String serialNo,
            String location,
            LocalDate calibrationValidTo,
            LocalDate purchaseDate,
            BigDecimal purchaseCost,
            MultipartFile photo) throws IOException {

        Instrument existing = instrumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + id));

        // Update basic text attributes if provided
        if (assetId != null && !assetId.trim().isEmpty()) existing.setAssetId(assetId.trim());
        if (name != null && !name.trim().isEmpty()) existing.setName(name.trim());
        if (make != null) existing.setMake(make.trim());
        if (serialNo != null) existing.setSerialNo(serialNo.trim());
        if (location != null) existing.setLocation(location.trim());

        // Update dates & costs
        existing.setCalibrationValidTo(calibrationValidTo);
        existing.setPurchaseDate(purchaseDate);
        existing.setPurchaseCost(purchaseCost);

        // PRESERVE EXISTING PHOTO: Only overwrite photoPath if a NEW photo file is uploaded
        if (photo != null && !photo.isEmpty()) {
            String newPhotoPath = fileStorageService.saveFile(photo);
            existing.setPhotoPath(newPhotoPath);
        }

        return instrumentRepository.save(existing);
    }


    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}