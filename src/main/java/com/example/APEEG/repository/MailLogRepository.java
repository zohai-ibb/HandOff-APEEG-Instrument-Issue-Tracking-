// MailLogRepository.java
package com.example.APEEG.repository;

import com.example.APEEG.model.MailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailLogRepository extends MongoRepository<MailLog, String> {
    List<MailLog> findByIssueRecordId(String issueRecordId);
}