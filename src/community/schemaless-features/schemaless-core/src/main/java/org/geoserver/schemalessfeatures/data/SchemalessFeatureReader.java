/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import org.geotools.data.FeatureReader;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public abstract class SchemalessFeatureReader implements FeatureReader<FeatureType, Feature> {

    protected SchemalessFeatureSource featureSource;

    public SchemalessFeatureReader(SchemalessFeatureSource featureSource) {
        this.featureSource = featureSource;
    }

    @Override
    public FeatureType getFeatureType() {
        return featureSource.getSchema();
    }
}
