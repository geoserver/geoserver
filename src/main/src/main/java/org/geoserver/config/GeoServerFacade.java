/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Collection;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Data access facade for geoserver configuration.
 *
 * @author ETj <etj at geo-solutions.it>
 * @author Justin Deoliveira, OpenGeo
 */
public interface GeoServerFacade {

    GeoServer getGeoServer();

    void setGeoServer(GeoServer geoServer);

    /** The global geoserver configuration. */
    GeoServerInfo getGlobal();

    /** Sets the global configuration. */
    void setGlobal(GeoServerInfo global);

    /** Saves the global geoserver configuration after modification. */
    void save(GeoServerInfo geoServer);

    /**
     * The settings configuration for the specified workspoace, or <code>null</code> if non exists.
     */
    SettingsInfo getSettings(WorkspaceInfo workspace);

    /** Adds a settings configuration for the specified workspace. */
    void add(SettingsInfo settings);

    /** Saves the settings configuration for the specified workspace. */
    void save(SettingsInfo settings);

    /** Removes the settings configuration for the specified workspace. */
    void remove(SettingsInfo settings);

    /** The logging configuration. */
    LoggingInfo getLogging();

    /** Sets logging configuration. */
    void setLogging(LoggingInfo logging);

    /** Saves the logging configuration. */
    void save(LoggingInfo logging);

    /** Adds a service to the configuration. */
    void add(ServiceInfo service);

    /** Removes a service from the configuration. */
    void remove(ServiceInfo service);

    /** Saves a service that has been modified. */
    void save(ServiceInfo service);

    /** GeoServer services. */
    Collection<? extends ServiceInfo> getServices();

    /** GeoServer services specific to the specified workspace. */
    Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace);

    /**
     * GeoServer global service filtered by class.
     *
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> T getService(Class<T> clazz);

    /**
     * GeoServer service specific to the specified workspace and filtered by class.
     *
     * @param workspace The workspace the service is specific to.
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz);

    /**
     * Looks up a service by id.
     *
     * @param id The id of the service.
     * @param clazz The type of the service.
     * @return The service with the specified id, or <code>null</code> if no such service coud be
     *     found.
     */
    <T extends ServiceInfo> T getService(String id, Class<T> clazz);

    /**
     * Looks up a service by name.
     *
     * @param name The name of the service.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz);

    /**
     * Looks up a service by name, specific to the specified workspace.
     *
     * @param name The name of the service.
     * @param workspace The workspace the service is specific to.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz);

    /** Disposes the configuration. */
    void dispose();
}
