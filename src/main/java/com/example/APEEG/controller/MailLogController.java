package com.example.APEEG.controller;

import com.example.APEEG.model.MailLog;
import com.example.APEEG.service.MailLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * GET /api/mail-logs
     * Returns only the mail logs relevant to the logged-in scientist.
     */
    @GetMapping
    public ResponseEntity<?> getAllLogsForLoggedInScientist() {
        try {
            List<MailLog> logs = mailLogService.getMyMailLogs();
            return ResponseEntity.ok(logs);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * GET /api/mail-logs/instrument/{instrumentId}
     * Returns mail logs for an instrument ONLY if the logged-in scientist is the owner.
     */
    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<?> getByInstrument(@PathVariable String instrumentId) {
        try {
            List<MailLog> logs = mailLogService.getByInstrumentId(instrumentId);
            return ResponseEntity.ok(logs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}