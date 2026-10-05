package com.example.APEEG.repository;

import com.example.APEEG.model.ScientistList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScientistListRepository extends MongoRepository<ScientistList, String> {

    // Fetch all scientist contacts created by a specific user ID
    List<ScientistList> findByOwnerUserId(String ownerUserId);
}