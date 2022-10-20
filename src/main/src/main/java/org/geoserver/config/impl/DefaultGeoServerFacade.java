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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    SettingsInfoLookup settings = new SettingsInfoLookup();
    LoggingInfo logging;
    ServiceInfoLookup services = new ServiceInfoLookup();

    GeoServer geoServer;

    public DefaultGeoServerFacade(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.global = geoServer.getFactory().createGlobal();
        this.logging = geoServer.getFactory().createLogging();
    }

    @Override
    public GeoServer getGeoServer() {
        return geoServer;
    }

    @Override
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public GeoServerInfo getGlobal() {
        if (global == null) {
            return null;
        }

        return ModificationProxy.create(global, GeoServerInfo.class);
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        resolve(global);
        setId(global.getSettings());
        this.global = global;
    }

    @Override
    public void save(GeoServerInfo global) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(global);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireGlobalModified(global, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        SettingsInfo s = settings.findByWorkspace(workspace.getId());
        return s == null ? null : ModificationProxy.create(s, SettingsInfo.class);
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

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        settings = (SettingsInfo) proxy.getProxyObject();
        geoServer.fireSettingsModified(settings, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    @Override
    public void remove(SettingsInfo s) {
        s = unwrap(s);
        settings.remove(s.getId());
    }

    @Override
    public LoggingInfo getLogging() {
        if (logging == null) {
            return null;
        }

        return ModificationProxy.create(logging, LoggingInfo.class);
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        this.logging = logging;
    }

    @Override
    public void save(LoggingInfo logging) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(logging);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireLoggingModified(logging, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    @Override
    public void add(ServiceInfo service) {
        // may be adding a proxy, need to unwrap
        service = unwrap(service);
        setId(service);
        service.setGeoServer(geoServer);

        services.add(service);
    }

    @Override
    public void save(ServiceInfo service) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(service);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireServiceModified(service, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    @Override
    public void remove(ServiceInfo service) {
        services.remove(service.getId());
    }

    @Override
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return getService((WorkspaceInfo) null, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        List<ServiceInfo> wsServices = services.findByWorkspace(workspace);
        Optional<T> found =
                wsServices.stream()
                        .filter(clazz::isInstance)
                        .map(clazz::cast)
                        .findFirst()
                        .map(s -> ModificationProxy.create(s, clazz));
        if (!found.isPresent()) {
            LOGGER.log(
                    Level.FINE,
                    "Could not locate service of type {0} in workspace {1}, available services were {2}",
                    new Object[] {clazz, workspace, wsServices});
        }
        return found.orElse(null);
    }

    @Override
    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        ServiceInfo serviceInfo = services.get(id);
        if (clazz.isInstance(serviceInfo)) {
            return ModificationProxy.create(clazz.cast(serviceInfo), clazz);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "Could not locate service of type {0} and id {1}, got {2}",
                    new Object[] {clazz, id, serviceInfo});
        }

        return null;
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        return getServiceByName(name, null, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {

        List<ServiceInfo> wsServices = services.findByWorkspace(workspace);
        Optional<T> found =
                wsServices.stream()
                        .filter(s -> name.equals(s.getName()))
                        .filter(clazz::isInstance)
                        .map(clazz::cast)
                        .findFirst()
                        .map(s -> ModificationProxy.create(s, clazz));

        if (!found.isPresent() && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "Could not locate service of type {0} in workspace {1} and name \'{2}\', available services were {3}",
                    new Object[] {clazz, workspace, name, wsServices});
        }

        return found.orElse(null);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices() {
        return getServices((WorkspaceInfo) null);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return ModificationProxy.createList(services.findByWorkspace(workspace), ServiceInfo.class);
    }

    @Override
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
            global.setClientProperties(new HashMap<>());
        }
        if (global.getCoverageAccess() == null) {
            global.setCoverageAccess(new CoverageAccessInfoImpl());
        }
    }

    public List<ServiceInfo> filter(Collection<ServiceInfo> services, WorkspaceInfo workspace) {
        List<ServiceInfo> list = new ArrayList<>();
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

    private static class SettingsInfoLookup {
        protected ConcurrentMap<String, SettingsInfo> idMap = new ConcurrentHashMap<>();
        protected ConcurrentMap<String, SettingsInfo> workspaceIdMap = new ConcurrentHashMap<>();

        public SettingsInfo findByWorkspace(String workspaceId) {
            return workspaceIdMap.get(workspaceId);
        }

        public void remove(String id) {
            SettingsInfo s = idMap.remove(id);
            if (null != s) {
                WorkspaceInfo ws = s.getWorkspace();
                if (ws != null) workspaceIdMap.remove(ws.getId());
            }
        }

        public void clear() {
            idMap.clear();
            workspaceIdMap.clear();
        }

        public void add(SettingsInfo s) {
            Objects.requireNonNull(s.getWorkspace());
            Objects.requireNonNull(s.getWorkspace().getId());
            idMap.put(s.getId(), s);
            workspaceIdMap.put(s.getWorkspace().getId(), s);
        }
    }

    private static class ServiceInfoLookup {
        private static final String NULL_WORKSPACE_ID = "";
        private Map<String, ServiceInfo> idMap = new ConcurrentHashMap<>();
        private Map<String, Map<String, ServiceInfo>> workspaceIdMap = new ConcurrentHashMap<>();
        // used by add/remove so remove() can clear an empty entry once all services for a given
        // workspace are removed and avoid memory leaks
        private Lock wsLock = new ReentrantLock();

        public void add(ServiceInfo s) {
            idMap.put(s.getId(), s);

            String wsId = workspaceId(s.getWorkspace());
            wsLock.lock();
            try {
                workspaceIdMap
                        .computeIfAbsent(wsId, id -> new ConcurrentHashMap<>())
                        .put(s.getId(), s);
            } finally {
                wsLock.unlock();
            }
        }

        public void remove(String id) {
            ServiceInfo s = idMap.remove(id);
            if (null != s) {
                String wsid = workspaceId(s.getWorkspace());
                wsLock.lock();
                try {
                    Map<String, ServiceInfo> byWs = workspaceIdMap.get(wsid);
                    byWs.remove(s.getId());
                    if (byWs.isEmpty()) {
                        // remove the entry and avoid memory leaks leaving empty maps
                        workspaceIdMap.remove(wsid);
                    }
                } finally {
                    wsLock.unlock();
                }
            }
        }

        public ServiceInfo get(String id) {
            return idMap.get(id);
        }

        public void clear() {
            idMap.clear();
            workspaceIdMap.clear();
        }

        public List<ServiceInfo> findByWorkspace(WorkspaceInfo workspace) {
            String workspaceId = workspaceId(workspace);
            return new ArrayList<>(
                    workspaceIdMap.getOrDefault(workspaceId, Collections.emptyMap()).values());
        }

        private String workspaceId(WorkspaceInfo ws) {
            return ws == null ? NULL_WORKSPACE_ID : ws.getId();
        }
    }
}
