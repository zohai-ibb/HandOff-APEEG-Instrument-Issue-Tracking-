package com.example.APEEG.controller;

import com.example.APEEG.model.MailLog;
import com.example.APEEG.service.MailLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mail-logs")
@CrossOrigin(origins = "*")
public class MailLogController {

    private final MailLogService mailLogService;

    public MailLogController(MailLogService mailLogService) {
        this.mailLogService = mailLogService;
    }

    @GetMapping
    public List<MailLog> getAllLogs() {
        return mailLogService.getAllLogs();
    }

    @GetMapping("/issue-record/{issueRecordId}")
    public List<MailLog> getByIssueRecord(@PathVariable String issueRecordId) {
        return mailLogService.getByIssueRecordId(issueRecordId);
    }
}