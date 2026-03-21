package com.mycompany.msr.amis;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Assignment {

    // ================= PROPERTIES =================
    private final IntegerProperty id;
    private final StringProperty person;
    private final StringProperty department;
    private final StringProperty equipmentType;
    private final IntegerProperty quantity;
    private final StringProperty date;

    // ================= CONSTRUCTOR =================
    public Assignment(int id, String person, String department,
                      String equipmentType, int quantity, String date) {

        this.id = new SimpleIntegerProperty(id);
        this.person = new SimpleStringProperty(person);
        this.department = new SimpleStringProperty(department);
        this.equipmentType = new SimpleStringProperty(equipmentType);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.date = new SimpleStringProperty(date);
    }

    // ================= GETTERS =================
    public int getId() {
        return id.get();
    }

    public String getPerson() {
        return person.get();
    }

    public String getDepartment() {
        return department.get();
    }

    public String getEquipmentType() {
        return equipmentType.get();
    }

    public int getQuantity() {
        return quantity.get();
    }

    public String getDate() {
        return date.get();
    }

    // ================= SETTERS =================
    public void setId(int id) {
        this.id.set(id);
    }

    public void setPerson(String person) {
        this.person.set(person);
    }

    public void setDepartment(String department) {
        this.department.set(department);
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType.set(equipmentType);
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public void setDate(String date) {
        this.date.set(date);
    }

    // ================= PROPERTY METHODS =================
    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty personProperty() {
        return person;
    }

    public StringProperty departmentProperty() {
        return department;
    }

    public StringProperty equipmentTypeProperty() {
        return equipmentType;
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public StringProperty dateProperty() {
        return date;
    }

    // ================= OPTIONAL (DEBUGGING) =================
    @Override
    public String toString() {
        return "Assignment{" +
                "id=" + getId() +
                ", person='" + getPerson() + '\'' +
                ", department='" + getDepartment() + '\'' +
                ", equipmentType='" + getEquipmentType() + '\'' +
                ", quantity=" + getQuantity() +
                ", date='" + getDate() + '\'' +
                '}';
    }
}