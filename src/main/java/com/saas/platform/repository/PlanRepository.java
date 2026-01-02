package com.saas.platform.repository;

import com.saas.platform.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByName(String name);
    boolean existsByName(String name);
    List<Plan> findByIsActiveTrue();
    List<Plan> findByIsCustomTrue();
}