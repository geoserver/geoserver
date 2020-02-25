/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.addressString;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.localAddress;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogEventImpl;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.cluster.Event;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

/**
 * Synchronizer that converts cluster events and dispatches them the GeoServer config/catalog.
 *
 * <p>This synchronizer assumes a shared data directory among nodes in the cluster.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class EventHzSynchronizer extends HzSynchronizer {

    private final ITopic<UUID> ackTopic;

    private final AckListener ackListener;

    public EventHzSynchronizer(HzCluster cluster, GeoServer gs) {
        super(cluster, gs);

        ackTopic = cluster.getHz().getTopic("geoserver.config.ack");
        ackTopic.addMessageListener(ackListener = new AckListener());
    }

    @Override
    protected void dispatch(Event e) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(format("%s - Publishing event %s", nodeId(), e));
        }

        final UUID evendId = e.getUUID();
        Set<Member> members = cluster.getHz().getCluster().getMembers();
        int expectedAcks = 0;
        for (Member member : members) {
            if (!(member.localMember())) {
                expectedAcks++;
            }
        }
        ackListener.expectedAckCounters.put(evendId, new AtomicInteger(expectedAcks));

        e.setSource(localAddress(cluster.getHz()));
        topic.publish(e);

        incCounter(getClass(), "dispatched");
        waitForAck(e);
    }

    private class AckListener implements MessageListener<UUID> {

        final ConcurrentMap<UUID, AtomicInteger> expectedAckCounters = Maps.newConcurrentMap();

        @Override
        public void onMessage(Message<UUID> message) {
            UUID eventId = message.getMessageObject();
            AtomicInteger countDown = expectedAckCounters.get(eventId);
            if (countDown != null) {
                countDown.decrementAndGet();
                String originAddr = null;
                Member publishingMember = message.getPublishingMember();
                if (publishingMember != null) {
                    InetSocketAddress socketAddress = publishingMember.getSocketAddress();
                    if (socketAddress != null) {
                        originAddr = addressString(socketAddress);
                    }
                }
                LOGGER.finer(
                        format("%s - Got ack on event %s from %s", nodeId(), eventId, originAddr));
            }
        }
    }

    protected final void ack(Event event) {
        UUID uuid = event.getUUID();
        ackTopic.publish(uuid);
        LOGGER.finer(format("%s - Sent ack for event %s", nodeId(), uuid));
    }

    private void waitForAck(Event event) {
        final UUID evendId = event.getUUID();
        final int maxWaitMillis = cluster.getAckTimeoutMillis();
        final int waitInterval = 100;
        LOGGER.fine(format("%s - Waiting for acks on %s", nodeId(), evendId));
        final AtomicInteger countDown = ackListener.expectedAckCounters.get(evendId);
        int waited = 0;
        try {
            while (waited < maxWaitMillis) {
                int remainingAcks = countDown.get();
                if (remainingAcks <= 0) {
                    return;
                }
                try {
                    Thread.sleep(waitInterval);
                    waited += waitInterval;
                } catch (InterruptedException ex) {
                    return;
                }
            }
            LOGGER.warning(
                    format(
                            "%s - After %dms, %d acks missing for event %s",
                            nodeId(), maxWaitMillis, countDown.get(), event));
        } finally {
            ackListener.expectedAckCounters.remove(evendId);
        }
    }

    @Override
    protected Future<?> processEvent(Event event) {
        Preconditions.checkState(isStarted());
        if (!(event instanceof ConfigChangeEvent)) {
            return null;
        }
        try {
            LOGGER.fine(format("%s - Processing event %s", nodeId(), event));
            ConfigChangeEvent ce = (ConfigChangeEvent) event;
            Class<? extends Info> clazz = ce.getObjectInterface();
            if (CatalogInfo.class.isAssignableFrom(clazz)) {
                processCatalogEvent(ce);
            } else {
                processGeoServerConfigEvent(ce);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, format("%s - Error processing event %s", nodeId(), event), e);
        } finally {
            ack(event);
        }
        return null;
    }

    private void processCatalogEvent(final ConfigChangeEvent event)
            throws NoSuchMethodException, SecurityException {

        Class<? extends Info> clazz = event.getObjectInterface();
        final Type t = event.getChangeType();
        final String id = event.getObjectId();
        final String name = event.getObjectName();
        final @Nullable String nativeName = event.getNativeName();
        // catalog event
        CatalogInfo subj;
        Method notifyMethod;
        CatalogEventImpl evt;

        final Catalog cat = cluster.getRawCatalog();

        switch (t) {
            case ADD:
                subj = getCatalogInfo(cat, id, clazz);
                notifyMethod =
                        CatalogListener.class.getMethod("handleAddEvent", CatalogAddEvent.class);
                evt = new CatalogAddEventImpl();
                break;
            case MODIFY:
                subj = getCatalogInfo(cat, id, clazz);
                notifyMethod =
                        CatalogListener.class.getMethod(
                                "handleModifyEvent", CatalogModifyEvent.class);
                evt = new CatalogModifyEventImpl();
                break;
            case POST_MODIFY:
                subj = getCatalogInfo(cat, id, clazz);
                notifyMethod =
                        CatalogListener.class.getMethod(
                                "handlePostModifyEvent", CatalogPostModifyEvent.class);
                evt = new CatalogPostModifyEventImpl();
                break;
            case REMOVE:
                notifyMethod =
                        CatalogListener.class.getMethod(
                                "handleRemoveEvent", CatalogRemoveEvent.class);
                evt = new CatalogRemoveEventImpl();
                RemovedObjectProxy proxy = new RemovedObjectProxy(id, name, clazz, nativeName);

                if (ResourceInfo.class.isAssignableFrom(clazz) && event.getStoreId() != null) {
                    proxy.addCatalogCollaborator(
                            "store", cat.getStore(event.getStoreId(), StoreInfo.class));
                }
                subj =
                        (CatalogInfo)
                                Proxy.newProxyInstance(
                                        getClass().getClassLoader(), new Class[] {clazz}, proxy);

                break;
            default:
                throw new IllegalStateException("Should not happen");
        }

        if (subj == null) { // can't happen if type == DELETE
            if (subj == null) {
                String message =
                        format(
                                "%s - Error processing event %s: object not found in catalog",
                                nodeId(), event);
                LOGGER.warning(message);
                return;
            }
        }

        evt.setSource(subj);
        try {
            for (CatalogListener l : ImmutableList.copyOf(cat.getListeners())) {
                // Don't notify self otherwise the event bounces back out into the
                // cluster.
                if (l != this
                        && isStarted()
                        && // HACK-HACK-HACK -- prevent infinite loop with update sequence listener
                        !"org.geoserver.config.UpdateSequenceListener"
                                .equals(l.getClass().getCanonicalName())) {
                    notifyMethod.invoke(l, evt);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(
                    Level.WARNING, format("%s - Event dispatch failed: %s", nodeId(), event), ex);
        }
    }

    private void processGeoServerConfigEvent(ConfigChangeEvent ce)
            throws NoSuchMethodException, SecurityException {

        final Class<? extends Info> clazz = ce.getObjectInterface();
        final String id = ce.getObjectId();
        final Catalog cat = cluster.getRawCatalog();
        boolean extraArguments = false;

        Info subj;
        Method notifyMethod;

        if (GeoServerInfo.class.isAssignableFrom(clazz)) {
            subj = gs.getGlobal();
            switch (ce.getChangeType()) {
                case MODIFY:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleGlobalChange",
                                    GeoServerInfo.class,
                                    List.class,
                                    List.class,
                                    List.class);
                    extraArguments = true;
                    break;
                default:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handlePostGlobalChange", GeoServerInfo.class);
            }
        } else if (SettingsInfo.class.isAssignableFrom(clazz)) {
            WorkspaceInfo ws =
                    ce.getWorkspaceId() != null ? cat.getWorkspace(ce.getWorkspaceId()) : null;
            subj = ws != null ? gs.getSettings(ws) : gs.getSettings();
            switch (ce.getChangeType()) {
                case MODIFY:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleSettingsModified",
                                    SettingsInfo.class,
                                    List.class,
                                    List.class,
                                    List.class);
                    extraArguments = true;
                    break;
                case REMOVE:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleSettingsRemoved", SettingsInfo.class);
                    break;

                case ADD:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleSettingsAdded", SettingsInfo.class);
                    break;
                default:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleSettingsPostModified", SettingsInfo.class);
            }
        } else if (LoggingInfo.class.isAssignableFrom(clazz)) {
            subj = gs.getLogging();
            switch (ce.getChangeType()) {
                case MODIFY:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleLoggingChange",
                                    LoggingInfo.class,
                                    List.class,
                                    List.class,
                                    List.class);
                    extraArguments = true;
                    break;
                default:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handlePostLoggingChange", LoggingInfo.class);
            }
        } else if (ServiceInfo.class.isAssignableFrom(clazz)) {
            subj = gs.getService(id, (Class<ServiceInfo>) clazz);
            switch (ce.getChangeType()) {
                case MODIFY:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handleServiceChange",
                                    ServiceInfo.class,
                                    List.class,
                                    List.class,
                                    List.class);
                    extraArguments = true;
                    break;
                default:
                    notifyMethod =
                            ConfigurationListener.class.getMethod(
                                    "handlePostServiceChange", ServiceInfo.class);
            }
        } else {
            throw new IllegalStateException("Unknown event type " + clazz);
        }

        for (ConfigurationListener l : gs.getListeners()) {
            try {
                if (l != this
                        && // HACK-HACK-HACK -- prevent infinite loop with update sequence listener
                        !"org.geoserver.config.UpdateSequenceListener"
                                .equals(l.getClass().getCanonicalName())) {
                    if (extraArguments) {
                        notifyMethod.invoke(
                                l,
                                subj,
                                ce.getPropertyNames(),
                                ce.getOldValues(),
                                ce.getNewValues());
                    } else {
                        notifyMethod.invoke(l, subj);
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(
                        Level.WARNING, format("%s - Event dispatch failed: %s", nodeId(), ce), ex);
            }
        }
    }

    private CatalogInfo getCatalogInfo(Catalog cat, String id, Class<? extends Info> clazz) {
        CatalogInfo subj = null;
        if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getWorkspace(id);
        } else if (NamespaceInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getNamespace(id);
        } else if (StoreInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getStore(id, (Class<StoreInfo>) clazz);
        } else if (ResourceInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getResource(id, (Class<ResourceInfo>) clazz);
        } else if (LayerInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getLayer(id);
        } else if (StyleInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getStyle(id);
        } else if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
            subj = cat.getLayerGroup(id);
        }
        return subj;
    }
}
