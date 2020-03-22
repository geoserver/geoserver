/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class GSRServiceXStreamLoader extends XStreamServiceLoader<GSRServiceInfo> {

    public GSRServiceXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "gsr");
    }

    @Override
    protected GSRServiceInfo createServiceFromScratch(GeoServer gs) {
        GSRServiceInfoImpl info = new GSRServiceInfoImpl();
        info.setName("GSR");
        info.setTitle("GeoServices REST API");
        return info;
    }

    @Override
    public Class<GSRServiceInfo> getServiceClass() {
        return GSRServiceInfo.class;
    }
}
