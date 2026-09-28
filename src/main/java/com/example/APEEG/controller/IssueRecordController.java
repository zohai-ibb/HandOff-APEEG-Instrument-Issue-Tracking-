package com.example.APEEG.controller;

import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.service.IssueRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing checkout and return APIs.
 */
@RestController
@RequestMapping("/api/issue-records")
public class IssueRecordController {

    private final IssueRecordService issueRecordService;

    public IssueRecordController(IssueRecordService issueRecordService) {
        this.issueRecordService = issueRecordService;
    }

    @GetMapping
    public List<IssueRecord> getAllRecords() {
        return issueRecordService.getAllRecords();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueRecord> getById(@PathVariable String id) {
        return issueRecordService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/borrower/{borrowerId}")
    public List<IssueRecord> getByBorrower(@PathVariable String borrowerId) {
        return issueRecordService.getByBorrowerScientistId(borrowerId);
    }

    @GetMapping("/owner/{ownerId}")
    public List<IssueRecord> getByOwner(@PathVariable String ownerId) {
        return issueRecordService.getByOwnerScientistId(ownerId);
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

    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnInstrument(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {

        try {
            String conditionIn = (payload != null && payload.containsKey("condition_in"))
                    ? payload.get("condition_in")
                    : "Returned intact";

            return issueRecordService.returnInstrument(id, conditionIn)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            // Returns HTTP 403 Forbidden if an unauthorized peer tries to process the return
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}