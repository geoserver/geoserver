/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.apache.commons.lang3.SerializationUtils;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Base class for named security service configuration objects.
 *
 * @author christian
 */
public class BaseSecurityNamedServiceConfig implements SecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String className;

    public BaseSecurityNamedServiceConfig() {}

    public BaseSecurityNamedServiceConfig(BaseSecurityNamedServiceConfig other) {
        name = other.getName();
        className = other.getClassName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /** @return the name of the service */
    @Override
    public String getName() {
        return name;
    }

    /** sets the name for a service */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /** @return the service class name */
    @Override
    public String getClassName() {
        return className;
    }

    /**
     * The class name of the service to be constructed The class must have a constructor with a
     * string argument, specifying the name of the service
     */
    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    /** Does nothing, subclasses may override. */
    @Override
    public void initBeforeSave() {}

    @Override
    public String toString() {
        return "BaseSecurityNamedServiceConfig [name="
                + name
                + ", className="
                + className
                + ", id="
                + id
                + "]";
    }

    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {

        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);

        BaseSecurityNamedServiceConfig target = SerializationUtils.clone(this);

        if (target != null) {
            if (allowEnvParametrization
                    && gsEnvironment != null
                    && GeoServerEnvironment.allowEnvParametrization()) {
                target.setName((String) gsEnvironment.resolveValue(name));
            }
        }

        return target;
    }
}
