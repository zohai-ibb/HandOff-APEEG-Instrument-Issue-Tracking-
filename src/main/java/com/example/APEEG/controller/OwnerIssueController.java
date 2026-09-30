package com.example.APEEG.controller;

import com.example.APEEG.dto.OwnerIssueRequestDTO;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.service.OwnerIssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing API endpoints for owner-initiated instrument issuing.
 */
@RestController
@RequestMapping("/api/issue-records/owner-issue")
@CrossOrigin(origins = "*")
public class OwnerIssueController {

    private final OwnerIssueService ownerIssueService;

    public OwnerIssueController(OwnerIssueService ownerIssueService) {
        this.ownerIssueService = ownerIssueService;
    }

    /**
     * POST /api/issue-records/owner-issue
     * Initiates instrument checkout directly from the owner's inventory for a selected registered scientist.
     */
    @PostMapping
    public ResponseEntity<?> issueToScientist(@RequestBody OwnerIssueRequestDTO dto) {
        try {
            IssueRecord createdRecord = ownerIssueService.issueToInternalScientist(
                    dto.getInstrumentId(),
                    dto.getBorrowerScientistId(),
                    dto.getStaffName(),
                    dto.getStaffEmail(),
                    dto.getDueDate(),
                    dto.getPurpose(),
                    dto.getConditionOut()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while processing the issue record: " + e.getMessage());
        }
    }
}