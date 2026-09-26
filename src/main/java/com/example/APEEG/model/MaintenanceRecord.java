package com.example.APEEG.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "maintenance_records")
@Data
public class MaintenanceRecord {

    public enum MaintenanceType {
        REPAIR,
        CALIBRATION
    }

    @Id
    private String id;

    @DBRef
    private Instrument instrument;

    private MaintenanceType type;

    @Field("sent_to")
    private String sentTo;

    @Field("out_date")
    private LocalDate outDate;

    @Field("expected_back")
    private LocalDate expectedBack;

    @Field("returned_on")
    private LocalDate returnedOn;

    private BigDecimal cost;

    @Field("certificate_path")
    private String certificatePath;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

//    public MaintenanceRecord() {}
//
//    // --- Getters and Setters ---
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public Instrument getInstrument() { return instrument; }
//    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
//
//    public MaintenanceType getType() { return type; }
//    public void setType(MaintenanceType type) { this.type = type; }
//
//    public String getSentTo() { return sentTo; }
//    public void setSentTo(String sentTo) { this.sentTo = sentTo; }
//
//    public LocalDate getOutDate() { return outDate; }
//    public void setOutDate(LocalDate outDate) { this.outDate = outDate; }
//
//    public LocalDate getExpectedBack() { return expectedBack; }
//    public void setExpectedBack(LocalDate expectedBack) { this.expectedBack = expectedBack; }
//
//    public LocalDate getReturnedOn() { return returnedOn; }
//    public void setReturnedOn(LocalDate returnedOn) { this.returnedOn = returnedOn; }
//
//    public BigDecimal getCost() { return cost; }
//    public void setCost(BigDecimal cost) { this.cost = cost; }
//
//    public String getCertificatePath() { return certificatePath; }
//    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

//Checked