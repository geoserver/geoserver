/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;

/**
 * Uses JDBCOpenSearchAccess.SOURCE_ATTRIBUTE to locate the source attribute name of a given mapped
 * property name
 *
 * @author Andrea Aime - GeoSolutions
 */
class SourcePropertyMapper {

    FeatureType schema;

    public SourcePropertyMapper(FeatureType schema) {
        // TODO: build a reverse map once here and be done with it
        this.schema = schema;
    }

    PropertyDescriptor getDescriptor(String name) {
        if (name.contains(":")) {
            String[] split = name.split(":");
            // TODO: find a way to make use of the prefix, or just get a full namespace...
            String prefix = split[0];
            String localName = split[1];
            for (PropertyDescriptor pd : schema.getDescriptors()) {
                if (localName.equals(pd.getName().getLocalPart())) {
                    return pd;
                }
            }
        } else {
            for (PropertyDescriptor pd : schema.getDescriptors()) {
                if (name.equals(pd.getName().getLocalPart())) {
                    return pd;
                }
            }
        }

        return null;
    }

    String getSourceName(String name) {
        PropertyDescriptor pd = getDescriptor(name);
        if (pd == null) {
            return null;
        } else {
            return (String) pd.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
        }
    }

    PropertyDescriptor getDescriptor(Name name) {
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            if (name.equals(pd.getName())) return pd;
        }
        return null;
    }

    String getSourceName(Name name) {
        PropertyDescriptor pd = getDescriptor(name);
        if (pd == null) {
            return null;
        } else {
            return (String) pd.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
        }
    }
}
