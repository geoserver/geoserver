/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

/**
 * Base class for named security service configuration objects.
 *
 * <p>In general, developers seeking to implement this interface should start from {@link
 * BaseSecurityNamedServiceConfig } which provides valid default implementations for all methods in
 * this interface.
 *
 * @author christian
 */
public interface SecurityNamedServiceConfig extends SecurityConfig {

    /**
     * Internal id of the config object.
     *
     * <p>This method is generally not useful to client code; the ID property is used within the
     * configuration persistence system.
     */
    String getId();

    /**
     * Sets internal id of the config object.
     *
     * <p>This method is generally not useful to client code; the ID property is used within the
     * configuration persistence system.
     */
    void setId(String newId);

    /** The name of the service. */
    String getName();

    /** Sets the name for a service. */
    void setName(String name);

    /** Name of class for implementation of the service. */
    String getClassName();

    /** Sets name of class for implementation of the service. */
    void setClassName(String className);

    /**
     * Method for the config object to initialize any properties before being saved for the first
     * time.
     *
     * <p>This method would typically be used to initialize properties not explicitly set by the
     * user, but that can be set based on other user initialized properties.
     */
    void initBeforeSave();
}
