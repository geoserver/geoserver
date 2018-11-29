/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class WCSResourceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(String service, ResourceInfo resource) {
        if (!"WCS".equalsIgnoreCase(service)) return false;
        return !(resource instanceof CoverageInfo);
    }
}
