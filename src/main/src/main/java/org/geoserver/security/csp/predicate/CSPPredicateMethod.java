/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.security.csp.CSPHttpRequestWrapper;
import org.springframework.http.HttpMethod;

/** CSP predicate that tests if the HTTP request method matches the provided set of methods. */
public class CSPPredicateMethod implements CSPPredicate {

    /** Splitter for a comma-separated list in a string */
    private static final Splitter COMMA_SPLITTER =
            Splitter.on(',').trimResults().omitEmptyStrings();

    /** The set of allowed HTTP methods */
    private final Set<String> methods;

    /**
     * @param methods the comma-separated list of allowed HTTP methods
     * @throws IllegalArgumentException if an invalid HTTP method is provided
     */
    public CSPPredicateMethod(String methods) {
        this.methods = parseMethods(methods);
    }

    /** @return whether the HTTP request method is allowed */
    @Override
    public boolean test(CSPHttpRequestWrapper request) {
        return this.methods.contains(request.getMethod());
    }

    /**
     * Parses the HTTP methods from the provided string and verifies that they are all valid HTTP
     * methods.
     *
     * @param methods the comma-separated list of allowed HTTP methods
     * @return the parsed set of allowed HTTP methods
     * @throws IllegalArgumentException if an invalid HTTP method is provided
     */
    private static Set<String> parseMethods(String methods) {
        Set<String> set =
                COMMA_SPLITTER
                        .splitToStream(methods)
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
        set.forEach(HttpMethod::valueOf);
        return Collections.unmodifiableSet(set);
    }
}
