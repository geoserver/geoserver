package org.geoserver.schemalessfeatures.mongodb.response;

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import java.util.List;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geoserver.wfs.json.ComplexGeoJsonWriterOptions;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.ComplexType;

public class MongoComplexGeoJsonWriterOptions implements ComplexGeoJsonWriterOptions {

    @Override
    public boolean canHandle(List<FeatureCollection> features) {
        boolean result = false;
        if (features != null && !features.isEmpty())
            result =
                    features.stream()
                            .allMatch(
                                    f ->
                                            f.getSchema() != null
                                                    && f.getSchema() instanceof DynamicFeatureType);
        return result;
    }

    @Override
    public boolean encodeComplexAttributeType() {
        return false;
    }

    @Override
    public boolean encodeNestedFeatureAsProperty(ComplexType complexType) {
        return true;
    }
}
