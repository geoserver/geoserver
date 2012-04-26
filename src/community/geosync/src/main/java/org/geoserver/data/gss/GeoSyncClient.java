package org.geoserver.data.gss;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.geogit.repository.Repository;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.storage.GeoSyncDatabase;
import org.geoserver.task.LongTask;
import org.geoserver.task.LongTaskMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.vfny.geoserver.global.GeoServerFeatureSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class GeoSyncClient implements DisposableBean {

    public static final String LAST_REPLICA_TIME = "LAST_REPLICA_TIME";

    private static final Logger LOGGER = Logging.getLogger(GeoSyncClient.class);

    private final HTTPClient httpClient;

    private final GeoSyncDatabase gssDb;

    private final GSS gss;

    private final Map<String, Capabilities> cachedCapabilities;

    private ScheduledExecutorService scheduledThreadPool;

    private Map<String, ActiveReplicationTask> runningTasks;

    public GeoSyncClient(final HTTPClient httpClient, final GSS gss) {
        this.httpClient = httpClient;
        this.gss = gss;
        this.gssDb = gss.getDatabase();
        this.runningTasks = new ConcurrentHashMap<String, ActiveReplicationTask>();
        this.cachedCapabilities = new ConcurrentHashMap<String, Capabilities>();

        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(
                "GeoSync Client Poller");
        threadFactory.setThreadPriority(Thread.MIN_PRIORITY + 1);
        threadFactory.setDaemon(true);
        scheduledThreadPool = Executors.newScheduledThreadPool(1, threadFactory);
        scheduledThreadPool.scheduleAtFixedRate(new PollTask(), 5, 5, TimeUnit.SECONDS);
    }

    private class PollTask implements Runnable {

        @Override
        public void run() {
            final LongTaskMonitor longTaskMonitor = LongTaskMonitor.get();

            // prune terminated tasks
            for (String url : runningTasks.keySet()) {
                ActiveReplicationTask task = runningTasks.get(url);
                if (task.isDone()) {
                    longTaskMonitor.removeTerminated(task);
                    runningTasks.remove(url);
                }
            }

            final boolean enabledOnly = true;
            final List<ServerSubscription> serverSubscriptions;
            serverSubscriptions = gss.getServerSubscriptions(enabledOnly);
            if (serverSubscriptions.size() == 0) {
                return;
            }

            for (ServerSubscription subscription : serverSubscriptions) {
                final Boolean useActiveReplication = subscription.getUseActiveReplication();
                final boolean isPollTask = useActiveReplication != null
                        && useActiveReplication.booleanValue();
                if (!isPollTask) {
                    continue;
                }
                final String url = subscription.getUrl();
                ActiveReplicationTask task = runningTasks.get(url);
                if (task != null) {
                    if (!task.isDone()) {
                        LOGGER.fine("Avoiding polling GSS as another task is still running: " + url);
                        continue;
                    }
                    longTaskMonitor.removeTerminated(task);
                }
                LOGGER.fine("Issuing GeoSync poll for changes to " + subscription.getUrl());
                task = new ActiveReplicationTask(subscription, GeoSyncClient.this);
                task.setAutoRemoveWhenFinished(true);
                longTaskMonitor.dispatch(task);
                runningTasks.put(url, task);
            }
        }
    }

    public Catalog getCatalog() {
        return gss.getCatalog();
    }

    /**
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    @Override
    public void destroy() throws Exception {
        scheduledThreadPool.shutdownNow();
    }

    public HTTPClient getHttpClient() {
        return httpClient;
    }

    public Repository getRepository() {
        return gss.getGeoGit().getRepository();
    }

    public boolean isSubscribedTo(final URL capabilitiesURL) {
        final String externalForm = capabilitiesURL.toExternalForm();
        final List<ServerSubscription> serverSubscriptions = gss.getServerSubscriptions(false);
        for (ServerSubscription s : serverSubscriptions) {
            if (externalForm.equalsIgnoreCase(capabilitiesURL.toExternalForm())) {
                return true;
            }
        }
        return false;
    }

    public Capabilities getCachedCapabilities(final String capabilitiesURI, final String user,
            final String password) throws IOException {

        Capabilities capabilities = cachedCapabilities.get(capabilitiesURI.toLowerCase());
        if (capabilities == null) {
            LongTask<Capabilities> capsTask = getCapabilities(capabilitiesURI, user, password);
            try {
                while (!capsTask.isDone()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (capsTask.getException() != null) {
                    throw new IOException("Exception getting GSS capabilities",
                            capsTask.getException());
                }
                capabilities = capsTask.getResult();
                cachedCapabilities.put(capabilitiesURI.toLowerCase(), capabilities);
            } finally {
                LongTaskMonitor.get().removeTerminated(capsTask);
            }
        }
        return capabilities;
    }

    public LongTask<Capabilities> getCapabilities(final String capabilitiesURI, final String user,
            final String password) throws IOException {

        LongTask<Capabilities> task = new LongTask<Capabilities>("GSS GetCapabilities",
                capabilitiesURI) {

            @Override
            protected Capabilities callInternal(ProgressListener progressListener) throws Exception {
                final GetMethod method = httpClient.get(capabilitiesURI, user, password);
                try {
                    InputStream responseBodyAsStream = method.getResponseBodyAsStream();
                    try {
                        DocumentBuilderFactory builderFactory = DocumentBuilderFactory
                                .newInstance();
                        builderFactory.setNamespaceAware(true);
                        Document capsDoc = builderFactory.newDocumentBuilder().parse(
                                responseBodyAsStream);

                        Element root = capsDoc.getDocumentElement();
                        if (root.getLocalName().equals("ExceptionReport")) {
                            String exMessage = root.getTextContent();
                            throw new IOException("Server returned exception '" + exMessage.trim()
                                    + "'");
                        }

                        Capabilities capabilities = new Capabilities(capsDoc);
                        return capabilities;
                    } catch (SAXException e) {
                        throw new IOException(e);
                    } catch (ParserConfigurationException e) {
                        throw new IOException(e);
                    } finally {
                        responseBodyAsStream.close();
                    }
                } finally {
                    method.releaseConnection();
                }
            }
        };

        final LongTaskMonitor longTaskMonitor = LongTaskMonitor.get();
        Future<Capabilities> future = longTaskMonitor.dispatch(task);
        return task;
    }

    public void subscribe(ServerSubscription subscription) {
        // TODO Auto-generated method stub

    }

    public DataStoreInfo getReplicaStoreInfo(final ServerSubscription subscriptionOpts)
            throws IOException {

        final String find = new URL(subscriptionOpts.getUrl()).toExternalForm().toLowerCase();

        List<DataStoreInfo> dataStores = getCatalog().getDataStores();
        for (DataStoreInfo storeInfo : dataStores) {
            Map<String, Serializable> connectionParameters = storeInfo.getConnectionParameters();
            Serializable URL = connectionParameters
                    .get(GeoSyncDataStoreFactory.GSS_CAPABILITIES_URL.key);
            if (URL != null) {
                String target = new URL(String.valueOf(URL)).toExternalForm().toLowerCase();
                if (find.equals(target)) {
                    return storeInfo;
                }
            }
        }
        throw new IOException("DataStoreInfo not found for replicated GSS "
                + subscriptionOpts.getUrl());
    }

    public Date getLastReplicationTime(final ServerSubscription subscriptionOpts)
            throws IOException {
        Preconditions.checkNotNull(subscriptionOpts);
        DataStoreInfo replicaStoreInfo = getReplicaStoreInfo(subscriptionOpts);
        Long lastReplicated = replicaStoreInfo.getMetadata().get(LAST_REPLICA_TIME, Long.class);
        if (lastReplicated != null) {
            return new Date(lastReplicated.longValue());
        }
        return null;
    }

    public void saveLastProcessedEntry(ServerSubscription subscriptionOpts, EntryImpl lastEntry)
            throws IOException {

        Preconditions.checkNotNull(subscriptionOpts);
        Preconditions.checkNotNull(lastEntry);

        Date updated = lastEntry.getUpdated();
        if (updated != null) {
            DataStoreInfo replicaStoreInfo = getReplicaStoreInfo(subscriptionOpts);
            replicaStoreInfo.getMetadata().put(LAST_REPLICA_TIME, Long.valueOf(updated.getTime()));
            getCatalog().save(replicaStoreInfo);
        }
    }

    public DataStore getOrCreateReplicatedStoreFor(final ServerSubscription subscriptionOpts,
            final Name finalTypeName, SimpleFeatureSource remoteSource) throws IOException {

        LOGGER.fine("Getting feature type from WFS DataStore for " + finalTypeName);

        SimpleFeatureType remoteSchema = remoteSource.getSchema();
        try {
            remoteSchema = retypeWgs84(remoteSchema);
            if (!remoteSchema.equals(remoteSource.getSchema())) {
                CoordinateReferenceSystem declaredCRS = remoteSchema.getCoordinateReferenceSystem();
                int srsHandling = ProjectionPolicy.REPROJECT_TO_DECLARED.getCode();
                remoteSource = GeoServerFeatureSource.create(remoteSource, remoteSchema, null,
                        declaredCRS, srsHandling);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }

        if (!finalTypeName.equals(remoteSchema.getName())) {
            // may the WFS report prefix:typeName instead of just typeName?
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName(finalTypeName);
            builder.addAll(remoteSchema.getAttributeDescriptors());
            remoteSchema = builder.buildFeatureType();
        }

        final Catalog catalog = getCatalog();
        final String namespaceURI = finalTypeName.getNamespaceURI();
        final String localName = finalTypeName.getLocalPart();

        final NamespaceInfo namespace = namespaceFor(namespaceURI, remoteSource.getName()
                .getLocalPart(), catalog);
        final WorkspaceInfo workspace = workspaceFor(namespace, catalog);

        final List<DataStoreInfo> dataStoresByWorkspace = catalog
                .getDataStoresByWorkspace(workspace);

        final String gssUrl = subscriptionOpts.getUrl();

        DataStoreInfo replicatingDataStoreInfo = null;

        for (DataStoreInfo ds : dataStoresByWorkspace) {
            Map<String, Serializable> params = ds.getConnectionParameters();
            String url = String.valueOf(params
                    .get(GeoSyncDataStoreFactory.GSS_CAPABILITIES_URL.key));
            if (gssUrl.toLowerCase().equals(url.toLowerCase())) {
                String replicatedNs = String.valueOf(params
                        .get(GeoSyncDataStoreFactory.GSS_REPLICATED_NAMESPACE.key));
                if (namespaceURI.equals(replicatedNs)) {
                    replicatingDataStoreInfo = ds;
                    break;
                }
            }
        }

        if (replicatingDataStoreInfo == null) {
            // such a replicating data store does not exist, create it
            replicatingDataStoreInfo = createReplicatingDataStore(subscriptionOpts, catalog,
                    workspace, namespace);
            catalog.save(replicatingDataStoreInfo);
        }

        final DataStore replcatingDataStore;
        replcatingDataStore = (DataStore) replicatingDataStoreInfo.getDataStore(null);
        try {
            SimpleFeatureType replicatedSchema = replcatingDataStore.getSchema(finalTypeName);
            boolean equals = replicatedSchema.equals(remoteSchema);
            if (!equals) {
                throw new IllegalStateException("Type " + finalTypeName
                        + " already exists in replicated data store but the "
                        + "schema doesn't match the remote one");
            }
            // found and has required schema, return it;
            return replcatingDataStore;
        } catch (Exception e) {
            LOGGER.info("Creating replicated type " + finalTypeName);
            replcatingDataStore.createSchema(remoteSchema);
        }

        CatalogBuilder b = new CatalogBuilder(catalog);
        b.setStore(replicatingDataStoreInfo);

        FeatureTypeInfo typeInfo = createTypeInfo(remoteSource, localName, b);
        catalog.add(typeInfo);

        LayerInfo layer = b.buildLayer(typeInfo);

        try {
            catalog.add(layer);
        } catch (Exception e) {
            catalog.remove(typeInfo);
            throw new IOException(e);
        }

        LOGGER.info("Auto publishing resource and layer for " + finalTypeName);
        return replcatingDataStore;
    }

    /**
     * GeoServer doesn't play well with a feature type that's natively lat/lon, so if this type
     * geometry is in ESPSG:4326 lat/lon returns a copy that's in EPSG:4325 lon/lat
     */
    private SimpleFeatureType retypeWgs84(SimpleFeatureType remoteSchema) throws Exception {
        GeometryDescriptor geometryDescriptor = remoteSchema.getGeometryDescriptor();
        if (null == geometryDescriptor) {
            return remoteSchema;
        }
        CoordinateReferenceSystem crs = geometryDescriptor.getCoordinateReferenceSystem();
        CoordinateReferenceSystem latLon = CRS.decode("urn:ogc:def:crs:EPSG::4326");
        if (CRS.equalsIgnoreMetadata(latLon, crs)) {
            crs = CRS.decode("EPSG:4326", true);
            String[] properties = new String[remoteSchema.getAttributeCount()];
            for (int i = 0; i < properties.length; i++) {
                properties[i] = remoteSchema.getDescriptor(i).getLocalName();
            }
            remoteSchema = DataUtilities.createSubType(remoteSchema, properties, crs);
        }
        return remoteSchema;
    }

    private FeatureTypeInfo createTypeInfo(final SimpleFeatureSource remoteSource,
            final String localName, CatalogBuilder b) throws IOException {

        FeatureTypeInfo typeInfo = b.buildFeatureType(remoteSource);

        GeometryDescriptor geometryDescriptor = remoteSource.getSchema().getGeometryDescriptor();
        CoordinateReferenceSystem nativeCRS = null;
        if (geometryDescriptor != null) {
            nativeCRS = geometryDescriptor.getCoordinateReferenceSystem();
        }
        ReferencedEnvelope bounds = remoteSource.getBounds();
        // GeoServerFeatureSource missbehaving?
        {
            if (!CRS.equalsIgnoreMetadata(nativeCRS, bounds.getCoordinateReferenceSystem())) {
                try {
                    bounds = (ReferencedEnvelope) bounds.toBounds(nativeCRS);
                } catch (TransformException e) {
                    Throwables.propagate(e);
                }
            }
        }

        typeInfo.setNativeName(localName);
        typeInfo.setName(localName);
        typeInfo.setTitle(remoteSource.getInfo().getTitle());
        typeInfo.setAbstract(remoteSource.getInfo().getDescription());
        typeInfo.setDescription("Replicated FeatureType by GSS");

        typeInfo.setNativeCRS(nativeCRS);
        typeInfo.setNativeBoundingBox(bounds);

        final String srs = CRS.toSRS(nativeCRS, false);
        if (srs != null) {
            typeInfo.setSRS(srs);
            typeInfo.setProjectionPolicy(ProjectionPolicy.NONE);
        } else {
            typeInfo.setSRS("EPSG:4326");
            typeInfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
        }
        try {
            typeInfo.setLatLonBoundingBox(bounds.transform(DefaultGeographicCRS.WGS84, true));
        } catch (Exception e) {
            typeInfo.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90,
                    DefaultGeographicCRS.WGS84));
        }
        return typeInfo;
    }

    private DataStoreInfo createReplicatingDataStore(final ServerSubscription prototypeParams,
            final Catalog catalog, final WorkspaceInfo workspace, final NamespaceInfo ns)
            throws IOException {

        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(workspace);
        DataStoreInfo dataStoreInfo = builder.buildDataStore("GeoSync_managed");
        dataStoreInfo.setType(GeoSyncDataStoreFactory.DISPLAY_NAME);
        dataStoreInfo.setEnabled(true);
        dataStoreInfo.setDescription("Replicates types from the " + ns.getURI() + " namespace");
        Map<String, Serializable> params = GeoSyncDataStoreFactory.createParams(prototypeParams);
        params.put(GeoSyncDataStoreFactory.GSS_REPLICATED_NAMESPACE.key, ns.getURI());
        dataStoreInfo.getConnectionParameters().putAll(params);

        return dataStoreInfo;
    }

    private WorkspaceInfo workspaceFor(NamespaceInfo namespace, Catalog catalog) {

        WorkspaceInfo ws = catalog.getWorkspaceByName(namespace.getPrefix());
        if (null == ws) {
            ws = catalog.getFactory().createWorkspace();
            ws.setName(namespace.getPrefix());
            catalog.add(ws);
        }
        return ws;
    }

    private NamespaceInfo namespaceFor(final String namespaceURI, final String remoteLocalName,
            Catalog catalog) {

        NamespaceInfo namespace = catalog.getNamespaceByURI(namespaceURI);
        if (namespace == null) {
            String prefferredPrefix = null;
            if (remoteLocalName.indexOf(':') > 0) {
                prefferredPrefix = remoteLocalName.substring(0, remoteLocalName.indexOf(':'));
            }
            String prefix = newPrefixFor(namespaceURI, catalog, prefferredPrefix);
            namespace = catalog.getFactory().createNamespace();
            namespace.setPrefix(prefix);
            namespace.setURI(namespaceURI);
            catalog.add(namespace);
        }
        return namespace;
    }

    /**
     * Figures out a prefix for the given namespace making sure it does not already exist in Catalog
     * 
     * @param prefferredPrefix
     */
    private String newPrefixFor(final String namespaceURI, final Catalog catalog,
            final String prefferredPrefix) {
        final String p = prefferredPrefix == null ? "replica" : prefferredPrefix;
        int i = 0;
        String prefix = p;
        while (true) {
            if (null == catalog.getNamespaceByPrefix(prefix)) {
                return prefix;
            }
            prefix = p + i;
            i++;
        }
    }

    private StyleInfo getDefaultStyle(SimpleFeatureType schema, Catalog catalog) {
        StyleInfo point = catalog.getStyleByName("point");
        StyleInfo line = catalog.getStyleByName("line");
        StyleInfo polygon = catalog.getStyleByName("polygon");
        // TODO: create a sensible style instead
        Preconditions.checkState(point != null, "There's no predefined style 'point'");
        Preconditions.checkState(line != null, "There's no predefined style 'line'");
        Preconditions.checkState(polygon != null, "There's no predefined style 'polygon'");
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        StyleInfo style = point;
        if (geometryDescriptor != null) {
            Class<?> binding = geometryDescriptor.getType().getBinding();
            if (LineString.class.equals(binding) || MultiLineString.class.equals(binding)) {
                style = line;
            } else if (Polygon.class.equals(binding) || MultiPolygon.class.equals(binding)) {
                style = polygon;
            }
        }
        return style;
    }

}
