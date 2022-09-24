/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
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
     * Combine default text and translations.
     *
     * @param textDefault Default text
     * @param textTranslations Available translations
     * @return
     */
    public static GrowableInternationalString growable(
            InternationalString textTranslations, String textDefault) {
        if (textTranslations != null) {
            if (textTranslations instanceof GrowableInternationalString) {
                GrowableInternationalString translations =
                        (GrowableInternationalString) textTranslations;
                GrowableInternationalString combined = new GrowableInternationalString(textDefault);
                for (Locale locale : translations.getLocales()) {
                    combined.add(locale, translations.toString(locale));
                }
                if (combined.toString(GeoServerDefaultLocale.get()) == null) {
                    combined.add(GeoServerDefaultLocale.get(), textDefault);
                }
                return combined;
            }
        }
        return new GrowableInternationalString(textDefault);
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

    /**
     * Single line alternative to use of conditional operator returning first non-null value.
     *
     * @param strings
     * @return first non-null string
     */
    public static String firstNonNull(String... strings) {
        if (strings == null || strings.length == 0) return null;

        for (String string : strings) {
            if (string != null) {
                return string;
            }
        }
        return null;
    }

    /**
     * First non-blank string ({@code null} and whitespace skipped).
     *
     * <p>The last item (even if empty) will be used if non of the earlier ones were suitable
     * allowing {@code ""} to be returned as the final fallback.
     *
     * @param strings
     * @return first non-blank string, or {@code null} if none found
     */
    public static String firstNonBlank(String... strings) {
        if (strings == null || strings.length == 0) return null;

        for (String string : strings) {
            if (StringUtils.isNotBlank(string)) {
                return string;
            }
        }
        return null;
    }
}
