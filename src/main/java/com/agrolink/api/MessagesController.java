package com.agrolink.api;

import com.agrolink.infrastructure.security.EncryptionService;
import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
public class MessagesController {
    private final SupabasePostgrestService postgrest;
    private final EncryptionService encryptionService;

    public MessagesController(SupabasePostgrestService postgrest, EncryptionService encryptionService) {
        this.postgrest = postgrest;
        this.encryptionService = encryptionService;
    }

    @PostMapping
    public ResponseEntity<String> send(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestBody Map<String, Object> body) {
        // Accept either already encrypted fields (content_ciphertext + iv) OR plaintext.
        Map<String,Object> payload = new HashMap<>(body);
        if (!payload.containsKey("content_ciphertext") && payload.containsKey("plaintext")) {
            String plaintext = String.valueOf(payload.get("plaintext"));
            String conversationId = String.valueOf(payload.get("conversation_id"));
            String senderId = String.valueOf(payload.get("sender_id"));
            String aad = conversationId + "|" + senderId;
            EncryptionService.EncryptionResult enc = encryptionService.encrypt(plaintext, aad);
            payload.remove("plaintext");
            payload.put("content_ciphertext", enc.ciphertextB64);
            payload.put("iv", enc.nonceB64);
        }
        return postgrest.insert("messages", payload, auth);
    }

    @GetMapping
    public ResponseEntity<String> list(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestParam String conversationId) {
        String query = "select=*&conversation_id=eq." + conversationId + "&order=created_at.asc";
        return postgrest.get("messages", query, auth);
    }
}
