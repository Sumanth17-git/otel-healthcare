package com.example.healthcare.compliance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AccessPolicy {

    @Id
    private String patientId;

    private boolean accessGranted;

    public AccessPolicy() {}

    public AccessPolicy(String patientId, boolean accessGranted) {
        this.patientId = patientId;
        this.accessGranted = accessGranted;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public boolean isAccessGranted() {
        return accessGranted;
    }

    public void setAccessGranted(boolean accessGranted) {
        this.accessGranted = accessGranted;
    }
}
