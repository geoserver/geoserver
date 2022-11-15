/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

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
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WebFeatureService;
import org.geoserver.wfs.WebFeatureService20;
import org.geotools.util.logging.Logging;

/** Provide description of WMS services for welcome page. */
public class WFSServiceDescriptionProvider extends ServiceDescriptionProvider {

    static final Logger LOGGER = Logging.getLogger(WFSServiceDescriptionProvider.class);

    /** Service type to cross-link between service description and service link description. */
    public static final String SERVICE_TYPE = "WFS";

    GeoServer geoserver;
    Catalog catalog;

    public WFSServiceDescriptionProvider(GeoServer gs) {
        this.geoserver = gs;
        catalog = gs.getCatalog();
    }

    /**
     * Lookup WFSInfo using workspaceInfo / layerInfo context.
     *
     * @param workspaceInfo Workspace, or null for global.
     * @param layerInfo Layer, LayerGroup, or null for any
     * @return WFSInfo if available for workspace, or global WFSInfo.
     */
    protected WFSInfo info(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        WFSInfo info = null;
        if (workspaceInfo != null) {
            info = geoserver.getService(workspaceInfo, WFSInfo.class);
        }
        if (info == null) {
            info = geoserver.getService(WFSInfo.class);
        }
        return info;
    }

    @Override
    public List<ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServiceDescription> descriptions = new ArrayList<>();

        WFSInfo info = info(workspaceInfo, layerInfo);
        if (workspaceInfo != null || geoserver.getGlobal().isGlobalServices()) {
            descriptions.add(description(SERVICE_TYPE, info, workspaceInfo, layerInfo));
        }
        return descriptions;
    }

    @Override
    protected String getVersionParameterName(Service service) {
        if (service.getVersion().getMajor().toString().equals("2")) {
            return "acceptversions";
        }
        return "version";
    }

    @Override
    public List<ServiceLinkDescription> getServiceLinks(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServiceLinkDescription> links = new ArrayList<>();

        if (workspaceInfo == null && !geoserver.getGlobal().isGlobalServices()) {
            return links;
        }

        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if ((service.getService() instanceof WebFeatureService20)
                    || (service.getService() instanceof WebFeatureService)) {
                String link = null;
                if (service.getOperations().contains("GetCapabilities")) {
                    link = getCapabilitiesURL(workspaceInfo, layerInfo, service);
                } else if (service.getCustomCapabilitiesLink() != null) {
                    link = service.getCustomCapabilitiesLink();
                }

                if (link != null) {
                    links.add(
                            new ServiceLinkDescription(
                                    SERVICE_TYPE,
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
