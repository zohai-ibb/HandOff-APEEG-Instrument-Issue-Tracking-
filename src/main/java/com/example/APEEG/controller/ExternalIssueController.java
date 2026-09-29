package com.example.APEEG.controller;

import com.example.APEEG.model.ExternalIssueRecord;
import com.example.APEEG.service.ExternalIssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external-issue-records")
public class ExternalIssueController {

    private final ExternalIssueService externalIssueService;

    public ExternalIssueController(ExternalIssueService externalIssueService) {
        this.externalIssueService = externalIssueService;
    }

    // GET: Retrieve all external issue records
    @GetMapping
    public List<ExternalIssueRecord> getAllRecords() {
        return externalIssueService.getAllRecords();
    }

    // GET: Retrieve single external record by MongoDB ID
    @GetMapping("/{id}")
    public ResponseEntity<ExternalIssueRecord> getById(@PathVariable String id) {
        return externalIssueService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET: Retrieve external records owned by a specific scientist ID
    @GetMapping("/owner/{ownerId}")
    public List<ExternalIssueRecord> getByOwner(@PathVariable String ownerId) {
        return externalIssueService.getByOwnerScientistId(ownerId);
    }

    // POST: Create external issue record (Owner issues to non-app scientist)
    @PostMapping
    public ResponseEntity<?> createExternalIssue(@RequestBody ExternalIssueRecord record) {
        try {
            ExternalIssueRecord saved = externalIssueService.createExternalIssue(record);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // PUT: Process return of an externally issued instrument
    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnExternalInstrument(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {

        String conditionIn = (payload != null && payload.containsKey("condition_in"))
                ? payload.get("condition_in")
                : "Returned intact";

        try {
            return externalIssueService.returnExternalInstrument(id, conditionIn)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}