package com.aqa.cc.nodestatus;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureStringPreferencesCodec {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "NodeStatus.VirtFusionConfig";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ENCRYPTED_PREFIX = "enc:v1:";

    String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ENCRYPTED_PREFIX
                    + encode(cipher.getIV())
                    + ":"
                    + encode(ciphertext);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt stored preference value.", exception);
        }
    }

    boolean isEncryptedValue(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isEmpty()) {
            return "";
        }
        if (!isEncryptedValue(storedValue)) {
            return storedValue;
        }

        try {
            String payload = storedValue.substring(ENCRYPTED_PREFIX.length());
            int separatorIndex = payload.indexOf(':');
            if (separatorIndex <= 0 || separatorIndex >= payload.length() - 1) {
                throw new IllegalStateException("Stored encrypted preference is malformed.");
            }

            byte[] iv = decode(payload.substring(0, separatorIndex));
            byte[] ciphertext = decode(payload.substring(separatorIndex + 1));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    new GCMParameterSpec(128, iv)
            );
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decrypt stored preference value.", exception);
        }
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
        );
        keyGenerator.init(
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
        );
        return keyGenerator.generateKey();
    }

    private String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private byte[] decode(String value) {
        try {
            return Base64.decode(value, Base64.NO_WRAP);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Stored encrypted preference uses invalid base64.", exception);
        }
    }
}
