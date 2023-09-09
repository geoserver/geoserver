/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;

/**
 * Maps joined simple features up to a complex Collection feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCCollectionFeatureStore extends AbstractMappingStore {

    static final Logger LOGGER = Logging.getLogger(JDBCCollectionFeatureStore.class);
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public JDBCCollectionFeatureStore(
            JDBCOpenSearchAccess openSearchAccess, FeatureType collectionFeatureType)
            throws IOException {
        super(openSearchAccess, collectionFeatureType);
    }

    @Override
    public SimpleFeatureSource getDelegateSource() throws IOException {
        SimpleFeatureSource delegate =
                openSearchAccess
                        .getDelegateStore()
                        .getFeatureSource(JDBCOpenSearchAccess.COLLECTION);
        // if we're in a OWS Dispatcher handled request, check for workspace
        if (Dispatcher.REQUEST.get() != null) {
            WorkspaceInfo workspaceInfo = LocalWorkspace.get();
            return new WorkspaceFeatureSource(delegate, workspaceInfo, openSearchAccess);
        } else {
            // otherwise just return the delegate because its coming from the REST API
            return delegate;
        }
    }

    @Override
    protected String getLinkTable() {
        return "collection_ogclink";
    }

    @Override
    protected String getLinkForeignKey() {
        return "collection_id";
    }

    @Override
    protected String getThumbnailTable() {
        return "collection_thumb";
    }

    @Override
    protected void featuresModified() {
        openSearchAccess.clearFeatureSourceCaches();
    }
}
