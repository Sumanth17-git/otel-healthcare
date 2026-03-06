package com.example.healthcare.auth;

import java.util.Base64;

public class HealthcareTokenUtil {

    public static String generate(String patientId) {
        String raw = patientId + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
