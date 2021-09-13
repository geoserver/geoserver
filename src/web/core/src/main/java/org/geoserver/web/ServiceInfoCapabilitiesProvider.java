/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo;

/**
 * Contributes standard GetCapabilities links for all the versions in all the {@link ServiceInfo}
 * implementations available to the {@link GeoServerApplication} using the GeoServer's {@code /ows}
 * standard OpenGIS Web Service entry point as the root of the GetCapabilities request.
 *
 * @author Gabriel Roldan
 * @see CapabilitiesHomePagePanel
 */
public class ServiceInfoCapabilitiesProvider implements CapabilitiesHomePageLinkProvider {

    /** @see org.geoserver.web.CapabilitiesHomePageLinkProvider#getCapabilitiesComponent */
    @Override
    public Component getCapabilitiesComponent(final String id) {

        List<CapsInfo> serviceInfoLinks = new ArrayList<>();

        List<Service> extensions = GeoServerExtensions.extensions(Service.class);
        for (Service service : extensions) {
            if (service.getCustomCapabilitiesLink() != null) {
                String serviceId = service.getId();
                String capsLink = service.getCustomCapabilitiesLink();
                CapsInfo ci = new CapsInfo(serviceId, service.getVersion(), capsLink);
                serviceInfoLinks.add(ci);
            } else if (service.getOperations().contains("GetCapabilities")) {
                String serviceId = service.getId();
                String capsLink =
                        "../ows?service="
                                + serviceId
                                + "&version="
                                + service.getVersion().toString()
                                + "&request=GetCapabilities";
                CapsInfo ci = new CapsInfo(serviceId, service.getVersion(), capsLink);
                serviceInfoLinks.add(ci);
            }
        }
        return new CapabilitiesHomePagePanel(id, serviceInfoLinks);
    }
}
