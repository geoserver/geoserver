/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.security.csp.CSPHttpRequestWrapper;

/**
 * CSP predicate that tests if the HTTP request query parameters that match the provided key regular expression has a
 * value that matches the provided value regular expression.
 */
public class CSPPredicateParameter implements CSPPredicate {

    /** The regular expression for query parameters to test */
    private final Pattern keyRegex;

    /** The regular expression for valid query parameter values */
    private final Pattern valueRegex;

    /**
     * @param keyRegex the regular expression for query parameters to test
     * @param valueRegex the regular expression for valid query parameter values
     * @throws IllegalArgumentException if either regular expression is not valid
     */
    public CSPPredicateParameter(String keyRegex, String valueRegex) {
        this.keyRegex = Pattern.compile(keyRegex);
        this.valueRegex = Pattern.compile(valueRegex);
    }

    /**
     * Gets the values of all query parameters with keys that match the key regular expression and tests them all
     * against the value regular expression.
     *
     * @return whether the query parameter values match the regular expression
     */
    @Override
    public boolean test(CSPHttpRequestWrapper request) {
        return getQueryParameters(request)
                .allMatch(value -> this.valueRegex.matcher(value).matches());
    }

    /**
     * Gets the values of all query parameters with keys that match the key regular expression. Returns an empty string
     * if no query parameter matched the regular expression.
     *
     * @param request the HTTP request
     * @return the query parameter values
     */
    private Stream<String> getQueryParameters(CSPHttpRequestWrapper request) {
        List<String> values = request.getParameterMap().entrySet().stream()
                .filter(e -> this.keyRegex.matcher(e.getKey()).matches())
                .map(Entry::getValue)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        return values.isEmpty() ? Stream.of("") : values.stream();
    }
}
