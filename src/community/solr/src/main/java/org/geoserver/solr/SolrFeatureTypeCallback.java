/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.solr;

import java.io.IOException;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.FeatureTypeCallback;
import org.geotools.data.DataAccess;
import org.geotools.data.solr.SolrDataStore;
import org.geotools.data.solr.SolrLayerConfiguration;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * 
 * Implementation of FeatureTypeInitializer extension point to initialize SOLR datastore
 * 
 * @see {@link FeatureTypeCallback}
 * 
 */
public class SolrFeatureTypeCallback implements FeatureTypeCallback {

    @Override
    public boolean canHandle(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        if (dataAccess instanceof SolrDataStore) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean initialize(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess, Name temporaryName)
            throws IOException {
        SolrLayerConfiguration configuration = (SolrLayerConfiguration) info.getMetadata().get(
                SolrLayerConfiguration.KEY);
        if (configuration != null) {
            SolrDataStore dataStore = (SolrDataStore) dataAccess;
            dataStore.setSolrConfigurations(configuration);
        }
        // we never use the temp name
        return false;
    }

    @Override
    public void dispose(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess, Name temporaryName)
            throws IOException {
        SolrLayerConfiguration configuration = (SolrLayerConfiguration) info.getMetadata().get(
                SolrLayerConfiguration.KEY);
        SolrDataStore dataStore = (SolrDataStore) dataAccess;
        dataStore.getSolrConfigurations().remove(configuration.getLayerName());
    }

    @Override
    public void flush(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess) throws IOException {
        // nothing to do
    }

}
