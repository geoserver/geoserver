/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package mil.nga.giat.elasticsearch;

import java.io.IOException;

import mil.nga.giat.data.elasticsearch.ElasticDataStore;
import mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration;
import static mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration.KEY;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.FeatureTypeCallback;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * 
 * Implementation of FeatureTypeInitializer extension point to initialize 
 * Elasticsearch datastore.
 * 
 * @see {@link FeatureTypeCallback}
 * 
 */
public class ElasticFeatureTypeCallback implements FeatureTypeCallback {

    @Override
    public boolean canHandle(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        if (dataAccess instanceof ElasticDataStore) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean initialize(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess, Name temporaryName)
                    throws IOException {

        ElasticLayerConfiguration layerConfig;
        layerConfig = (ElasticLayerConfiguration) info.getMetadata().get(KEY);
        if (layerConfig == null) {
            layerConfig = new ElasticLayerConfiguration(info.getName());
        }

        ((ElasticDataStore) dataAccess).setLayerConfiguration(layerConfig);

        return false;
    }

    @Override
    public void dispose(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess, Name temporaryName)
                    throws IOException {
        ElasticLayerConfiguration layerConfig;
        layerConfig = (ElasticLayerConfiguration) info.getMetadata().get(KEY);
        if (layerConfig != null) {
            layerConfig.getAttributes().remove(info.getName());
            ((ElasticDataStore) dataAccess).getDocTypes().remove(info.getQualifiedName());
        }
    }

    @Override
    public void flush(FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess) throws IOException {
        // nothing to do
    }

}
