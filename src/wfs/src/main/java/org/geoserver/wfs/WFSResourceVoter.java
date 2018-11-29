/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceVoter;

public class WFSResourceVoter implements ServiceResourceVoter {

    @Override
    public boolean hideService(String service, ResourceInfo resource) {
        if (service == null || !service.equalsIgnoreCase("WFS")) return false;
        return !(resource instanceof FeatureTypeInfo);
    }
}
