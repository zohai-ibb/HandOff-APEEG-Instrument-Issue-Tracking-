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
            IssueRecord saved = issueRecordService.createIssueRecord(record);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<IssueRecord> returnInstrument(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {

        String conditionIn = payload.getOrDefault("conditionIn", "Returned intact");
        return issueRecordService.returnInstrument(id, conditionIn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}