package com.example.APEEG.repository;

import com.example.APEEG.model.MailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailLogRepository extends MongoRepository<MailLog, String> {

    // Fetch all mail logs associated with a specific checkout record ID
    List<MailLog> findByIssueRecordId(String issueRecordId);

    // Fetch all mail logs associated with a specific instrument ID
    List<MailLog> findByInstrumentId(String instrumentId);

    // Fetch mail logs filtered by delivery status (SENT or FAILED)
    List<MailLog> findByDeliveryStatus(MailLog.DeliveryStatus deliveryStatus);
}