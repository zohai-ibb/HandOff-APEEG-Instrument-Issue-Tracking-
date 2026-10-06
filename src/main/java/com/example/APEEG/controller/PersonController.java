package com.example.APEEG.controller;

import com.example.APEEG.model.Person;
import com.example.APEEG.service.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller exposing Person/Scientist profile endpoints.
 * Includes strict ownership checks for profile modifications.
 */
@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<Person> getAllPersons() {
        return personService.getAllPersons();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> getPersonById(@PathVariable String id) {
        return personService.getPersonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createPerson(@RequestBody Person person) {
        try {
            Person saved = personService.createPerson(person);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST: Upload or update profile photo for a Person.
     * Endpoint: POST /api/persons/{id}/photo
     * Header: Content-Type: multipart/form-data
     */
    @PostMapping(value = "/{id}/photo", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadProfilePhoto(
            @PathVariable String id,
            @RequestPart("photo") MultipartFile photo) {
        try {
            Person loggedInScientist = getAuthenticatedScientist();

            // Ownership check: Scientist can only upload photo to their own profile
            if (loggedInScientist != null && !loggedInScientist.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Forbidden: You are not authorized to update another scientist's profile photo.");
            }

            Person updatedPerson = personService.updateProfilePhoto(id, photo);
            return ResponseEntity.ok(updatedPerson);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile photo: " + e.getMessage());
        }
    }

    /**
     * PUT: Updates profile details.
     * Enforces that scientists can ONLY edit their own profile.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePerson(@PathVariable String id, @RequestBody Person details) {
        Person loggedInScientist = getAuthenticatedScientist();

        if (loggedInScientist != null && !loggedInScientist.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Forbidden: You are not authorized to update another scientist's profile.");
        }

        return personService.updatePerson(id, details)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE: Deletes a scientist profile.
     * Restricted to self-deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePerson(@PathVariable String id) {
        Person loggedInScientist = getAuthenticatedScientist();

        if (loggedInScientist != null && !loggedInScientist.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Forbidden: You cannot delete another scientist's account.");
        }

        if (personService.deletePerson(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}