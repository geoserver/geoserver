/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.hib;

import java.util.Collection;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.hibernate.AbstractHibFacade;
import org.geoserver.ows.util.OwsUtils;
import org.hibernate.Query;

public class HibGeoServerFacade extends AbstractHibFacade implements GeoServerFacade {

    GeoServer geoServer;
    
    public GeoServer getGeoServer() {
        return geoServer;
    }
    
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }
    
    //
    // global
    //
    public GeoServerInfo getGlobal() {
        return (GeoServerInfo) first( query("from ", GeoServerInfo.class) );
    }
    
    public void setGlobal(GeoServerInfo global) {
        GeoServerInfo existing = getGlobal();
        set(existing, global, GeoServerInfo.class);
    }

    
    public void save(GeoServerInfo geoServer) {
        merge(geoServer);
    }

    //
    // settings
    //
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not implemnted");
    }

    public void add(SettingsInfo settings) {
        throw new UnsupportedOperationException("not implemnted");
    }

    public void save(SettingsInfo settings) {
        throw new UnsupportedOperationException("not implemnted");
    }

    public void remove(SettingsInfo settings) {
        throw new UnsupportedOperationException("not implemnted");
    }

    //
    // logging
    //
    public void setLogging(LoggingInfo logging) {
        LoggingInfo existing = getLogging();
        set(existing, logging, LoggingInfo.class);
    }

    public LoggingInfo getLogging() {
        return (LoggingInfo) first( query("from ", LoggingInfo.class) );
    }

    public void save(LoggingInfo logging) {
        merge(logging);
    }

    //
    // services
    //
    public void add(ServiceInfo service) {
        //service id's are assigned by the application, so we don't clear them when persisting
        persist(service, false);
    }
    
    public void save(ServiceInfo service) {
        merge(service);
    }

    public void remove(ServiceInfo service) {
        delete(service);
    }
    
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return (T) first( query("from ", clazz) );
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        throw new UnsupportedOperationException("not implemented");
    }

    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return (T) first( query("from ", clazz, " where id = ", param(id)) );
    }

    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        Query query = query("from ", clazz, " where name = ", param(name));
        return (T) first(query);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, WorkspaceInfo workspace,
            Class<T> clazz) {
        throw new UnsupportedOperationException("not implemented");
    }

    public Collection<? extends ServiceInfo> getServices() {
        return list(ServiceInfo.class);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void dispose() {
    }
    
    <T extends Info> void  set(T existing, T info, Class<T> clazz) {
        if (existing != null) {
            if (info != null) {
                OwsUtils.copy(info, existing, clazz);
                merge(existing);
            }
            else {
                delete(existing);
            }
        }
        else {
            if (info != null) {
                persist(info);
            }
        }
    }

}
