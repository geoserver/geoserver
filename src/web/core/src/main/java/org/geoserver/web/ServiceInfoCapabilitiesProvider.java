/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.Component;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo;
import org.geotools.text.Text;

/**
 * Contributes standard GetCapabilities links for all the versions in all the {@link ServiceInfo}
 * implementations available to the {@link GeoServerApplication} using the GeoServer's {@code /ows}
 * standard OpenGIS Web Service entry point as the root of the GetCapabilities request.
 *
 * @author Gabriel Roldan
 * @see CapabilitiesHomePagePanel
 */
public class ServiceInfoCapabilitiesProvider implements CapabilitiesHomePageLinkProvider {
    /*
    @Override
    public List<ServicesPanel.ServiceDescription> getServices(String workspace, String layer)  {
        List<ServicesPanel.ServiceDescription> descriptions = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            String serviceId = service.getId();
            String namespace = service.getNamespace();

            descriptions.add(
                new ServicesPanel.ServiceDescription(
                    serviceId.toLowerCase(),
                    Text.text(serviceId.toUpperCase()),
                    Text.text(namespace)
                )
            );
        }
        return descriptions;
    }

    public List<ServicesPanel.ServiceLinkDescription> getServiceLinks(
            String workspace, String layer) {
        List<ServicesPanel.ServiceLinkDescription> links = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            String serviceId = service.getId();
            String namespace = service.getNamespace();
            String link = null;
            if (service.getOperations().contains("GetCapabilities")) {
                if (namespace != null){
                    link = "../"
                            + namespace
                            + "/ows?service="
                            + serviceId
                            + "&version="
                            + service.getVersion().toString()
                            + "&request=GetCapabilities";
                }
                else {
                    link = "../ows?service="
                            + serviceId
                            + "&version="
                            + service.getVersion().toString()
                            + "&request=GetCapabilities";
                }
            }
            else if (service.getCustomCapabilitiesLink() != null) {
                link = service.getCustomCapabilitiesLink();
            }

            if( link != null ){
                links.add(
                    new ServicesPanel.ServiceLinkDescription(
                        serviceId.toLowerCase(),
                        service.getVersion(),
                        link,
                        namespace)
                );
            }
        }
        return links;
    }
    */

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
