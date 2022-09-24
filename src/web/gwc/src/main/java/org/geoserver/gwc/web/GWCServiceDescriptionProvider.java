/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.util.Version;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/** Provide description of GeoWebCache services for welcome page. */
public class GWCServiceDescriptionProvider extends ServiceDescriptionProvider {

    private final GWC gwc;
    GeoServer geoserver;
    Catalog catalog;

    public GWCServiceDescriptionProvider(GWC gwc, GeoServer gs) {
        this.gwc = gwc;
        this.geoserver = gs;
        catalog = gs.getCatalog();
    }

    /**
     * Lookup WMTSInfo using workspaceInfo / layerInfo context.
     *
     * @param workspaceInfo Workspace, or null for global.
     * @param layerInfo Layer, LayerGroup, or null for any
     * @return WMTSInfo if available for workspace, or global WMTSInfo.
     */
    protected WMTSInfo info(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        WMTSInfo info = null;
        if (workspaceInfo != null) {
            info = geoserver.getService(workspaceInfo, WMTSInfo.class);
        }
        if (info == null) {
            info = geoserver.getService(WMTSInfo.class);
        }
        return info;
    }

    @Override
    public List<ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceDescription> descriptions = new ArrayList<>();
        WMTSInfo info = info(workspaceInfo, layerInfo);

        if (workspaceInfo != null || geoserver.getGlobal().isGlobalServices()) {
            descriptions.add(description(info, workspaceInfo, layerInfo));
        }
        return descriptions;
    }

    @Override
    public List<ServiceLinkDescription> getServiceLinks(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceLinkDescription> links = new ArrayList<>();

        if (workspaceInfo == null && !geoserver.getGlobal().isGlobalServices()) {
            return links;
        }

        WMTSInfo info = info(workspaceInfo, layerInfo);

        final GeoServerApplication app = GeoServerApplication.get();
        final GWCConfig gwcConfig = gwc.getConfig();

        try {
            if (gwcConfig.isWMSCEnabled() && null != app.getBean("gwcServiceWMS")) {
                links.add(
                        new ServiceLinkDescription(
                                "wmts",
                                new Version("1.1.1"),
                                "../gwc/service/wms?request=GetCapabilities&version=1.1.1&tiled=true",
                                workspaceInfo != null ? workspaceInfo.getName() : null,
                                layerInfo != null ? layerInfo.getName() : null,
                                "WMS-C"));
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }

        try {
            if (info.isEnabled() && null != app.getBean("gwcServiceWMTS")) {
                links.add(
                        new ServiceLinkDescription(
                                "wmts",
                                new Version("1.1.1"),
                                "../gwc/service/wmts?services=WMTS&version=1.1.1&request=GetCapabilities",
                                workspaceInfo != null ? workspaceInfo.getName() : null,
                                layerInfo != null ? layerInfo.getName() : null,
                                "WMTS"));
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }
        try {
            if (gwcConfig.isTMSEnabled() && null != app.getBean("gwcServiceTMS")) {
                links.add(
                        new ServiceLinkDescription(
                                "wmts",
                                new Version("1.0.0"),
                                "../gwc/service/tms/1.0.0",
                                workspaceInfo != null ? workspaceInfo.getName() : null,
                                layerInfo != null ? layerInfo.getName() : null,
                                "TMS"));
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }
        return links;
    }
}
