/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */ package org.geoserver.wcs.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSResourceVoter;
import org.geoserver.wcs.WebCoverageService100;
import org.geoserver.wcs.WebCoverageService111;
import org.geoserver.wcs2_0.WebCoverageService20;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.util.logging.Logging;

/** Provide description of WMS services for welcome page. */
public class WCSServiceDescriptionProvider extends ServiceDescriptionProvider {

    static final Logger LOGGER = Logging.getLogger(WCSServiceDescriptionProvider.class);

    /** Service type to cross-link between service description and service link description. */
    public static final String SERVICE_TYPE = "WCS";

    GeoServer geoserver;
    Catalog catalog;

    public WCSServiceDescriptionProvider(GeoServer gs) {
        this.geoserver = gs;
        catalog = gs.getCatalog();
    }

    /**
     * Lookup WCSInfo using workspaceInfo / layerInfo context.
     *
     * @param workspaceInfo Workspace, or null for global.
     * @param layerInfo Layer, LayerGroup, or null for any
     * @return WCSInfo if available for workspace, or global WCSInfo.
     */
    protected WCSInfo info(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        WCSInfo info = null;
        if (workspaceInfo != null) {
            info = geoserver.getService(workspaceInfo, WCSInfo.class);
        }
        if (info == null) {
            info = geoserver.getService(WCSInfo.class);
        }
        return info;
    }

    @Override
    protected boolean isAvailable(String serviceType, ServiceInfo serviceInfo, PublishedInfo layerInfo) {
        if (layerInfo != null && layerInfo instanceof LayerInfo) {
            WCSResourceVoter voter = new WCSResourceVoter();
            if (voter.hideService(serviceType, ((LayerInfo) layerInfo).getResource())) {
                return false;
            }
        }
        return super.isAvailable(serviceType, serviceInfo, layerInfo);
    }

    @Override
    public List<ServiceDescription> getServices(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceDescription> descriptions = new ArrayList<>();
        WCSInfo info = info(workspaceInfo, layerInfo);

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
    public List<ServiceLinkDescription> getServiceLinks(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        List<ServiceLinkDescription> links = new ArrayList<>();

        if (workspaceInfo == null && !geoserver.getGlobal().isGlobalServices()) {
            return links;
        }

        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if ((service.getService() instanceof WebCoverageService111)
                    || (service.getService() instanceof WebCoverageService100)
                    || (service.getService() instanceof WebCoverageService20)) {

                String link = null;
                if (service.getOperations().contains("GetCapabilities")) {
                    link = getCapabilitiesURL(workspaceInfo, layerInfo, service);
                } else if (service.getCustomCapabilitiesLink() != null) {
                    link = service.getCustomCapabilitiesLink();
                }

                if (link != null) {
                    links.add(new ServiceLinkDescription(
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
