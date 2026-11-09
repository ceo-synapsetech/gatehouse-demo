package com.gatehouse.demo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String securitySignupCode;
    private static final Set<String> VALID_UNITS = Set.of("A-204", "B-101", "C-302");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._@+-]{3,120}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public ApiController(AccessRequestRepository requests, AppNotificationRepository notifications,
            DeviceRegistrationRepository deviceTokens, FcmPushService push, AppUserRepository users,
            PasswordEncoder passwordEncoder, @Value("${SECURITY_SIGNUP_CODE:}") String securitySignupCode) {
        this.requests = requests;
        this.notifications = notifications;
        this.deviceTokens = deviceTokens;
        this.push = push;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.securitySignupCode = securitySignupCode;
    }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/auth/me") public Map<String, String> me(Authentication auth) {
        AppUser account = account(auth);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("username", account.getUsername());
        result.put("role", account.getRole());
        result.put("name", account.getDisplayName());
        if (account.getUnit() != null) result.put("unit", account.getUnit());
        return result;
    }

    @PostMapping("/auth/register") public Map<String, String> register(@RequestBody RegisterAccount body) {
        if (body.role() == null || (!body.role().equals("RESIDENT") && !body.role().equals("SECURITY")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid account type");
        String username = body.username() == null ? "" : body.username().trim();
        if (body.role().equals("RESIDENT")) username = username.toLowerCase(java.util.Locale.ROOT);
        if (!USERNAME.matcher(username).matches() ||
                (body.role().equals("RESIDENT") && !EMAIL.matcher(username).matches()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email or guard ID");
        if (body.password() == null || body.password().length() < 8 || body.password().length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        if (body.displayName() == null || body.displayName().isBlank() || body.displayName().trim().length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your name");

        String unit = null;
        if (body.role().equals("RESIDENT")) {
            unit = body.unit() == null ? "" : body.unit().trim().toUpperCase(java.util.Locale.ROOT);
            if (!VALID_UNITS.contains(unit))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid home unit");
        } else {
            if (securitySignupCode.isBlank())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Security signup is not configured yet");
            String suppliedCode = body.inviteCode() == null ? "" : body.inviteCode();
            if (!MessageDigest.isEqual(securitySignupCode.getBytes(StandardCharsets.UTF_8), suppliedCode.getBytes(StandardCharsets.UTF_8)))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The security invite code is not valid");
        }
        if (users.existsById(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists with that ID");

        users.save(new AppUser(username, passwordEncoder.encode(body.password()), body.role(),
            body.displayName().trim(), unit));
        return Map.of("status", "created", "username", username);
    }

    @GetMapping("/requests") public List<RequestView> list(Authentication auth) {
        boolean resident = isResident(auth);
        List<AccessRequest> rows = resident
            ? requests.findByUnitOrderByCreatedAtDesc(account(auth).getUnit())
            : requests.findAllByOrderByCreatedAtDesc();
        return rows.stream().map(this::view).toList();
    }

    @PostMapping("/requests") public RequestView create(@RequestBody CreateRequest body) {
        if (body.unit() == null || !VALID_UNITS.contains(body.unit()) || blank(body.kind()) || blank(body.person()) || blank(body.details()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid sample unit, person, type, and details are required");
        AccessRequest saved = requests.save(new AccessRequest(body.unit(), body.kind(), body.person().trim(), body.details().trim()));
        for (AppUser resident : users.findByRoleAndUnit("RESIDENT", saved.getUnit())) {
            String recipient = resident.getUsername();
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
        String residentUnit = account(auth).getUnit();
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
        if (!isResident(auth))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private AppUser account(Authentication auth) {
        return users.findById(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private static boolean blank(String s) { return s == null || s.isBlank() || s.length() > 500; }
    private RequestView view(AccessRequest r) {
        String residentName = users.findFirstByRoleAndUnitOrderByUsernameAsc("RESIDENT", r.getUnit())
            .map(AppUser::getDisplayName).orElse("Resident");
        return new RequestView(r.getId(), r.getUnit(), residentName, r.getKind(), r.getPerson(), r.getDetails(), r.getStatus(), r.getCreatedAt());
    }
    private static NotificationView notificationView(AppNotification n) {
        return new NotificationView(n.getId(), n.getRequestId(), n.getTitle(), n.getMessage(), n.getCreatedAt(), n.getReadAt());
    }
    public record CreateRequest(String unit, String kind, String person, String details) {}
    public record DeviceRequest(String token) {}
    public record RegisterAccount(String username, String password, String displayName, String role, String unit, String inviteCode) {}
    public record Decision(String status) {}
    public record RequestView(Long id, String unit, String residentName, String kind, String person, String details, AccessRequest.Status status, java.time.Instant createdAt) {}
    public record NotificationView(Long id, Long requestId, String title, String message, java.time.Instant createdAt, java.time.Instant readAt) {}
}
