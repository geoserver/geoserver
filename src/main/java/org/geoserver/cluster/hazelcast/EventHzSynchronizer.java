package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Queue;
import java.util.logging.Level;

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
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogEventImpl;
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

import com.google.common.collect.ImmutableList;

/**
 * Synchronizer that converts cluster events and dispatches them the GeoServer config/catalog.
 * <p>
 * This synchronizer assumes a shared data directory among nodes in the cluster.
 * </p> 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class EventHzSynchronizer extends HzSynchronizer {

    public EventHzSynchronizer(HzCluster cluster, GeoServer gs) {
        super(cluster, gs);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processEventQueue(Queue<Event> q) throws Exception {
        Catalog cat = gs.getCatalog();
        Iterator<Event> it = q.iterator();
        while (it.hasNext() && isStarted()) {
            final Event event = it.next();
            it.remove();
            LOGGER.fine(format("%s - Processing event %s", nodeId(), event));
            if (event instanceof ConfigChangeEvent) {
                ConfigChangeEvent ce = (ConfigChangeEvent) event;
                Type t = ce.getChangeType();
                Class<? extends Info> clazz = ce.getObjectInterface();
                String id = ce.getObjectId();
                String name = ce.getObjectName();

                if (CatalogInfo.class.isAssignableFrom(clazz)) {
                    //catalog event
                    CatalogInfo subj;
                    Method notifyMethod;
                    CatalogEventImpl evt;
                    switch(t) {
                    case ADD:
                        subj = getCatalogInfo(cat, id, clazz);
                        notifyMethod = CatalogListener.class.getMethod("handleAddEvent", CatalogAddEvent.class);
                        evt = new CatalogAddEventImpl();
                        break;
                    case MODIFY:
                        subj = getCatalogInfo(cat, id, clazz);
                        notifyMethod = CatalogListener.class.getMethod("handlePostModifyEvent", CatalogPostModifyEvent.class);
                        evt = new CatalogPostModifyEventImpl();
                        break;
                    case REMOVE:
                        notifyMethod = CatalogListener.class.getMethod("handleRemoveEvent", CatalogRemoveEvent.class);
                        evt = new CatalogRemoveEventImpl();
                        subj = (CatalogInfo) Proxy.newProxyInstance(getClass().getClassLoader(), 
                                new Class[]{clazz}, new RemovedObjectProxy(id, name));
                        break;
                    default:
                        throw new IllegalStateException("Should not happen");
                    }
                    
                    if (subj == null) {//can't happen if type == DELETE
                        ConfigChangeEvent removeEvent = new ConfigChangeEvent(id, name, clazz,
                                Type.REMOVE);
                        if (queue.contains(removeEvent)) {
                            LOGGER.fine(format("%s - Ignoring event %s, a remove is queued.",
                                    nodeId(), event));
                            continue;
                        }
                        //this could be latency in the catalog itself, abort processing since
                        // events need to processed in order and further events might depend 
                        // on this event
                        Catalog rawCatalog = cluster.getRawCatalog();
                        subj = getCatalogInfo(rawCatalog, id, clazz);
                        if (subj == null) {
                            String message = format(
                                    "%s - Error processing event %s: object not found in catalog",
                                    nodeId(), event);
                            LOGGER.warning(message);
                            continue;
                        }else{
                            String message = format(
                                    "%s - Object for event %s not found in secured catalog but found in RAW catalog!",
                                    nodeId(), event);
                            LOGGER.info(message);
                        }
                    }
                    
                    evt.setSource(subj);
                    try {
                        for (CatalogListener l: ImmutableList.copyOf(cat.getListeners())){
                            // Don't notify self otherwise the event bounces back out into the
                            // cluster.
                            if (l != this && isStarted()) {
                                notifyMethod.invoke(l, evt);
                            }
                        }
                    }
                    catch(Exception ex) {
                        LOGGER.log(Level.WARNING,
                                format("%s - Event dispatch failed: %s", nodeId(), event), ex);
                    }

                }
                else {
                    Info subj;
                    Method notifyMethod;
                    
                    if(GeoServerInfo.class.isAssignableFrom(clazz)){
                        subj = gs.getGlobal();
                        notifyMethod = ConfigurationListener.class.getMethod("handlePostGlobalChange", GeoServerInfo.class);
                    } else if (SettingsInfo.class.isAssignableFrom(clazz)) {
                        WorkspaceInfo ws = ce.getWorkspaceId() != null ? 
                                cat.getWorkspace(ce.getWorkspaceId()) : null;
                        subj = ws != null ? gs.getSettings(ws) : gs.getSettings();
                        notifyMethod = ConfigurationListener.class.getMethod("handleSettingsPostModified", SettingsInfo.class);
                    } else if (LoggingInfo.class.isAssignableFrom(clazz)) {
                        subj = gs.getLogging();
                        notifyMethod = ConfigurationListener.class.getMethod("handlePostLoggingChange", LoggingInfo.class);
                    } else if (ServiceInfo.class.isAssignableFrom(clazz)) {
                        subj = gs.getService(id, (Class<ServiceInfo>) clazz);
                        notifyMethod = ConfigurationListener.class.getMethod("handlePostServiceChange", ServiceInfo.class);
                    } else {
                        throw new IllegalStateException("Unknown event type "+clazz);
                    }

                    for (ConfigurationListener l : gs.getListeners()) {
                        try {
                            if(l!=this) notifyMethod.invoke(l, subj);
                        }
                        catch(Exception ex) {
                            LOGGER.log(Level.WARNING,
                                    format("%s - Event dispatch failed: %s", nodeId(), event), ex);
                        }
                    }
                }
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
