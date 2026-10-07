package com.example.APEEG.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
@Data
@Document(collection = "issue_records")
public class IssueRecord {

    @Id
    private String id;

    @DBRef
    private Instrument instrument;

    @DBRef
    @Field("owner_scientist")
    @JsonProperty("owner_scientist")
    private Person ownerScientist;

    // REMOVE @DBRef HERE so MongoDB embeds the Person snapshot directly
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
    private LocalDate issueDate;

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

    // Issuing condition photo path
    @Field("condition_photo_path")
    @JsonProperty("condition_photo_path")
    private String conditionPhotoPath;

    // Return condition photo path
    @Field("condition_in_photo_path")
    @JsonProperty("condition_in_photo_path")
    private String conditionInPhotoPath;

    private State state = State.OPEN;

    public enum State {
        OPEN,
        RETURNED,
        CANCELLED
    }

//    // Getters and Setters
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public Instrument getInstrument() { return instrument; }
//    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
//
//    public Person getOwnerScientist() { return ownerScientist; }
//    public void setOwnerScientist(Person ownerScientist) { this.ownerScientist = ownerScientist; }
//
//    public Person getBorrowerScientist() { return borrowerScientist; }
//    public void setBorrowerScientist(Person borrowerScientist) { this.borrowerScientist = borrowerScientist; }
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
//    public LocalDate getActualReturnDate() { return actualReturnDate; }
//    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }
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
//    public String getConditionPhotoPath() { return conditionPhotoPath; }
//    public void setConditionPhotoPath(String conditionPhotoPath) { this.conditionPhotoPath = conditionPhotoPath; }
//
//    public String getConditionInPhotoPath() { return conditionInPhotoPath; }
//    public void setConditionInPhotoPath(String conditionInPhotoPath) { this.conditionInPhotoPath = conditionInPhotoPath; }
//
//    public State getState() { return state; }
//    public void setState(State state) { this.state = state; }
}