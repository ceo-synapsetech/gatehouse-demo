package com.gatehouse.demo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    List<AppUser> findByRoleAndUnit(String role, String unit);
    Optional<AppUser> findFirstByRoleAndUnitOrderByUsernameAsc(String role, String unit);
    List<AppUser> findByApprovalStatusOrderByUsernameAsc(String approvalStatus);
}
