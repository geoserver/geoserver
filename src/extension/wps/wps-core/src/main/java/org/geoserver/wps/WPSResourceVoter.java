/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class WPSResourceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(ResourceInfo resource) {
        return true;
    }

    @Override
    public String serviceName() {
        return "WPS";
    }
}
