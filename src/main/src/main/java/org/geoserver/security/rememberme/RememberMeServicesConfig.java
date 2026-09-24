/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rememberme;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.BaseSecurityNamedServiceConfig;

/**
 * Configuration object for remember me services.
 *
 * <p>The key that signs the remember-me cookie comes from the {@link #KEY_PROPERTY} property when set, otherwise from a
 * random value generated once per run. A cluster shares cookies by setting the property to the same value on every
 * node. Without it each node uses its own random key, so a cookie is valid only on the node that issued it and only
 * until that node restarts.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RememberMeServicesConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;

    /** System or environment property that sets the remember-me cookie key across a cluster. */
    public static final String KEY_PROPERTY = "GEOSERVER_REMEMBERME_KEY";

    private static final int RANDOM_KEY_BYTES = 32;

    private static final SecureRandom SR = new SecureRandom();
    private static final String resolvedKey = resolveKey();

    static String resolveKey() {
        String configured = GeoServerExtensions.getProperty(KEY_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        byte[] bytes = new byte[RANDOM_KEY_BYTES];
        SR.nextBytes(bytes);
        return String.format("%064x", new BigInteger(1, bytes));
    }

    public RememberMeServicesConfig() {}

    public RememberMeServicesConfig(RememberMeServicesConfig other) {
        super(other);
    }

    /** Returns the key that signs the remember-me cookie, from {@link #KEY_PROPERTY} or a random value. */
    public String getKey() {
        return resolvedKey;
    }
}
