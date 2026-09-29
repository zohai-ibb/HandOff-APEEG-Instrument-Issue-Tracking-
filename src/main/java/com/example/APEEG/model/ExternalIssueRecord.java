package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an equipment loan to an external scientist who is not registered in the system.
 * Embedded string fields store contact details for the external borrower instead of @DBRef references.
 */
@Data
@Document(collection = "external_issue_records")
public class ExternalIssueRecord {

    public enum State {
        OPEN,       // Active checkout
        RETURNED,   // Checked back in by owner
        CANCELLED   // Transaction voided
    }

    @Id
    private String id; // MongoDB primary key

    // DBRef referencing the target instrument document
    @DBRef
    private Instrument instrument;

    // DBRef referencing the owning scientist (authenticated app user)
    @DBRef
    @Field("owner_scientist")
    @JsonProperty("owner_scientist")
    private Person ownerScientist;

    // Direct plain text attributes for non-registered external scientist
    @Field("external_borrower_name")
    @JsonProperty("external_borrower_name")
    private String externalBorrowerName;

    @Field("external_borrower_email")
    @JsonProperty("external_borrower_email")
    private String externalBorrowerEmail;

    @Field("external_borrower_organization")
    @JsonProperty("external_borrower_organization")
    private String externalBorrowerOrganization;

    // Intermediary staff member contact details
    @Field("staff_name")
    @JsonProperty("staff_name")
    private String staffName;

    @Field("staff_email")
    @JsonProperty("staff_email")
    private String staffEmail;

    // Timeline and operational details
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

    private State state = State.OPEN; // Defaults directly to OPEN state

    @CreatedDate
    @Field("created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

//    public ExternalIssueRecord() {}

//    // --- Getters and Setters ---
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public Instrument getInstrument() { return instrument; }
//    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
//
//    public Person getOwnerScientist() { return ownerScientist; }
//    public void setOwnerScientist(Person ownerScientist) { this.ownerScientist = ownerScientist; }
//
//    public String getExternalBorrowerName() { return externalBorrowerName; }
//    public void setExternalBorrowerName(String externalBorrowerName) { this.externalBorrowerName = externalBorrowerName; }
//
//    public String getExternalBorrowerEmail() { return externalBorrowerEmail; }
//    public void setExternalBorrowerEmail(String externalBorrowerEmail) { this.externalBorrowerEmail = externalBorrowerEmail; }
//
//    public String getExternalBorrowerOrganization() { return externalBorrowerOrganization; }
//    public void setExternalBorrowerOrganization(String externalBorrowerOrganization) { this.externalBorrowerOrganization = externalBorrowerOrganization; }
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