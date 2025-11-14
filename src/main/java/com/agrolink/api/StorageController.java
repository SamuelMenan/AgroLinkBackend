package com.agrolink.api;

import com.agrolink.infrastructure.supabase.SupabaseStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {
    private final SupabaseStorageService storage;

    public StorageController(SupabaseStorageService storage) { this.storage = storage; }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam String bucket,
                                         @RequestParam String path,
                                         @RequestParam("file") MultipartFile file) throws Exception {
        return storage.upload(bucket, path, file.getBytes(), file.getContentType());
    }

    @GetMapping("/public-url")
    public String publicUrl(@RequestParam String bucket, @RequestParam String path) {
        return storage.publicUrl(bucket, path);
    }
}
