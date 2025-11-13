package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {
    private final SupabasePostgrestService postgrest;

    public OrdersController(SupabasePostgrestService postgrest) {
        this.postgrest = postgrest;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestBody Map<String, Object> payload) {
        return postgrest.insert("orders", payload, auth);
    }

    @GetMapping
    public ResponseEntity<String> list(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestParam(value = "q", required = false) String q) {
        return postgrest.get("orders", q, auth);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                               @PathVariable String id,
                                               @RequestBody Map<String, Object> payload) {
        // expects payload {"status":"enviado"}
        return postgrest.update("orders", payload, "id=eq." + id, auth);
    }
}
