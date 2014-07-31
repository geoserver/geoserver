package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.localAddress;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.localIPAsString;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.yammer.metrics.Metrics;

/**
 * Base hazelcast based synchronizer that does event collapsing.
 * <p>
 * This synchronizer maintains a thread safe queue that is populated with events as they occur. Upon
 * receiving of an event a new runnable is scheduled and run after a short delay (default 5 sec).
 * The runnable calls the {@link #processEventQueue(Queue)} method to be implemented by subclasses.
 * </p>
 * <p>
 * This synchronizer events messages received from the same source.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class HzSynchronizer extends GeoServerSynchronizer implements
        MessageListener<Event> {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    HzCluster cluster;

    ITopic<Event> topic;

    /** event queue */
    Queue<Event> queue;

    /** event processor */
    ScheduledExecutorService executor;

    /** geoserver configuration */
    protected GeoServer gs;

    private volatile boolean started;

    ScheduledExecutorService getNewExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
                "HzSynchronizer-%d").build());
    }

    public HzSynchronizer(HzCluster cluster, GeoServer gs) {
        this.cluster = cluster;
        this.gs = gs;

        topic = cluster.getHz().getTopic("geoserver.config");
        topic.addMessageListener(this);

        queue = new ConcurrentLinkedQueue<Event>();
        executor = getNewExecutor();

        gs.addListener(this);
        gs.getCatalog().addListener(this);
    }

    @Override
    public void onMessage(Message<Event> message) {
        Event e = message.getMessageObject();
        if(!isStarted()){
            //wait for service to be fully started before processing events.
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(format("%s - Ignoring message: %s. Service is not yet started.",
                        nodeId(), e));
            }
            return;
        }
        Metrics.newCounter(getClass(), "recieved").inc();
        if (localAddress(cluster.getHz()).equals(e.getSource())) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(format("%s - Skipping message generated locally: %s", nodeId(), e));
            }
            return;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(format("%s - Received event %s", nodeId(), e));
        }
        // queue the event to be processed
        queue.add(message.getMessageObject());
        // schedule job to process the event with a short delay
        final int syncDelay = configWatcher.get().getSyncDelay();
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    return;
                }

                try {
                    processEventQueue(queue);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, format("%s - Event processing failed", nodeId()), e);
                }

                Metrics.newCounter(getClass(), "reloads").inc();
            }
        }, syncDelay, TimeUnit.SECONDS);
    }

    protected void dispatch(Event e) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(format("%s - Publishing event %s", nodeId(), e));
        }

        e.setSource(localAddress(cluster.getHz()));
        topic.publish(e);

        Metrics.newCounter(getClass(), "dispatched").inc();
    }

    /**
     * Processes the event queue.
     * <p>
     * <b>Note:</b> It is the responsibility of subclasses to clear events from the queue as they
     * are processed.
     * </p>
     */
    protected abstract void processEventQueue(Queue<Event> q) throws Exception;

    ConfigChangeEvent newChangeEvent(CatalogEvent evt, Type type) {
        return newChangeEvent(evt.getSource(), type);
    }

    ConfigChangeEvent newChangeEvent(Info subj, Type type) {
        String name = (String) (OwsUtils.has(subj, "name") ? OwsUtils.get(subj, "name") : null);
        WorkspaceInfo ws = (WorkspaceInfo) (OwsUtils.has(subj, "workspace") ? OwsUtils.get(subj,
                "workspace") : null);

        ConfigChangeEvent ev = new ConfigChangeEvent(subj.getId(), name, subj.getClass(), type);
        if (ws != null) {
            ev.setWorkspaceId(ws.getId());
        }
        return ev;
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.ADD));
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.MODIFY));
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        dispatch(newChangeEvent(event, Type.REMOVE));
    }

    @Override
    public void handleGlobalChange(GeoServerInfo global, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
        // optimization for update sequence
        if (propertyNames.size() == 1 && propertyNames.contains("updateSequence")) {
            return;
        }
        dispatch(newChangeEvent(global, Type.MODIFY));
    }

    @Override
    public void handlePostServiceChange(ServiceInfo service) {
        dispatch(newChangeEvent(service, Type.MODIFY));
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
    public void handleSettingsPostModified(SettingsInfo settings) {
        dispatch(newChangeEvent(settings, Type.MODIFY));
    }

    @Override
    public void handleSettingsRemoved(SettingsInfo settings) {
        dispatch(newChangeEvent(settings, Type.REMOVE));
    }

    protected String nodeId() {
        return localIPAsString(cluster.getHz());
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
