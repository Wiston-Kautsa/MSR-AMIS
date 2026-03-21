package com.mycompany.msr.amis;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Distribution {

    // ================= PROPERTIES =================
    private final IntegerProperty id;
    private final StringProperty assetCode;
    private final StringProperty serialNumber;
    private final StringProperty assignedTo;
    private final StringProperty phone;
    private final StringProperty nid;
    private final ObjectProperty<LocalDate> distributionDate;

    // ✅ ADDED (missing)
    private final StringProperty assignmentId;
    private final StringProperty status;

    // ================= DATE FORMAT =================
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ================= DEFAULT =================
    public Distribution() {
        this(0, "", "", "", "", "", LocalDate.now());
    }

    // ================= MAIN (FULL) =================
    public Distribution(
            int id,
            String assetCode,
            String serialNumber,
            String assignedTo,
            String phone,
            String nid,
            LocalDate distributionDate
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.assetCode = new SimpleStringProperty(safe(assetCode));
        this.serialNumber = new SimpleStringProperty(safe(serialNumber));
        this.assignedTo = new SimpleStringProperty(safe(assignedTo));
        this.phone = new SimpleStringProperty(safe(phone));
        this.nid = new SimpleStringProperty(safe(nid));
        this.distributionDate = new SimpleObjectProperty<>(
                distributionDate != null ? distributionDate : LocalDate.now()
        );

        // ✅ initialize added fields
        this.assignmentId = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("ACTIVE");
    }

    // ================= SIMPLIFIED CONSTRUCTOR =================
    public Distribution(
            String assetCode,
            String serialNumber,
            String assignedTo,
            String phone,
            String nid
    ) {
        this(0, assetCode, serialNumber, assignedTo, phone, nid, LocalDate.now());
    }

    // ================= NULL SAFETY =================
    private String safe(String value) {
        return value == null ? "" : value;
    }

    // ================= GETTERS =================
    public int getId() { return id.get(); }

    public String getAssetCode() { return assetCode.get(); }

    public String getSerialNumber() { return serialNumber.get(); }

    public String getAssignedTo() { return assignedTo.get(); }

    public String getPhone() { return phone.get(); }

    public String getNid() { return nid.get(); }

    public LocalDate getDistributionDate() { return distributionDate.get(); }

    // ================= ADDED GETTERS =================
    public String getAssignmentId() { return assignmentId.get(); }

    public String getStatus() { return status.get(); }

    // controller expects this name
    public String getDate() { return getFormattedDate(); }

    // ================= FORMATTED DATE =================
    public String getFormattedDate() {
        return getDistributionDate().format(FORMATTER);
    }

    // ================= SETTERS =================
    public void setId(int value) { id.set(value); }

    public void setAssetCode(String value) { assetCode.set(safe(value)); }

    public void setSerialNumber(String value) { serialNumber.set(safe(value)); }

    public void setAssignedTo(String value) { assignedTo.set(safe(value)); }

    public void setPhone(String value) { phone.set(safe(value)); }

    public void setNid(String value) { nid.set(safe(value)); }

    public void setDistributionDate(LocalDate value) {
        distributionDate.set(value != null ? value : LocalDate.now());
    }

    // ================= ADDED SETTERS =================
    public void setAssignmentId(String value) { assignmentId.set(safe(value)); }

    public void setStatus(String value) { status.set(safe(value)); }

    // ================= PROPERTIES =================
    public IntegerProperty idProperty() { return id; }

    public StringProperty assetCodeProperty() { return assetCode; }

    public StringProperty serialNumberProperty() { return serialNumber; }

    public StringProperty assignedToProperty() { return assignedTo; }

    public StringProperty phoneProperty() { return phone; }

    public StringProperty nidProperty() { return nid; }

    public ObjectProperty<LocalDate> distributionDateProperty() { return distributionDate; }

    // ================= ADDED PROPERTY METHODS =================
    public StringProperty assignmentIdProperty() { return assignmentId; }

    public StringProperty statusProperty() { return status; }

    // ================= DEBUG =================
    @Override
    public String toString() {
        return "Distribution{" +
                "id=" + getId() +
                ", assetCode='" + getAssetCode() + '\'' +
                ", serialNumber='" + getSerialNumber() + '\'' +
                ", assignedTo='" + getAssignedTo() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", nid='" + getNid() + '\'' +
                ", assignmentId='" + getAssignmentId() + '\'' +
                ", status='" + getStatus() + '\'' +
                ", date=" + getFormattedDate() +
                '}';
    }
}