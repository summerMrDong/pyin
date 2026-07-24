package com.pyin.plugin.system.clientcredential.support;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class ClientCredentialSecretCodec {

    private static final String KEY_SEED = "pyin-client-credential-secret-v1";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec secretKeySpec;

    public ClientCredentialSecretCodec(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void initialize() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(KEY_SEED.getBytes(StandardCharsets.UTF_8));
            this.secretKeySpec = new SecretKeySpec(java.util.Arrays.copyOf(key, 16), "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize client credential secret key", exception);
        }
    }

    public String hash(String rawSecret) {
        return passwordEncoder.encode(rawSecret);
    }

    public boolean matches(String rawSecret, String storedHash) {
        return passwordEncoder.matches(rawSecret, storedHash);
    }

    public String encrypt(String rawSecret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(rawSecret.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt access secret", exception);
        }
    }

    public String decrypt(String encryptedSecret) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedSecret);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt access secret", exception);
        }
    }
}
