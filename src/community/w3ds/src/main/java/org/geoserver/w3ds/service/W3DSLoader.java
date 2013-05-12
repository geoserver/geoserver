/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;

public class W3DSLoader extends LegacyServiceLoader<ServiceInfo> {

    public Class<ServiceInfo> getServiceClass() {
        return ServiceInfo.class;
    }
    
    public ServiceInfo load(LegacyServicesReader reader, GeoServer geoServer)
            throws Exception {
        W3DSInfoImpl w3ds = new W3DSInfoImpl();
        w3ds.setEnabled(true);
        w3ds.setName("w3ds");
        w3ds.setTitle("Web 3D Service");
        List<String> versions = new ArrayList<String>();
        versions.add("0.0.4");
        w3ds.setVersions(versions);
        return w3ds;
    }

}
