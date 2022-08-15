/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import org.apache.wicket.util.string.Strings;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.util.InternationalStringUtils;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServicesPanel;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WebMapService;
import org.opengis.util.InternationalString;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide description of WMS services for welcome page.
 */
public class WMSServiceDescriptionProvider implements ServiceDescriptionProvider {

    public List<ServicesPanel.ServiceDescription> getServices(String workspace, String layer){
        List<ServicesPanel.ServiceDescription> descriptions = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if(service.getService() instanceof WebMapService){
                WebMapService wms = (WebMapService) service.getService();
                WMSInfo info = wms.getServiceInfo();

                String serviceId = service.getId();
                boolean available = info.isEnabled();

                InternationalString title = InternationalStringUtils.growable(
                    info.getInternationalTitle(),
                    Strings.isEmpty(info.getTitle()) ? serviceId.toUpperCase() : info.getTitle()
                );
                InternationalString description = InternationalStringUtils.growable(
                    info.getInternationalAbstract(),
                    Strings.isEmpty(info.getAbstract()) ? null : info.getAbstract()
                );
                if (workspace == null) {
                    ServicesPanel.ServiceDescription serviceDescription =  new ServicesPanel.ServiceDescription(
                            serviceId.toLowerCase(),
                            title,
                            description,
                            available,
                            null,
                            null);
                    if (!descriptions.contains(serviceDescription)) {
                        descriptions.add(serviceDescription);
                    }
                }
            }
        }
        return descriptions;
    }

    public List<ServicesPanel.ServiceLinkDescription> getServiceLinks(String workspace, String layer){
        List<ServicesPanel.ServiceLinkDescription> links = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if(service.getService() instanceof WebMapService){
                WebMapService wms = (WebMapService) service.getService();
                WMSInfo info = wms.getServiceInfo();

                String serviceId = service.getId();
                String namespace = service.getNamespace();
                boolean available = info.isEnabled();

                String link = null;
                if (service.getOperations().contains("GetCapabilities")) {
                    if (workspace != null){
                        link = "../"
                                + workspace
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
        }
        return links;
    }

}
