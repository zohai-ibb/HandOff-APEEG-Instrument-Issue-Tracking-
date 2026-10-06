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
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public PersonService(PersonRepository personRepository, FileStorageService fileStorageService) {
        this.personRepository = personRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.fileStorageService = fileStorageService;
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
        if (person.getEmail() != null && personRepository.findByEmail(person.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Scientist with email " + person.getEmail() + " is already registered.");
        }

        // Encrypt password before persisting to MongoDB
        if (person.getPassword() != null && !person.getPassword().isEmpty()) {
            person.setPassword(passwordEncoder.encode(person.getPassword()));
        }

        if (person.getDepartment() == null || person.getDepartment().trim().isEmpty()) {
            person.setDepartment("APEEG");
        }

        return personRepository.save(person);
    }

    /**
     * Handles physical file storage and updates photoPath in MongoDB
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

            // Hash new password if supplied during profile update
            if (details.getPassword() != null && !details.getPassword().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(details.getPassword()));
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

    /**
     * Authenticates a Scientist using email and raw password against the stored BCrypt hash.
     */
    public Optional<Person> authenticate(String email, String rawPassword) {
        Optional<Person> personOpt = personRepository.findByEmail(email);
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            if (person.getPassword() != null && passwordEncoder.matches(rawPassword, person.getPassword())) {
                return Optional.of(person);
            }
        }
        return Optional.empty();
    }
}