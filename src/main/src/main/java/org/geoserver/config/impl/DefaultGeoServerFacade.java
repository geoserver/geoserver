/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

public class DefaultGeoServerFacade implements GeoServerFacade {

    static final Logger LOGGER = Logging.getLogger(DefaultGeoServerFacade.class);

    GeoServerInfo global;
    List<SettingsInfo> settings = new ArrayList<SettingsInfo>();
    LoggingInfo logging;
    List<ServiceInfo> services = new ArrayList<ServiceInfo>();

    GeoServer geoServer;

    public DefaultGeoServerFacade(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.global = geoServer.getFactory().createGlobal();
        this.logging = geoServer.getFactory().createLogging();
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public GeoServerInfo getGlobal() {
        if (global == null) {
            return null;
        }

        return ModificationProxy.create(global, GeoServerInfo.class);
    }

    public void setGlobal(GeoServerInfo global) {
        resolve(global);
        setId(global.getSettings());
        this.global = global;
    }

    public void save(GeoServerInfo global) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(global);

        List propertyNames = proxy.getPropertyNames();
        List oldValues = proxy.getOldValues();
        List newValues = proxy.getNewValues();

        geoServer.fireGlobalModified(global, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        for (SettingsInfo s : settings) {
            if (s.getWorkspace().equals(workspace)) {
                return ModificationProxy.create(s, SettingsInfo.class);
            }
        }
        return null;
    }

    @Override
    public void add(SettingsInfo s) {
        s = unwrap(s);
        setId(s);
        settings.add(s);
    }

    @Override
    public void save(SettingsInfo settings) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(settings);

        List propertyNames = proxy.getPropertyNames();
        List oldValues = proxy.getOldValues();
        List newValues = proxy.getNewValues();

        settings = (SettingsInfo) proxy.getProxyObject();
        geoServer.fireSettingsModified(settings, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    @Override
    public void remove(SettingsInfo s) {
        s = unwrap(s);
        settings.remove(s);
    }

    public LoggingInfo getLogging() {
        if (logging == null) {
            return null;
        }

        return ModificationProxy.create(logging, LoggingInfo.class);
    }

    public void setLogging(LoggingInfo logging) {
        this.logging = logging;
    }

    public void save(LoggingInfo logging) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(logging);

        List propertyNames = proxy.getPropertyNames();
        List oldValues = proxy.getOldValues();
        List newValues = proxy.getNewValues();

        geoServer.fireLoggingModified(logging, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    public void add(ServiceInfo service) {
        // may be adding a proxy, need to unwrap
        service = unwrap(service);
        setId(service);
        service.setGeoServer(geoServer);

        services.add(service);
    }

    public void save(ServiceInfo service) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(service);

        List propertyNames = proxy.getPropertyNames();
        List oldValues = proxy.getOldValues();
        List newValues = proxy.getNewValues();

        geoServer.fireServiceModified(service, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    public void remove(ServiceInfo service) {
        services.remove(service);
    }

    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return find(clazz, null, services);
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return find(clazz, workspace, services);
    }

    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        for (ServiceInfo si : services) {
            if (id.equals(si.getId())) {
                return ModificationProxy.create((T) si, clazz);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + clazz
                            + " and id '"
                            + id
                            + "', available services were "
                            + services);
        }

        return null;
    }

    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        return findByName(name, null, clazz, services);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return findByName(name, workspace, clazz, services);
    }

    public Collection<? extends ServiceInfo> getServices() {
        return ModificationProxy.createList(filter(services, null), ServiceInfo.class);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return ModificationProxy.createList(filter(services, workspace), ServiceInfo.class);
    }

    public void dispose() {
        if (global != null) global.dispose();
        if (settings != null) settings.clear();
        if (services != null) services.clear();
    }

    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    protected void resolve(GeoServerInfo info) {
        GeoServerInfoImpl global = (GeoServerInfoImpl) info;
        if (global.getMetadata() == null) {
            global.setMetadata(new MetadataMap());
        }
        if (global.getClientProperties() == null) {
            global.setClientProperties(new HashMap<Object, Object>());
        }
        if (global.getCoverageAccess() == null) {
            global.setCoverageAccess(new CoverageAccessInfoImpl());
        }
    }

    <T extends ServiceInfo> T find(
            Class<T> clazz, WorkspaceInfo workspace, List<ServiceInfo> services) {
        for (ServiceInfo si : services) {
            if (clazz.isAssignableFrom(si.getClass()) && wsEquals(workspace, si.getWorkspace())) {

                return ModificationProxy.create((T) si, clazz);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + clazz
                            + " in workspace "
                            + workspace
                            + ", available services were "
                            + services);
        }

        return null;
    }

    <T extends ServiceInfo> T findByName(
            String name, WorkspaceInfo workspace, Class<T> clazz, List<ServiceInfo> services) {
        for (ServiceInfo si : services) {
            if (name.equals(si.getName()) && wsEquals(workspace, si.getWorkspace())) {
                return ModificationProxy.create((T) si, clazz);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + clazz
                            + " in workspace "
                            + workspace
                            + " and name '"
                            + name
                            + "', available services were "
                            + services);
        }

        return null;
    }

    public List filter(List<ServiceInfo> services, WorkspaceInfo workspace) {
        List<ServiceInfo> list = new ArrayList();
        for (ServiceInfo si : services) {
            if (wsEquals(workspace, si.getWorkspace())) {
                list.add(si);
            }
        }
        return list;
    }

    boolean wsEquals(WorkspaceInfo ws1, WorkspaceInfo ws2) {
        if (ws1 == null) {
            return ws2 == null;
        }

        return ws1.equals(ws2);
    }

    protected void setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }
}
