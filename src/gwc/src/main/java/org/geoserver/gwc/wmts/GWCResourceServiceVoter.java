/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;
import org.geoserver.gwc.GWC;

/** Hide GWC services if a tile layer is not defined. */
public class GWCResourceServiceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(String serviceType, ResourceInfo resource) {
        // WMTS service request hide service from list if there is no tile layer
        if (!"WMTS".equalsIgnoreCase(serviceType)) return false;

        return !GWC.get().hasTileLayer(resource);
    }
}
