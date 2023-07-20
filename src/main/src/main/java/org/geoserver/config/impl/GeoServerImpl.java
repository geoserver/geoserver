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
import java.util.function.Consumer;
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
    List<ConfigurationListener> listeners = new ArrayList<>();

    public GeoServerImpl() {
        this.facade = new DefaultGeoServerFacade(this);
    }

    @Override
    public GeoServerFacade getFacade() {
        return facade;
    }

    public void setFacade(GeoServerFacade facade) {
        this.facade = facade;
        facade.setGeoServer(this);
    }

    @Override
    public GeoServerFactory getFactory() {
        return factory;
    }

    @Override
    public void setFactory(GeoServerFactory factory) {
        this.factory = factory;
    }

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;

        // This instance of check is has to be here because this Geoserver cannot be injected
        // into LocalWorkspaceCatalog because it causes a circular reference
        if (catalog instanceof LocalWorkspaceCatalog) {
            LocalWorkspaceCatalog lwCatalog = (LocalWorkspaceCatalog) catalog;
            lwCatalog.setGeoServer(this);
        }
    }

    @Override
    public GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);

        // fire the modification event
        fireGlobalPostModified();
    }

    @Override
    public SettingsInfo getSettings() {
        SettingsInfo settings = null;
        if (LocalWorkspace.get() != null) {
            settings = getSettings(LocalWorkspace.get());
        }
        return settings != null ? settings : getGlobal().getSettings();
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        return facade.getSettings(workspace);
    }

    @Override
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

    @Override
    public void save(SettingsInfo settings) {
        validate(settings);

        facade.save(settings);
        fireSettingsPostModified(settings);
    }

    @Override
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

    @Override
    public void fireSettingsModified(
            SettingsInfo settings,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {
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

    @Override
    public LoggingInfo getLogging() {
        return facade.getLogging();
    }

    @Override
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

    @Override
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

    @Override
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        WorkspaceInfo ws = LocalWorkspace.get();
        T service = ws != null ? facade.getService(ws, clazz) : null;
        service = service != null ? service : facade.getService(clazz);
        if (service == null) {
            if (ws != null) {
                LOGGER.log(
                        Level.CONFIG,
                        "Could not locate service info configuration of type "
                                + clazz
                                + ", for local workspace "
                                + ws.getName());
            } else {
                LOGGER.log(
                        Level.CONFIG,
                        "Could not locate service info configuration of type "
                                + clazz
                                + ", for global service");
            }
        }

        return service;
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getService(workspace, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return facade.getService(id, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        T service =
                LocalWorkspace.get() != null
                        ? facade.getServiceByName(name, LocalWorkspace.get(), clazz)
                        : null;
        return service != null ? service : facade.getServiceByName(name, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getServiceByName(name, workspace, clazz);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices() {
        Collection<? extends ServiceInfo> services =
                LocalWorkspace.get() != null ? facade.getServices(LocalWorkspace.get()) : null;
        return services != null ? services : facade.getServices();
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return facade.getServices(workspace);
    }

    @Override
    public void remove(ServiceInfo service) {
        facade.remove(service);

        fireServiceRemoved(service);
    }

    @Override
    public void save(GeoServerInfo geoServer) {
        facade.save(geoServer);

        // fire post modification event
        fireGlobalPostModified();
    }

    @Override
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

    @Override
    public void fireGlobalModified(
            GeoServerInfo global,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {

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

    @Override
    public void fireLoggingModified(
            LoggingInfo logging,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {

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

    @Override
    public void save(ServiceInfo service) {
        validate(service);

        facade.save(service);

        // fire post modification event
        firePostServiceModified(service);
    }

    void validate(ServiceInfo service) {
        CatalogImpl.validateKeywords(service.getKeywords());
    }

    @Override
    public void fireServiceModified(
            ServiceInfo service,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {

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

    @Override
    public void addListener(ConfigurationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigurationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<ConfigurationListener> getListeners() {
        return listeners;
    }

    @Override
    public void dispose() {
        try {
            callLifecycleHandlers(GeoServerLifecycleHandler::onDispose, "onDispose");
        } finally {
            // internal cleanup
            if (catalog != null) catalog.dispose();
            if (facade != null) facade.dispose();
        }
    }

    @Override
    public void reload() throws Exception {
        this.reload(null);
    }

    @Override
    public void reload(Catalog newCatalog) throws Exception {
        // notify start of reload
        callLifecycleHandlers(GeoServerLifecycleHandler::beforeReload, "beforeReload");

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
            callLifecycleHandlers(GeoServerLifecycleHandler::onReload, "onReload");
        }
    }

    @Override
    public void reset() {
        // drop all the catalog store/feature types/raster caches
        catalog.getResourcePool().dispose();

        // reset the referencing subsystem
        CRS.reset("all");

        // look for pluggable handlers
        callLifecycleHandlers(GeoServerLifecycleHandler::onReset, "onReset");
    }

    /**
     * Lookup current GeoServerLifecycleHandler and perform a callback on them.
     *
     * @param callback to perform.
     * @param name of callback to generate a possible error message.
     */
    private static void callLifecycleHandlers(
            Consumer<GeoServerLifecycleHandler> callback, String name) {
        List<GeoServerLifecycleHandler> handlers =
                GeoServerExtensions.extensions(GeoServerLifecycleHandler.class);
        for (GeoServerLifecycleHandler handler : handlers) {
            try {
                callback.accept(handler);
            } catch (Throwable t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during " + name,
                        t);
            }
        }
    }
}
