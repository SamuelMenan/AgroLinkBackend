package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductsController {
    private final SupabasePostgrestService postgrest;

    public ProductsController(SupabasePostgrestService postgrest) { this.postgrest = postgrest; }

    @GetMapping
    public ResponseEntity<String> list(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestParam(value = "q", required = false) String rawQuery) {
        // rawQuery may include select/order/filters e.g. "select=*&order=created_at.desc&limit=60"
        return postgrest.get("products", rawQuery, auth);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestBody Map<String, Object> payload) {
        return postgrest.insert("products", payload, auth);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> update(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @PathVariable String id,
                                         @RequestBody Map<String, Object> payload) {
        // filters: id eq
        return postgrest.update("products", payload, "id=eq." + id, auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @PathVariable String id) {
        return postgrest.delete("products", "id=eq." + id, auth);
    }
}
