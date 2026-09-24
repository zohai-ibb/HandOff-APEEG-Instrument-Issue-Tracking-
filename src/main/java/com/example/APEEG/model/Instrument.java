package com.example.APEEG.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "instruments")
public class Instrument {

    public enum Status {
        AVAILABLE,
        ISSUED,
        MAINTENANCE,
        CALIBRATION_DUE,
        DAMAGED,
        CONDEMNED,
        LOST
    }

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("asset_id")
    private String assetId;

    private String name;
    private String make;

    @Field("serial_no")
    private String serialNo;

    private Integer quantity = 1;
    private String location;

    @DBRef
    @Field("owner_scientist")
    private Person ownerScientist;

    private Status status = Status.AVAILABLE;

    @Field("calibration_valid_to")
    private LocalDate calibrationValidTo;

    @Field("purchase_date")
    private LocalDate purchaseDate;

    @Field("purchase_cost")
    private BigDecimal purchaseCost;

    private String accessories;

    @Field("photo_path")
    private String photoPath;

    @Field("manual_path")
    private String manualPath;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    public Instrument() {}

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Person getOwnerScientist() { return ownerScientist; }
    public void setOwnerScientist(Person ownerScientist) { this.ownerScientist = ownerScientist; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getCalibrationValidTo() { return calibrationValidTo; }
    public void setCalibrationValidTo(LocalDate calibrationValidTo) { this.calibrationValidTo = calibrationValidTo; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public void setPurchaseCost(BigDecimal purchaseCost) { this.purchaseCost = purchaseCost; }

    public String getAccessories() { return accessories; }
    public void setAccessories(String accessories) { this.accessories = accessories; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getManualPath() { return manualPath; }
    public void setManualPath(String manualPath) { this.manualPath = manualPath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}