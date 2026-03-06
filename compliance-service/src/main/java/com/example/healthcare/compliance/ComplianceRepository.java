package com.example.healthcare.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceRepository extends JpaRepository<AccessPolicy, String> {
}
