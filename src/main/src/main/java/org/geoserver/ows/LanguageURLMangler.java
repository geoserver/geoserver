/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Mangles service for i18n AcceptLanguages and Languages parameters.
 *
 * <p>Based on the documentation
 * https://docs.geoserver.org/latest/en/user/services/internationalization/index.html this class
 * provide the capability to handle the AcceptLanguages and also the custom INSPIRE extension
 * Language parameter
 * (https://docs.geoserver.org/stable/en/user/extensions/inspire/using.html#internationalization-support)
 * for support capabilities in multiple languages
 */
public class LanguageURLMangler implements URLMangler {

    public static final String ACCEPT_LANGUAGES = "AcceptLanguages";
    public static final String LANGUAGE = "Language";

    private final Predicate<Object> filterEmtpyString = language -> !((String) language).isEmpty();

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        if (URLType.SERVICE.equals(type)) {
            processLanguageParam()
                    .ifPresent(maybeLanguage -> kvp.put(LANGUAGE, (String) maybeLanguage));
        }
    }

    /**
     * @return Getting the rawKvp from the Request inside the thread local search for parameter
     *     Language or AcceptLanguages and return the first one if present or the first occurrence
     *     of a language code (if present) inside the second one
     */
    protected Optional<Object> processLanguageParam() {
        return Optional.ofNullable(Dispatcher.REQUEST.get())
                .map(Request::getRawKvp)
                .map(rawKvp -> getLanguage(rawKvp).orElse(getAcceptLanguages(rawKvp).orElse("")))
                .filter(filterEmtpyString);
    }

    /**
     * @param rawKvp Map of request parameter
     * @return the value of Language parameter inside the {@link Request#rawKvp} map or an
     *     Optional.empty
     */
    protected Optional<Object> getLanguage(Map<String, Object> rawKvp) {
        return Optional.ofNullable(rawKvp.get(LANGUAGE)).filter(filterEmtpyString);
    }

    /**
     * If exists, take the AcceptLanguages parameter from the rawKvp map parameter, splitting it
     * using the regular expression [\\s,]+ which split by space and comma then take the first one
     * if exists
     *
     * @param rawKvp Map of request parameter
     * @return the first language code found in AcceptLanguages otherwise an Optional.empty
     */
    protected Optional<String> getAcceptLanguages(Map<String, Object> rawKvp) {
        return Optional.ofNullable(rawKvp.get(ACCEPT_LANGUAGES))
                .flatMap(
                        value ->
                                Arrays.stream(((String) value).split("[\\s,]+"))
                                        .findFirst()
                                        .filter(language -> !language.isEmpty()));
    }
}
