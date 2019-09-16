/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.api.Link;
import org.geoserver.api.SampleDataProvider;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Applies all {@link SampleDataProvider} found in the application context to the layer, returning
 * the required links
 */
@Component
public class SampleDataSupport implements ApplicationContextAware {
    private List<SampleDataProvider> providers;

    public List<Link> getSampleDataFor(LayerInfo layer) {
        return providers
                .stream()
                .flatMap(p -> p.getSampleData(layer).stream())
                .collect(Collectors.toList());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        providers = GeoServerExtensions.extensions(SampleDataProvider.class, applicationContext);
    }
}
