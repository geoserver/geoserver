/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import static org.geotools.data.elasticsearch.ElasticLayerConfiguration.KEY;

import org.geoserver.catalog.FeatureTypeCallback;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.api.data.DataAccess;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.elasticsearch.ElasticDataStore;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of FeatureTypeInitializer extension point to initialize Elasticsearch datastore.
 *
 * @see FeatureTypeCallback
 */
class ElasticFeatureTypeCallback implements FeatureTypeCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticFeatureTypeCallback.class);

    @Override
    public boolean canHandle(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        return dataAccess instanceof ElasticDataStore;
    }

    @Override
    public boolean initialize(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName) {

        ElasticLayerConfiguration layerConfig;
        layerConfig = (ElasticLayerConfiguration) info.getMetadata().get(KEY);
        if (layerConfig == null) {
            layerConfig = new ElasticLayerConfiguration(info.getName());
            LOGGER.debug(
                    "Created new empty ElasticSearch layer configuration for {} because none was found in the FeatureTypeInfo metadata",
                    info.getName());
        }

        ((ElasticDataStore) dataAccess).setLayerConfiguration(layerConfig);

        return false;
    }

    @Override
    public void dispose(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName) {
        final ElasticLayerConfiguration layerConfig =
                (ElasticLayerConfiguration) info.getMetadata().get(KEY);
        if (layerConfig != null) {
            layerConfig.getAttributes().stream()
                    .filter(attr -> attr.getName().equals(info.getName()))
                    .findFirst()
                    .ifPresent(attribute -> layerConfig.getAttributes().remove(attribute));
            ((ElasticDataStore) dataAccess).getDocTypes().remove(info.getQualifiedName());
        }
    }

    @Override
    public void flush(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        // nothing to do
    }
}
