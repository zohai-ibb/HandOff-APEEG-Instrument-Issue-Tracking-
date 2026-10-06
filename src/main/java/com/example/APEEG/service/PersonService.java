package com.example.APEEG.service;

import com.example.APEEG.model.Person;
import com.example.APEEG.repository.PersonRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder passwordEncoder;

    public PersonService(PersonRepository personRepository, FileStorageService fileStorageService) {
        this.personRepository = personRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = new BCryptPasswordEncoder();
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
        if (person.getPassword() != null && !person.getPassword().isEmpty()) {
            person.setPassword(passwordEncoder.encode(person.getPassword()));
        }
        return personRepository.save(person);
    }

    /**
     * Upload and update profile photo for a Person
     */
    public Person updateProfilePhoto(String id, MultipartFile file) throws IOException {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found with id: " + id));

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.saveFile(file);
            person.setPhotoPath(path);
        }

        return personRepository.save(person);
    }

    public Optional<Person> updatePerson(String id, Person details) {
        return personRepository.findById(id).map(existing -> {
            existing.setName(details.getName());
            existing.setEmail(details.getEmail());
            existing.setMobile(details.getMobile());
            existing.setDepartment(details.getDepartment());
            if (details.getPhotoPath() != null) {
                existing.setPhotoPath(details.getPhotoPath());
            }
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