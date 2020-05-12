/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.map.Layer;
import org.geotools.renderer.SymbolizersPreProcessor;

/**
 * Provides the SymbolizersPreProcessor collection available on Spring context.
 *
 * @author Fernando Mino - Geosolutions
 */
public class SymbolizersPreProcessorsProviderImpl implements SymbolizersPreProcessorsProvider {

    private List<SymbolizersPreProcessor> preProcessors;

    public SymbolizersPreProcessorsProviderImpl() {}

    @Override
    public List<SymbolizersPreProcessor> getSymbolizerPreProcessors() {
        if (preProcessors != null) {
            return preProcessors;
        } else {
            return buildSymbolizerPreProcessors();
        }
    }

    @Override
    public List<SymbolizersPreProcessor> getSymbolizerPreProcessors(Collection<Layer> layers) {
        if (layers == null) return getSymbolizerPreProcessors();
        return getSymbolizerPreProcessors()
                .stream()
                .filter(pp -> layers.stream().anyMatch(layer -> pp.appliesTo(layer)))
                .collect(Collectors.toList());
    }

    /** Initialize the {@link SymbolizersPreProcessor} collection. */
    private synchronized List<SymbolizersPreProcessor> buildSymbolizerPreProcessors() {
        if (preProcessors == null) {
            preProcessors = GeoServerExtensions.extensions(SymbolizersPreProcessor.class);
        }
        return preProcessors;
    }

    /**
     * Returns the default {@link SymbolizersPreProcessorsProvider} instance for the current Spring
     * context.
     */
    public static SymbolizersPreProcessorsProvider getInstance() {
        return GeoServerExtensions.bean(SymbolizersPreProcessorsProvider.class);
    }
}
