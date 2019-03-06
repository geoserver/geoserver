/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.web;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;

/**
 * Extension point for generating UI dedicated for particular {@link
 * org.geoserver.generatedgeometries.core.GeometryGenerationStrategy} implementation. Should be
 * instantiated as Spring bean.
 */
public interface GeometryGenerationStrategyUIGenerator {

    /**
     * The name of the UI that can be used for identifying it. Usually it is the same as name of
     * {@link org.geoserver.generatedgeometries.core.GeometryGenerationStrategy} associated with the
     * generator.
     *
     * @return the name of UI generator
     */
    String getName();

    /**
     * Factory for UI dedicated for the strategy.
     *
     * @param id component id
     * @param model component model
     * @return root of the UI components hierarchy
     */
    Component createUI(String id, IModel model);

    /**
     * Configuration helper for the associated strategy.
     *
     * @param info feature layer info
     */
    void configure(FeatureTypeInfo info);
}
