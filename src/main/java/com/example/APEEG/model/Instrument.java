package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
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
@Data
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

    // FIELD FOR PERSISTED QR IMAGE
    @Field("qr_photo")
    @JsonProperty("qr_photo")
    private String qrPhoto;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

}