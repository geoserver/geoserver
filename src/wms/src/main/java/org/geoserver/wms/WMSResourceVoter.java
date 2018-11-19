/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class WMSResourceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(ResourceInfo resource) {
        return false;
    }

    @Override
    public String serviceName() {
        return "WMS";
    }
}
