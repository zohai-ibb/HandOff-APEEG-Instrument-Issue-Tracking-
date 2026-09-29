package com.example.APEEG.controller;

import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.service.IssueRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * GET /api/issue-records
     * Returns ONLY records related to the calling scientist (as Borrower or Owner).
     */
    @GetMapping
    public ResponseEntity<?> getAllRecordsForCallingScientist() {
        try {
            List<IssueRecord> records = issueRecordService.getMyIssueRecords();
            return ResponseEntity.ok(records);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * GET /api/issue-records/{id}
     * Returns the record ONLY if the caller is the borrower or owner.
     */
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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}