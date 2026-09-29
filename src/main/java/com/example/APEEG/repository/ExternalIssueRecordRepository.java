package com.example.APEEG.repository;

import com.example.APEEG.model.ExternalIssueRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalIssueRecordRepository extends MongoRepository<ExternalIssueRecord, String> {

    // Fetch external loan records created by a specific owner scientist
    List<ExternalIssueRecord> findByOwnerScientistId(String ownerId);

    // Fetch records by loan status (OPEN, RETURNED, CANCELLED)
    List<ExternalIssueRecord> findByState(ExternalIssueRecord.State state);

    // Fetch loan records by external borrower email
    List<ExternalIssueRecord> findByExternalBorrowerEmail(String email);
}