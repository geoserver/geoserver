/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WebMapService;
import org.geotools.util.logging.Logging;

/** Provide description of WMS services for welcome page. */
public class WMSServiceDescriptionProvider extends ServiceDescriptionProvider {

    static final Logger LOGGER = Logging.getLogger(WMSServiceDescriptionProvider.class);

    /** Service type to cross-link between service description and service link description. */
    public static final String SERVICE_TYPE = "WMS";

    GeoServer geoserver;
    Catalog catalog;

    public WMSServiceDescriptionProvider(GeoServer gs) {
        super(SERVICE_TYPE);
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
    public List<ServiceDescription> getServices(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceDescription> descriptions = new ArrayList<>();
        WMSInfo info = info(workspaceInfo, layerInfo);

        if (workspaceInfo != null || geoserver.getGlobal().isGlobalServices()) {
            descriptions.add(description(serviceType, info, workspaceInfo, layerInfo));
        }
        return descriptions;
    }

    @Override
    public List<ServiceLinkDescription> getServiceLinks(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServiceLinkDescription> links = new ArrayList<>();

        if (workspaceInfo == null && !geoserver.getGlobal().isGlobalServices()) {
            return links;
        }
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if (service.getService() instanceof WebMapService) {
                String link = null;
                if (service.getOperations().contains("GetCapabilities")) {
                    link = getCapabilitiesURL(workspaceInfo, layerInfo, service);
                } else if (service.getCustomCapabilitiesLink() != null) {
                    link = service.getCustomCapabilitiesLink();
                }

                if (link != null) {
                    links.add(new ServiceLinkDescription(
                            serviceType,
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
