/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

/**
 * Base class for extensions of {@link GeoServerFactory} for creating {@link ServiceInfo} objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class ServiceFactoryExtension<T extends ServiceInfo>
        implements GeoServerFactory.Extension {

    Class<T> serviceClass;

    protected ServiceFactoryExtension(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public <T> boolean canCreate(Class<T> clazz) {
        return serviceClass.equals(clazz);
    }
}
