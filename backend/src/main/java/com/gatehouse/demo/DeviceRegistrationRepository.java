package com.gatehouse.demo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, Long> {
    Optional<DeviceRegistration> findByToken(String token);
    List<DeviceRegistration> findByRecipient(String recipient);
}
