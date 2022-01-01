/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.LocaleUtils;
import org.geotools.util.logging.Logging;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Wicket does not appear to provide a list of Locales for which a translation is available. This
 * class scans, once, the classpath to find all files named "GeoServerApplication_*.properties" and
 * returns a list of Locale found.
 */
public class LocalizationsFinder {

    static final Logger LOGGER = Logging.getLogger(LocalizationsFinder.class);

    private static List<Locale> LOCALES;

    static {
        try {
            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver(
                            LocalizationsFinder.class.getClassLoader());
            LinkedHashSet<Locale> locales = new LinkedHashSet<>();
            for (Resource resource :
                    resolver.getResources("classpath*:/GeoServerApplication_*.properties")) {
                String name = resource.getFilename();
                if (name != null) {
                    try {
                        int idx = name.lastIndexOf("."); // guaranteed to be there by pattern
                        String language = name.substring("GeoServerApplication_".length(), idx);
                        Locale locale = LocaleUtils.toLocale(language);
                        locales.add(locale);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.FINE,
                                "Skipping file " + name + ", could not extract a Locale from it",
                                e);
                    }
                }
            }
            locales.add(Locale.ENGLISH);
            ArrayList<Locale> localesList = new ArrayList<>(locales);
            localesList.sort(Comparator.comparing(l -> l.toString()));
            LOCALES = Collections.unmodifiableList(localesList);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to lookup UI translations, defaulting to English");
            LOCALES = Arrays.asList(Locale.ENGLISH);
        }
    }

    public static List<Locale> getAvailableLocales() {
        return LOCALES;
    }
}
