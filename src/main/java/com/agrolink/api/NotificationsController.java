package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {
    private final SupabasePostgrestService postgrest;

    public NotificationsController(SupabasePostgrestService postgrest) { this.postgrest = postgrest; }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<String> listByUser(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @PathVariable String userId,
                                             @RequestParam(defaultValue = "12") int limit) {
        String q = "select=*&user_id=eq." + userId + "&order=created_at.desc&limit=" + limit;
        return postgrest.get("notifications", q, auth);
    }

    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<String> unreadCount(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable String userId) {
        String q = "select=id&user_id=eq." + userId + "&read_at=is.null"; // client can count ids
        return postgrest.get("notifications", q, auth);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestBody Map<String,Object> body) {
        return postgrest.insert("notifications", body, auth);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markRead(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @PathVariable String id) {
        Map<String,Object> payload = Map.of("read_at", Instant.now().toString());
        return postgrest.update("notifications", payload, "id=eq."+id, auth);
    }

    @PatchMapping("/read-all/{userId}")
    public ResponseEntity<String> markAllRead(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable String userId) {
        Map<String,Object> payload = Map.of("read_at", Instant.now().toString());
        return postgrest.update("notifications", payload, "user_id=eq."+userId+"&read_at=is.null", auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @PathVariable String id) {
        return postgrest.delete("notifications", "id=eq."+id, auth);
    }
}
