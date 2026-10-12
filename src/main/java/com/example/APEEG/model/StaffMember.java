package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "staff_members")
@Data
public class StaffMember {

    @Id
    private String id;

    private String name;

    private String email;

    private String mobile;

    private String department;

    @DBRef
    @Field("owner_scientist")
    @JsonProperty("owner_scientist")
    private Person ownerScientist;

    @CreatedDate
    @Field("created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public StaffMember() {}

    public StaffMember(String name, String email, String mobile, String department, Person ownerScientist) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.department = department != null ? department : "APEEG";
        this.ownerScientist = ownerScientist;
        this.createdAt = LocalDateTime.now();
    }
}