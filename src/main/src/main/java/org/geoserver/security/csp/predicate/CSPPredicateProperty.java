/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;
import org.geoserver.security.csp.CSPHttpRequestWrapper;
import org.geoserver.security.csp.CSPUtils;

/**
 * CSP predicate that tests if the specified property matches the provided regular expression. The system property value
 * and whether it is valid is cached during instantiation.
 */
public class CSPPredicateProperty implements CSPPredicate {

    /** The property key */
    private final String key;

    /** The regular expression for valid property values */
    private final Pattern valueRegex;

    /** The system property value */
    private final String propertyValue;

    /** Cached flag for whether the system property is valid */
    private final boolean propertyMatches;

    /**
     * @param key the property key
     * @param valueRegex the regular expression for valid property values
     * @throws IllegalArgumentException if not a GeoServer/GeoTools/GeoWebCache property or the regular expression is
     *     not valid
     */
    public CSPPredicateProperty(String key, String valueRegex) {
        Preconditions.checkArgument(
                CSPUtils.PROPERTY_KEY_REGEX.matcher(key).matches(), "Property key not allowed: %s", key);
        this.key = key;
        this.valueRegex = Pattern.compile(valueRegex);
        this.propertyValue = CSPUtils.getStringProperty(this.key, "");
        this.propertyMatches = this.valueRegex.matcher(this.propertyValue).matches();
    }

    /**
     * Returns the cached value if the specified system property contained a non-empty value. Otherwise, checks if the
     * key matches a field in the {@code CSPConfiguration} object and tests it against the regular expression.
     *
     * @return whether the property value matches the provided regular expression
     */
    @Override
    public boolean test(CSPHttpRequestWrapper request) {
        if (this.propertyValue.isEmpty()) {
            String value = request.getConfig().getField(this.key);
            if (!value.isEmpty()) {
                return this.valueRegex.matcher(value).matches();
            }
        }
        return this.propertyMatches;
    }
}
