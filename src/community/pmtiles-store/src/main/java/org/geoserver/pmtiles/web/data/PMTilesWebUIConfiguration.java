/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the GeoServer PMTiles plugin.
 *
 * <p>This configuration class defines the beans required for PMTiles integration with GeoServer:
 *
 * <ul>
 *   <li>{@link DataStorePanelInfo} {@code pmtilesDataStorePanel}: data store edit panel metadata for
 *       {@link PMTilesDataStoreEditPanel}
 * </ul>
 *
 * @see PMTilesPluginConfiguration
 * @see PMTilesWmsIntegrationConfiguration
 */
@Configuration(proxyBeanMethods = false)
public class PMTilesWebUIConfiguration {

    @Bean
    @SuppressWarnings("unchecked")
    DataStorePanelInfo pmtilesDataStorePanel() {
        DataStorePanelInfo panelInfo = new DataStorePanelInfo();
        panelInfo.setId("pmtilesDataStorePanel");
        panelInfo.setFactoryClass(org.geotools.pmtiles.store.PMTilesDataStoreFactory.class);
        Class<?> componentClass = org.geoserver.pmtiles.web.data.PMTilesDataStoreEditPanel.class;
        panelInfo.setComponentClass((Class<StoreEditPanel>) componentClass);
        panelInfo.setIconBase(org.geoserver.pmtiles.web.data.PMTilesDataStoreEditPanel.class);
        panelInfo.setIcon("img/protomaps_icon.svg");
        return panelInfo;
    }
}
