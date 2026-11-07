package com.gatehouse.demo;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FcmPushService {
    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);
    private final FirebaseApp firebaseApp;

    public FcmPushService(@Value("${FCM_SERVICE_ACCOUNT_JSON:}") String serviceAccountJson) {
        FirebaseApp configured = null;
        if (!serviceAccountJson.isBlank()) {
            try {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
                configured = FirebaseApp.initializeApp(
                    FirebaseOptions.builder().setCredentials(credentials).build(), "gatehouse-push");
                log.info("Firebase Cloud Messaging is configured.");
            } catch (IOException | IllegalStateException e) {
                log.error("Firebase Cloud Messaging could not be initialized.", e);
            }
        } else {
            log.info("FCM_SERVICE_ACCOUNT_JSON is not set; push delivery is disabled.");
        }
        this.firebaseApp = configured;
    }

    public void sendToRecipient(String recipient, AppNotification notification, List<DeviceRegistration> devices) {
        if (firebaseApp == null || devices.isEmpty()) return;
        for (DeviceRegistration device : devices) {
            Message message = Message.builder()
                .setToken(device.getToken())
                .setNotification(Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getMessage())
                    .build())
                .putData("notificationId", notification.getId().toString())
                .putData("requestId", notification.getRequestId().toString())
                .build();
            try {
                FirebaseMessaging.getInstance(firebaseApp).sendAsync(message).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("FCM send interrupted for resident {}.", recipient);
                return;
            } catch (ExecutionException e) {
                log.warn("FCM delivery failed for resident {}: {}", recipient, e.getCause().getMessage());
            }
        }
    }
}
