/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.web.data.resource.generatedGeometries.methodology;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.web.data.resource.generatedGeometries.GeometryGenerationMethodology;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

public class DummyGeometryGenerationMethodology implements GeometryGenerationMethodology {

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public SimpleFeatureType defineGeometryAttributeFor(SimpleFeatureType simpleFeatureType) {
        return simpleFeatureType;
    }

    @Override
    public SimpleFeature generateGeometry(SimpleFeature simpleFeature) {
        return simpleFeature;
    }

    @Override
    public Filter convertFilter(Filter filter) throws RuntimeException {
        return filter;
    }

    @Override
    public Component createUI(String id, IModel model) {
        return new DummyGGMPanel(id, model);
    }

}
