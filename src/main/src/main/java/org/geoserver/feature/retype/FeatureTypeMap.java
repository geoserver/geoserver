/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import org.geotools.data.Join;
import org.geotools.data.Query;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A support class containing the old feature type name, the new one, the old feature type, and the
 * new one
 *
 * @author Andrea Aime
 */
class FeatureTypeMap {
    String originalName;

    String name;

    SimpleFeatureType originalFeatureType;

    SimpleFeatureType featureType;

    public FeatureTypeMap(String originalName, String name) {
        this.originalName = originalName;
        this.name = name;
    }

    public FeatureTypeMap(SimpleFeatureType originalFeatureType, SimpleFeatureType featureType) {
        this.originalFeatureType = originalFeatureType;
        this.featureType = featureType;
        this.originalName = originalFeatureType.getTypeName();
        this.name = featureType.getTypeName();
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getName() {
        return name;
    }

    public SimpleFeatureType getOriginalFeatureType() {
        return originalFeatureType;
    }

    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    /** * Takes into account eventual joins */
    public SimpleFeatureType getFeatureType(Query query) {
        SimpleFeatureType result;
        if (query.getPropertyNames() != Query.ALL_NAMES) {
            result = SimpleFeatureTypeBuilder.retype(featureType, query.getPropertyNames());
        } else {
            result = featureType;
        }

        // add back the joined features in case of join
        if (!query.getJoins().isEmpty()) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(result);
            for (Join join : query.getJoins()) {
                String joinedFeatureAttribute = join.getAlias();
                if (joinedFeatureAttribute == null) {
                    joinedFeatureAttribute = join.getTypeName();
                }
                tb.add(joinedFeatureAttribute, SimpleFeature.class);
            }
            result = tb.buildFeatureType();
        }

        return result;
    }

    public boolean isUnchanged() {
        return originalName.equals(name);
    }

    public void setFeatureTypes(SimpleFeatureType original, SimpleFeatureType transformed) {
        this.originalFeatureType = original;
        this.featureType = transformed;
    }
}
