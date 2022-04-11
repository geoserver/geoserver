/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;
import java.util.Optional;
import org.geoserver.ows.util.KvpMap;

/** Mangles service URL's based on the AcceptLanguages and Languages parameters */
public class LanguageURLMangler implements URLMangler {

    public static final String ACCEPT_LANGUAGES = "AcceptLanguages";
    public static final String LANGUAGE = "Language";

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        if (Dispatcher.REQUEST.get() == null || !type.equals(URLType.SERVICE)) return;

        final Map<String, Object> rawKvp =
                Optional.ofNullable(Dispatcher.REQUEST.get().rawKvp).orElse(new KvpMap<>());

        String language = "";

        if (rawKvp.containsKey(ACCEPT_LANGUAGES)) {
            String acceptLanguages = (String) rawKvp.get(ACCEPT_LANGUAGES);
            if (acceptLanguages != null) {
                String commaSplit = acceptLanguages.split(",")[0];
                String spaceSplit = commaSplit.split(" ")[0];
                if (spaceSplit.length() >= 1) {
                    language = spaceSplit;
                } else {
                    language = commaSplit;
                }
            }
        }

        if (rawKvp.containsKey(LANGUAGE)) {
            language = (String) rawKvp.get(LANGUAGE);
        }

        if (language != null && language.length() > 0) {
            kvp.put(LANGUAGE, language.trim());
        }
    }
}
