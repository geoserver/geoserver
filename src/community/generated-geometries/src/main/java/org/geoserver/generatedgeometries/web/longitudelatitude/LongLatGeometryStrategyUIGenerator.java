/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.web.longitudelatitude;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy;
import org.geoserver.generatedgeometries.web.GeometryGenerationStrategyUIGenerator;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.Serializable;

public class LongLatGeometryStrategyUIGenerator implements GeometryGenerationStrategyUIGenerator, Serializable {

    private static final long serialVersionUID = 1L;

    private LongLatGeometryConfigurationPanel longLatConfigPanel;

    private final LongLatGeometryGenerationStrategy strategy;

    public LongLatGeometryStrategyUIGenerator(LongLatGeometryGenerationStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public String getName() {
        return strategy.getName();
    }

    @Override
    public Component createUI(String id, IModel model) {
        if (longLatConfigPanel == null) {
            longLatConfigPanel = new LongLatGeometryConfigurationPanel(id, model);
        }
        return longLatConfigPanel;
    }

    @Override
    public void configure(FeatureTypeInfo info) throws ConfigurationException {
        strategy.setCongiguration(longLatConfigPanel.getLongLatConfiguration());
        strategy.configure(info);
    }
}
