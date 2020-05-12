/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Collection;
import java.util.List;
import org.geotools.map.Layer;
import org.geotools.renderer.SymbolizersPreProcessor;

/**
 * Provides the collection of {@link SymbolizersPreProcessor} available for the execution context.
 *
 * @author Fernando Mino - Geosolutions
 */
public interface SymbolizersPreProcessorsProvider {

    /**
     * Returns the collection of {@link SymbolizersPreProcessor} available for the execution
     * context.
     */
    List<SymbolizersPreProcessor> getSymbolizerPreProcessors();

    /**
     * Returns the collection of {@link SymbolizersPreProcessor} available for the execution
     * context, filtered by the provided layers list.
     */
    List<SymbolizersPreProcessor> getSymbolizerPreProcessors(Collection<Layer> layers);
}
