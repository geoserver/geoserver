/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geoserver.gwc.GWC.tileLayerName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.LegacyTileLayerInfoLoader;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;

/**
 * GeoSever initialization hook that preserves backwards compatible GWC configuration at start up.
 *
 * <p>For instance, this initializer:
 *
 * <ul>
 *   <i> Creates a <data directory>/gwc-gs.xml configuration file if it doesn't exist, populated
 *   with old defaults global configuration for gwc layers based on geoserver layers and layer
 *   groups.
 *   <li>Configures a gwc {@link GeoServerTileLayerInfoImpl tile layer} for every {@link LayerInfo}
 *       and {@link LayerGroupInfo} matching the old defaults, also only if {@code gwc-gs.xml}
 *       didn't already exist.
 *   <li>Upgrades the direct WMS integration configuration from an old config. Before using {@code
 *       gwc-gs.xml} to hold the integrated GWC configuration, the only property configured was
 *       whether the direct WMS integration option was enabled, and it was saved as part of the
 *       {@link WMSInfo} metadata map under the {@code GWC_WMS_Integration} key. This method removes
 *       that key from WMSInfo if present and sets its value to the {@code GWCConfig} instead.
 * </ul>
 *
 * @author groldan
 * @see GeoServerInitializer
 */
public class GWCInitializer implements GeoServerReinitializer {

    private static final Logger LOGGER = Logging.getLogger(GWCInitializer.class);

    /**
     * {@link WMSInfo#getMetadata() WMSInfo metadata} key used in < 2.1.2 to hold the gwc direct wms
     * integration enablement flag
     */
    static String WMS_INTEGRATION_ENABLED_KEY = "GWC_WMS_Integration";

    private final GWCConfigPersister configPersister;

    private final Catalog rawCatalog;

    private final TileLayerCatalog tileLayerCatalog;

    private ConfigurableBlobStore blobStore;

    public GWCInitializer(
            GWCConfigPersister configPersister,
            Catalog rawCatalog,
            TileLayerCatalog tileLayerCatalog) {
        this.configPersister = configPersister;
        this.rawCatalog = rawCatalog;
        this.tileLayerCatalog = tileLayerCatalog;
    }

