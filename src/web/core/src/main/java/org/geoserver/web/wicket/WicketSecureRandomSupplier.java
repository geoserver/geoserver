/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import org.apache.wicket.core.random.ISecureRandomSupplier;
import org.geoserver.security.CryptoProviders;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

/**
 * Provides the {@link SecureRandom} Wicket uses for session identifiers and nonces. Replaces Wicket's own default,
 * which names {@code SHA1PRNG} and fails wherever a FIPS provider is in force.
 */
public class WicketSecureRandomSupplier implements ISecureRandomSupplier {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web");

    /**
     * Takes the generator of the crypto provider GeoServer registered, when that provider ranks above the JDK ones. A
     * FIPS-validated provider refuses to make a key from any other generator, so its own has to win.
     *
     * <p>Where the JDK providers come first, it asks for {@code DRBG} by name. A plain {@code new SecureRandom()} gives
     * {@code SHA1PRNG} there, while {@code DRBG} is a modern generator the SUN provider offers.
     */
    @SuppressFBWarnings("DMI_RANDOM_USED_ONLY_ONCE") // the generator is kept in Holder and serves every request
    static SecureRandom createSecureRandom() {
        SecureRandom random = new SecureRandom();
        // compared by identity: the registered provider is a single instance, and Provider inherits an equals that
        // compares the whole service map instead
        if (random.getProvider() == CryptoProviders.getProvider()) {
            return random;
        }
        try {
            return SecureRandom.getInstance("DRBG");
        } catch (NoSuchAlgorithmException e) {
            // the FIPS status page reports which generator answers instead, so this needs no detail
            LOGGER.fine("SecureRandom algorithm 'DRBG' is not available, using the provider default instead");
            return random;
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
