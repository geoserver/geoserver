package org.geoserver.cluster.hazelcast;

import static org.geoserver.cluster.hazelcast.HazelcastUtil.localAddress;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cluster.Event;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.yammer.metrics.Metrics;

public class SimpleHzSynchronizer extends HzSynchronizer {

    /** geoserver configuration */
    GeoServer gs;

    /** event queue */
    Queue<Event> queue;

    /** event processor */
    ScheduledExecutorService executor;

    /** lock during reload */
    AtomicBoolean reloadLock = new AtomicBoolean();

    public SimpleHzSynchronizer(HazelcastInstance hz, GeoServer gs) {
        super(hz);
        this.gs = gs;

        queue = new ConcurrentLinkedQueue<Event>();
        executor = Executors.newSingleThreadScheduledExecutor();

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

                //lock during reload
                reloadLock.set(true);
                try {
                    queue.clear();
                    gs.reload();

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Reload failed", e);
                }
                finally {
                    reloadLock.set(false);
                }

                Metrics.newCounter(getClass(), "reloads").inc();
            }
        }, configWatcher.get().getSyncDelay(), TimeUnit.SECONDS);
    }

    protected void dispatch() {
        //check lock, if locked it means event in response to configuration reload, don't propagate
        if (reloadLock.get()) {
            return;
        }
    
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Publishing event");
        }
        topic.publish(newEvent());
        Metrics.newCounter(getClass(), "dispatched").inc();
    }


    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        dispatch();
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event)
            throws CatalogException {
        dispatch();
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event)
            throws CatalogException {
        dispatch();
    }

    @Override
    public void handleGlobalChange(GeoServerInfo global, List<String> propertyNames, 
        List<Object> oldValues, List<Object> newValues) {
        //optimization for update sequence
        if (propertyNames.size() == 1 && propertyNames.contains("updateSequence")) {
            return;
        }
        dispatch();
    }

    @Override
    public void handlePostServiceChange(ServiceInfo service) {
        dispatch();
    }

    @Override
    public void handleServiceRemove(ServiceInfo service) {
        dispatch();
    }

    @Override
    public void handleSettingsAdded(SettingsInfo settings) {
        dispatch();
    }

    @Override
    public void handleSettingsPostModified(SettingsInfo settings) {
        dispatch();
    }

    @Override
    public void handleSettingsRemoved(SettingsInfo settings) {
        dispatch();
    }
}
