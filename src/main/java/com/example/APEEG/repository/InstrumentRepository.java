// InstrumentRepository.java
package com.example.APEEG.repository;

import com.example.APEEG.model.Instrument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends MongoRepository<Instrument, String> {
    Optional<Instrument> findByAssetId(String assetId);
    List<Instrument> findByOwnerScientistId(String ownerId);
    List<Instrument> findByStatus(Instrument.Status status);
    List<Instrument> findByName(String name);
}