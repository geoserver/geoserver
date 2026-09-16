/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.security;

import org.geoserver.platform.GeoServerExtensions;

/**
 * Lets a deployment pick the security defaults GeoServer writes into a new data directory, for the cases where its
 * crypto provider cannot work with the usual ones. Without an implementation GeoServer keeps its own defaults.
 *
 * <p>Only add a {@link Setting} when more than one value is workable. An algorithm every provider implements needs no
 * setting, the code just uses it.
 *
 * <p>Implementations are Spring beans, asked only while GeoServer creates configuration that is not there yet. An
 * existing data directory keeps what it was set up with, so installing or removing an implementation never rewrites
 * configuration behind the admin's back.
 */
public interface SecurityDefaults {

    /** A default that a restricted deployment may need to choose differently. */
    enum Setting {
        /** Bean name of the encoder for passwords in the catalog, such as store connection parameters. */
        CONFIG_PASSWORD_ENCODER,
        /** Bean name of the encoder for passwords held by user group services. */
        USER_GROUP_PASSWORD_ENCODER,
        /** Class name of the master password provider implementation storing the master password. */
        MASTER_PASSWORD_PROVIDER,
        /**
         * {@link java.security.KeyStore} type of the GeoServer keystore. Used only when a keystore is created. An
         * existing file has to be read with the type it was written with, so moving an old data directory means
         * converting the file, not changing this setting.
         */
        KEYSTORE_TYPE
    }

    /**
     * The value to use for the given setting, or null to leave GeoServer's own default in place; an implementation only
     * has to answer for the settings it cares about.
     */
    String get(Setting setting);

    /** The value the first implementation offers for the setting, or {@code fallback} when none does. */
    static String get(Setting setting, String fallback) {
        for (SecurityDefaults defaults : GeoServerExtensions.extensions(SecurityDefaults.class)) {
            String value = defaults.get(setting);
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }
}
