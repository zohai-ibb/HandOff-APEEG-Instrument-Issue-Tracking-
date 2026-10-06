package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "persons")
@Data
public class Person {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String mobile;
    private String department = "APEEG";

    // New profile photo field
    @Field("photo_path")
    @JsonProperty("photo_path")
    private String photoPath;

    @Field("is_active")
    private Boolean isActive = true;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

//    public Person() {}
//
//    public Person(String email, String id, String name, String mobile, String department, Boolean isActive) {
//        this.email = email;
//        this.id = id;
//        this.name = name;
//        this.mobile = mobile;
//        this.department = department;
//        this.isActive = isActive;
//    }
//
//    public Person(String id, String name, String email, String mobile) {
//        this.id = id;
//        this.name = name;
//        this.email = email;
//        this.mobile = mobile;
//        this.createdAt = LocalDateTime.now();
//    }
//
//    // --- Getters and Setters ---
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//
//    public String getMobile() { return mobile; }
//    public void setMobile(String mobile) { this.mobile = mobile; }
//
//    public String getDepartment() { return department; }
//    public void setDepartment(String department) { this.department = department; }
//
//    public Boolean getIsActive() { return isActive; }
//    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}