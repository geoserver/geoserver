package org.geoserver.cluster.hazelcast;

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
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.cluster.Event;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

import com.hazelcast.core.HazelcastInstance;

/**
 * Synchronizer that converts cluster events and dispatches them the GeoServer config/catalog.
 * <p>
 * This synchronizer assumes a shared data directory among nodes in the cluster.
 * </p> 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class EventHzSynchronizer extends HzSynchronizer {

    public EventHzSynchronizer(HazelcastInstance hz, GeoServer gs) {
        super(hz, gs);
    }

    @Override
    protected void processEventQueue(Queue<Event> q) throws Exception {
        Catalog cat = gs.getCatalog();
        Iterator<Event> it = q.iterator();
        while (it.hasNext()) {
            Event e = it.next();
            if (e instanceof ConfigChangeEvent) {
                ConfigChangeEvent ce = (ConfigChangeEvent) e;
                Type t = ce.getChangeType();
                Class<? extends Info> clazz = ce.getObjectInterface();
                String id = ce.getObjectId();
                String name = ce.getObjectName();

                if (CatalogInfo.class.isAssignableFrom(clazz)) {
                    //catalog event
                    CatalogInfo subj = null;
                    if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
                        subj = cat.getWorkspace(id);
                    }
                    else if (NamespaceInfo.class.isAssignableFrom(clazz)) {
                        subj = cat.getNamespace(id);
                    }
                    else if (StoreInfo.class.isAssignableFrom(clazz)) {
                        subj = (CatalogInfo) cat.getStore(id, (Class) clazz);
                    }
                    else if (ResourceInfo.class.isAssignableFrom(clazz)) {
                        subj = (CatalogInfo) cat.getResource(id, (Class) clazz);
                    }
                    else if (LayerInfo.class.isAssignableFrom(clazz)) {
                        subj = cat.getLayer(id);
                    }
                    else if (StyleInfo.class.isAssignableFrom(clazz)) {
                        subj = cat.getStyle(id);
                    }
                    else if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
                        subj = cat.getLayerGroup(id);
                    }

                    switch(t) {
                    case ADD:
                    case MODIFY:
                        if (subj == null) {
                            //this could be latency in the catalog itself, abort processing since
                            // events need to processed in order and further events might depend 
                            // on this event
                            LOGGER.warning(String.format("Received %s event for (%s, %s) but could" 
                                + " not find in catalog", t.name(), id, clazz.getSimpleName()));
                            return;
                        }

                        try {
                            if (t == Type.ADD) {
                                cat.fireAdded(subj);
                            }
                            else {
                                cat.firePostModified(subj);
                            }
                            
                        }
                        catch(Exception ex) {
                            LOGGER.log(Level.WARNING, "Event dispatch failed", ex);
                        }
                        break;

                    case REMOVE:
                        //since we don't have a subject to dispatch in this case we proxy for it
                        subj = (CatalogInfo) Proxy.newProxyInstance(getClass().getClassLoader(), 
                            new Class[]{clazz}, new RemovedObjectProxy(id, name));
                        cat.fireRemoved(subj);
                    }
                }
                else {
                    //geoserver event
                    //JD: GEoServer doesn't expose the firePostModified() event triggers to 
                    // we do it manually, TODO: add this to the public interface
                    if (GeoServerInfo.class.isAssignableFrom(clazz)) {
                        GeoServerInfo subj = gs.getGlobal();
                        for (ConfigurationListener l : gs.getListeners()) {
                            try {
                                l.handlePostGlobalChange(subj);
                            }
                            catch(Exception ex) {
                                LOGGER.log(Level.WARNING, "Event dispatch failed", ex);
                            }
                        }
                    }
                    else if (SettingsInfo.class.isAssignableFrom(clazz)) {
                        
                        WorkspaceInfo ws = ce.getWorkspaceId() != null ? 
                            cat.getWorkspace(ce.getWorkspaceId()) : null;
                        SettingsInfo subj = ws != null ? gs.getSettings(ws) : gs.getSettings();
                        for (ConfigurationListener l : gs.getListeners()) {
                            try {
                                l.handleSettingsPostModified(subj);
                            }
                            catch(Exception ex) {
                                LOGGER.log(Level.WARNING, "Event dispatch failed", ex);
                            }
                        }
                    }
                    else if (LoggingInfo.class.isAssignableFrom(clazz)) {
                        LoggingInfo subj = gs.getLogging();
                        for (ConfigurationListener l : gs.getListeners()) {
                            try {
                                l.handlePostLoggingChange(subj);
                            }
                            catch(Exception ex) {
                                LOGGER.log(Level.WARNING, "Event dispatch failed", ex);
                            }
                        }
                    }
                    else if (ServiceInfo.class.isAssignableFrom(clazz)) {
                        ServiceInfo subj = gs.getService(id, (Class) clazz);
                        for (ConfigurationListener l : gs.getListeners()) {
                            try {
                                l.handlePostServiceChange(subj);
                            }
                            catch(Exception ex) {
                                LOGGER.log(Level.WARNING, "Event dispatch failed", ex);
                            }
                        }
                    }
                }
            }
            
            it.remove();
        }
    }

}
