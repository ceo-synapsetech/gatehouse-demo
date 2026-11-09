package com.gatehouse.demo;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
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
    private static final String RESIDENT_CHANNEL_ID = "resident_visitor_approvals";
    private final FirebaseApp firebaseApp;
    private final DeviceRegistrationRepository deviceRegistrations;

    public FcmPushService(
            @Value("${FCM_SERVICE_ACCOUNT_JSON:}") String serviceAccountJson,
            DeviceRegistrationRepository deviceRegistrations) {
        this.deviceRegistrations = deviceRegistrations;
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
        if (firebaseApp == null) {
            log.warn("Push not sent to resident {} because Firebase is not configured.", recipient);
            return;
        }
        if (devices.isEmpty()) {
            log.warn("Push not sent to resident {} because no device is registered.", recipient);
            return;
        }

        for (DeviceRegistration device : devices) {
            AndroidNotification androidNotification = AndroidNotification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getMessage())
                .setChannelId(RESIDENT_CHANNEL_ID)
                .setDefaultSound(true)
                .setDefaultVibrateTimings(true)
                .build();
            Message message = Message.builder()
                .setToken(device.getToken())
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(androidNotification)
                    .build())
                .putData("notificationId", notification.getId().toString())
                .putData("requestId", notification.getRequestId().toString())
                .putData("title", notification.getTitle())
                .putData("body", notification.getMessage())
                .build();
            try {
                String messageId = FirebaseMessaging.getInstance(firebaseApp).sendAsync(message).get();
                log.info("FCM accepted visitor alert for resident {} (message {}).", recipient, messageId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("FCM send interrupted for resident {}.", recipient);
                return;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof FirebaseMessagingException messagingException
                        && messagingException.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deviceRegistrations.delete(device);
                    log.warn("Removed an expired FCM registration for resident {}.", recipient);
                } else {
                    log.warn("FCM delivery failed for resident {}: {}", recipient,
                        cause == null ? e.getMessage() : cause.getMessage());
                }
            }
        }
    }
}
