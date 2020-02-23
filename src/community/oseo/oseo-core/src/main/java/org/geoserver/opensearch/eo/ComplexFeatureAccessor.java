/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

/**
 * Utility class to get complex feature attribute values (actual values, not property wrappers). Can
 * be used to simplify access to values when they are single valued and do not involve
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ComplexFeatureAccessor {

    /**
     * Returns a single attribute value assuming the attribute is in the same namespace as the
     * feature
     */
    public static Object value(Feature feature, String attribute) {
        String prefix = feature.getType().getName().getNamespaceURI();
        return value(feature, prefix, attribute);
    }

    /** Returns a single attribute value looking it up by qualified name */
    public static Object value(Feature feature, String namespace, String attribute) {
        Property property = feature.getProperty(new NameImpl(namespace, attribute));
        if (property == null) {
            return null;
        } else {
            Object value = property.getValue();
            return value;
        }
    }
}
