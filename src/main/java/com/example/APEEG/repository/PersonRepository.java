// PersonRepository.java
package com.example.APEEG.repository;

import com.example.APEEG.model.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends MongoRepository<Person, String> {
    Optional<Person> findByEmail(String email);
}