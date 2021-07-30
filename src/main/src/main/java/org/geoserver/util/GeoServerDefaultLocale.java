/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.Locale;

/**
 * A utility class that provide methods that act on the private ThreadLocal in order to set, get and
 * remove the default GeoServer locale.
 */
public class GeoServerDefaultLocale {

    private static final ThreadLocal<Locale> DEFAULT_LOCALE = new InheritableThreadLocal<>();

    /** @param locale set the default locale to the underlying thread local. */
    public static void set(Locale locale) {
        DEFAULT_LOCALE.set(locale);
    }

    /**
     * @return the default GeoServer locale if set. Otherwise returns {@link Locale#getDefault()}.
     */
    public static Locale get() {
        return DEFAULT_LOCALE.get();
    }

    /** remove the default locale from the underlying ThreadLocal. */
    public static void remove() {
        DEFAULT_LOCALE.remove();
    }
}
