/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geotools.util.Version;

public class CSWLegacyLoader extends LegacyServiceLoader<CSWInfo> {

    public Class<CSWInfo> getServiceClass() {
        return CSWInfo.class;
    }
    
    public CSWInfo load(LegacyServicesReader reader, GeoServer geoServer)
            throws Exception {
        
        CSWInfoImpl wps = new CSWInfoImpl();
        wps.setId("csw");
        wps.setGeoServer(geoServer);
        wps.getVersions().add( new Version( "2.0.2") );
        
        return wps;
    }

}

