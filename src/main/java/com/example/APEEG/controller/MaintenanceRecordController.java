package com.example.APEEG.controller;

import com.example.APEEG.model.MaintenanceRecord;
import com.example.APEEG.service.MaintenanceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-records")
@CrossOrigin(origins = "*")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping
    public List<MaintenanceRecord> getAll() {
        return maintenanceRecordService.getAllMaintenanceRecords();
    }

    @GetMapping("/instrument/{instrumentId}")
    public List<MaintenanceRecord> getByInstrument(@PathVariable String instrumentId) {
        return maintenanceRecordService.getByInstrumentId(instrumentId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MaintenanceRecord record) {
        try {
            MaintenanceRecord saved = maintenanceRecordService.sendToMaintenance(record);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnFromMaintenance(
            @PathVariable String id,
            @RequestParam(required = false) String certificatePath) {

        try {
            return maintenanceRecordService.returnFromMaintenance(id, certificatePath)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}