package com.example.APEEG.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "mail_logs")
@Data
public class MailLog {

    // Trigger category for the email notification
    public enum MailType {
        ISSUE,
        APPROVAL_REQUEST,
        ADVANCE_NOTICE,
        OVERDUE,
        RETURN,
        CALIBRATION,
        MANUAL_REMINDER
    }

    // SMTP delivery execution status
    public enum DeliveryStatus {
        SENT,
        FAILED
    }

    @Id
    private String id; // Unique MongoDB Document ID

    // Reference to the active checkout transaction (Nullable for calibration alerts)
    @DBRef
    @Field("issue_record")
    @JsonProperty("issue_record")
    private IssueRecord issueRecord;

    // Reference to the physical hardware instrument (Nullable)
    @DBRef
    private Instrument instrument;

    private MailType type; // Type of email dispatched

    private String recipients; // Comma-separated email addresses

    private String subject; // Email subject line

    private String body; // Plain-text email body contents

    @CreatedDate
    @Field("sent_at")
    @JsonProperty("sent_at")
    private LocalDateTime sentAt; // Automatic timestamp upon logging

    @Field("delivery_status")
    @JsonProperty("delivery_status")
    private DeliveryStatus deliveryStatus; // SENT or FAILED

    private String error; // Thrown exception or SMTP error trace (null if SENT)

//    // Default Constructor
//    public MailLog() {}
//
//    // --- Getters and Setters ---
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public IssueRecord getIssueRecord() { return issueRecord; }
//    public void setIssueRecord(IssueRecord issueRecord) { this.issueRecord = issueRecord; }
//
//    public Instrument getInstrument() { return instrument; }
//    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
//
//    public MailType getType() { return type; }
//    public void setType(MailType type) { this.type = type; }
//
//    public String getRecipients() { return recipients; }
//    public void setRecipients(String recipients) { this.recipients = recipients; }
//
//    public String getSubject() { return subject; }
//    public void setSubject(String subject) { this.subject = subject; }
//
//    public String getBody() { return body; }
//    public void setBody(String body) { this.body = body; }
//
//    public LocalDateTime getSentAt() { return sentAt; }
//    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
//
//    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
//    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
//
//    public String getError() { return error; }
//    public void setError(String error) { this.error = error; }
}