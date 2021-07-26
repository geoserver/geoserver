/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;

/** Mangles service URL's based on the AcceptLanguages and Languages parameters */
public class LanguageURLMangler implements URLMangler {

    public static final String ACCEPT_LANGUAGES = "AcceptLanguages";
    public static final String LANGUAGE = "Language";

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        if (Dispatcher.REQUEST.get() == null) return;

        String language = "";
        Map<String, String[]> parameterMap =
                Dispatcher.REQUEST.get().getHttpRequest().getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(LANGUAGE)
                    && entry.getValue() != null
                    && entry.getValue()[0] != null) language = entry.getValue()[0];
            if (entry.getKey().equalsIgnoreCase(ACCEPT_LANGUAGES)
                    && entry.getValue() != null
                    && entry.getValue()[0] != null) language = entry.getValue()[0];
        }

        if (language.length() > 0) {
            kvp.put("language", language.trim());
        }
    }
}
