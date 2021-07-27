/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/** Mangles service URL's based on the AcceptLanguages and Languages parameters */
public class LanguageURLMangler implements URLMangler {

    public static final String ACCEPT_LANGUAGES = "AcceptLanguages";
    public static final String LANGUAGE = "Language";
    private String languageParameter = "";
    private String acceptLanguagesParameter = "";

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        if (Dispatcher.REQUEST.get() == null || !type.equals(URLType.SERVICE)) return;

        String language = "";
        HttpServletRequest httpRequest = Dispatcher.REQUEST.get().getHttpRequest();
        Enumeration<String> parameterNames = httpRequest.getParameterNames();
        Collections.list(parameterNames)
                .forEach(
                        parameter -> {
                            if (parameter.equalsIgnoreCase(LANGUAGE)) {
                                languageParameter = parameter;
                            } else if (parameter.equalsIgnoreCase(ACCEPT_LANGUAGES)) {
                                acceptLanguagesParameter = parameter;
                            }
                        });

        String acceptLanguages = httpRequest.getParameter(acceptLanguagesParameter);
        language = httpRequest.getParameter(languageParameter);

        if (acceptLanguages != null) {
            String comaSplit = acceptLanguages.split(",")[0];
            String spaceSplit = comaSplit.split(" ")[0];
            if (spaceSplit.length() >= 1) {
                language = spaceSplit;
            } else {
                language = comaSplit;
            }
        }

        if (language != null && language.length() > 0) {
            kvp.put("Language", language.trim());
        }
    }
}
