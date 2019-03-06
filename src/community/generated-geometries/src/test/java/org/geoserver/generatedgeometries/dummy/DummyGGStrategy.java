/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.dummy;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.generatedgeometries.core.GeometryGenerationStrategy;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

public class DummyGGStrategy implements GeometryGenerationStrategy {

    public static final String NAME = "dummy";

    public boolean configured;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public FeatureType defineGeometryAttributeFor(FeatureTypeInfo info, FeatureType featureType) {
        return featureType;
    }

    @Override
    public Feature generateGeometry(
            FeatureTypeInfo info, FeatureType schema, Feature simpleFeature) {
        return simpleFeature;
    }

    @Override
    public Filter convertFilter(FeatureTypeInfo featureTypeInfo, Filter filter)
            throws RuntimeException {
        return filter;
    }

    @Override
    public Query convertQuery(FeatureTypeInfo info, Query query) {
        return query;
    }

    @Override
    public void configure(FeatureTypeInfo info) {
        configured = true;
    }

    @Override
    public boolean canHandle(FeatureTypeInfo info, FeatureType featureType) {
        return true;
    }
}
