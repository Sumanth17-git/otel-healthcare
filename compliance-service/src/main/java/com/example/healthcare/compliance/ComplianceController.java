package com.example.healthcare.compliance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compliance")
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    private final ComplianceRepository repo;

    public ComplianceController(ComplianceRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/check/{patientId}")
    public boolean check(@PathVariable String patientId) {

        // put business context into MDC
        MDC.put("patientId", patientId);

        AccessPolicy policy = repo.findById(patientId)
                .orElse(new AccessPolicy(patientId, true)); // default allow

        log.info("Compliance check for patient, accessGranted={}", policy.isAccessGranted());

        MDC.clear();
        return policy.isAccessGranted();
    }
}
