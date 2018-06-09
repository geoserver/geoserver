/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

/**
 * Extension point for loading and saving the configuration of a service.
 *
 * <p>Instances of this class are registered in a spring context:
 *
 * <pre>
 * &lt;bean id="org.geoserver.wfs.WFSLoader"/>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ServiceLoader<T extends ServiceInfo> {

    /**
     * The id of the service.
     *
     * <p>Examples: "wfs","wms","wcs",etc...
     */
    // String getServiceId();
    Class<T> getServiceClass();

    /**
     * Loads the service.
     *
     * @param gs The GeoServer configuartion.
     * @throws Exception Any errors that occur while loading the service.
     */
    T load(GeoServer gs) throws Exception;

    /**
     * Saves the service.
     *
     * @param service The serfvice.
     * @param gs The GeoServer configuration.
     * @throws Exception Any errors that occur while saving the service.
     */
    void save(T service, GeoServer gs) throws Exception;

    /**
     * Creates a new service from scratch.
     *
     * @param gs The GeoServer configuration.
     * @throws Exception Any errors that occur while saving the service.
     */
    T create(GeoServer gs) throws Exception;
}
