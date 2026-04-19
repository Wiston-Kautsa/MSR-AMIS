package com.mycompany.msr.amis;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ReturnRecord {

    private final StringProperty assetCode;
    private final StringProperty serialNumber;
    private final StringProperty equipmentName;
    private final StringProperty category;
    private final StringProperty source;
    private final StringProperty entryDate;
    private final StringProperty returnedBy;
    private final StringProperty phone;
    private final StringProperty nid;
    private final StringProperty returnCondition;
    private final StringProperty remarks;
    private final StringProperty returnDate;

    public ReturnRecord(
            String assetCode,
            String serialNumber,
            String equipmentName,
            String category,
            String source,
            String entryDate,
            String returnedBy,
            String phone,
            String nid,
            String returnCondition,
            String remarks,
            String returnDate
    ) {
        this.assetCode = new SimpleStringProperty(safe(assetCode));
        this.serialNumber = new SimpleStringProperty(safe(serialNumber));
        this.equipmentName = new SimpleStringProperty(safe(equipmentName));
        this.category = new SimpleStringProperty(safe(category));
        this.source = new SimpleStringProperty(safe(source));
        this.entryDate = new SimpleStringProperty(safe(entryDate));
        this.returnedBy = new SimpleStringProperty(safe(returnedBy));
        this.phone = new SimpleStringProperty(safe(phone));
        this.nid = new SimpleStringProperty(safe(nid));
        this.returnCondition = new SimpleStringProperty(safe(returnCondition));
        this.remarks = new SimpleStringProperty(safe(remarks));
        this.returnDate = new SimpleStringProperty(safe(returnDate));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public String getAssetCode() { return assetCode.get(); }
    public String getSerialNumber() { return serialNumber.get(); }
    public String getEquipmentName() { return equipmentName.get(); }
    public String getCategory() { return category.get(); }
    public String getSource() { return source.get(); }
    public String getEntryDate() { return entryDate.get(); }
    public String getReturnedBy() { return returnedBy.get(); }
    public String getPhone() { return phone.get(); }
    public String getNid() { return nid.get(); }
    public String getReturnCondition() { return returnCondition.get(); }
    public String getRemarks() { return remarks.get(); }
    public String getReturnDate() { return returnDate.get(); }

    public StringProperty assetCodeProperty() { return assetCode; }
    public StringProperty serialNumberProperty() { return serialNumber; }
    public StringProperty equipmentNameProperty() { return equipmentName; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty sourceProperty() { return source; }
    public StringProperty entryDateProperty() { return entryDate; }
    public StringProperty returnedByProperty() { return returnedBy; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty nidProperty() { return nid; }
    public StringProperty returnConditionProperty() { return returnCondition; }
    public StringProperty remarksProperty() { return remarks; }
    public StringProperty returnDateProperty() { return returnDate; }
}
