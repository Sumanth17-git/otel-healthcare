package com.example.healthcare.patient;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Patient {

    @Id
    private String patientId;
    private String password;
    private String accessPin;

    // Required by JPA
    public Patient() {
    }

    public Patient(String patientId, String password, String accessPin) {
        this.patientId = patientId;
        this.password = password;
        this.accessPin = accessPin;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessPin() {
        return accessPin;
    }

    public void setAccessPin(String accessPin) {
        this.accessPin = accessPin;
    }
}
