package org.geoserver.config;

import java.util.Collection;

/**
 * Data access facade for geoserver configuration.
 *
 * @author ETj <etj at geo-solutions.it>
 * @author Justin Deoliveira, OpenGeo
 * 
 */
public interface GeoServerFacade {

    GeoServer getGeoServer();
    
    void setGeoServer(GeoServer geoServer);
    
    /**
     * The global geoserver configuration.
     */
    GeoServerInfo getGlobal();

    /**
     * Sets the global configuration.
     */
    void setGlobal( GeoServerInfo global );
    
    /**
     * Saves the global geoserver configuration after modification.
     */
    void save(GeoServerInfo geoServer);

    /**
     * The logging configuration.
     */
    LoggingInfo getLogging();
    
    /**
     * Sets logging configuration.
     */
    void setLogging( LoggingInfo logging );
    
    /**
     * Saves the logging configuration.
     */
    void save(LoggingInfo logging);
    
    /**
     * Adds a service to the configuration.
     */
    void add(ServiceInfo service);

    /**
     * Removes a service from the configuration.
     */
    void remove(ServiceInfo service);

    /**
     * Saves a service that has been modified.
     */
    void save(ServiceInfo service);

    /**
     * GeoServer services.
     * 
     */
    Collection<? extends ServiceInfo> getServices();

    /**
     * GeoServer services filtered by class.
     * 
     * @param clazz
     *                The class of the services to return.
     */
    <T extends ServiceInfo> T getService(Class<T> clazz);

    /**
     * Looks up a service by id.
     * 
     * @param id
     *                The id of the service.
     * @param clazz The type of the service.
     * 
     * @return The service with the specified id, or <code>null</code> if no
     *         such service coud be found.
     */
    <T extends ServiceInfo> T getService(String id, Class<T> clazz);

    /**
     * Looks up a service by name.
     * 
     * @param name The name of the service.
     * @param clazz The type of the service.
     * 
     * @return The service with the specified name or <code>null</code> if no
     *         such service could be found.
     */
    <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz );
    
    /**
     * Disposes the configuration. 
     */
    void dispose();
}
