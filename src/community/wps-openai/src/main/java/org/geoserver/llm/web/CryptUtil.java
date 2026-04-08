/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm.web;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;

/** Utilities to encrypt and decrypt OpenAI Key for storage */
public class CryptUtil {
    private static final GeoServerSecurityManager securityManager =
            GeoServerExtensions.bean(GeoServerSecurityManager.class);

    public static String encrypt(String value) {
        return value != null
                ? securityManager.getConfigPasswordEncryptionHelper().encode(value)
                : null;
    }

    public static String decrypt(String value) {
        return value != null
                ? securityManager.getConfigPasswordEncryptionHelper().decode(value)
                : null;
    }
}
