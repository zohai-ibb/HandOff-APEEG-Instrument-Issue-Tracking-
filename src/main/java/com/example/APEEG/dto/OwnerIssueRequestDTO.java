package com.example.APEEG.dto;

import java.time.LocalDate;

/**
 * DTO capturing raw JSON payload parameters for owner-initiated instrument issuing.
 */
public class OwnerIssueRequestDTO {

    private String instrumentId;
    private String scientistListId; // ID from private scientist_list collection

    // Intermediary Staff Details
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

    public String getScientistListId() {
        return scientistListId;
    }

    public void setScientistListId(String scientistListId) {
        this.scientistListId = scientistListId;
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