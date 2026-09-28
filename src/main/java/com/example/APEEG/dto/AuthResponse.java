package com.example.APEEG.dto;

import com.example.APEEG.model.Person;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object returned upon successful scientist authentication.
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    // Signed JWT token to be attached in Bearer header on future requests
    private String token;

    // Authenticated scientist profile document
    private Person scientist;
}