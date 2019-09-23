/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.localAddress;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.cluster.Event;
import org.geoserver.cluster.GeoServerSynchronizer;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

/**
 * Base hazelcast based synchronizer that does event collapsing.
 *
 * <p>This synchronizer maintains a thread safe queue that is populated with events as they occur.
 * Upon receiving of an event a new runnable is scheduled and run after a short delay (default 5
 * sec). The runnable calls the {@link #processEvent(Queue)} method to be implemented by subclasses.
 *
 * <p>This synchronizer events messages received from the same source.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class HzSynchronizer extends GeoServerSynchronizer
        implements MessageListener<Event> {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    private final MetricRegistry registry = new MetricRegistry();

    protected final HzCluster cluster;

    protected final ITopic<Event> topic;

    /** event processor */
    private final ScheduledExecutorService executor;

    /** geoserver configuration */
    protected final GeoServer gs;

    private volatile boolean started;

    ScheduledExecutorService getNewExecutor() {
        return Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("HzSynchronizer-%d").build());
    }

    public HzSynchronizer(HzCluster cluster, GeoServer gs) {
        this.cluster = cluster;
        this.gs = gs;

        topic = cluster.getHz().getTopic("geoserver.config");
        topic.addMessageListener(this);

        executor = getNewExecutor();

        gs.addListener(this);
        gs.getCatalog().addListener(this);
    }

    @Override
    public void onMessage(Message<Event> message) {
        Event event = message.getMessageObject();
        if (!isStarted()) {
            // wait for service to be fully started before processing events.
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(format("Ignoring message: %s. Service is not started.", event));
            }
            return;
        }
        incCounter(getClass(), "recieved");
        if (localAddress(cluster.getHz()).equals(event.getSource())) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(
                        format("%s - Skipping message generated locally: %s", nodeId(), event));
            }
            return;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(format("%s - Received event %s", nodeId(), event));
        }

        // schedule job to process the event with a short delay
        final int syncDelay = configWatcher.get().getSyncDelay();
        executor.schedule(new EventWorker(event), syncDelay, TimeUnit.SECONDS);
    }

    private class EventWorker implements Callable<Future<?>> {

        private Event event;

        public EventWorker(Event event) {
            this.event = event;
        }

        @Override
        public Future<?> call() {
            if (!isStarted()) {
                return null;
            }
            Future<?> future = null;
            try {
                future = processEvent(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, format("%s - Event processing failed", nodeId()), e);
            }

            incCounter(getClass(), "reloads");
            return future;
        }
    }

    protected abstract void dispatch(Event e);

    /**
     * Processes the event queue.
     *
     * <p><b>Note:</b> It is the responsibility of subclasses to clear events from the queue as they
     * are processed.
     */
    protected abstract Future<?> processEvent(Event event);

    ConfigChangeEvent newChangeEvent(CatalogEvent evt, Type type) {
        return newChangeEvent(evt.getSource(), type);
    }

    ConfigChangeEvent newChangeEvent(Info subj, Type type) {
        String name = (String) (OwsUtils.has(subj, "name") ? OwsUtils.get(subj, "name") : null);
        WorkspaceInfo ws =
                (WorkspaceInfo)
                        (OwsUtils.has(subj, "workspace") ? OwsUtils.get(subj, "workspace") : null);

        StoreInfo store =
                (StoreInfo) (OwsUtils.has(subj, "store") ? OwsUtils.get(subj, "store") : null);

        ConfigChangeEvent ev = new ConfigChangeEvent(subj.getId(), name, subj.getClass(), type);
        if (ws != null) {
            ev.setWorkspaceId(ws.getId());
        }
        if (store != null) {
            ev.setStoreId(store.getId());
        }
        if (subj instanceof ResourceInfo) {
            ev.setNativeName(((ResourceInfo) subj).getNativeName());
        }
        return ev;
    }

    ConfigChangeEvent newChangeEvent(
            Info subj,
            Type type,
            List<String> propNames,
            List<Object> oldValues,
            List<Object> newValues) {
        ConfigChangeEvent event = newChangeEvent(subj, type);
        event.setPropertyNames(propNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);
        return event;
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.ADD));
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.MODIFY));
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.POST_MODIFY));
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.REMOVE));
    }

    @Override
    public void handleGlobalChange(
            GeoServerInfo global,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        // optimization for update sequence
        if (propertyNames.size() == 1 && propertyNames.contains("updateSequence")) {
            return;
        }
        dispatch(newChangeEvent(global, Type.MODIFY, propertyNames, oldValues, newValues));
    }

    @Override
    public void handlePostGlobalChange(GeoServerInfo global) {
        dispatch(newChangeEvent(global, Type.POST_MODIFY));
    }

    @Override
    public void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        dispatch(newChangeEvent(service, Type.MODIFY, propertyNames, oldValues, newValues));
    }

    @Override
    public void handlePostServiceChange(ServiceInfo service) {
        dispatch(newChangeEvent(service, Type.POST_MODIFY));
    }

    @Override
    public void handleServiceRemove(ServiceInfo service) {
        dispatch(newChangeEvent(service, Type.REMOVE));
    }

    @Override
    public void handleSettingsAdded(SettingsInfo settings) {
        dispatch(newChangeEvent(settings, Type.ADD));
    }

    @Override
    public void handleSettingsModified(
            SettingsInfo settings,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        // optimization for update sequence
        if (propertyNames.size() == 1 && propertyNames.contains("updateSequence")) {
            return;
        }
        dispatch(newChangeEvent(settings, Type.MODIFY, propertyNames, oldValues, newValues));
    }

    @Override
    public void handleSettingsPostModified(SettingsInfo settings) {
        dispatch(newChangeEvent(settings, Type.POST_MODIFY));
    }

    @Override
    public void handleSettingsRemoved(SettingsInfo settings) {
        dispatch(newChangeEvent(settings, Type.REMOVE));
    }

    /** Increments the counter for the specified class and name by one. */
    protected void incCounter(Class<?> clazz, String name) {
        this.registry.counter(MetricRegistry.name(clazz, name)).inc();
    }

    protected String nodeId() {
        return HazelcastUtil.nodeId(cluster);
    }

    public void start() {
        LOGGER.info(format("%s - Enabling processing of configuration change events", nodeId()));
        this.started = true;
    }

    public boolean isStarted() {
        return this.started;
    }

    public void stop() {
        LOGGER.info("Disabling processing of configuration change events");
        this.started = false;
    }
}
