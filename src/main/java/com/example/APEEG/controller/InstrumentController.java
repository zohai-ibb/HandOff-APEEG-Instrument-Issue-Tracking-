package com.example.APEEG.controller;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.service.InstrumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public List<Instrument> getAllInstruments() {
        return instrumentService.getAllInstruments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Instrument> getInstrumentById(@PathVariable String id) {
        return instrumentService.getInstrumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/scan")
    public ResponseEntity<Instrument> getByAssetId(@RequestParam String assetId) {
        return instrumentService.getByAssetId(assetId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/owner/{ownerId}")
    public List<Instrument> getByOwner(@PathVariable String ownerId) {
        return instrumentService.getByOwnerScientistId(ownerId);
    }

    @GetMapping("/status/{status}")
    public List<Instrument> getByStatus(@PathVariable Instrument.Status status) {
        return instrumentService.getByStatus(status);
    }

    @PostMapping
    public ResponseEntity<?> createInstrument(@RequestBody Instrument instrument) {
        try {
            Instrument saved = instrumentService.createInstrument(instrument);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateInstrument(@PathVariable String id, @RequestBody Instrument details) {
        try {
            Instrument updated = instrumentService.updateInstrument(id, details);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInstrument(@PathVariable String id) {
        try {
            instrumentService.deleteInstrument(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Standard JSON Endpoint (Without photo upload)
     * Header: Content-Type: application/json
     */
    @PostMapping(consumes = {"application/json"})
    public ResponseEntity<?> createInstrumentJson(@RequestBody Instrument instrument) {
        try {
            Instrument saved = instrumentService.createInstrument(instrument);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Multipart Form-Data Endpoint (With photo, purchase date & cost upload)
     * Header: Content-Type: multipart/form-data
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createInstrumentWithPhoto(
            @RequestParam("asset_id") String assetId,
            @RequestParam("name") String name,
            @RequestParam("make") String make,
            @RequestParam(value = "serial_no", required = false) String serialNo,
            @RequestParam("location") String location,
            @RequestParam(value = "calibration_valid_to", required = false) String calibrationValidTo,
            @RequestParam(value = "purchase_date", required = false) String purchaseDate,
            @RequestParam(value = "purchase_cost", required = false) BigDecimal purchaseCost,
            @RequestParam(value = "status", required = false, defaultValue = "AVAILABLE") String status,
            @RequestParam(value = "quantity", required = false, defaultValue = "1") Integer quantity,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        try {
            if (assetId == null || assetId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Validation Error: asset_id is required.");
            }

            LocalDate parsedCalibrationDate = (calibrationValidTo != null && !calibrationValidTo.trim().isEmpty())
                    ? LocalDate.parse(calibrationValidTo.trim())
                    : null;

            LocalDate parsedPurchaseDate = (purchaseDate != null && !purchaseDate.trim().isEmpty())
                    ? LocalDate.parse(purchaseDate.trim())
                    : null;

            Instrument saved = instrumentService.createInstrumentWithPhoto(
                    assetId.trim(),
                    name.trim(),
                    make.trim(),
                    serialNo != null ? serialNo.trim() : null,
                    location.trim(),
                    parsedCalibrationDate,
                    parsedPurchaseDate,
                    purchaseCost,
                    status,
                    quantity,
                    photo
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add instrument: " + e.getMessage());
        }
    }
}