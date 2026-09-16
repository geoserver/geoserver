/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.scramble;
import static org.geoserver.security.SecurityUtils.toBytes;

import java.io.IOException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Master password provider storing the password at a url like {@link URLMasterPasswordProvider}, but protecting it with
 * algorithms every crypto provider offers.
 *
 * <p>The standard provider protects the password with password based encryption, which FIPS does not allow, see
 * {@link AesGcmCipher}. The master password is read before anything else, so an installation limited to approved
 * algorithms cannot start unless a provider like this one is available.
 *
 * <p>The file holds {@code salt || iv || ciphertext}. The salt is kept there because this provider is what unlocks the
 * keystore, so it cannot store anything inside it.
 */
public final class AesGcmMasterPasswordProvider extends AbstractURLMasterPasswordProvider {

    /**
     * Making the key is slow on purpose. The master password is read again on every keystore access, with a new
     * provider each time, so the result cannot be kept in the instance. Caching it here gives away no secret: the key
     * depends on the salt and on a constant of this class, and both can be read already, one in the file holding the
     * password and one in the jar.
     */
    private static volatile DerivedKey derived;

    @Override
    protected byte[] encrypt(char[] passwd) {
        byte[] salt = AesGcmCipher.randomBytes(AesGcmCipher.SALT_LENGTH);
        byte[] plainText = toBytes(passwd);
        try {
            byte[] encrypted = AesGcmCipher.encrypt(key(salt), plainText);
            byte[] result = new byte[salt.length + encrypted.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(encrypted, 0, result, salt.length, encrypted.length);
            return result;
        } finally {
            scramble(plainText);
        }
    }

    @Override
    protected byte[] decrypt(byte[] passwd) {
        if (passwd.length <= AesGcmCipher.SALT_LENGTH) {
            throw new RuntimeException("Stored master password is too short to hold a derivation salt");
        }
        byte[] salt = Arrays.copyOfRange(passwd, 0, AesGcmCipher.SALT_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(passwd, AesGcmCipher.SALT_LENGTH, passwd.length);
        return AesGcmCipher.decrypt(key(salt), encrypted);
    }

    private SecretKey key(byte[] salt) {
        DerivedKey current = derived;
        if (current != null && Arrays.equals(current.salt(), salt)) {
            return current.key();
        }
        char[] password = key();
        try {
            SecretKey key = AesGcmCipher.deriveKey(password, salt);
            derived = new DerivedKey(salt.clone(), key);
            return key;
        } finally {
            scramble(password);
        }
    }

    private record DerivedKey(byte[] salt, SecretKey key) {}

    public static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public void configure(XStreamPersister xp) {
            super.configure(xp);
            xp.getXStream().alias("aesGcmUrlProvider", URLMasterPasswordProviderConfig.class);
        }

        @Override
        public Class<? extends MasterPasswordProvider> getMasterPasswordProviderClass() {
            return AesGcmMasterPasswordProvider.class;
        }

        @Override
        public MasterPasswordProvider createMasterPasswordProvider(MasterPasswordProviderConfig config)
                throws IOException {
            return new AesGcmMasterPasswordProvider();
        }

        @Override
        public SecurityConfigValidator createConfigurationValidator(GeoServerSecurityManager securityManager) {
            return new URLMasterPasswordProvider.URLMasterPasswordProviderValidator(securityManager);
        }
    }
}
