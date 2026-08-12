/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.apache.wicket.core.util.crypt.AbstractJceCrypt;

/**
 * Encrypts URL parameters with AES-CBC, using an initialization vector (IV) derived from the key. The same URL always
 * gives the same encrypted text inside one session, as it did with the class Wicket installs by default.
 *
 * <p>Same cipher as Wicket's own {@link org.apache.wicket.core.util.crypt.AESCrypt}, {@code AES/CBC/PKCS5Padding}. The
 * one difference is the vector: {@code AESCrypt} draws a new random one for every URL and writes it in front of the
 * encrypted text. That cannot be used here. Wicket renders a page, builds the URL for that page a second time and
 * compares it with the URL the browser asked for, in {@code WebPageRenderer}; when the two differ it answers with a
 * redirect. A new vector each time makes them always differ, so the browser is sent in a circle. Wicket's default
 * class, {@code SunJceCrypt}, gives a repeatable text just like this one; its cipher is a password based one and no
 * FIPS-validated provider offers it.
 */
class AesCbcCrypt extends AbstractJceCrypt {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private final SecretKey key;
    private final IvParameterSpec iv;

    AesCbcCrypt(SecretKey key) {
        this.key = key;
        this.iv = new IvParameterSpec(fixedIv(key));
    }

    /** A vector tied to the key, so no all zero vector ends up in the source; the key is already per session. */
    private static byte[] fixedIv(SecretKey key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
            return Arrays.copyOf(digest, IV_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot derive the vector to encrypt URLs with", e);
        }
    }

    @Override
    protected byte[] encrypt(byte[] plainBytes) {
        return crypt(Cipher.ENCRYPT_MODE, plainBytes);
    }

    @Override
    protected byte[] decrypt(byte[] encryptedBytes) {
        return crypt(Cipher.DECRYPT_MODE, encryptedBytes);
    }

    private byte[] crypt(int mode, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, iv);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Could not " + (mode == Cipher.ENCRYPT_MODE ? "encrypt" : "decrypt")
                            + " an URL, check the crypto provider offers " + TRANSFORMATION,
                    e);
        }
    }
}
