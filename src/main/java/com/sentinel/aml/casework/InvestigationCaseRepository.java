package com.sentinel.aml.casework;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigationCaseRepository extends JpaRepository<InvestigationCase, Long> {
    Optional<InvestigationCase> findByAlertId(Long alertId);

    @EntityGraph(attributePaths = {"alert", "alert.customer", "alert.account"})
    List<InvestigationCase> findAllByOrderByCreatedAtDesc();
}