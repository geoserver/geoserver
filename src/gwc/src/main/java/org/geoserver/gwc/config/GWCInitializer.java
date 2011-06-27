/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.logging.Logging;
import org.springframework.util.Assert;

public class GWCInitializer implements GeoServerInitializer {

    private static final Logger LOGGER = Logging.getLogger(GWCInitializer.class);

    private final GWCConfigPersister configPersister;

    private final Catalog rawCatalog;

    public GWCInitializer(GWCConfigPersister configPersister, Catalog rawCatalog) {
        this.configPersister = configPersister;
        this.rawCatalog = rawCatalog;
    }

    /**
     * @see org.geoserver.config.GeoServerInitializer#initialize(org.geoserver.config.GeoServer)
     */
    public void initialize(final GeoServer geoServer) throws Exception {
        LOGGER.info("Initializing GeoServer specific GWC configuration from "
                + GWCConfigPersister.GWC_CONFIG_FILE);

        File configFile = configPersister.findConfigFile();
        if (configFile == null) {
            LOGGER.fine("GWC's GeoServer specific configuration not found, creating with old defaults");
            GWCConfig oldDefaults = GWCConfig.getOldDefaults();
            upgradeWMSIntegrationConfig(geoServer, oldDefaults);
            createDefaultTileLayerInfos(oldDefaults);
            configPersister.save(oldDefaults);
        }

        final GWCConfig gwcConfig = configPersister.getConfig();
        Assert.notNull(gwcConfig);
    }

    /**
     * This method is called only the first time the {@link GWCConfig} is initialized and is used
     * maintain backwards compatibility with the old GWC defaults.
     */
    private void createDefaultTileLayerInfos(final GWCConfig gwcConfig) {
        for (LayerInfo layer : rawCatalog.getLayers()) {
            try {
                GeoServerTileLayerInfo tileLayerInfo = GeoServerTileLayerInfo.create(layer,
                        gwcConfig);
                MetadataMap metadata = layer.getMetadata();
                tileLayerInfo.saveTo(metadata);
                rawCatalog.save(layer);
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Error occurred saving default GWC Tile Layer settings for Layer '"
                                + layer.getName() + "'", e);
            }
        }

        for (LayerGroupInfo layer : rawCatalog.getLayerGroups()) {
            try {
                GeoServerTileLayerInfo tileLayerInfo = GeoServerTileLayerInfo.create(layer,
                        gwcConfig);
                MetadataMap metadata = layer.getMetadata();
                tileLayerInfo.saveTo(metadata);
                rawCatalog.save(layer);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,
                        "Error occurred saving default GWC Tile Layer settings for LayerGroup '"
                                + layer.getName() + "'", e);
            }
        }
    }

    /**
     * Before using {@code gwc-gs.xml} to hold the integrated GWC configuration, the only property
     * configured was whether the direct WMS integration option was enabled, and it was saved as
     * part of the {@link WMSInfo} metadata map under the {@code GWC_WMS_Integration} key. This
     * method removes that key from WMSInfo if present and sets its value to the {@code gwcConfig}
     * instead.
     */
    private void upgradeWMSIntegrationConfig(final GeoServer geoServer, final GWCConfig gwcConfig)
            throws IOException {
        // Check whether we're using the old way of storing this information, and get rid of it
        WMSInfo service = geoServer.getService(WMSInfo.class);
        if (service != null) {
            MetadataMap metadata = service.getMetadata();
            if (service != null && metadata != null) {
                String WMS_INTEGRATION_ENABLED_KEY = "GWC_WMS_Integration";
                Boolean storedValue = metadata.get(WMS_INTEGRATION_ENABLED_KEY, Boolean.class);
                if (storedValue != null) {
                    boolean enabled = storedValue.booleanValue();
                    gwcConfig.setDirectWMSIntegrationEnabled(enabled);
                    metadata.remove(WMS_INTEGRATION_ENABLED_KEY);
                    geoServer.save(service);
                    configPersister.save(gwcConfig);
                }
            }
        }
    }

}
