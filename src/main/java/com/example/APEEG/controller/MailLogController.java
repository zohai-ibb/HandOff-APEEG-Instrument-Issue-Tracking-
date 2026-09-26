package com.example.APEEG.controller;

import com.example.APEEG.model.MailLog;
import com.example.APEEG.service.MailLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mail-logs")
@CrossOrigin(origins = "*")
public class MailLogController {

    private final MailLogService mailLogService;

    // Inject Service via Constructor
    public MailLogController(MailLogService mailLogService) {
        this.mailLogService = mailLogService;
    }

    // GET: Retrieve all logged email dispatches
    @GetMapping
    public List<MailLog> getAllLogs() {
        return mailLogService.getAllLogs();
    }

    // GET: Retrieve logs for a specific issue record
    @GetMapping("/issue-record/{issueRecordId}")
    public List<MailLog> getByIssueRecord(@PathVariable String issueRecordId) {
        return mailLogService.getByIssueRecordId(issueRecordId);
    }

    // GET: Retrieve logs for a specific instrument
    @GetMapping("/instrument/{instrumentId}")
    public List<MailLog> getByInstrument(@PathVariable String instrumentId) {
        return mailLogService.getByInstrumentId(instrumentId);
    }

    // GET: Retrieve all failed mail delivery attempts
    @GetMapping("/failed")
    public List<MailLog> getFailedLogs() {
        return mailLogService.getFailedLogs();
    }
}