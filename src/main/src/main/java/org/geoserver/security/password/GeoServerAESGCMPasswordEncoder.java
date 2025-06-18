/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.toChars;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password Encoder using AES-GCM encryption with NIST-approved algorithms.
 *
 * <p>This implementation uses:
 *
 * <ul>
 *   <li>AES-GCM for encryption (NIST-approved)
 *   <li>PBKDF2 with HMAC-SHA256 for key derivation (NIST-approved)
 *   <li>256-bit keys for maximum security
 *   <li>12-byte IV for GCM mode
 * </ul>
 *
 * <p>The salt parameter is not used, this implementation computes a random salt as default.
 *
 * @author christian
 */
public class GeoServerAESGCMPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int ITERATIONS = 100000;

    private GeoServerSecurityManager securityManager;
    private KeyStoreProvider keystoreProvider;
    private String keyAliasInKeyStore = KeyStoreProviderImpl.CONFIGPASSWORDKEY;

    public GeoServerAESGCMPasswordEncoder() {
        setReversible(true);
    }

    @Override
    public void initialize(GeoServerSecurityManager securityManager) throws IOException {
        this.securityManager = securityManager;
        this.keystoreProvider = securityManager.getKeyStoreProvider();
    }

    @Override
    public void initializeFor(GeoServerUserGroupService service) throws IOException {
        if (!keystoreProvider.hasUserGroupKey(service.getName())) {
            throw new IOException("No key alias: "
                    + keystoreProvider.aliasForGroupService(service.getName())
                    + " in key store: "
                    + keystoreProvider.getResource().path());
        }
        keyAliasInKeyStore = keystoreProvider.aliasForGroupService(service.getName());
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                try {
                    return encrypt(rawPassword.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encode password", e);
                }
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                try {
                    String decrypted = decrypt(encodedPassword);
                    return rawPassword.toString().equals(decrypted);
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                try {
                    return encrypt(new String(rawPass));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encode password", e);
                }
            }

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                try {
                    String decrypted = decrypt(encPass);
                    return Arrays.equals(decrypted.toCharArray(), rawPass);
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    private String encrypt(String plaintext) throws Exception {
        // Get the encryption key from keystore
        byte[] keyBytes = getEncryptionKey();
        if (keyBytes == null) {
            throw new IOException("No encryption key available");
        }

        // Generate random salt and IV
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Derive key using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        KeySpec spec = new PBEKeySpec(toChars(keyBytes), salt, ITERATIONS, KEY_LENGTH);
        SecretKey derivedKey = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(derivedKey.getEncoded(), "AES");

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine salt + iv + ciphertext and encode
        byte[] combined = new byte[salt.length + iv.length + ciphertext.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(ciphertext, 0, combined, salt.length + iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String encrypted) throws Exception {
        // Get the encryption key from keystore
        byte[] keyBytes = getEncryptionKey();
        if (keyBytes == null) {
            throw new IOException("No encryption key available");
        }

        // Decode and split
        byte[] combined = Base64.getDecoder().decode(encrypted);
        byte[] salt = Arrays.copyOfRange(combined, 0, 16);
        byte[] iv = Arrays.copyOfRange(combined, 16, 16 + IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(combined, 16 + IV_LENGTH, combined.length);

        // Derive key using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        KeySpec spec = new PBEKeySpec(toChars(keyBytes), salt, ITERATIONS, KEY_LENGTH);
        SecretKey derivedKey = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(derivedKey.getEncoded(), "AES");

        // Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, "UTF-8");
    }

    private byte[] getEncryptionKey() throws IOException {
        if (keystoreProvider != null) {
            SecretKey key = keystoreProvider.getSecretKey(keyAliasInKeyStore);
            return key != null ? key.getEncoded() : null;
        }
        return null;
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.ENCRYPT;
    }

    @Override
    public String decode(String encPass) throws UnsupportedOperationException {
        try {
            return decrypt(removePrefix(encPass));
        } catch (Exception e) {
            throw new UnsupportedOperationException("Failed to decode password", e);
        }
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        try {
            return decrypt(removePrefix(encPass)).toCharArray();
        } catch (Exception e) {
            throw new UnsupportedOperationException("Failed to decode password", e);
        }
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return createCharEncoder().encodePassword(rawPassword.toString().toCharArray(), null);
    }
}
