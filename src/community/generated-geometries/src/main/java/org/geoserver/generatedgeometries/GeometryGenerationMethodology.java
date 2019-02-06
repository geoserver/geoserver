/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.Serializable;

/**
 * Describes particular methodology used by generated-geometries extension.
 *
 * <p>A methodology is responsible for creating 'on-the-fly' definition of geometry attribute for
 * the layer based on its data definition, as well as generating geometry from the data.
 */
public interface GeometryGenerationMethodology extends Serializable {

    /**
     * The name of the methodology that can be used for identifying it.
     *
     * @return the name
     */
    String getName();

    /**
     * Enhances definition of the feature without geometry with {@link GeometryAttribute} based on
     * particular feature attributes.
     *
     * @param simpleFeatureType source feature type without geometry attribute
     * @return simple feature type with geometry attribute
     */
    SimpleFeatureType defineGeometryAttributeFor(SimpleFeatureType simpleFeatureType)
            throws ConfigurationException;

    /**
     * Generates {@link org.locationtech.jts.geom.Geometry} from simple type's attributes and sets
     * it as default geometry.
     *
     * @param simpleFeature source feature
     * @return simple feature with geometry
     */
    SimpleFeature generateGeometry(SimpleFeature simpleFeature);

    /**
     * Converts given filter operating on source CRS into implemented methodology's CRS.
     *
     * <p>Throws a subclass of {@link RuntimeException} in case of failure.
     *
     * @param filter source filter
     * @return transformed
     */
    Filter convertFilter(Filter filter);

    /**
     * A factory of UI for methodology configuration.
     *
     * @param id identifier of UI component wrapper
     * @param model data model passed to UI
     * @return root of UI component tree
     */
    Component createUI(String id, IModel model);
}
