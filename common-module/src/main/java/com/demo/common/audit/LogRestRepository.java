package com.demo.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for the LOG_REST audit table.
 */
@Repository
public interface LogRestRepository extends JpaRepository<LogRest, Long> {
}
