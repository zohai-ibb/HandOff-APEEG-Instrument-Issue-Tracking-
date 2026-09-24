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

    // GET: Fetch all issue records
    @GetMapping
    public List<IssueRecord> getAllRecords() {
        return issueRecordService.getAllRecords();
    }

    // GET: Fetch record by Mongo document ID
    @GetMapping("/{id}")
    public ResponseEntity<IssueRecord> getById(@PathVariable String id) {
        return issueRecordService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET: Fetch records where a specific scientist is the borrower
    @GetMapping("/borrower/{borrowerId}")
    public List<IssueRecord> getByBorrower(@PathVariable String borrowerId) {
        return issueRecordService.getByBorrowerScientistId(borrowerId);
    }

    // GET: Fetch records where a specific scientist is the owner
    @GetMapping("/owner/{ownerId}")
    public List<IssueRecord> getByOwner(@PathVariable String ownerId) {
        return issueRecordService.getByOwnerScientistId(ownerId);
    }

    // GET: Filter records by State enum (OPEN, RETURNED, CANCELLED)
    @GetMapping("/state/{state}")
    public List<IssueRecord> getByState(@PathVariable IssueRecord.State state) {
        return issueRecordService.getByState(state);
    }

    // POST: Create direct issue (No approval required)
    @PostMapping
    public ResponseEntity<?> createIssueRecord(@RequestBody IssueRecord record) {
        try {
            IssueRecord saved = issueRecordService.createDirectIssue(record);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT: Process return of an instrument
    @PutMapping("/{id}/return")
    public ResponseEntity<IssueRecord> returnInstrument(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {

        String conditionIn = (payload != null && payload.containsKey("condition_in"))
                ? payload.get("condition_in")
                : "Returned intact";

        return issueRecordService.returnInstrument(id, conditionIn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}