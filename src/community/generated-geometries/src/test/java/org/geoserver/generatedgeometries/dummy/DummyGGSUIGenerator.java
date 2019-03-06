/*
 * (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.dummy;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.generatedgeometries.web.GeometryGenerationStrategyUIGenerator;

public class DummyGGSUIGenerator implements GeometryGenerationStrategyUIGenerator {

    private final DummyGGStrategy dummyGGStrategy;

    public DummyGGSUIGenerator(DummyGGStrategy dummyGGStrategy) {
        this.dummyGGStrategy = dummyGGStrategy;
    }

    @Override
    public String getName() {
        return dummyGGStrategy.getName();
    }

    @Override
    public Component createUI(String id, IModel model) {
        return new DummyGGSPanel(id, model);
    }

    @Override
    public void configure(FeatureTypeInfo info) {
        dummyGGStrategy.configure(info);
    }
}
