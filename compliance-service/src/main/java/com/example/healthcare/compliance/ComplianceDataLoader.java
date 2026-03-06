package com.example.healthcare.compliance;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ComplianceDataLoader {

    @Bean
    CommandLineRunner loadPolicies(ComplianceRepository repo) {
        return args -> {
            repo.save(new AccessPolicy("patient001", true));
            repo.save(new AccessPolicy("staff001", false)); // blocked user
        };
    }
}
