/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;

/**
 * Maps joined simple features up to a complex Collection feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCCollectionFeatureSource extends AbstractMappingSource {

    static final Logger LOGGER = Logging.getLogger(JDBCCollectionFeatureSource.class);

    public JDBCCollectionFeatureSource(JDBCOpenSearchAccess openSearchAccess,
            FeatureType collectionFeatureType) throws IOException {
        super(openSearchAccess, collectionFeatureType);
    }

    protected SimpleFeatureSource getDelegateCollectionSource() throws IOException {
        return openSearchAccess.getDelegateStore()
                .getFeatureSource(JDBCOpenSearchAccess.COLLECTION);
    }

    @Override
    protected String getMetadataTable() {
        return "collection_metadata";
    }

    @Override
    protected String getLinkTable() {
        return "collection_ogclink";
    }

    @Override
    protected String getLinkForeignKey() {
        return "collection_id";
    }

}
