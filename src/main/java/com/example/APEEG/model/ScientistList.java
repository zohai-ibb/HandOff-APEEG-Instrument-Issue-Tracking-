package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Model representing a custom/external Scientist contact added by a user.
 * Stored in the 'scientist_list' collection to keep each user's directory isolated from 'persons'.
 */
@Document(collection = "scientist_list")
@Data
public class ScientistList {

    @Id
    private String id;

    private String name;

    private String email;

    private String mobile;

    private String department;

    // References the registered app user (Person) who added this scientist contact
    @DBRef
    @Field("owner_user")
    @JsonProperty("owner_user")
    private Person ownerUser;

    @CreatedDate
    @Field("created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ScientistList() {}

    public ScientistList(String name, String email, String mobile, String department, Person ownerUser) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.department = department != null ? department : "APEEG";
        this.ownerUser = ownerUser;
        this.createdAt = LocalDateTime.now();
    }
}