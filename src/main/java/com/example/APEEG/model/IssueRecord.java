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
        OPEN,       // Active checkout (Direct issue, no approval required)
        RETURNED,   // Instrument checked back in
        CANCELLED   // Transaction voided/revoked
    }

    @Id
    private String id;

    @DBRef
    private Instrument instrument;

    @DBRef
    @Field("borrower_scientist")
    @JsonProperty("borrower_scientist")
    private Person borrowerScientist;

    @DBRef
    @Field("owner_scientist")
    @JsonProperty("owner_scientist")
    private Person ownerScientist;

    @Field("staff_name")
    @JsonProperty("staff_name")
    private String staffName;

    @Field("staff_email")
    @JsonProperty("staff_email")
    private String staffEmail;

    @Field("issue_date")
    @JsonProperty("issue_date")
    private LocalDate issueDate;

    @Field("due_date")
    @JsonProperty("due_date")
    private LocalDate dueDate;

    private String purpose;

    @Field("condition_out")
    @JsonProperty("condition_out")
    private String conditionOut;

    @Field("condition_in")
    @JsonProperty("condition_in")
    private String conditionIn;

    @Field("actual_return_date")
    @JsonProperty("actual_return_date")
    private LocalDate actualReturnDate;

    private State state = State.OPEN; // Defaults directly to OPEN

    @CreatedDate
    @Field("created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

//    public IssueRecord() {}
//
//    // --- Getters and Setters ---
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public Instrument getInstrument() { return instrument; }
//    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
//
//    public Person getBorrowerScientist() { return borrowerScientist; }
//    public void setBorrowerScientist(Person borrowerScientist) { this.borrowerScientist = borrowerScientist; }
//
//    public Person getOwnerScientist() { return ownerScientist; }
//    public void setOwnerScientist(Person ownerScientist) { this.ownerScientist = ownerScientist; }
//
//    public String getStaffName() { return staffName; }
//    public void setStaffName(String staffName) { this.staffName = staffName; }
//
//    public String getStaffEmail() { return staffEmail; }
//    public void setStaffEmail(String staffEmail) { this.staffEmail = staffEmail; }
//
//    public LocalDate getIssueDate() { return issueDate; }
//    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
//
//    public LocalDate getDueDate() { return dueDate; }
//    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
//
//    public String getPurpose() { return purpose; }
//    public void setPurpose(String purpose) { this.purpose = purpose; }
//
//    public String getConditionOut() { return conditionOut; }
//    public void setConditionOut(String conditionOut) { this.conditionOut = conditionOut; }
//
//    public String getConditionIn() { return conditionIn; }
//    public void setConditionIn(String conditionIn) { this.conditionIn = conditionIn; }
//
//    public LocalDate getActualReturnDate() { return actualReturnDate; }
//    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }
//
//    public State getState() { return state; }
//    public void setState(State state) { this.state = state; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}