// MaintenanceRecordRepository.java
package com.example.APEEG.repository;

import com.example.APEEG.model.MaintenanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends MongoRepository<MaintenanceRecord, String> {
    List<MaintenanceRecord> findByInstrumentId(String instrumentId);
}