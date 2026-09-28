// This file is only for testing purpose to check weather mail logs are working otherwise every mail log will be maintained at 9AM daily

package com.example.APEEG.controller;

import com.example.APEEG.service.MailSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
public class MailSchedulerController {

    private final MailSchedulerService mailSchedulerService;

    public MailSchedulerController(MailSchedulerService mailSchedulerService) {
        this.mailSchedulerService = mailSchedulerService;
    }

    // POST: Manual trigger to run the daily reminder engine immediately
    @PostMapping("/run-daily-job")
    public ResponseEntity<String> triggerDailyJob() {
        mailSchedulerService.runDailyReminderEngine();
        return ResponseEntity.ok("Daily scheduled mail engine executed successfully.");
    }
}