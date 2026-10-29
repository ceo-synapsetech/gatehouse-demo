package com.gatehouse.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findAllByOrderByCreatedAtDesc();
    List<AccessRequest> findByUnitOrderByCreatedAtDesc(String unit);
}
