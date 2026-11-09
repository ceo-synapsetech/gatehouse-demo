package com.gatehouse.demo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    List<AppUser> findByRoleAndUnit(String role, String unit);
    boolean existsByRoleAndUnitAndEnabledTrue(String role, String unit);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AppUser account where account.username = :username")
    Optional<AppUser> lockByUsername(@Param("username") String username);
    Optional<AppUser> findFirstByRoleAndUnitOrderByUsernameAsc(String role, String unit);
    List<AppUser> findByApprovalStatusOrderByUsernameAsc(String approvalStatus);
}
