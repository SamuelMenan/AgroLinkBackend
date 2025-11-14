package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UsersPublicInfoController {
    private final SupabasePostgrestService postgrest;

    public UsersPublicInfoController(SupabasePostgrestService postgrest) {
        this.postgrest = postgrest;
    }

    @PostMapping("/public-info")
    public ResponseEntity<String> publicInfo(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {

        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List)) {
            return ResponseEntity.badRequest().body("Campo 'ids' requerido");
        }

        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) idsObj;
        if (rawIds.isEmpty()) {
            return ResponseEntity.ok("[]");
        }

        List<String> ids = rawIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        String inList = ids.stream()
                .map(id -> "\"" + id.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));

        // En user_profiles, la clave es user_id (no id)
        String query = "user_id=in.(" + inList + ")";
        return postgrest.get("user_profiles", query, auth);
    }
}
