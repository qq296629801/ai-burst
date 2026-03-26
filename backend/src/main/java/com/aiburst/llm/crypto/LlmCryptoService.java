package com.aiburst.llm.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class LlmCryptoService {

    private static final String AES = "AES/CBC/PKCS5Padding";

    @Value("${aiburst.llm.crypto.secret}")
    private String secret;

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] key = sha256(secret);
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] enc = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("encrypt api key failed", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[16];
            System.arraycopy(all, 0, iv, 0, 16);
            byte[] data = new byte[all.length - 16];
            System.arraycopy(all, 16, data, 0, data.length);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha256(secret), "AES"), new IvParameterSpec(iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt api key failed", e);
        }
    }

    private static byte[] sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(s.getBytes(StandardCharsets.UTF_8));
    }
}
