/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import org.apache.wicket.core.random.ISecureRandomSupplier;
import org.geotools.util.logging.Logging;

/**
 * Provides a {@link SecureRandom} instance for Wicket. Prefers the {@code DRBG} algorithm and falls back to the JVM
 * default if unavailable.
 */
public class WicketSecureRandomSupplier implements ISecureRandomSupplier {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web");

    static SecureRandom createSecureRandom() {
        try {
            return SecureRandom.getInstance("DRBG");
        } catch (NoSuchAlgorithmException e1) {
            LOGGER.warning("SecureRandom algorithm 'DRBG' is not available. "
                    + "Falling back to default SecureRandom implementation.");
            return new SecureRandom();
        }
    }

    private static final class Holder {
        private static final SecureRandom INSTANCE = createSecureRandom();
    }

    @Override
    public SecureRandom getRandom() {
        return Holder.INSTANCE;
    }
}
