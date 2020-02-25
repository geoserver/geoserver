/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import static org.geoserver.ows.util.OwsUtils.resolveCollections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.referencing.CRS;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class GeoServerImpl implements GeoServer, ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(GeoServerImpl.class);

    /** factory for creating objects */
    GeoServerFactory factory = new GeoServerFactoryImpl(this);

    /** the catalog */
    Catalog catalog;

    /** data access object */
    GeoServerFacade facade;

    /** listeners */
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

        // This instance of check is has to be here because this Geoserver cannot be injected
        // into LocalWorkspaceCatalog because it causes a circular reference
        if (catalog instanceof LocalWorkspaceCatalog) {
            LocalWorkspaceCatalog lwCatalog = (LocalWorkspaceCatalog) catalog;
            lwCatalog.setGeoServer(this);
        }
    }

    public GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }

    public void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);

        // fire the modification event
        fireGlobalPostModified();
    }

    public SettingsInfo getSettings() {
        SettingsInfo settings = null;
        if (LocalWorkspace.get() != null) {
            settings = getSettings(LocalWorkspace.get());
        }
        return settings != null ? settings : getGlobal().getSettings();
    }

    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        return facade.getSettings(workspace);
    }

    public void add(SettingsInfo settings) {
        validate(settings);
        resolve(settings);

        WorkspaceInfo workspace = settings.getWorkspace();
        if (facade.getSettings(workspace) != null) {
            throw new IllegalArgumentException(
                    "Settings already exist for workspace '" + workspace.getName() + "'");
        }

        facade.add(settings);
        fireSettingsAdded(settings);
    }

    public void save(SettingsInfo settings) {
        validate(settings);

        facade.save(settings);
        fireSettingsPostModified(settings);
    }

    public void remove(SettingsInfo settings) {
        facade.remove(settings);

        fireSettingsRemoved(settings);
    }

    void validate(SettingsInfo settings) {
        WorkspaceInfo workspace = settings.getWorkspace();
        if (workspace == null) {
            throw new IllegalArgumentException("Settings must be part of a workspace");
        }
    }

    void resolve(SettingsInfo settings) {
        resolveCollections(settings);
    }

    void fireSettingsAdded(SettingsInfo settings) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsAdded(settings);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    public void fireSettingsModified(
            SettingsInfo settings, List<String> changed, List oldValues, List newValues) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsModified(settings, changed, oldValues, newValues);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    void fireSettingsPostModified(SettingsInfo settings) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsPostModified(settings);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    void fireSettingsRemoved(SettingsInfo settings) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsRemoved(settings);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    public LoggingInfo getLogging() {
        return facade.getLogging();
    }

    public void setLogging(LoggingInfo logging) {
        facade.setLogging(logging);
        fireLoggingPostModified();
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (factory instanceof ApplicationContextAware) {
            ((ApplicationContextAware) factory).setApplicationContext(context);
        }
    }

    public void add(ServiceInfo service) {
        if (service.getId() != null
                && facade.getService(service.getId(), ServiceInfo.class) != null) {
            throw new IllegalArgumentException(
                    "service with id '" + service.getId() + "' already exists");
        }

        resolve(service);
        WorkspaceInfo workspace = service.getWorkspace();
        if (workspace != null) {
            if (facade.getServiceByName(service.getName(), workspace, ServiceInfo.class) != null) {
                throw new IllegalArgumentException(
                        "service with name '"
                                + service.getName()
                                + "' already exists in workspace '"
                                + workspace.getName()
                                + "'");
            }
        }
        facade.add(service);

        // fire post modification event
        firePostServiceModified(service);
    }

    void resolve(ServiceInfo service) {
        resolveCollections(service);
    }

    public static <T> T unwrap(T obj) {
        return DefaultGeoServerFacade.unwrap(obj);
    }

    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        WorkspaceInfo ws = LocalWorkspace.get();
        T service = ws != null ? facade.getService(ws, clazz) : null;
        service = service != null ? service : facade.getService(clazz);
        if (service == null) {
            LOGGER.log(
                    Level.SEVERE,
                    "Could not locate service of type " + clazz + ", local workspace is " + ws);
        }

        return service;
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getService(workspace, clazz);
    }

    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return facade.getService(id, clazz);
    }

    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        T service =
                LocalWorkspace.get() != null
                        ? facade.getServiceByName(name, LocalWorkspace.get(), clazz)
                        : null;
        return service != null ? service : facade.getServiceByName(name, clazz);
    }

    public <T extends ServiceInfo> T getServiceByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getServiceByName(name, workspace, clazz);
    }

    public Collection<? extends ServiceInfo> getServices() {
        Collection<? extends ServiceInfo> services =
                LocalWorkspace.get() != null ? facade.getServices(LocalWorkspace.get()) : null;
        return services != null ? services : facade.getServices();
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return facade.getServices(workspace);
    }

    public void remove(ServiceInfo service) {
        facade.remove(service);

        fireServiceRemoved(service);
    }

    public void save(GeoServerInfo geoServer) {
        facade.save(geoServer);

        // fire post modification event
        fireGlobalPostModified();
    }

    public void save(LoggingInfo logging) {
        facade.save(logging);

        // fire post modification event
        fireLoggingPostModified();
    }

    void fireGlobalPostModified() {
        for (ConfigurationListener l : listeners) {
            try {
                l.handlePostGlobalChange(facade.getGlobal());
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    public void fireGlobalModified(
            GeoServerInfo global, List<String> changed, List oldValues, List newValues) {

        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleGlobalChange(global, changed, oldValues, newValues);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    public void fireLoggingModified(
            LoggingInfo logging, List<String> changed, List oldValues, List newValues) {

        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleLoggingChange(logging, changed, oldValues, newValues);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    void fireLoggingPostModified() {
        for (ConfigurationListener l : listeners) {
            try {
                l.handlePostLoggingChange(facade.getLogging());
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    public void save(ServiceInfo service) {
        validate(service);

        facade.save(service);

        // fire post modification event
        firePostServiceModified(service);
    }

    void validate(ServiceInfo service) {
        CatalogImpl.validateKeywords(service.getKeywords());
    }

    public void fireServiceModified(
            ServiceInfo service, List<String> changed, List oldValues, List newValues) {

        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleServiceChange(service, changed, oldValues, newValues);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    void firePostServiceModified(ServiceInfo service) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handlePostServiceChange(service);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    void fireServiceRemoved(ServiceInfo service) {
        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleServiceRemove(service);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred processing a configuration change listener",
                        e);
            }
        }
    }

    public void addListener(ConfigurationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ConfigurationListener listener) {
        listeners.remove(listener);
    }

    public Collection<ConfigurationListener> getListeners() {
        return listeners;
    }

    public void dispose() {
        // look for pluggable handlers
        for (GeoServerLifecycleHandler handler :
                GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onDispose();
            } catch (Throwable t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during dispose",
                        t);
            }
        }

        // internal cleanup

        if (catalog != null) catalog.dispose();
        if (facade != null) facade.dispose();
    }

    public void reload() throws Exception {
        this.reload(null);
    }

    public void reload(Catalog newCatalog) throws Exception {
        // notify start of reload
        List<GeoServerLifecycleHandler> handlers =
                GeoServerExtensions.extensions(GeoServerLifecycleHandler.class);
        for (GeoServerLifecycleHandler handler : handlers) {
            try {
                handler.beforeReload();
            } catch (Throwable t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during reload",
                        t);
            }
        }

        // perform the reload
        try {
            // flush caches
            reset();

            // reload configuration
            GeoServerLoaderProxy loader = GeoServerExtensions.bean(GeoServerLoaderProxy.class);
            synchronized (org.geoserver.config.GeoServer.CONFIGURATION_LOCK) {
                getCatalog().getResourcePool().dispose();

                if (newCatalog != null) {
                    dispose();

                    // reload catalog, make sure we reload the underlying catalog, not any wrappers
                    Catalog catalog = getCatalog();
                    if (catalog instanceof Wrapper) {
                        catalog = ((Wrapper) getCatalog()).unwrap(Catalog.class);
                    }

                    ((CatalogImpl) catalog).sync((CatalogImpl) newCatalog);
                    ((CatalogImpl) catalog).resolve();
                } else {
                    loader.reload();
                }
            }
        } finally {
            // notify end of reload
            for (GeoServerLifecycleHandler handler : handlers) {
                try {
                    handler.onReload();
                } catch (Throwable t) {
                    LOGGER.log(
                            Level.SEVERE,
                            "A GeoServer lifecycle handler threw an exception during reload",
                            t);
                }
            }
        }
    }

    public void reset() {
        // drop all the catalog store/feature types/raster caches
        catalog.getResourcePool().dispose();

        // reset the referencing subsystem
        CRS.reset("all");

        // look for pluggable handlers
        for (GeoServerLifecycleHandler handler :
                GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onReset();
            } catch (Throwable t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during reset",
                        t);
            }
        }
    }
}
