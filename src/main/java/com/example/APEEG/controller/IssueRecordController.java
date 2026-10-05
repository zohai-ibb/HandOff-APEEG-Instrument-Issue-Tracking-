package com.example.APEEG.controller;

import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.service.IssueRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issue-records")
@CrossOrigin(origins = "*")
public class IssueRecordController {

    private final IssueRecordService issueRecordService;

    public IssueRecordController(IssueRecordService issueRecordService) {
        this.issueRecordService = issueRecordService;
    }

    @GetMapping
    public ResponseEntity<?> getAllRecordsForCallingScientist() {
        try {
            List<IssueRecord> records = issueRecordService.getMyIssueRecords();
            return ResponseEntity.ok(records);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return issueRecordService.getByIdSecured(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<?> getByBorrower(@PathVariable String borrowerId) {
        try {
            return ResponseEntity.ok(issueRecordService.getByBorrowerScientistId(borrowerId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<?> getByOwner(@PathVariable String ownerId) {
        try {
            return ResponseEntity.ok(issueRecordService.getByOwnerScientistId(ownerId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createIssueRecord(@RequestBody IssueRecord record) {
        try {
            IssueRecord saved = issueRecordService.createDirectIssue(record);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 1. JSON Return Endpoint
     * Body JSON: { "condition_in": "GOOD" } or { "condition_in": "BAD" }
     */
    @PutMapping(value = "/{id}/return", consumes = {"application/json"})
    public ResponseEntity<?> processReturnJson(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {

        try {
            String conditionIn = (payload != null && payload.containsKey("condition_in"))
                    ? payload.get("condition_in")
                    : "GOOD";

            return issueRecordService.returnInstrument(id, conditionIn, null)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process return: " + e.getMessage());
        }
    }

    /**
     * 2. Multipart Form-Data Return Endpoint (With Condition Photo Upload)
     * Form Field: condition_in = "GOOD" or "BAD"
     * Form File: photo = image binary
     */
    @PutMapping(value = "/{id}/return/photo", consumes = {"multipart/form-data"})
    public ResponseEntity<?> processReturnWithPhoto(
            @PathVariable String id,
            @RequestParam(value = "condition_in", required = false, defaultValue = "GOOD") String conditionIn,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        try {
            return issueRecordService.returnInstrument(id, conditionIn, photo)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process return: " + e.getMessage());
        }
    }
}