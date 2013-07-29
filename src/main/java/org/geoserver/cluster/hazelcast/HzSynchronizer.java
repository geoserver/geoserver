package org.geoserver.cluster.hazelcast;

import static org.geoserver.cluster.hazelcast.HazelcastUtil.*;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.geoserver.cluster.Event;
import org.geoserver.cluster.GeoServerSynchronizer;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.yammer.metrics.Metrics;

/**
 * Base hazelcast based synchronizer that does event collapsing.
 * <p>
 * This synchronizer maintains a thread safe queue that is populated with events as they occur.
 * Upon receiving of an event a new runnable is scheduled and run after a short delay 
 * (default 5 sec). The runnable calls the {@link #processEventQueue(Queue)} method to be 
 * implemented by subclasses. 
 * </p>
 * <p>
 * This synchronizer events messages received from the same source.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class HzSynchronizer extends GeoServerSynchronizer implements MessageListener<Event> {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    HazelcastInstance hz;

    ITopic<Event> topic;
    
    /** event queue */
    Queue<Event> queue;

    /** event processor */
    ScheduledExecutorService executor;

    /** geoserver configuration */
    protected GeoServer gs;
    
    ScheduledExecutorService getNewExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }
    
    public HzSynchronizer(HazelcastInstance hz, GeoServer gs) {
        this.hz = hz;
        this.gs = gs;

        topic = hz.getTopic("geoserver.config");
        topic.addMessageListener(this);
        
        queue = new ConcurrentLinkedQueue<Event>();
        executor = getNewExecutor();

        gs.addListener(this);
        gs.getCatalog().addListener(this);
    }

    @Override
    public void onMessage(Message<Event> message) {
        Metrics.newCounter(getClass(), "recieved").inc();

        Event e = message.getMessageObject();
        if (localAddress(hz).equals(e.getSource())) {
            LOGGER.finer("Skipping message generated locally " + message);
            return;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Message recieved: " + message);
        }

        //queue the event to be processed
        queue.add(message.getMessageObject());

        //schedule job to process the event with a short delay
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    return;
                }

                try {
                    processEventQueue(queue);
                }
                catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Event processing failed", e);
                }

                Metrics.newCounter(getClass(), "reloads").inc();
            }
        }, configWatcher.get().getSyncDelay(), TimeUnit.SECONDS);
    }

    protected void dispatch(Event e) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Publishing event");
        }

        e.setSource(localAddress(hz));
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
        WorkspaceInfo ws = (WorkspaceInfo) (OwsUtils.has(subj, "workspace") ? 
            OwsUtils.get(subj, "workspace") : null);

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
        //optimization for update sequence
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
}
