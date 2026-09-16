/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import java.security.GeneralSecurityException;
import java.security.Security;
import javax.crypto.Cipher;

/**
 * Lets tests of the jasypt based password encryption skip where the crypto provider cannot run it.
 *
 * <p>The tests are skipped because they cannot apply, not to make a build pass. They cover {@code crypt1} and
 * {@code crypt2}, which a FIPS installation cannot use at all, and the stored values they read back can only exist in
 * an installation that already uses those encoders.
 */
public final class JasyptDefaults {

    /** Provider name the {@code crypt2} encoder asks for. The FIPS jars register under a different one. */
    public static final String BOUNCY_CASTLE = "BC";

    private JasyptDefaults() {}

    /**
     * Jasypt asks for {@code SHA1PRNG} by name to make its salt, and no FIPS-validated provider has it. GeoServer's own
     * classes do not call jasypt, so only the tests comparing against jasypt output are affected.
     */
    public static void assumeUsable() {
        assumeTrue(
                "jasypt salts with SHA1PRNG, which this crypto provider does not offer",
                Security.getProviders("SecureRandom.SHA1PRNG") != null);
    }

    /**
     * The {@code crypt1} and {@code crypt2} encoders, and everything built on them. No FIPS provider has a password
     * based cipher of any kind, so asking for the one jasypt uses answers the question. Ask for the cipher, not for the
     * key factory that goes with it: the key factory can be there while the cipher is not.
     */
    public static void assumePbeUsable() {
        try {
            Cipher.getInstance("PBEWithMD5AndDES");
        } catch (GeneralSecurityException e) {
            assumeNoException("this crypto provider offers no password based cipher", e);
        }
    }

    /**
     * The {@code crypt2} encoder names its provider, unlike {@code crypt1}. The FIPS jars register under another name
     * and have no password based cipher, so the encoder cannot be built there at all.
     */
    public static void assumeStrongPbeUsable() {
        assumeTrue(
                "crypt2 needs the regular BouncyCastle provider, which this classpath does not have",
                Security.getProvider(BOUNCY_CASTLE) != null);
    }
}
