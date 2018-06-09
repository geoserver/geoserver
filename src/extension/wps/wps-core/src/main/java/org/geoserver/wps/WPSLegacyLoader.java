/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geotools.util.Version;

public class WPSLegacyLoader extends LegacyServiceLoader<WPSInfo> {

    public Class<WPSInfo> getServiceClass() {
        return WPSInfo.class;
    }

    public WPSInfo load(LegacyServicesReader reader, GeoServer geoServer) throws Exception {

        WPSInfoImpl wps = new WPSInfoImpl();
        wps.setId("wps");
        wps.setGeoServer(geoServer);
        wps.getVersions().add(new Version("1.0.0"));

        return wps;
    }
}