    /** @see org.geoserver.config.GeoServerInitializer#initialize(org.geoserver.config.GeoServer) */
    public void initialize(final GeoServer geoServer) throws Exception {
        LOGGER.info(
                "Initializing GeoServer specific GWC configuration from "
                        + GWCConfigPersister.GWC_CONFIG_FILE);

        final Version currentVersion = new Version("1.1.0");
        final Resource configFile = configPersister.findConfigFile();
        if (configFile == null || configFile.getType() != Type.RESOURCE) {
            LOGGER.fine(
                    "GWC's GeoServer specific configuration not found, creating with old defaults");
            GWCConfig oldDefaults = GWCConfig.getOldDefaults();
            oldDefaults.setVersion(currentVersion.toString());
            upgradeWMSIntegrationConfig(geoServer, oldDefaults);
            createDefaultTileLayerInfos(oldDefaults);
            configPersister.save(oldDefaults);
        }

        final GWCConfig config = configPersister.getConfig();
        final Version version = new Version(config.getVersion());

        if (version.compareTo(new Version("2.0.0")) < 0 && config.isWMTSEnabled() != null) {
            // setting WMTS enabling information based on old GWC configuration setting
            WMTSInfo globalServiceInfo = geoServer.getFacade().getService(WMTSInfo.class);
            globalServiceInfo.setEnabled(config.isWMTSEnabled());
            geoServer.save(globalServiceInfo);
            // overriding configuration
            config.setWMTSEnabled(null);
            configPersister.save(config);
        }

        if (currentVersion.compareTo(version) > 0) {
            // got the global config file, so old defaults are already in place if need be. Now
            // check whether we need to migrate the configuration from the Layer/GroupInfo metadata
            // maps to the tile layer catalog store
            moveTileLayerInfosToTileLayerCatalog();
            config.setVersion(currentVersion.toString());
            configPersister.save(config);
        }

        final GWCConfig gwcConfig = configPersister.getConfig();
        checkNotNull(gwcConfig);

        // Setting default CacheProvider class if not present
        if (gwcConfig.getCacheProviderClass() == null
                || gwcConfig.getCacheProviderClass().isEmpty()) {
            gwcConfig.setCacheProviderClass(GuavaCacheProvider.class.toString());
            configPersister.save(gwcConfig);
        }

        // Setting default Cache Configuration
        if (gwcConfig.getCacheConfigurations() == null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Setting default CacheConfiguration");
            }
            Map<String, CacheConfiguration> map = new HashMap<String, CacheConfiguration>();
            map.put(GuavaCacheProvider.class.toString(), new CacheConfiguration());
            gwcConfig.setCacheConfigurations(map);
            configPersister.save(gwcConfig);
        } else {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("CacheConfiguration loaded");
            }
        }

        // Change ConfigurableBlobStore behavior
        if (blobStore != null) {
            String cacheProviderClass = gwcConfig.getCacheProviderClass();
            if (!blobStore.getCacheProviders().containsKey(cacheProviderClass)) {
                gwcConfig.setCacheProviderClass(GuavaCacheProvider.class.toString());
                configPersister.save(gwcConfig);
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest(
                            "Unable to find: "
                                    + cacheProviderClass
                                    + ", used default configuration");
                }
            }
            blobStore.setChanged(gwcConfig, true);
            CacheProvider cache = blobStore.getCache();
            // Add all the various Layers to avoid caching
            addLayersToNotCache(cache, gwcConfig);
        }
    }

    /**
     * Migrates the configuration of geoserver tile layers from the Layer/GroupInfo metadata maps to
     * the {@link #tileLayerCatalog}
     */
    private void moveTileLayerInfosToTileLayerCatalog() {

        for (LayerInfo layer : rawCatalog.getLayers()) {
            if (!CatalogConfiguration.isLayerExposable(layer)) {
                continue;
            }
            try {
                GeoServerTileLayerInfo tileLayerInfo;
                tileLayerInfo = LegacyTileLayerInfoLoader.load(layer);
                if (tileLayerInfo != null) {
                    tileLayerCatalog.save(tileLayerInfo);
                    MetadataMap metadata = layer.getMetadata();
                    LegacyTileLayerInfoLoader.clear(metadata);
                    rawCatalog.save(layer);
                }
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Error migrating GWC Tile Layer settings for Layer '"
                                + layer.getName()
                                + "'",
                        e);
            }
        }

        for (LayerGroupInfo layer : rawCatalog.getLayerGroups()) {
            try {
                GeoServerTileLayerInfo tileLayerInfo;
                tileLayerInfo = LegacyTileLayerInfoLoader.load(layer);
                if (tileLayerInfo != null) {
                    tileLayerCatalog.save(tileLayerInfo);
                    MetadataMap metadata = layer.getMetadata();
                    LegacyTileLayerInfoLoader.clear(metadata);
                    rawCatalog.save(layer);
                }
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Error occurred saving default GWC Tile Layer settings for LayerGroup '"
                                + tileLayerName(layer)
                                + "'",
                        e);
            }
        }
    }

    /**
     * This method is called only the first time the {@link GWCConfig} is initialized and is used to
     * maintain backwards compatibility with the old GWC defaults.
     */
    private void createDefaultTileLayerInfos(final GWCConfig defaultSettings) {
        checkArgument(defaultSettings.isSane());
        for (LayerInfo layer : rawCatalog.getLayers()) {
            if (!CatalogConfiguration.isLayerExposable(layer)) {
                continue;
            }
            try {
                GeoServerTileLayerInfo tileLayerInfo;
                tileLayerInfo = TileLayerInfoUtil.loadOrCreate(layer, defaultSettings);
                tileLayerCatalog.save(tileLayerInfo);
                MetadataMap metadata = layer.getMetadata();
                if (metadata.containsKey(LegacyTileLayerInfoLoader.CONFIG_KEY_ENABLED)) {
                    LegacyTileLayerInfoLoader.clear(metadata);
                    rawCatalog.save(layer);
                }
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Error occurred saving default GWC Tile Layer settings for Layer '"
                                + layer.getName()
                                + "'",
                        e);
            }
        }

        for (LayerGroupInfo layer : rawCatalog.getLayerGroups()) {
            try {
                GeoServerTileLayerInfo tileLayerInfo;
                tileLayerInfo = TileLayerInfoUtil.loadOrCreate(layer, defaultSettings);
                tileLayerCatalog.save(tileLayerInfo);

                MetadataMap metadata = layer.getMetadata();
                if (metadata.containsKey(LegacyTileLayerInfoLoader.CONFIG_KEY_ENABLED)) {
                    LegacyTileLayerInfoLoader.clear(metadata);
                    rawCatalog.save(layer);
                }
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Error occurred saving default GWC Tile Layer settings for LayerGroup '"
                                + tileLayerName(layer)
                                + "'",
                        e);
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
                Boolean storedValue = metadata.get(WMS_INTEGRATION_ENABLED_KEY, Boolean.class);
                if (storedValue != null) {
                    boolean enabled = storedValue.booleanValue();
                    gwcConfig.setDirectWMSIntegrationEnabled(enabled);
                    metadata.remove(WMS_INTEGRATION_ENABLED_KEY);
                    geoServer.save(service);
                }
            }
        }
    }

    /**
     * Private method for adding all the Layer that must not be cached to the {@link CacheProvider}
     * instance.
     */
    private void addLayersToNotCache(CacheProvider cache, GWCConfig defaultSettings) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Adding Layers to avoid In Memory Caching");
        }
        List<PublishedInfo> publisheds = new ArrayList<>(rawCatalog.getLayers());
        publisheds.addAll(rawCatalog.getLayerGroups());
        publisheds
                .parallelStream()
                .forEach(
                        layer -> {
                            try {
                                // Check if the Layer must not be cached
                                GeoServerTileLayerInfo tileLayerInfo =
                                        tileLayerCatalog.getLayerById(layer.getId());
                                if (tileLayerInfo != null
                                        && tileLayerInfo.isEnabled()
                                        && !tileLayerInfo.isInMemoryCached()) {
                                    // Add it to the cache
                                    synchronized (cache) {
                                        cache.addUncachedLayer(tileLayerInfo.getName());
                                    }
                                }
                            } catch (RuntimeException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Error occurred retrieving Layer '" + layer.getName() + "'",
                                        e);
                            }
                        });
    }

    /** Setter for the blobStore parameter */
    public void setBlobStore(ConfigurableBlobStore blobStore) {
        this.blobStore = blobStore;
    }
}
