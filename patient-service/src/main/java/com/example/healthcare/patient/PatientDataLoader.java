package com.example.healthcare.patient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PatientDataLoader {

    @Bean
    CommandLineRunner init(PatientRepository repo) {
        return args -> {
            if (!repo.existsById("patient001")) {
                repo.save(new Patient("patient001", "pass123", "4321"));
                repo.save(new Patient("staff001", "admin123", "9999"));
            }
        };
    }
}
