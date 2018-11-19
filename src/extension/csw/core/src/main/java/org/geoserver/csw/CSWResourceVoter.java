/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class CSWResourceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(ResourceInfo resource) {
        return false;
    }

    @Override
    public String serviceName() {
        return "CSW";
    }
}
