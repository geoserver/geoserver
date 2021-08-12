/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import org.geotools.util.GrowableInternationalString;
import org.opengis.util.InternationalString;

/** Utility class that provides some methods to deal with InternationalString. */
public class InternationalStringUtils {

    /**
     * Create a GrowableInternationalString from an InternationalString.
     *
     * @param internationalString the InternationalString from which create a
     *     GrowableInternationalString.
     * @return a GrowableInternationalString or null.
     */
    public static GrowableInternationalString growable(InternationalString internationalString) {
        GrowableInternationalString result = null;
        if (internationalString != null)
            result = new GrowableInternationalString(internationalString);
        return result;
    }

    /**
     * Return the string value or the default InternationalString value if the string one is null.
     * By default is meant the value that matches the {@link GeoServerDefaultLocale}.
     *
     * @param string the string value.
     * @param internationalString the internationalString instance.
     * @return the string value or the internationalString default value.
     */
    public static String getOrDefault(String string, InternationalString internationalString) {
        String result = string;
        if (result == null && internationalString != null)
            result = internationalString.toString(GeoServerDefaultLocale.get());
        return result;
    }
}
