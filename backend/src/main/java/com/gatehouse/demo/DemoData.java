package com.gatehouse.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoData {
    @Bean CommandLineRunner seedDemoRequests(AccessRequestRepository requests) {
        return args -> {
            if (requests.count() == 0) {
                requests.save(new AccessRequest("A-204", "Delivery / visitor", "Demo Courier", "Sample grocery delivery"));
                AccessRequest approved = new AccessRequest("B-101", "Taking item out", "Sample Resident", "Old chair · sample item");
                approved.setStatus(AccessRequest.Status.APPROVED);
                requests.save(approved);
            }
        };
    }
}
