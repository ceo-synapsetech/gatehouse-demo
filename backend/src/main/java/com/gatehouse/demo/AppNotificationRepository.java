package com.gatehouse.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByRecipientOrderByCreatedAtDesc(String recipient);
}
