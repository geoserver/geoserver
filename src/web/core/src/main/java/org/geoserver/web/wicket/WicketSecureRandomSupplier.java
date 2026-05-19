package org.geoserver.web.wicket;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import org.apache.wicket.core.random.ISecureRandomSupplier;
import org.geotools.util.logging.Logging;

public class WicketSecureRandomSupplier implements ISecureRandomSupplier {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web");

    private static final class Holder {
        private static final SecureRandom INSTANCE;

        static {
            SecureRandom secureRandom;
            try {
                secureRandom = SecureRandom.getInstance("DRBG");
            } catch (NoSuchAlgorithmException e1) {
                LOGGER.warning("SecureRandom algorithm 'DRBG' is not available. "
                        + "Falling back to default SecureRandom implementation. "
                        + "This may affect the strength or compliance of generated randomness ");
                secureRandom = new SecureRandom();
            }
            INSTANCE = secureRandom;
        }
    }

    @Override
    public SecureRandom getRandom() {
        return Holder.INSTANCE;
    }
}
