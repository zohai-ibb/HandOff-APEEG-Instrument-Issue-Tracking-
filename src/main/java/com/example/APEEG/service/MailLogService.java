package com.example.APEEG.service;

import com.example.APEEG.model.MailLog;
import com.example.APEEG.repository.MailLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailLogService {

    private final MailLogRepository mailLogRepository;

    // Inject Repository via Constructor
    public MailLogService(MailLogRepository mailLogRepository) {
        this.mailLogRepository = mailLogRepository;
    }

    // Retrieve full communication history
    public List<MailLog> getAllLogs() {
        return mailLogRepository.findAll();
    }

    // Retrieve logs for a specific issue record
    public List<MailLog> getByIssueRecordId(String issueRecordId) {
        return mailLogRepository.findByIssueRecordId(issueRecordId);
    }

    // Retrieve logs for a specific instrument
    public List<MailLog> getByInstrumentId(String instrumentId) {
        return mailLogRepository.findByInstrumentId(instrumentId);
    }

    // Retrieve failed email attempts for monitoring
    public List<MailLog> getFailedLogs() {
        return mailLogRepository.findByDeliveryStatus(MailLog.DeliveryStatus.FAILED);
    }

    // Persist a new mail dispatch attempt to MongoDB
    public MailLog recordMailAttempt(MailLog mailLog) {
        return mailLogRepository.save(mailLog);
    }
}