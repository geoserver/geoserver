/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.dummy;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.generatedgeometries.GeometryGenerationStrategy;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

public class DummyGeometryGenerationStrategy implements GeometryGenerationStrategy {

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public FeatureType defineGeometryAttributeFor(FeatureTypeInfo info, FeatureType simpleFeatureType) {
        return simpleFeatureType;
    }

    @Override
    public Feature generateGeometry(FeatureTypeInfo info, FeatureType schema, Feature simpleFeature) {
        return simpleFeature;
    }

    @Override
    public Filter convertFilter(FeatureTypeInfo featureTypeInfo, Filter filter) throws RuntimeException {
        return filter;
    }

    @Override
    public Query convertQuery(FeatureTypeInfo info, Query query) {
        return query;
    }

    @Override
    public Component createUI(String id, IModel model) {
        return new DummyGGMPanel(id, model);
    }
}
