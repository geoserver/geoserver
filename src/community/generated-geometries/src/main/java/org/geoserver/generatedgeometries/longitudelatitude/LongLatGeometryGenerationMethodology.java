/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.longitudelatitude;

import com.google.common.base.Preconditions;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.generatedgeometries.GeometryGenerationMethodology;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.vfny.geoserver.global.ConfigurationException;

/**
 * Implementation of geometry generation methodology for long/lat attributes in the layer.
 */
public class LongLatGeometryGenerationMethodology implements GeometryGenerationMethodology {

    private static final long serialVersionUID = 1L;

    private static final String NAME = "longLat";

    private LongLatGeometryConfigurationPanel ui;

    static class LongLatConfiguration {
        final String attributeName;
        final AttributeDescriptor longAttributeDescriptor;
        final AttributeDescriptor latAttributeDescriptor;

        LongLatConfiguration(
                String attributeName,
                AttributeDescriptor longAttributeDescriptor,
                AttributeDescriptor latAttributeDescriptor) {
            this.attributeName = attributeName;
            this.longAttributeDescriptor = longAttributeDescriptor;
            this.latAttributeDescriptor = latAttributeDescriptor;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SimpleFeatureType defineGeometryAttributeFor(SimpleFeatureType src)
            throws ConfigurationException {
        Preconditions.checkNotNull(
                ui, "configuration cannot be null; createUI() method has not been called");
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor ad : src.getAttributeDescriptors()) {
            sftb.add(ad);
        }

        LongLatConfiguration configuration = ui.getLongLatConfiguration();
        createGeometry(
                sftb,
                configuration.attributeName,
                configuration.longAttributeDescriptor,
                configuration.latAttributeDescriptor);
        return sftb.buildFeatureType();
    }

    private void createGeometry(
            SimpleFeatureTypeBuilder sftb,
            String generatedGeometryAttrName,
            AttributeDescriptor longAttr,
            AttributeDescriptor latAttr) {
        // TODO: implement geometry attribute definition
    }

    @Override
    public SimpleFeature generateGeometry(SimpleFeature simpleFeature) {
        // TODO: create geometry from longitude and latitude attributes
        return simpleFeature;
    }

    @Override
    public Filter convertFilter(Filter filter) throws RuntimeException {
        // TODO: implement
        return filter;
    }

    @Override
    public Component createUI(String id, IModel model) {
        return ui = new LongLatGeometryConfigurationPanel(id, model);
    }
}
