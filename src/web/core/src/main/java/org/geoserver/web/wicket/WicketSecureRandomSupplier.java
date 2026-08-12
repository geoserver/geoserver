/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import org.apache.wicket.core.random.ISecureRandomSupplier;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

/**
 * Provides the {@link SecureRandom} Wicket uses for session identifiers and nonces. Replaces Wicket's own default,
 * which names {@code SHA1PRNG} and fails wherever a FIPS provider is in force.
 */
public class WicketSecureRandomSupplier implements ISecureRandomSupplier {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web");

    /**
     * Asks for {@code DRBG} by name. A plain {@code new SecureRandom()} on a normal JDK gives {@code SHA1PRNG}, which a
     * FIPS system does not allow, while {@code DRBG} is a modern generator the SUN provider offers.
     *
     * <p>On a FIPS system the SUN provider is emptied, so {@code DRBG} is not there either. The fallback then gives
     * whatever generator the FIPS provider offers, which is the right one to use on that machine.
     */
    @SuppressFBWarnings("DMI_RANDOM_USED_ONLY_ONCE") // the generator is kept in Holder and serves every request
    static SecureRandom createSecureRandom() {
        try {
            return SecureRandom.getInstance("DRBG");
        } catch (NoSuchAlgorithmException e) {
            // the FIPS status page reports which generator answers instead, so this needs no detail
            LOGGER.fine("SecureRandom algorithm 'DRBG' is not available, using the provider default instead");
            return new SecureRandom();
        }
    }

    /** Held lazily: the generator has to be picked after the crypto provider is registered, not at class load. */
    private static class Holder {
        static final SecureRandom RANDOM = createSecureRandom();
    }

    @Override
    public SecureRandom getRandom() {
        return Holder.RANDOM;
    }
}
