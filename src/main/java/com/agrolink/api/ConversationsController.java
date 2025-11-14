package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1//conversations")
public class ConversationsController {
    private final SupabasePostgrestService postgrest;

    public ConversationsController(SupabasePostgrestService postgrest) { this.postgrest = postgrest; }

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(value = "Authorization", required = false) String auth) {
        // empty payload lets Supabase default columns (id UUID default, created_at NOW())
        return postgrest.insert("conversations", Map.of(), auth);
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<String> addParticipant(@RequestHeader(value = "Authorization", required = false) String auth,
                                                 @PathVariable String id,
                                                 @RequestBody Map<String, Object> body) {
        // body expects { "user_id": "uuid" }
        Map<String,Object> payload = Map.of(
                "conversation_id", id,
                "user_id", body.get("user_id")
        );
        return postgrest.insert("conversation_participants", payload, auth);
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<String> listByUser(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @PathVariable String userId) {
        // Return participant rows for user; client can extract conversation IDs
        String query = "select=conversation_id&user_id=eq." + userId + "&order=conversation_id.asc";
        return postgrest.get("conversation_participants", query, auth);
    }
}
