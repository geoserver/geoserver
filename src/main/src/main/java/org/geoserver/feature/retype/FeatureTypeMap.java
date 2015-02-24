/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A support class containing the old feature type name, the new one, the old
 * feature type, and the new one
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

    public boolean isUnchanged() {
        return originalName.equals(name);
    }

    public void setFeatureTypes(SimpleFeatureType original, SimpleFeatureType transformed) {
        this.originalFeatureType = original;
        this.featureType = transformed;
    }

}
