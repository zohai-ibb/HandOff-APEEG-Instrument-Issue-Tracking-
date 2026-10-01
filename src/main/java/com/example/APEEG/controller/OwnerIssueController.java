package com.example.APEEG.controller;

import com.example.APEEG.dto.OwnerIssueRequestDTO; // Or your request payload DTO
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.service.OwnerIssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/issue-records/owner-issue")
@CrossOrigin(origins = "*")
public class OwnerIssueController {

    private final OwnerIssueService ownerIssueService;

    public OwnerIssueController(OwnerIssueService ownerIssueService) {
        this.ownerIssueService = ownerIssueService;
    }

    /**
     * 1. Standard JSON Endpoint (Postman / Web UI without file upload)
     * Endpoint: POST /api/issue-records/owner-issue
     * Header: Content-Type: application/json
     */
    @PostMapping(consumes = {"application/json"})
    public ResponseEntity<?> issueToScientistJson(@RequestBody OwnerIssueRequestDTO dto) {
        try {
            IssueRecord createdRecord = ownerIssueService.issueToInternalScientist(
                    dto.getInstrumentId(),
                    dto.getBorrowerScientistId(),
                    dto.getStaffName(),
                    dto.getStaffEmail(),
                    dto.getDueDate(),
                    dto.getPurpose(),
                    dto.getConditionOut(),
                    null // No photo attached for plain JSON request
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to issue instrument: " + e.getMessage());
        }
    }

    /**
     * 2. Multipart Form-Data Endpoint (When capturing/uploading a physical picture)
     * Endpoint: POST /api/issue-records/owner-issue/photo
     * Header: Content-Type: multipart/form-data
     */
    @PostMapping(value = "/photo", consumes = {"multipart/form-data"})
    public ResponseEntity<?> issueToScientistWithPhoto(
            @RequestParam("instrumentId") String instrumentId,
            @RequestParam("borrowerScientistId") String borrowerScientistId,
            @RequestParam(value = "staffName", required = false) String staffName,
            @RequestParam(value = "staffEmail", required = false) String staffEmail,
            @RequestParam(value = "dueDate", required = false) String dueDate,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "conditionOut", required = false) String conditionOut,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        try {
            LocalDate parsedDueDate = (dueDate != null && !dueDate.trim().isEmpty())
                    ? LocalDate.parse(dueDate)
                    : LocalDate.now().plusDays(14);

            IssueRecord createdRecord = ownerIssueService.issueToInternalScientist(
                    instrumentId,
                    borrowerScientistId,
                    staffName,
                    staffEmail,
                    parsedDueDate,
                    purpose,
                    conditionOut,
                    photo
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to issue instrument: " + e.getMessage());
        }
    }
}