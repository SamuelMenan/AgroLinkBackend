package com.agrolink.infrastructure.security;

import org.springframework.stereotype.Component;
import io.github.cdimascio.dotenv.Dotenv;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM symmetric encryption service.
 * Key is sourced from environment variable ENCRYPTION_KEY.
 * Accepted formats:
 *  - 64 hex characters (256-bit)
 *  - Base64 encoded 32-byte key
 */
@Component
public class EncryptionService {
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_LEN = 12; // Recommended for GCM
    private SecretKeySpec keySpec; // null si deshabilitado
    private final SecureRandom random = new SecureRandom();
    private boolean enabled;

    public EncryptionService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String raw = dotenv.get("ENCRYPTION_KEY");
        if (raw == null || raw.isBlank()) {
            System.err.println("[WARN] ENCRYPTION_KEY no definido. Cifrado deshabilitado, se retornará texto plano.");
            keySpec = null;
            enabled = false;
            return;
        }
        byte[] keyBytes;
        try {
            if (raw.matches("[0-9a-fA-F]{64}")) {
                keyBytes = hexToBytes(raw);
            } else {
                keyBytes = Base64.getDecoder().decode(raw);
            }
            if (keyBytes.length != 32) {
                System.err.println("[WARN] ENCRYPTION_KEY longitud inválida. Se deshabilita cifrado.");
                keySpec = null;
                enabled = false;
                return;
            }
            keySpec = new SecretKeySpec(keyBytes, "AES");
            enabled = true;
        } catch (Exception e) {
            System.err.println("[WARN] ENCRYPTION_KEY malformado. Cifrado deshabilitado.");
            keySpec = null;
            enabled = false;
        }
    }

    public EncryptionResult encrypt(String plaintext, String aad) {
        if (!enabled || keySpec == null) {
            // Modo deshabilitado: devolver texto plano base64 y nonce vacío
            return new EncryptionResult(Base64.getEncoder().encodeToString(plaintext.getBytes()), "");
        }
        try {
            byte[] nonce = new byte[NONCE_LEN];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
            if (aad != null && !aad.isBlank()) {
                cipher.updateAAD(aad.getBytes());
            }
            byte[] ct = cipher.doFinal(plaintext.getBytes());
            return new EncryptionResult(Base64.getEncoder().encodeToString(ct), Base64.getEncoder().encodeToString(nonce));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertextB64, String nonceB64, String aad) {
        if (!enabled || keySpec == null) {
            // Modo deshabilitado: retornar contenido base64-decoded
            try {
                return new String(Base64.getDecoder().decode(ciphertextB64));
            } catch (Exception ignore) {
                return ciphertextB64;
            }
        }
        try {
            byte[] ct = Base64.getDecoder().decode(ciphertextB64);
            byte[] nonce = Base64.getDecoder().decode(nonceB64);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, nonce);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            if (aad != null && !aad.isBlank()) {
                cipher.updateAAD(aad.getBytes());
            }
            byte[] pt = cipher.doFinal(ct);
            return new String(pt);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    public static class EncryptionResult {
        public final String ciphertextB64;
        public final String nonceB64;
        public EncryptionResult(String c, String n) { this.ciphertextB64 = c; this.nonceB64 = n; }
    }
}
