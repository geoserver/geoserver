/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.security.Provider;
import java.security.Security;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import org.geoserver.platform.security.CryptoProviderSupplier;
import org.geotools.util.logging.Logging;

/** Registers GeoServer's crypto provider on first use, and returns it from {@link #getProvider()}. */
public final class CryptoProviders {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** Name of the regular BouncyCastle provider class, absent from the FIPS-validated distribution. */
    private static final String BC_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    private static final Provider PROVIDER = register();

    private CryptoProviders() {}

    /**
     * Registers the provider on the first call and returns it. Call it as early as possible, and ignore the result when
     * only the registration is wanted.
     */
    public static Provider getProvider() {
        return PROVIDER;
    }

    private static Provider register() {
        CryptoProviderSupplier supplier =
                ServiceLoader.load(CryptoProviderSupplier.class).findFirst().orElseGet(BouncyCastleSupplier::new);
        Provider provider = supplier.getProvider();
        int position = Security.insertProviderAt(provider, supplier.getPosition());
        if (position == -1) {
            // already there, another class loader or a previous call got here first
            provider = Security.getProvider(provider.getName());
        }
        LOGGER.config("Registered crypto provider " + provider.getName() + " at position " + position);
        return provider;
    }

    /**
     * Used when nothing else supplies a provider. It builds the provider by name rather than by a direct reference,
     * which would tie every GeoServer build to the regular BouncyCastle jars. A FIPS deployment cannot have those on
     * the classpath: it uses the FIPS-validated jars instead, and they have no such class.
     */
    private static class BouncyCastleSupplier implements CryptoProviderSupplier {
        @Override
        public Provider getProvider() {
            try {
                return Class.forName(BC_PROVIDER_CLASS)
                        .asSubclass(Provider.class)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Could not create the default crypto provider " + BC_PROVIDER_CLASS
                                + ". Either the BouncyCastle jars are missing, or they are the FIPS-validated ones and"
                                + " no CryptoProviderSupplier was registered to select the FIPS provider.",
                        e);
            }
        }
    }
}
