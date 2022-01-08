/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.solr;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.FeatureTypeCallback;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataAccess;
import org.geotools.data.solr.SolrDataStore;
import org.geotools.data.solr.SolrLayerConfiguration;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Implementation of FeatureTypeInitializer extension point to initialize SOLR datastore.
 *
 * @see {@link FeatureTypeCallback}
 */
public class SolrFeatureTypeCallback implements FeatureTypeCallback, CatalogListener {

    public SolrFeatureTypeCallback() {}

    @Override
    public boolean canHandle(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        if (dataAccess instanceof SolrDataStore) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean initialize(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException {
        SolrLayerConfiguration configuration =
                (SolrLayerConfiguration) info.getMetadata().get(SolrLayerConfiguration.KEY);
        if (configuration != null) {
            SolrDataStore dataStore = (SolrDataStore) dataAccess;
            dataStore.setSolrConfigurations(configuration);
        }
        // we never use the temp name
        return false;
    }

    @Override
    public void dispose(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException {
        SolrLayerConfiguration configuration =
                (SolrLayerConfiguration) info.getMetadata().get(SolrLayerConfiguration.KEY);
        SolrDataStore dataStore = (SolrDataStore) dataAccess;
        dataStore.getSolrConfigurations().remove(configuration.getLayerName());
    }

    @Override
    public void flush(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess)
            throws IOException {
        // nothing to do
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (event.getSource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo ft = (FeatureTypeInfo) event.getSource();
            Serializable config = ft.getMetadata().get(SolrLayerConfiguration.KEY);
            if (config instanceof SolrLayerConfiguration) {
                updateSolrConfiguration(ft, (SolrLayerConfiguration) config);
            }
        }
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // remove the configuration if the layer is a SOLR one
        if (event.getSource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo ft = (FeatureTypeInfo) event.getSource();
            Serializable config = ft.getMetadata().get(SolrLayerConfiguration.KEY);
            if (config instanceof SolrLayerConfiguration) {
                SolrLayerConfiguration slc = (SolrLayerConfiguration) config;
                // go directly to the resource pool to avoid security wrappers
                try {
                    DataAccess<? extends FeatureType, ? extends Feature> dataStore =
                            getCatalog().getResourcePool().getDataStore(ft.getStore());
                    if (dataStore instanceof SolrDataStore) {
                        SolrDataStore solr = (SolrDataStore) dataStore;
                        solr.getSolrConfigurations().remove(slc.getLayerName());
                    }
                } catch (IOException e) {
                    throw new CatalogException(
                            "Failed to remove layer configuration from data store", e);
                }
            }
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // nothing to do
        // System.out.println(event);
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        // remove the configuration if the layer is a SOLR one
        if (event.getSource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo ft = (FeatureTypeInfo) event.getSource();
            Serializable config = ft.getMetadata().get(SolrLayerConfiguration.KEY);
            if (config instanceof SolrLayerConfiguration) {
                SolrLayerConfiguration slc = (SolrLayerConfiguration) config;
                if (!ft.getName().equals(slc.getLayerName())) {
                    updateSolrConfiguration(ft, slc);
                }
            }
        }
    }

    private void updateSolrConfiguration(FeatureTypeInfo ft, SolrLayerConfiguration slc) {
        // go directly to the resource pool to avoid security wrappers
        try {
            DataAccess<? extends FeatureType, ? extends Feature> dataStore =
                    getCatalog().getResourcePool().getDataStore(ft.getStore());
            if (dataStore instanceof SolrDataStore) {
                SolrDataStore solr = (SolrDataStore) dataStore;
                solr.getSolrConfigurations().remove(slc.getLayerName());
                slc.setLayerName(ft.getName());
                solr.setSolrConfigurations(slc);
            }
        } catch (IOException e) {
            throw new CatalogException("Failed to remove layer configuration from data store", e);
        }
        FeatureTypeInfo proxy = getCatalog().getFeatureType(ft.getId());
        proxy.setNativeName(ft.getName());
        proxy.getMetadata().put(SolrLayerConfiguration.KEY, slc);
        getCatalog().save(proxy);
    }

    Catalog getCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    @Override
    public void reloaded() {
        // nothing to do
    }

    /**
     * Spring bean used for doing initializing logic with beans Catalog and SolrFeatureTypeCallback
     * available and loaded.
     *
     * @author Fernando Mino - Geosolutions
     */
    public static class CatalogInitializer {

        private static final Logger LOGGER = Logging.getLogger(CatalogInitializer.class);

        private Catalog catalog;
        private SolrFeatureTypeCallback solrFeatureTypeCallback;

        public CatalogInitializer(
                Catalog catalog, SolrFeatureTypeCallback solrFeatureTypeCallback) {
            super();
            this.catalog = catalog;
            this.solrFeatureTypeCallback = solrFeatureTypeCallback;
        }

        public void initBean() {
            LOGGER.info("Registering solrFeatureTypeCallback on catalog.");
            catalog.addListener(solrFeatureTypeCallback);
        }
    }
}
