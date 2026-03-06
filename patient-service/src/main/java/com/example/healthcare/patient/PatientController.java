package com.example.healthcare.patient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);
    private final PatientRepository repo;

    public PatientController(PatientRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/validate")
    public boolean validate(@RequestBody Map<String, String> req) {
        String patientId = req.get("patientId");
        log.info("Validating patient {}", patientId);

        return repo.findById(patientId)
                .map(p ->
                        p.getPassword().equals(req.get("password")) &&
                        p.getAccessPin().equals(req.get("accessPin"))
                )
                .orElse(false);
    }
}
