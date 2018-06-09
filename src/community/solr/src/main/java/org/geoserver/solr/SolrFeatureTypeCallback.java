/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.solr;

import java.io.IOException;
import java.io.Serializable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.FeatureTypeCallback;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geotools.data.DataAccess;
import org.geotools.data.solr.SolrDataStore;
import org.geotools.data.solr.SolrLayerConfiguration;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Implementation of FeatureTypeInitializer extension point to initialize SOLR datastore
 *
 * @see {@link FeatureTypeCallback}
 */
public class SolrFeatureTypeCallback implements FeatureTypeCallback, CatalogListener {

    Catalog catalog;

    public SolrFeatureTypeCallback(Catalog catalog) {
        this.catalog = catalog;
        catalog.addListener(this);
    }

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
                            catalog.getResourcePool().getDataStore(ft.getStore());
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
        System.out.println(event);
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
                    catalog.getResourcePool().getDataStore(ft.getStore());
            if (dataStore instanceof SolrDataStore) {
                SolrDataStore solr = (SolrDataStore) dataStore;
                solr.getSolrConfigurations().remove(slc.getLayerName());
                slc.setLayerName(ft.getName());
                solr.setSolrConfigurations(slc);
            }
        } catch (IOException e) {
            throw new CatalogException("Failed to remove layer configuration from data store", e);
        }
        FeatureTypeInfo proxy = catalog.getFeatureType(ft.getId());
        proxy.setNativeName(ft.getName());
        proxy.getMetadata().put(SolrLayerConfiguration.KEY, slc);
        catalog.save(proxy);
    }

    @Override
    public void reloaded() {
        // nothing to do
    }
}
