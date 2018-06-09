/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Handles feature types derived from cascaded WFS Stored Queries. Stored Query configuration is
 * stored within the FeatureTypeInfo metadata.
 */
public class CascadedStoredQueryCallback implements FeatureTypeCallback {

    @Override
    public boolean canHandle(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {

        return dataAccess instanceof WFSDataStore
                && info.getMetadata() != null
                && (info.getMetadata().get(FeatureTypeInfo.STORED_QUERY_CONFIGURATION)
                        instanceof StoredQueryConfiguration);
    }

    @Override
    public boolean initialize(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException {

        StoredQueryConfiguration sqc =
                info.getMetadata()
                        .get(
                                FeatureTypeInfo.STORED_QUERY_CONFIGURATION,
                                StoredQueryConfiguration.class);
        WFSDataStore wstore = (WFSDataStore) dataAccess;

        String localPart = info.getName();
        boolean usesTemporary = false;
        if (temporaryName != null) {
            localPart = temporaryName.getLocalPart();
            usesTemporary = true;
        }

        if (!wstore.getConfiguredStoredQueries().containsValue(localPart)) {
            wstore.addStoredQuery(localPart, sqc.getStoredQueryId());
        }
        return usesTemporary;
    }

    @Override
    public void flush(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess)
            throws IOException {
        // nothing to do
    }

    @Override
    public void dispose(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException {
        WFSDataStore wstore = (WFSDataStore) dataAccess;
        wstore.removeStoredQuery(temporaryName.getLocalPart());
    }
}
