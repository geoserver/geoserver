/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.util.Version;

/** Provide description of WMS services for welcome page. */
public class WCSServiceDescriptionProvider extends ServiceDescriptionProvider {

    /** Service type to cross-link between service description and service link description. */
    private static final String SERVICE_TYPE = "WCS";

    private static final List<Version> SUPPORTED_VERSIONS =
            Arrays.asList(new Version("1.0.0"), new Version("1.1.1"), new Version("2.0.1"));

    GeoServer geoserver;
    Catalog catalog;

    public WCSServiceDescriptionProvider(GeoServer gs) {
        super(SERVICE_TYPE);
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
        if (layerInfo != null && layerInfo instanceof LayerInfo info) {
            WCSResourceVoter voter = new WCSResourceVoter();
            if (voter.hideService(serviceType, info.getResource())) {
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
            descriptions.add(description(serviceType, info, workspaceInfo, layerInfo));
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

        WCSInfo info = info(workspaceInfo, layerInfo);
        List<Version> disabledVersions = (info != null) ? info.getDisabledVersions() : null;

        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if (SERVICE_TYPE.equalsIgnoreCase(service.getId()) && SUPPORTED_VERSIONS.contains(service.getVersion())) {
                if (disabledVersions != null && disabledVersions.contains(service.getVersion())) {
                    continue;
                }

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
