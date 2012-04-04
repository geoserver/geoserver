/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

/**
 * Base class for named security service configuration objects.
 * 
 * @author christian
 */
public interface SecurityNamedServiceConfig extends SecurityConfig {

    /**
     * Internal id of the config object. 
     * <p>
     * This method should be used by client code.
     * </p>
     */
    String getId();

    /**
     * Sets internal id of the config object.
     */
    void setId(String newId);

    /**
     * The name of the service.
     */
    String getName();

    /**
     * Sets the name for a service.
     */
    void setName(String name);

    /**
     * Name of class for implementation of the service.
     */
    String getClassName();

    /**
     * Sets name of class for implementation of the service.
     */
    void setClassName(String className);

    /**
     * Method for the config object to initialize any properties before being saved for the first
     * time.
     * <p>
     * This method would typically be used to initialize properties not explicitly set by the 
     * user, but that can be set based on other user initialized properties.
     * </p>
     */
    void initBeforeSave();
}
