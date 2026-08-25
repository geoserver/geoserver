/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import org.geoserver.platform.security.KeyStoreFormat;
import org.geoserver.platform.security.SecurityDefaults;

/**
 * Tells GeoServer which security components to use in a new data directory under FIPS.
 *
 * <p>The normal defaults do not work there. The {@code crypt1} and {@code crypt2} encoders and the standard master
 * password provider all encrypt with a password, and FIPS forbids that. {@code crypt2} also needs an algorithm that
 * only the regular BouncyCastle jars provide.
 *
 * <p>The replacements all ship with GeoServer core and use common algorithms, so any provider can run them. A FIPS
 * installation has no other option. They are not used by default because data dirs are encoded with the old defaults,
 * and the new ones would not read them.
 *
 * <p>The names are strings instead of classes on purpose. This module then depends on gs-platform alone, never on
 * gs-main, so gs-main can depend on this module instead during the fips-test builds.
 *
 * <p>Returns nothing when the regular BouncyCastle jars are on the classpath. The FIPS provider does not start in that
 * case, so nothing could read a BCFKS keystore and the new data directory would be broken. GeoServer's own defaults at
 * least work, and {@link FipsCryptoProviderSupplier} reports the real problem.
 */
public class FipsSecurityDefaults implements SecurityDefaults {

    /** Bean name of the AES-GCM password encoder, the {@code crypt3} prefix. */
    static final String AES_GCM_ENCODER = "aesGcmPasswordEncoder";

    /** BouncyCastle's own keystore format, the only one the FIPS-validated provider offers. */
    static final String BCFKS_KEYSTORE = KeyStoreFormat.BCFKS.name();

    static final String AES_GCM_MASTER_PASSWORD_PROVIDER =
            "org.geoserver.security.password.AesGcmMasterPasswordProvider";

    @Override
    public String get(Setting setting) {
        if (!FipsSetup.isFipsClasspath()) {
            return null;
        }
        return switch (setting) {
            case CONFIG_PASSWORD_ENCODER, USER_GROUP_PASSWORD_ENCODER -> AES_GCM_ENCODER;
            case MASTER_PASSWORD_PROVIDER -> AES_GCM_MASTER_PASSWORD_PROVIDER;
            case KEYSTORE_TYPE -> BCFKS_KEYSTORE;
        };
    }
}
