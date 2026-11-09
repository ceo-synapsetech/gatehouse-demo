package com.gatehouse.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoData {
    @Bean CommandLineRunner seedDemoData(AccessRequestRepository requests, AppUserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsById("guard01")) {
                users.save(new AppUser("guard01", encoder.encode("demo123"), "SECURITY", "Gatehouse Security", null));
            }
            if (!users.existsById("ananya@example.com")) {
                users.save(new AppUser("ananya@example.com", encoder.encode("demo123"), "RESIDENT", "Ananya Shah", "A-204"));
            }
            if (requests.count() == 0) {
                requests.save(new AccessRequest("A-204", "Delivery / visitor", "Demo Courier", "Sample grocery delivery"));
                AccessRequest approved = new AccessRequest("A-101", "Taking item out", "Sample Resident", "Old chair · sample item");
                approved.setStatus(AccessRequest.Status.APPROVED);
                requests.save(approved);
            }
        };
    }
}
