package com.example.APEEG.service;

import com.example.APEEG.model.MailLog;
import com.example.APEEG.repository.MailLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailLogService {

    private final MailLogRepository mailLogRepository;

    public MailLogService(MailLogRepository mailLogRepository) {
        this.mailLogRepository = mailLogRepository;
    }

    public List<MailLog> getAllLogs() {
        return mailLogRepository.findAll();
    }

    public List<MailLog> getByIssueRecordId(String issueRecordId) {
        return mailLogRepository.findByIssueRecordId(issueRecordId);
    }

    public MailLog logMail(MailLog mailLog) {
        return mailLogRepository.save(mailLog);
    }
}