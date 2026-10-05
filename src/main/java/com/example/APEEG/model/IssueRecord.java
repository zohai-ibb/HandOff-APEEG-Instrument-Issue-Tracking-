package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "issue_records")
@Data
public class IssueRecord {

    public enum State {
        OPEN,
        RETURNED,
        CANCELLED
    }

    @Id
    private String id;

    @DBRef
    private Instrument instrument;

    @DBRef
    @Field("owner_scientist")
    @JsonProperty("owner_scientist")
    private Person ownerScientist;

    // EMBEDDED SNAPSHOT: No @DBRef here so external contacts don't need a DB ID in 'persons'
    @Field("borrower_scientist")
    @JsonProperty("borrower_scientist")
    private Person borrowerScientist;

    @Field("staff_name")
    @JsonProperty("staff_name")
    private String staffName;

    @Field("staff_email")
    @JsonProperty("staff_email")
    private String staffEmail;

    @Field("issue_date")
    @JsonProperty("issue_date")
    private LocalDate issueDate = LocalDate.now();

    @Field("due_date")
    @JsonProperty("due_date")
    private LocalDate dueDate;

    @Field("actual_return_date")
    @JsonProperty("actual_return_date")
    private LocalDate actualReturnDate;

    private String purpose;

    @Field("condition_out")
    @JsonProperty("condition_out")
    private String conditionOut;

    @Field("condition_in")
    @JsonProperty("condition_in")
    private String conditionIn;

    @Field("condition_photo_path")
    @JsonProperty("condition_photo_path")
    private String conditionPhotoPath;

    private State state = State.OPEN;

    @CreatedDate
    @Field("created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public IssueRecord() {}
}