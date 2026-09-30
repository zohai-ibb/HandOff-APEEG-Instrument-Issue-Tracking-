package com.example.APEEG.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object encapsulating raw HTTP payload fields sent when
 * an Instrument Owner issues an item to another internal registered Scientist.
 */
public class OwnerIssueRequestDTO {

    private String instrumentId;
    private String borrowerScientistId;

    // Intermediary Staff Member Details
    private String staffName;
    private String staffEmail;

    private LocalDate dueDate;
    private String purpose;
    private String conditionOut;

    public OwnerIssueRequestDTO() {}

    // --- Getters and Setters ---
    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getBorrowerScientistId() {
        return borrowerScientistId;
    }

    public void setBorrowerScientistId(String borrowerScientistId) {
        this.borrowerScientistId = borrowerScientistId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffEmail() {
        return staffEmail;
    }

    public void setStaffEmail(String staffEmail) {
        this.staffEmail = staffEmail;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getConditionOut() {
        return conditionOut;
    }

    public void setConditionOut(String conditionOut) {
        this.conditionOut = conditionOut;
    }
}