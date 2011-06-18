/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.geoserver.config.ServiceInfo;
import org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo;
import org.geotools.util.Version;

/**
 * Contributes standard GetCapabilities links for all the versions in all the {@link ServiceInfo}
 * implementations available to the {@link GeoServerApplication} using the GeoServer's {@code /ows}
 * standard OpenGIS Web Service entry point as the root of the GetCapabilities request.
 * 
 * @author Gabriel Roldan
 * @see CapabilitiesHomePagePanel
 */
public class ServiceInfoCapabilitiesProvider implements CapabilitiesHomePageLinkProvider {

    /**
     * 
     * @see org.geoserver.web.CapabilitiesHomePageLinkProvider#getCapabilitiesComponent
     */
    public Component getCapabilitiesComponent(final String id) {

        List<CapsInfo> serviceInfoLinks = new ArrayList<CapabilitiesHomePagePanel.CapsInfo>();

        GeoServerApplication app = GeoServerApplication.get();
        for (ServiceInfo si : app.getGeoServer().getServices()) {
            for (Version v : si.getVersions()) {
                String serviceId = si.getId();
                String capsLink = "../ows?service=" + serviceId + "&version=" + v.toString()
                        + "&request=GetCapabilities";
                CapsInfo ci = new CapsInfo(serviceId, v, capsLink);
                serviceInfoLinks.add(ci);
            }
        }
        return new CapabilitiesHomePagePanel(id, serviceInfoLinks);

    }
}
