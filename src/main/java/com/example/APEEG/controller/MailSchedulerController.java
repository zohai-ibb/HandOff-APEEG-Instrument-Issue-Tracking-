package com.example.APEEG.controller;

import com.example.APEEG.service.MailSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller providing REST access to execute background scheduled jobs on demand during development.
 */
@RestController
@RequestMapping("/api/scheduler")
@CrossOrigin(origins = "*")
public class MailSchedulerController {

    private final MailSchedulerService mailSchedulerService;

    public MailSchedulerController(MailSchedulerService mailSchedulerService) {
        this.mailSchedulerService = mailSchedulerService;
    }

    /**
     * POST /api/scheduler/run-daily-job
     * Triggers the daily overdue, advance notice, and calibration check engine immediately.
     */
    @PostMapping("/run-daily-job")
    public ResponseEntity<String> triggerDailyJob() {
        mailSchedulerService.runDailyReminderEngine();
        return ResponseEntity.ok("Daily scheduled mail engine executed successfully.");
    }
}