/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import org.geoserver.smartdataloader.metadata.jdbc.DefaultJdbcHelper;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcDataStoreMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcDataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcHelper;

/** Factory class that builds a DataStoreMetadata based on the DataStoreMetadataConfig passed as argument. */
public class DataStoreMetadataFactory {

    public DataStoreMetadata getDataStoreMetadata(DataStoreMetadataConfig config) throws Exception {
        return getDataStoreMetadata(config, null);
    }

    public DataStoreMetadata getDataStoreMetadata(DataStoreMetadataConfig config, JdbcHelper jdbcHelper)
            throws Exception {
        if (config.getType().equals(JdbcDataStoreMetadataConfig.TYPE)) {
            JdbcDataStoreMetadataConfig jdmp = (JdbcDataStoreMetadataConfig) config;
            JdbcHelper helper = (jdbcHelper != null) ? jdbcHelper : new DefaultJdbcHelper();
            DataStoreMetadata store = new JdbcDataStoreMetadata(jdmp, helper);
            store.load();
            return store;
        }
        return null;
    }
}
