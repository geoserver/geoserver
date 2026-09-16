/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.scramble;
import static org.geoserver.security.SecurityUtils.toBytes;
import static org.geoserver.security.SecurityUtils.toChars;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Reversible password encoder built on AES-GCM. It uses only algorithms that both the normal JDK providers and the
 * FIPS-validated ones offer, so the same encoded values can be read on either kind of installation.
 *
 * <p>The older password based encoders cannot work under FIPS, see {@link AesGcmCipher}.
 *
 * <p>The encoded form is {@code base64(}{@link AesGcmCipher#encrypt}{@code )}. Unlike the older encoders, a changed
 * value is detected and refused instead of decoding into meaningless characters.
 *
 * <p>The {@code char[]} methods wipe the plain text before returning, so it never reaches an object that cannot be
 * cleared. The {@code String} methods cannot do that, because {@link PasswordEncoder} takes and returns a
 * {@code String}: there the plain text stays on the heap until it is collected.
 */
public class GeoServerAesGcmPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    private KeyStoreProvider keystoreProvider;
    private String keyAliasInKeyStore = KeyStoreProviderImpl.CONFIGPASSWORDKEY;

    @Override
    public void initialize(GeoServerSecurityManager securityManager) throws IOException {
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
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.ENCRYPT;
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                // a user may have no password at all; it has to stay missing rather than
                // become the encryption of an empty value, like the other reversible encoders do
                if (rawPassword == null) {
                    return null;
                }
                return encrypt(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return decodeInternal(encodedPassword).equals(rawPassword.toString());
            }
        };
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                if (rawPass == null) {
                    return null;
                }
                byte[] bytes = toBytes(rawPass);
                try {
                    return encrypt(bytes);
                } finally {
                    scramble(bytes);
                }
            }

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                byte[] decrypted = decryptToBytes(encPass);
                char[] chars = toChars(decrypted);
                try {
                    return Arrays.equals(chars, rawPass);
                } finally {
                    scramble(decrypted);
                    scramble(chars);
                }
            }
        };
    }

    /** Re-encodes an already encoded password, same contract as the other reversible encoders. */
    @Override
    public String encode(CharSequence rawPassword) {
        return createCharEncoder().encodePassword(decodeToCharArray(rawPassword.toString()), null);
    }

    @Override
    public String decode(String encPass) throws UnsupportedOperationException {
        return decodeInternal(stripPrefix(encPass));
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        byte[] decrypted = decryptToBytes(stripPrefix(encPass));
        try {
            return toChars(decrypted);
        } finally {
            scramble(decrypted);
        }
    }

    private String decodeInternal(String encPass) {
        byte[] decrypted = decryptToBytes(encPass);
        try {
            return new String(decrypted, StandardCharsets.UTF_8);
        } finally {
            scramble(decrypted);
        }
    }

    private String encrypt(byte[] plainText) {
        return Base64.getEncoder().encodeToString(AesGcmCipher.encrypt(key(), plainText));
    }

    private byte[] decryptToBytes(String encPass) {
        return AesGcmCipher.decrypt(key(), Base64.getDecoder().decode(encPass));
    }

    /**
     * The keystore holds the derived key, not this class. Callers get a new encoder for every operation, so a key kept
     * here would be derived again each time, and that takes hundreds of milliseconds.
     *
     * <p>Never write to the keystore here. This runs while encoding, and nothing puts those calls in order, so two
     * threads could write two different keys and whatever the losing one encrypted could never be decrypted.
     */
    private SecretKey key() {
        try {
            return keystoreProvider.getDerivedKey(keyAliasInKeyStore, AesGcmCipher::deriveKey);
        } catch (IOException e) {
            throw new RuntimeException("Cannot derive the key for alias: " + keyAliasInKeyStore, e);
        }
    }
}
