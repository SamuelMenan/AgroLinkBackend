package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import com.agrolink.infrastructure.supabase.SupabaseStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/purchase-agreements")
public class PurchaseAgreementsController {
    private final SupabasePostgrestService postgrest;
    private final SupabaseStorageService storage;

    public PurchaseAgreementsController(SupabasePostgrestService postgrest, SupabaseStorageService storage) {
        this.postgrest = postgrest;
        this.storage = storage;
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestBody Map<String, Object> payload) {
        return postgrest.insert("purchase_agreements", payload, auth);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<String> confirm(@RequestHeader(value = "Authorization", required = false) String auth,
                                          @PathVariable String id) {
        Map<String, Object> patch = Map.of("status", "confirmed");
        return postgrest.update("purchase_agreements", patch, "id=eq." + id, auth);
    }

    @PostMapping(value = "/{id}/payment-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPaymentProof(@RequestHeader(value = "Authorization", required = false) String auth,
                                                                  @PathVariable String id,
                                                                  @RequestPart("file") MultipartFile file) {
        try {
            String bucket = "message-attachments";
            String path = id + "/" + (file.getOriginalFilename() == null ? ("proof-" + System.currentTimeMillis()) : file.getOriginalFilename());
            storage.upload(bucket, path, file.getBytes(), file.getContentType());
            String publicUrl = storage.publicUrl(bucket, path);
            Map<String, Object> patch = Map.of("payment_proof_url", publicUrl);
            postgrest.update("purchase_agreements", patch, "id=eq." + id, auth);
            return ResponseEntity.ok(Map.of("payment_proof_url", publicUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete-payment")
    public ResponseEntity<String> completePayment(@RequestHeader(value = "Authorization", required = false) String auth,
                                                  @PathVariable String id) {
        Map<String, Object> patch = Map.of("status", "paid");
        return postgrest.update("purchase_agreements", patch, "id=eq." + id, auth);
    }
}
