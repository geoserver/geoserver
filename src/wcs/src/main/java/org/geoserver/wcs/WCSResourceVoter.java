/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class WCSResourceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(ResourceInfo resource) {
        if (resource instanceof FeatureTypeInfo) {
            return true;
        }
        return false;
    }

    @Override
    public String serviceName() {
        return "WCS";
    }
}
