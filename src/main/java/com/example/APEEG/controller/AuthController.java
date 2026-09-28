package com.example.APEEG.controller;

import com.example.APEEG.dto.AuthRequest;
import com.example.APEEG.dto.AuthResponse;
import com.example.APEEG.model.Person;
import com.example.APEEG.security.JwtTokenProvider;
import com.example.APEEG.service.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller exposing endpoints for scientist registration and authentication.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final PersonService personService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(PersonService personService, JwtTokenProvider tokenProvider) {
        this.personService = personService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * POST: Handles scientist sign-up. Encrypts password and stores Person document.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerScientist(@RequestBody Person person) {
        try {
            // Save new scientist (PersonService handles BCrypt encoding)
            Person registered = personService.createPerson(person);

            // Issue token immediately upon registration
            String token = tokenProvider.generateToken(registered.getId(), registered.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, registered));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST: Authenticates scientist credentials and returns JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        // Verify email and raw password against stored BCrypt hash
        Optional<Person> personOpt = personService.authenticate(authRequest.getEmail(), authRequest.getPassword());

        if (personOpt.isPresent()) {
            Person scientist = personOpt.get();
            // Generate token upon matching credentials
            String token = tokenProvider.generateToken(scientist.getId(), scientist.getEmail());

            return ResponseEntity.ok(new AuthResponse(token, scientist));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
    }
}