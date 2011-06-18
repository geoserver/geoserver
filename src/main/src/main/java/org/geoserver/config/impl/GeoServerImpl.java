/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;

public class GeoServerImpl implements GeoServer {
    
    private static final Logger LOGGER = Logging.getLogger(GeoServerImpl.class);

    /**
     * factory for creating objects
     */
    GeoServerFactory factory = new GeoServerFactoryImpl(this);
    
    /**
     * the catalog
     */
    Catalog catalog;
    
    /**
     * data access object
     */
    GeoServerFacade facade;
    
    /**
     * listeners
     */
    List<ConfigurationListener> listeners = new ArrayList<ConfigurationListener>();

    public GeoServerImpl() {
        this.facade = new DefaultGeoServerFacade(this);
    }
    
    public GeoServerFacade getFacade() {
        return facade;
    }
    
    public void setFacade(GeoServerFacade facade) {
        this.facade = facade;
        facade.setGeoServer(this);
    }
    
    public GeoServerFactory getFactory() {
        return factory;
    }
    
    public void setFactory(GeoServerFactory factory) {
        this.factory = factory;
    }

    public Catalog getCatalog() {
        return catalog;
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }
    
    public GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }
    
    public void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);
        
        //fire the modification event
        fireGlobalPostModified();
    }
    
    public LoggingInfo getLogging() {
        return facade.getLogging();
    }
    
    public void setLogging(LoggingInfo logging) {
        facade.setLogging(logging);
        fireLoggingPostModified();
    }
    
    public void add(ServiceInfo service) {
        if ( service.getId() == null ) {
            throw new NullPointerException( "service id must not be null" );
        }
        if ( facade.getService(service.getId(), ServiceInfo.class) != null) {
            throw new IllegalArgumentException( "service with id '" + service.getId() + "' already exists" );
        }
        facade.add(service);
        
        //fire post modification event
        firePostServiceModified(service);
    }

    public static <T> T unwrap(T obj) {
        return DefaultGeoServerFacade.unwrap(obj);
    }
    
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return facade.getService(clazz);
    }

    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return facade.getService(id, clazz);
    }

    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        return facade.getServiceByName(name, clazz);
    }

    public Collection<? extends ServiceInfo> getServices() {
        return facade.getServices();
    }
    
    public void remove(ServiceInfo service) {
        facade.remove(service);
    }

    public void save(GeoServerInfo geoServer) {
        facade.save(geoServer);
        
        //fire post modification event
        fireGlobalPostModified();
    }

    public void save(LoggingInfo logging) {
        facade.save(logging);
        
        //fire post modification event
        fireLoggingPostModified();
    } 
    
    void fireGlobalPostModified() {
        for ( ConfigurationListener l : listeners ) {
            try {
                l.handlePostGlobalChange( facade.getGlobal() );
            }
            catch( Exception e ) {
                LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
            }
        }
    }
    
    public void fireGlobalModified(GeoServerInfo global, List<String> changed, List oldValues, 
        List newValues) {
        
        for ( ConfigurationListener l : getListeners() ) {
            try {
                l.handleGlobalChange( global, changed, oldValues, newValues);
            }
            catch( Exception e ) {
                LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
            }
        }
    }

    public void fireLoggingModified(LoggingInfo logging, List<String> changed, List oldValues, 
            List newValues) {
            
        for ( ConfigurationListener l : getListeners() ) {
            try {
                l.handleLoggingChange( logging, changed, oldValues, newValues);
            }
            catch( Exception e ) {
                LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
            }
        }
    }
    
    void fireLoggingPostModified() {
        for ( ConfigurationListener l : listeners ) {
            try {
                l.handlePostLoggingChange( facade.getLogging() );
            }
            catch( Exception e ) {
                LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
            }
        }
    }
    
    public void save(ServiceInfo service) {
        facade.save(service);
        
        //fire post modification event
        firePostServiceModified(service);
    }

    public void fireServiceModified(ServiceInfo service, List<String> changed, List oldValues, 
            List newValues) {
            
        for ( ConfigurationListener l : getListeners() ) {
            try {
                l.handleServiceChange( service, changed, oldValues, newValues);
            }
            catch( Exception e ) {
                LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
            }
        }
    }
    
    void firePostServiceModified(ServiceInfo service) {
        for ( ConfigurationListener l : listeners ) {
            try {
                l.handlePostServiceChange( service );
            }
            catch( Exception e ) {
                LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
            }
        }
    }
    
    public void addListener(ConfigurationListener listener) {
        listeners.add( listener );
    }
    
    public void removeListener(ConfigurationListener listener) {
        listeners.remove( listener );
    }
    
    public Collection<ConfigurationListener> getListeners() {
        return listeners;
    }
    
    public void dispose() {
        // look for pluggable handlers
        for(GeoServerLifecycleHandler handler : GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onDispose();
            } catch(Throwable t) {
                LOGGER.log(Level.SEVERE, "A GeoServer lifecycle handler threw an exception during dispose", t);
            }
        }

        // internal cleanup
        
        if ( catalog != null ) catalog.dispose();
        if ( facade != null ) facade.dispose();
    }

    public void reload() throws Exception {
        // flush caches
        reset();
        
        // reload configuration
        GeoServerLoaderProxy loader = GeoServerExtensions.bean(GeoServerLoaderProxy.class);
        synchronized (org.geoserver.config.GeoServer.CONFIGURATION_LOCK) {
            getCatalog().getResourcePool().dispose();
            loader.reload();
        }
        
        // look for pluggable handlers
        for(GeoServerLifecycleHandler handler : GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onReload();
            } catch(Throwable t) {
                LOGGER.log(Level.SEVERE, "A GeoServer lifecycle handler threw an exception during reload", t);
            }
        }
    }

    public void reset() {
        // drop all the catalog store/feature types/raster caches
        catalog.getResourcePool().dispose();
        
        // reset the referencing subsystem
        CRS.reset("all");
        
        // look for pluggable handlers
        for(GeoServerLifecycleHandler handler : GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onReset();
            } catch(Throwable t) {
                LOGGER.log(Level.SEVERE, "A GeoServer lifecycle handler threw an exception during reset", t);
            }
        }
    }
}
