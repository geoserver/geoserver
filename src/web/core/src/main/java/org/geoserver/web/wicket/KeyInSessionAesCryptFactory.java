/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.core.util.crypt.AbstractKeyInSessionCryptFactory;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.io.IClusterable;

/**
 * Builds the object that encrypts URL parameters, with a new key for each session. See {@link AesCbcCrypt} for the
 * cipher.
 *
 * <p>It replaces the factory Wicket installs by default, which asks SunJCE for {@code PBEWithMD5AndDES}. That cipher
 * takes a password, but a FIPS-validated provider offers no password based cipher at all.
 */
public class KeyInSessionAesCryptFactory extends AbstractKeyInSessionCryptFactory<KeyInSessionAesCryptFactory.Key> {

    /** Holds the key bytes rather than the {@link SecretKey}, which is not serializable across a cluster. */
    public static class Key implements IClusterable {
        private final byte[] bytes;

        Key(byte[] bytes) {
            this.bytes = bytes;
        }
    }

    @Override
    protected Key generateKey(Session session) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(
                    256,
                    Application.get().getSecuritySettings().getRandomSupplier().getRandom());
            return new Key(generator.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot generate a key to encrypt URLs with", e);
        }
    }

    @Override
    protected ICrypt createCrypt(Key key) {
        return new AesCbcCrypt(new SecretKeySpec(key.bytes, "AES"));
    }
}
