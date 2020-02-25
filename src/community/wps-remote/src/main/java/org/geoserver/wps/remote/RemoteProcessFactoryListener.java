/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import org.opengis.feature.type.Name;

/**
 * Interface allowing a {@link RemoteProcessFactory} instance to listen to the {@link
 * RemoteProcessClient} messages.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface RemoteProcessFactoryListener {

    /**
     * Registers a new {@link RemoteProcess} upon a new remote service presentation; from now on a
     * new WPS Process will be available
     */
    public void registerProcess(RemoteServiceDescriptor serviceDescriptor);

    /**
     * De-registers a {@link RemoteProcess} upon a {@link RemoteProcessClient} request; the WPS
     * Process won't be available anymore
     */
    public void deregisterProcess(Name name);
}
