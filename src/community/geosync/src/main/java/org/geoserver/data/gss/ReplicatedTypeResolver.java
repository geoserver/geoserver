package org.geoserver.data.gss;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.bxml.FeatureTypeProvider;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Preconditions;

public class ReplicatedTypeResolver implements FeatureTypeProvider {

    private static final Logger LOGGER = Logging.getLogger(ReplicatedTypeResolver.class);

    private final GeoSyncClient geoSyncClient;

    private final ServerSubscription subscriptionOpts;

    // cached WFS datastore. See getRemoteWFS()
    private DataStore remoteWFS;

    public ReplicatedTypeResolver(final GeoSyncClient geoSyncClient,
            final ServerSubscription subscriptionOpts) {

        this.geoSyncClient = geoSyncClient;
        this.subscriptionOpts = subscriptionOpts;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FeatureType resolveFeatureType(Name typeName) throws IOException {
        FeatureSource store = resolveFeatureSource(typeName);
        return store.getSchema();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FeatureStore resolveFeatureStore(Name typeName) throws IOException {
        FeatureSource store = resolveFeatureSource(typeName);
        Preconditions.checkArgument(store instanceof FeatureStore,
                "Found read only FeatureSource for " + typeName);
        return (FeatureStore) store;
    }

    /**
     * @see org.geoserver.bxml.FeatureTypeProvider#resolveFeatureSource(org.opengis.feature.type.Name)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public FeatureSource resolveFeatureSource(final Name typeName) throws IOException {

        FeatureSource store = getFromCatalog(typeName);

        if (store == null) {
            LOGGER.info("Creating replicated type " + typeName);

            SimpleFeatureSource remoteSource;
            try {
                final DataStore remoteWfs = getRemoreWFS();
                remoteSource = remoteWfs.getFeatureSource(typeName);
            } catch (IOException e) {
                LOGGER.info("Cached WFS DataStore doesn't contain FeatureType " + typeName
                        + ". Recreating WFS DataStore...");
                this.remoteWFS.dispose();
                this.remoteWFS = null;
                final DataStore remoteWfs = getRemoreWFS();
                remoteSource = remoteWfs.getFeatureSource(typeName);
                LOGGER.info("Remote WFS recreated successfully and FeatureType obtained for "
                        + typeName);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }

            final DataStore replcatingDataStore = geoSyncClient.getOrCreateReplicatedStoreFor(
                    subscriptionOpts, typeName, remoteSource);
            Preconditions.checkState(replcatingDataStore != null);

            LOGGER.info("Obtaining replicated feature store from Catalog for " + typeName);
            store = getFromCatalog(typeName);
            Preconditions.checkState(store != null, typeName + " not published");

        }
        return store;
    }

    private DataStore getRemoreWFS() throws IOException {
        if (remoteWFS != null) {
            return remoteWFS;
        }
        final URL remoteWFSURL = getRemoveWFSCapabilitiesURL();
        LOGGER.info("Obtaining WFS DataStore for " + remoteWFSURL);
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WFSDataStoreFactory.URL.key, remoteWFSURL);
        if (subscriptionOpts.getUser() != null) {
            params.put(WFSDataStoreFactory.USERNAME.key, subscriptionOpts.getUser());
            params.put(WFSDataStoreFactory.PASSWORD.key, subscriptionOpts.getPassword());
        }
        params.put(WFSDataStoreFactory.TIMEOUT.key, 10000);
        params.put(WFSDataStoreFactory.TRY_GZIP.key, Boolean.TRUE);

        remoteWFS = DataStoreFinder.getDataStore(params);
        return remoteWFS;
    }

    private URL getRemoveWFSCapabilitiesURL() throws IOException {
        /*
         * WARNING: We're assuming here that the GSS GetCapabilities base url is the same than the
         * source server's WFS base url, but the GSS capabilities doesn't declare the target WFS
         * endpoint nor what feature types it exposes.
         */
        final String owsBaseUrl = subscriptionOpts.getUrl();
        URL url = new URL(owsBaseUrl);
        String base = url.getProtocol() + "://" + url.getHost();
        if (url.getPort() > 0) {
            base += ":" + url.getPort();
        }
        base += url.getPath();

        URL wfsGetCaps = new URL(base + "?service=WFS&version=1.1.0&request=GetCapabilities");
        return wfsGetCaps;
    }

    @SuppressWarnings("rawtypes")
    private FeatureStore getFromCatalog(final Name typeName) throws IOException {
        FeatureStore store = null;
        final Catalog catalog = geoSyncClient.getCatalog();
        final FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(typeName);
        if (typeInfo != null) {
            store = (FeatureStore) typeInfo.getFeatureSource(null, null);
            // /Preconditions.checkState(store instanceof VersioningFeatureStore);
            return store;
        }
        return store;
    }

}
