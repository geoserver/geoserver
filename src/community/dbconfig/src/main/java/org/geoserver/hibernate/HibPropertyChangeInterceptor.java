/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Hibernate interceptor which forwards hibernate events to the catalog.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class HibPropertyChangeInterceptor extends EmptyInterceptor implements ApplicationContextAware {

    
    /**
     * A thread local used to prevent the propagation of the same event over and over which will
     * happen if an event listener makes another query against hibernate
     */
    static ThreadLocal<List> events = new ThreadLocal();
    //TODO: a post request hook to clean up this thread local
    
    private final static Logger LOGGER = Logging.getLogger(HibPropertyChangeInterceptor.class);

    /**
     * spring app context
     */
    ApplicationContext appContext;
    
    /**
     * catalog
     */
    Catalog catalog;
    
    /**
     * config
     */
    GeoServer geoServer;

    public HibPropertyChangeInterceptor() {
        
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }
    
    protected Catalog catalog() {
        if (catalog == null) {
            synchronized (this) {
                if (catalog == null) {
                    catalog = (Catalog) GeoServerExtensions.bean("catalog", appContext);
                }
            }
        }
        return catalog;
    }

    protected GeoServer geoServer() {
        if (geoServer == null) {
            synchronized (this) {
                if (geoServer == null) {
                    geoServer = GeoServerExtensions.bean(GeoServer.class, appContext);
                }
            }
        }
        return geoServer;
    }
    
    public void afterTransactionCompletion(Transaction tx) {
        if (!tx.wasRolledBack()) {
            //TODO: fire post modified... maybe via thread local?
        } 
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState,
            Object[] previousState, String[] propertyNames, Type[] types) {
        
        List<String> propertyNamesChanged = new ArrayList<String>();
        List<Object> oldValues = new ArrayList<Object>();
        List<Object> newValues = new ArrayList<Object>();

        if (previousState != null) {
            for (int i = 0; i < propertyNames.length; i++) {
                Object oldValue = previousState[i];
                Object newValue = currentState[i];

                if (!Utilities.equals(oldValue, newValue)) {
                    propertyNamesChanged.add(propertyNames[i]);
                    oldValues.add(oldValue);
                    newValues.add(newValue);
                }
            }
        } else {
            for (int i = 0; i < propertyNames.length; i++) {
                propertyNamesChanged.add(propertyNames[i]);
            }
        }
        
        Info info = (Info) entity;
        Event e = new Event(info, propertyNamesChanged);
        
        if (events.get() == null) {
            events.set(new ArrayList());
        }
        
        if (events.get().contains(e)) {
            return false;
        }
        
        events.get().add(e);
        try {
            if (!filterEvent(info, propertyNamesChanged, oldValues, newValues)) {
                if (info instanceof CatalogInfo) {
                    catalog().fireModified((CatalogInfo)info, propertyNamesChanged, oldValues, newValues);    
                }
                else {
                    if (info instanceof GeoServerInfo) {
                        geoServer().fireGlobalModified((GeoServerInfo)info, propertyNamesChanged, oldValues, newValues);
                    }
                    else if (info instanceof LoggingInfo) {
                        geoServer().fireLoggingModified((LoggingInfo)info, propertyNamesChanged, oldValues, newValues);
                    }
                    else if (info instanceof ServiceInfo) {
                        geoServer().fireServiceModified((ServiceInfo)info, propertyNamesChanged, oldValues, newValues);
                    }
                }
            }
        } 
        finally {
            events.get().remove(e);
        }
        
        return false;
    }
    
    /*
     * method to filter out situations in which we don;t want to throw an event
     */
    protected boolean filterEvent(
        Info entity, List<String> propertyNamesChanged, List oldValues, List newValues) {
        
        //handle default namespace/workspace changing or default datastore
        if ((entity instanceof WorkspaceInfo || entity instanceof NamespaceInfo || entity instanceof StoreInfo) && 
                propertyNamesChanged.size() == 1 && propertyNamesChanged.contains("default")) {
            return true;
        }
        
        return false;
    }
    
    static class Event {
        Info info;
        List<String> changed;
        
        Event(Info info, List<String> changed) {
            this.info = info;
            this.changed = changed;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((changed == null) ? 0 : changed.hashCode());
            result = prime * result + ((info == null) ? 0 : info.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Event other = (Event) obj;
            if (changed == null) {
                if (other.changed != null)
                    return false;
            } else if (!changed.equals(other.changed))
                return false;
            if (info == null) {
                if (other.info != null)
                    return false;
            } else if (!info.equals(other.info))
                return false;
            return true;
        }
        
    }
}
