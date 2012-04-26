package org.geoserver.data.gss;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geogit.repository.Repository;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.util.SimpleInternationalString;

import com.google.common.base.Preconditions;

/**
 * GeoSynchronization Service DataStore that supports replication and synchronization through GSS
 * 1.0.
 * <ul>
 * <li>Set the GSS capabilities URL
 * <li>Set the upstream GSS user credentials
 * <li>Set whether to replicate passively -default- (opening up a port and subscribing to the GSS
 * replication feed) or actively (setting a poll interval), or both. Active replication depends on
 * whether the server implements subscription.
 * <li>Set datastore mode to _replication only_, (in which case the DataStore will publish read-only
 * data), or to _syncrhinization_ (in which case the DataStore will publish read/write data and push
 * up local changes back to the server)
 * <li>Set the surrogate datastore parameters Map </p>
 * 
 * @author groldan
 * 
 */
public class GeoSyncDataStoreFactory implements DataStoreFactorySpi {

    public static final String DISPLAY_NAME = "OGC GeoSync 1.0";

    /**
     * URL of the GSS GetCapabilities document, mandatory
     */
    public static final Param GSS_CAPABILITIES_URL = new Param("GSS_CAPABILITIES", URL.class,
            new SimpleInternationalString("GSS GetCapabilities URL"), true,
            "http(s)://<server>[:<port>]/<context>?service=GSS&version=1.0.0&request=GetCapabilities");

    /**
     * URL of the GSS GetCapabilities document, mandatory
     */
    public static final Param GSS_REPLICATED_NAMESPACE = new Param("namespace", String.class,
            new SimpleInternationalString("Replicated namespace"), true,
            "http://www.geoserver.org/geosync/replication");

    /**
     * GSS authentication user name, optional
     */
    public static final Param GSS_USER = new Param("GSS_USER", String.class, "Auth user name",
            false);

    /**
     * GSS authentication password, optional, but required if user name is provided
     */
    public static final Param GSS_PASSWORD = new Param("GSS_PASSWORD", String.class,
            "Auth password", false, null, Collections.singletonMap(Param.IS_PASSWORD, Boolean.TRUE));

    /**
     * Whether to do passive replication (subscribing to the GSS replication feed), optional,
     * defaults to {@code true}, but server must support subscription.
     */
    public static final Param GSS_PASSIVE_REPLICATION = new Param("GSS_PASSIVE_REPLICATION",
            Boolean.class, "Subscribe for passive replicaton", false, Boolean.FALSE);

    /**
     * Whether to use active replication (polling the GSS for changes every a specified time
     * lapse).Optional, defaults to {@code false}.
     */
    public static final Param GSS_ACTIVE_REPLICATION = new Param("GSS_ACTIVE_REPLICATION",
            Boolean.class, "Poll server for replication", false, Boolean.TRUE);

    /**
     * Poll interval to use if active replication is enabled. Optional, defaults to 10 seconds.
     */
    public static final Param GSS_POLL_INTERVAL_SECS = new Param("GSS_POLL_INTERVAL_SECS",
            Integer.class, "Replication poll time (seconds)", false, Integer.valueOf(10));

    /**
     * @see org.geotools.factory.Factory#getImplementationHints()
     */
    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return "GeoSynchronization Service Client Data Store";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] { GSS_CAPABILITIES_URL, GSS_REPLICATED_NAMESPACE, GSS_USER,
                GSS_PASSWORD, GSS_PASSIVE_REPLICATION, GSS_ACTIVE_REPLICATION,
                GSS_POLL_INTERVAL_SECS };
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        final boolean canProcess = params.containsKey(GSS_CAPABILITIES_URL.key)
                && params.containsKey(GSS_REPLICATED_NAMESPACE.key);
        return canProcess;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DataStore createDataStore(final Map<String, Serializable> params) throws IOException {
        Preconditions.checkNotNull(params, "params");

        final ServerSubscription subscription = createSubscription(params);

        final GeoSyncClient geoSyncClient = GeoServerExtensions.bean(GeoSyncClient.class);
        Preconditions.checkNotNull(geoSyncClient);
        URL capabilitiesURL = new URL(subscription.getUrl());
        if (!geoSyncClient.isSubscribedTo(capabilitiesURL)) {
            geoSyncClient.subscribe(subscription);
        }

        Repository repo = geoSyncClient.getRepository();
        GeoSyncDataStore geoSyncDataStore = new GeoSyncDataStore(subscription, repo);
        return geoSyncDataStore;
    }

    public static ServerSubscription createSubscription(final Map<String, Serializable> params)
            throws IOException {

        final URL capabilitiesURL = lookUp(GSS_CAPABILITIES_URL, params, URL.class);
        final String namespace = lookUp(GSS_REPLICATED_NAMESPACE, params, String.class);
        final String user = lookUp(GSS_USER, params, String.class);
        final String password = lookUp(GSS_PASSWORD, params, String.class);
        final Boolean usePassiveReplication = lookUp(GSS_PASSIVE_REPLICATION, params, Boolean.class);
        final Boolean useActiveReplication = lookUp(GSS_ACTIVE_REPLICATION, params, Boolean.class);
        final Integer activeReplPollIntervalSecs = lookUp(GSS_POLL_INTERVAL_SECS, params,
                Integer.class);

        ServerSubscription serverSubscription = new ServerSubscription();
        serverSubscription.setUrl(capabilitiesURL.toExternalForm());
        serverSubscription.setReplicatedNamespace(namespace);
        serverSubscription.setUser(user);
        serverSubscription.setPassword(password);
        serverSubscription.setUsePassiveReplication(usePassiveReplication);
        serverSubscription.setUseActiveReplication(useActiveReplication);
        serverSubscription.setActiveReplicationPollIntervalSecs(activeReplPollIntervalSecs);
        return serverSubscription;
    }

    public static Map<String, Serializable> createParams(final ServerSubscription s)
            throws IOException {

        Map<String, Serializable> params = new HashMap<String, Serializable>();

        final URL capabilitiesURL = new URL(s.getUrl());
        final String namespace = s.getReplicatedNamespace();
        final String user = s.getUser();
        final String password = s.getPassword();
        final Boolean usePassiveReplication = s.getUsePassiveReplication();
        final Boolean useActiveReplication = s.getUseActiveReplication();
        final Integer activeReplPollIntervalSecs = s.getActiveReplPollIntervalSecs();

        params.put(GSS_CAPABILITIES_URL.key, capabilitiesURL);
        params.put(GSS_REPLICATED_NAMESPACE.key, namespace);
        params.put(GSS_USER.key, user);
        params.put(GSS_PASSWORD.key, password);
        params.put(GSS_PASSIVE_REPLICATION.key, usePassiveReplication);
        params.put(GSS_ACTIVE_REPLICATION.key, useActiveReplication);
        params.put(GSS_POLL_INTERVAL_SECS.key, activeReplPollIntervalSecs);

        return params;
    }

    @SuppressWarnings("unchecked")
    private static <T> T lookUp(final Param param, final Map<String, Serializable> params,
            final Class<T> type) throws IOException {
        Object lookUp = param.lookUp(params);
        if (lookUp == null) {
            lookUp = param.sample;
        }
        return (T) lookUp;
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        throw new UnsupportedOperationException("createNewDataStore operation is not supported by "
                + getClass().getSimpleName());
    }
}
