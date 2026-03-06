package com.example.healthcare.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/auth")
public class PatientAuthController {

    private static final Logger log = LoggerFactory.getLogger(PatientAuthController.class);

    private final RestTemplate rest = new RestTemplate();

    @Value("${patient.service.url:http://localhost:8081}")
    private String patientServiceUrl;

    @Value("${compliance.service.url:http://localhost:8082}")
    private String complianceServiceUrl;

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> req) {

        String patientId = req.get("patientId");

        // MDC context (will be auto-enriched by New Relic / tracing agent)
        MDC.put("patientId", patientId);

        log.info("Login request received");

        Boolean valid = rest.postForObject(
                patientServiceUrl + "/patients/validate",
                req,
                Boolean.class
        );

        Boolean allowed = rest.postForObject(
                complianceServiceUrl + "/compliance/check/" + patientId,
                null,
                Boolean.class
        );

        log.info("Validation result valid={} allowed={}", valid, allowed);

        if (Boolean.TRUE.equals(valid) && Boolean.TRUE.equals(allowed)) {
            String token = HealthcareTokenUtil.generate(patientId);
            log.info("Authentication success");
            MDC.clear();
            return token;
        }

        log.warn("Authentication failed");
        MDC.clear();
        throw new RuntimeException("Invalid credentials or blocked by compliance policy");
    }
}
