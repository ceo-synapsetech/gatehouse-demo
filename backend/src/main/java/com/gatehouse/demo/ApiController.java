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
    private static final Map<String, String> HOME_BY_RESIDENT = Map.of("ananya@example.com", "A-204");
    private static final Map<String, String> RESIDENT_BY_UNIT = Map.of("A-204", "Ananya Shah", "B-101", "Priya Nair", "C-302", "Neha Kapoor");

    public ApiController(AccessRequestRepository requests) { this.requests = requests; }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/auth/me") public Map<String, String> me(Authentication auth) {
        boolean resident = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
        return resident ? Map.of("username", auth.getName(), "role", "RESIDENT", "unit", HOME_BY_RESIDENT.get(auth.getName()))
            : Map.of("username", auth.getName(), "role", "SECURITY");
    }

    @GetMapping("/requests") public List<RequestView> list(Authentication auth) {
        boolean resident = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
        List<AccessRequest> rows = resident ? requests.findByUnitOrderByCreatedAtDesc(HOME_BY_RESIDENT.get(auth.getName())) : requests.findAllByOrderByCreatedAtDesc();
        return rows.stream().map(ApiController::view).toList();
    }

    @PostMapping("/requests") public RequestView create(@RequestBody CreateRequest body) {
        if (body.unit() == null || !RESIDENT_BY_UNIT.containsKey(body.unit()) || blank(body.kind()) || blank(body.person()) || blank(body.details()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid sample unit, person, type, and details are required");
        return view(requests.save(new AccessRequest(body.unit(), body.kind(), body.person().trim(), body.details().trim())));
    }

    @PatchMapping("/requests/{id}/decision") public RequestView decide(@PathVariable Long id, @RequestBody Decision body, Authentication auth) {
        String residentUnit = HOME_BY_RESIDENT.get(auth.getName());
        AccessRequest row = requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (residentUnit == null || !residentUnit.equals(row.getUnit())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!"APPROVED".equals(body.status()) && !"DENIED".equals(body.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        row.setStatus(AccessRequest.Status.valueOf(body.status()));
        return view(requests.save(row));
    }

    private static boolean blank(String s) { return s == null || s.isBlank() || s.length() > 500; }
    private static RequestView view(AccessRequest r) { return new RequestView(r.getId(), r.getUnit(), RESIDENT_BY_UNIT.get(r.getUnit()), r.getKind(), r.getPerson(), r.getDetails(), r.getStatus(), r.getCreatedAt()); }
    public record CreateRequest(String unit, String kind, String person, String details) {}
    public record Decision(String status) {}
    public record RequestView(Long id, String unit, String residentName, String kind, String person, String details, AccessRequest.Status status, java.time.Instant createdAt) {}
}
