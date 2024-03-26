/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import com.google.common.base.Preconditions;
import org.geoserver.security.csp.CSPHttpRequestWrapper;
import org.geoserver.security.csp.CSPUtils;

/**
 * CSP predicate that tests if the specified class exists. The value is determined and cached during
 * instantiation.
 */
public class CSPPredicateClass implements CSPPredicate {

    /** Cached flag for whether the class exists */
    private final boolean value;

    /**
     * @param name the class name
     * @throws IllegalArgumentException if not a GeoServer/GeoTools/GeoWebCache class
     */
    public CSPPredicateClass(String name) {
        Preconditions.checkArgument(
                CSPUtils.CLASS_NAME_REGEX.matcher(name).matches(),
                "Class name not allowed: %s",
                name);
        this.value = classExists(name);
    }

    /** @return whether a class with the provided name exists */
    @Override
    public boolean test(CSPHttpRequestWrapper request) {
        return this.value;
    }

    /**
     * Checks whether a class with the provided name exists.
     *
     * @param name the class name
     * @return whether the class exists
     */
    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
