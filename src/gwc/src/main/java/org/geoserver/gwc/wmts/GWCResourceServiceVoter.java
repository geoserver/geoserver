package org.geoserver.gwc.wmts;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class GWCResourceServiceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(String service, ResourceInfo resource) {
        // on WMTS service request hide service from list, because it isn't useful for Service
        // disable GUI
        if ("WMTS".equalsIgnoreCase(service)) {
            return true;
        }
        return false;
    }
}
