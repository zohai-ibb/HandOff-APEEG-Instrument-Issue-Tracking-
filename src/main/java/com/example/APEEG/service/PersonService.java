package com.example.APEEG.service;

import com.example.APEEG.model.Person;
import com.example.APEEG.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    public Optional<Person> getPersonById(String id) {
        return personRepository.findById(id);
    }

    public Optional<Person> getPersonByEmail(String email) {
        return personRepository.findByEmail(email);
    }

    public Person createPerson(Person person) {
        if (personRepository.findByEmail(person.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Scientist with email " + person.getEmail() + " already exists.");
        }
        return personRepository.save(person);
    }

    public Optional<Person> updatePerson(String id, Person details) {
        return personRepository.findById(id).map(existing -> {
            existing.setName(details.getName());
            existing.setEmail(details.getEmail());
            existing.setMobile(details.getMobile());
            existing.setDepartment(details.getDepartment());
            if (details.getIsActive() != null) {
                existing.setIsActive(details.getIsActive());
            }
            return personRepository.save(existing);
        });
    }

    public boolean deletePerson(String id) {
        if (personRepository.existsById(id)) {
            personRepository.deleteById(id);
            return true;
        }
        return false;
    }
}