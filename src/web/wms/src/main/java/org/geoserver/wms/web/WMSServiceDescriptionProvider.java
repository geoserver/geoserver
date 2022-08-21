/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.util.InternationalStringUtils;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServicesPanel;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WebMapService;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/** Provide description of WMS services for welcome page. */
public class WMSServiceDescriptionProvider extends ServiceDescriptionProvider {

    static final Logger LOGGER = Logging.getLogger(WMSServiceDescriptionProvider.class);

    GeoServer geoserver;
    Catalog catalog;

    public WMSServiceDescriptionProvider(GeoServer gs) {
        this.geoserver = gs;
        catalog = gs.getCatalog();
    }

    /**
     * Lookup WMSInfo using workspaceInfo / layerInfo context.
     *
     * @param workspaceInfo Workspace, or null for global.
     * @param layerInfo Layer, LayerGroup, or null for any
     * @return WMSInfo if available for workspace, or global WMSInfo.
     */
    protected WMSInfo info(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        WMSInfo info = null;
        if (workspaceInfo != null) {
            info = geoserver.getService(workspaceInfo, WMSInfo.class);
        }
        if (info == null) {
            info = geoserver.getService(WMSInfo.class);
        }
        return info;
    }

    @Override
    public List<ServicesPanel.ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServicesPanel.ServiceDescription> descriptions = new ArrayList<>();
        WMSInfo info = info(workspaceInfo, layerInfo);

        String serviceId = "wms";
        boolean available = info.isEnabled();
        if (layerInfo instanceof LayerInfo) {
            ServiceResourceProvider provider =
                    GeoServerExtensions.bean(ServiceResourceProvider.class);
            List<String> layerServices =
                    provider.getServicesForResource(((LayerInfo) layerInfo).getResource());
            available = layerServices.contains(serviceId.toUpperCase());
        }

        InternationalString title =
                InternationalStringUtils.growable(
                        info.getInternationalTitle(),
                        Strings.isEmpty(info.getTitle())
                                ? serviceId.toUpperCase()
                                : info.getTitle());
        InternationalString description =
                InternationalStringUtils.growable(
                        info.getInternationalAbstract(),
                        Strings.isEmpty(info.getAbstract()) ? null : info.getAbstract());

        ServicesPanel.ServiceDescription serviceDescription =
                new ServicesPanel.ServiceDescription(
                        serviceId.toLowerCase(),
                        title,
                        description,
                        available,
                        workspaceInfo != null ? workspaceInfo.getName() : null,
                        layerInfo != null ? layerInfo.getName() : null);

        descriptions.add(serviceDescription);
        return descriptions;
    }

    @Override
    public List<ServicesPanel.ServiceLinkDescription> getServiceLinks(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServicesPanel.ServiceLinkDescription> links = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if (service.getService() instanceof WebMapService) {
                String serviceId = service.getId();

                String link = null;
                if (service.getOperations().contains("GetCapabilities")) {
                    link = getCapabilitiesURL(workspaceInfo, layerInfo, service);
                } else if (service.getCustomCapabilitiesLink() != null) {
                    link = service.getCustomCapabilitiesLink();
                }

                if (link != null) {
                    links.add(
                            new ServicesPanel.ServiceLinkDescription(
                                    serviceId.toLowerCase(),
                                    service.getVersion(),
                                    link,
                                    workspaceInfo != null ? workspaceInfo.getName() : null,
                                    layerInfo != null ? layerInfo.getName() : null));
                }
            }
        }
        return links;
    }
}
