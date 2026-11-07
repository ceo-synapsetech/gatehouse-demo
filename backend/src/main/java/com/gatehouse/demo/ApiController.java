package com.gatehouse.demo;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
public class ApiController {
    private final AccessRequestRepository requests;
    private final AppNotificationRepository notifications;
    private final DeviceRegistrationRepository deviceTokens;
    private final FcmPushService push;
    private static final Map<String, String> HOME_BY_RESIDENT = Map.of("ananya@example.com", "A-204");
    private static final Map<String, String> RESIDENT_BY_UNIT = Map.of("A-204", "Ananya Shah", "B-101", "Priya Nair", "C-302", "Neha Kapoor");
    private static final Map<String, String> ACCOUNT_BY_UNIT = Map.of("A-204", "ananya@example.com");

    public ApiController(AccessRequestRepository requests, AppNotificationRepository notifications,
            DeviceRegistrationRepository deviceTokens, FcmPushService push) {
        this.requests = requests;
        this.notifications = notifications;
        this.deviceTokens = deviceTokens;
        this.push = push;
    }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/auth/me") public Map<String, String> me(Authentication auth) {
        boolean resident = isResident(auth);
        return resident ? Map.of("username", auth.getName(), "role", "RESIDENT", "unit", HOME_BY_RESIDENT.get(auth.getName()))
            : Map.of("username", auth.getName(), "role", "SECURITY");
    }

    @GetMapping("/requests") public List<RequestView> list(Authentication auth) {
        boolean resident = isResident(auth);
        List<AccessRequest> rows = resident ? requests.findByUnitOrderByCreatedAtDesc(HOME_BY_RESIDENT.get(auth.getName())) : requests.findAllByOrderByCreatedAtDesc();
        return rows.stream().map(ApiController::view).toList();
    }

    @PostMapping("/requests") public RequestView create(@RequestBody CreateRequest body) {
        if (body.unit() == null || !RESIDENT_BY_UNIT.containsKey(body.unit()) || blank(body.kind()) || blank(body.person()) || blank(body.details()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid sample unit, person, type, and details are required");
        AccessRequest saved = requests.save(new AccessRequest(body.unit(), body.kind(), body.person().trim(), body.details().trim()));
        String recipient = ACCOUNT_BY_UNIT.get(saved.getUnit());
        if (recipient != null) {
            AppNotification notification = notifications.save(new AppNotification(recipient, saved.getId(), "Visitor approval needed",
                saved.getPerson() + " · " + saved.getKind() + " is waiting for your decision."));
            push.sendToRecipient(recipient, notification, deviceTokens.findByRecipient(recipient));
        }
        return view(saved);
    }

    @PostMapping("/devices") public Map<String, String> registerDevice(@RequestBody DeviceRequest body, Authentication auth) {
        requireResident(auth);
        if (body.token() == null || body.token().isBlank() || body.token().length() > 4096)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid device token is required");
        DeviceRegistration registration = deviceTokens.findByToken(body.token())
            .orElseGet(() -> new DeviceRegistration(auth.getName(), body.token()));
        registration.setRecipient(auth.getName());
        deviceTokens.save(registration);
        return Map.of("status", "registered");
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/devices")
    public Map<String, String> unregisterDevice(@RequestBody DeviceRequest body, Authentication auth) {
        requireResident(auth);
        if (body.token() != null) {
            deviceTokens.findByToken(body.token())
                .filter(device -> auth.getName().equals(device.getRecipient()))
                .ifPresent(deviceTokens::delete);
        }
        return Map.of("status", "unregistered");
    }

    @GetMapping("/notifications") public List<NotificationView> notifications(Authentication auth) {
        requireResident(auth);
        return this.notifications.findByRecipientOrderByCreatedAtDesc(auth.getName()).stream()
            .map(ApiController::notificationView).toList();
    }

    @PatchMapping("/notifications/{id}/read") public NotificationView markNotificationRead(@PathVariable Long id, Authentication auth) {
        requireResident(auth);
        AppNotification notification = notifications.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!auth.getName().equals(notification.getRecipient())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        notification.markRead();
        return notificationView(notifications.save(notification));
    }

    @PatchMapping("/requests/{id}/decision") public RequestView decide(@PathVariable Long id, @RequestBody Decision body, Authentication auth) {
        String residentUnit = HOME_BY_RESIDENT.get(auth.getName());
        AccessRequest row = requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (residentUnit == null || !residentUnit.equals(row.getUnit())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!"APPROVED".equals(body.status()) && !"DENIED".equals(body.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        row.setStatus(AccessRequest.Status.valueOf(body.status()));
        return view(requests.save(row));
    }

    private static boolean isResident(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
    }
    private static void requireResident(Authentication auth) {
        if (!isResident(auth) || !HOME_BY_RESIDENT.containsKey(auth.getName()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private static boolean blank(String s) { return s == null || s.isBlank() || s.length() > 500; }
    private static RequestView view(AccessRequest r) { return new RequestView(r.getId(), r.getUnit(), RESIDENT_BY_UNIT.get(r.getUnit()), r.getKind(), r.getPerson(), r.getDetails(), r.getStatus(), r.getCreatedAt()); }
    private static NotificationView notificationView(AppNotification n) {
        return new NotificationView(n.getId(), n.getRequestId(), n.getTitle(), n.getMessage(), n.getCreatedAt(), n.getReadAt());
    }
    public record CreateRequest(String unit, String kind, String person, String details) {}
    public record DeviceRequest(String token) {}
    public record Decision(String status) {}
    public record RequestView(Long id, String unit, String residentName, String kind, String person, String details, AccessRequest.Status status, java.time.Instant createdAt) {}
    public record NotificationView(Long id, Long requestId, String title, String message, java.time.Instant createdAt, java.time.Instant readAt) {}
}
