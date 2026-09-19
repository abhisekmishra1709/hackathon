package com.sentinel.aml.alert;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findByDedupeKey(String dedupeKey);

    @EntityGraph(attributePaths = {"customer", "account", "evidence"})
    List<Alert> findAllByOrderByRiskScoreDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "account", "evidence"})
    Optional<Alert> findWithEvidenceById(Long id);
}