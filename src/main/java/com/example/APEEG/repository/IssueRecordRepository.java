package com.example.APEEG.repository;

import com.example.APEEG.model.IssueRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRecordRepository extends MongoRepository<IssueRecord, String> {
    List<IssueRecord> findByBorrowerScientistId(String borrowerId);
    List<IssueRecord> findByOwnerScientistId(String ownerId);
    List<IssueRecord> findByInstrumentIdAndState(String instrumentId, IssueRecord.State state);
    List<IssueRecord> findByState(IssueRecord.State state);
}