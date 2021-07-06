/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

public class LanguagesDispatcherCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(LanguagesDispatcherCallback.class);

    // the inspire Language query parameter name
    static final String LANGUAGE_PARAM = "LANGUAGE";

    // ows accept languages param consumed by GeoServer
    // for international content in capabilities
    static final String ACCEPT_LANGUAGES_PARAM = "ACCEPTLANGUAGES";

    @Override
    public Request init(Request request) {
        Map<String, Object> rawKvp = request.getRawKvp();
        if (rawKvp != null && rawKvp.containsKey(LANGUAGE_PARAM)) {
            String value = String.valueOf(rawKvp.get(LANGUAGE_PARAM));
            try {
                Properties mappings = InspireDirectoryManager.get().getLanguagesMappings();
                String isoLang = mappings.getProperty(value);
                String supportedLanguages =
                        mappings.keySet()
                                .stream()
                                .map(s -> s.toString())
                                .collect(Collectors.joining(","));
                if (isoLang == null) {
                    LOGGER.info(
                            "A Language parameter was provided in the request but it cannot be resolved to a ISO lang code."
                                    + " Parameter value is "
                                    + value
                                    + " while supported languages are "
                                    + supportedLanguages);
                }
                rawKvp.put(ACCEPT_LANGUAGES_PARAM, isoLang);
                rawKvp.put(LANGUAGE_PARAM, isoLang);
                if (request.getKvp() != null) {
                    request.getKvp().put(ACCEPT_LANGUAGES_PARAM, isoLang);
                    request.getKvp().put(LANGUAGE_PARAM, isoLang);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return super.init(request);
    }
}
