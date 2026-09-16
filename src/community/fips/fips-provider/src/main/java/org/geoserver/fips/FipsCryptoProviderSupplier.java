/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import java.security.Provider;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.geoserver.platform.security.CryptoProviderSupplier;

/**
 * Selects the FIPS-validated BouncyCastle provider, in approved-only mode and ahead of the JDK providers.
 *
 * <p>If this module is on the classpath, the deployment is a FIPS one. There is no switch to turn it off, because the
 * jars it brings replace the regular BouncyCastle ones rather than sit next to them.
 *
 * <p>An installation that ended up with both sets of jars cannot work at all, so {@link #getProvider()} refuses to
 * start and its message says which jars to remove. The only place that classpath is built on purpose is
 * {@code FipsWithoutFipsJarsTest}, which checks that refusal.
 */
public class FipsCryptoProviderSupplier implements CryptoProviderSupplier {

    @Override
    public Provider getProvider() {
        if (!FipsSetup.isFipsClasspath()) {
            throw new IllegalStateException("The FIPS modules are installed but the regular BouncyCastle jars are on"
                    + " the classpath, where the FIPS-validated ones have to replace them. GeoServer cannot run FIPS"
                    + " this way; remove the regular jars, or remove the FIPS modules.");
        }
        // approved-only mode makes non-approved algorithms throw rather than silently run. The
        // provider reads it while its class initializes, so it has to be set before that happens.
        if (System.getProperty(FipsSetup.APPROVED_ONLY_PROPERTY) == null) {
            System.setProperty(FipsSetup.APPROVED_ONLY_PROPERTY, "true");
        }
        return new BouncyCastleFipsProvider();
    }

    @Override
    public int getPosition() {
        return 1;
    }
}
