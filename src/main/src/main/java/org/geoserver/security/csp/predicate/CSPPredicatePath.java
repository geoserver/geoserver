/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import com.google.common.base.Strings;
import java.util.regex.Pattern;
import org.geoserver.security.csp.CSPHttpRequestWrapper;

/** CSP predicate that tests if the HTTP request path matches the provided regular expression. */
public class CSPPredicatePath implements CSPPredicate {

    /** The regular expression for valid request paths */
    private final Pattern regex;

    /**
     * @param regex the regular expression for valid request paths
     * @throws IllegalArgumentException if the regular expression is not valid
     */
    public CSPPredicatePath(String regex) {
        this.regex = Pattern.compile(regex);
    }

    /** @return whether the HTTP request request path matches the regular expression */
    @Override
    public boolean test(CSPHttpRequestWrapper request) {
        return this.regex.matcher(Strings.nullToEmpty(request.getPathInfo())).matches();
    }
}
