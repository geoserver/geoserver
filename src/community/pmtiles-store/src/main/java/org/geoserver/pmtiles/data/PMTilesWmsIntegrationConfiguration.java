/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.data;

import org.geoserver.vectortiles.wms.VectorTilesRequestScaleDenominatorHook;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the GeoServer PMTiles plugin integration with the WMS service.
 *
 * <p>This configuration class defines the beans required for PMTiles integration with GeoServer:
 *
 * <ul>
 *   <li>{@link VectorTilesRequestScaleDenominatorHook} - Sets scale denominator for optimal zoom level selection
 * </ul>
 *
 * @see VectorTilesRequestScaleDenominatorHook
 */
@Configuration(proxyBeanMethods = false)
public class PMTilesWmsIntegrationConfiguration {
    /**
     * Creates the dispatcher callback hook for setting the scale denominator on WMS requests.
     *
     * @return the scale denominator hook bean
     */
    @Bean
    VectorTilesRequestScaleDenominatorHook vectorTilesScaleSetter() {
        return new VectorTilesRequestScaleDenominatorHook();
    }
}
