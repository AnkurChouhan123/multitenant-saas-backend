package com.saas.platform.repository;

import com.saas.platform.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    
    /**
     * Find plan by name
     */
    Optional<Plan> findByName(String name);
    
    /**
     * Find all active plans
     */
    List<Plan> findByIsActiveTrue();
    
    /**
     * Find all custom plans
     */
    List<Plan> findByIsCustomTrue();
    
    /**
     * Check if plan name already exists
     */
    boolean existsByName(String name);
}