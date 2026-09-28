package com.example.APEEG.controller;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.service.InstrumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}