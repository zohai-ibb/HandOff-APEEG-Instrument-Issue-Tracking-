package com.example.APEEG.dto;

import lombok.Data;

/**
 * Data Transfer Object capturing credentials submitted during scientist login.
 */
@Data
public class AuthRequest {

    // Registered CSIR-CBRI scientist email
    private String email;

    // Raw plaintext password entered in login form
    private String password;
}