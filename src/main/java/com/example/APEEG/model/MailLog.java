package com.example.APEEG.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "mail_logs")
public class MailLog {

    public enum MailType {
        ISSUE,
        APPROVAL_REQUEST,
        ADVANCE_NOTICE,
        OVERDUE,
        RETURN,
        CALIBRATION,
        MANUAL_REMINDER
    }

    public enum DeliveryStatus {
        SENT,
        FAILED
    }

    @Id
    private String id;

    @DBRef
    @Field("issue_record")
    private IssueRecord issueRecord;

    @DBRef
    private Instrument instrument;

    private MailType type;
    private String recipients;
    private String subject;
    private String body;

    @CreatedDate
    @Field("sent_at")
    private LocalDateTime sentAt;

    @Field("delivery_status")
    private DeliveryStatus deliveryStatus;

    private String error;

    public MailLog() {}

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public IssueRecord getIssueRecord() { return issueRecord; }
    public void setIssueRecord(IssueRecord issueRecord) { this.issueRecord = issueRecord; }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }

    public MailType getType() { return type; }
    public void setType(MailType type) { this.type = type; }

    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}