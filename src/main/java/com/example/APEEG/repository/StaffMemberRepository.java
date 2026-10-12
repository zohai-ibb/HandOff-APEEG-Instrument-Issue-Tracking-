package com.example.APEEG.repository;

import com.example.APEEG.model.StaffMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffMemberRepository extends MongoRepository<StaffMember, String> {

    // Fetch all staff members belonging to the logged-in scientist
    List<StaffMember> findByOwnerScientistId(String ownerScientistId);

    // Optional: Search by email under a specific owner scientist to avoid duplicate entries
    Optional<StaffMember> findByOwnerScientistIdAndEmail(String ownerScientistId, String email);
}