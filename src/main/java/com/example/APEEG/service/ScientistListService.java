package com.example.APEEG.service;

import com.example.APEEG.model.Person;
import com.example.APEEG.model.ScientistList;
import com.example.APEEG.repository.ScientistListRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScientistListService {

    private final ScientistListRepository scientistListRepository;

    public ScientistListService(ScientistListRepository scientistListRepository) {
        this.scientistListRepository = scientistListRepository;
    }

    /**
     * Adds a new scientist contact to the logged-in user's directory.
     */
    public ScientistList addScientist(ScientistList scientist) {
        Person loggedInUser = getAuthenticatedUser();
        if (loggedInUser == null) {
            throw new SecurityException("Unauthorized: User authentication required.");
        }

        if (scientist.getName() == null || scientist.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Scientist name is required.");
        }
        if (scientist.getEmail() == null || scientist.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Scientist email is required.");
        }

        // Link contact directly to the logged-in user
        scientist.setOwnerUser(loggedInUser);
        if (scientist.getDepartment() == null || scientist.getDepartment().trim().isEmpty()) {
            scientist.setDepartment("APEEG");
        }

        return scientistListRepository.save(scientist);
    }

    /**
     * Retrieves all scientist contacts created by the currently logged-in user.
     */
    public List<ScientistList> getMyScientists() {
        Person loggedInUser = getAuthenticatedUser();
        if (loggedInUser == null) {
            throw new SecurityException("Unauthorized: User authentication required.");
        }
        return scientistListRepository.findByOwnerUserId(loggedInUser.getId());
    }

    private Person getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}