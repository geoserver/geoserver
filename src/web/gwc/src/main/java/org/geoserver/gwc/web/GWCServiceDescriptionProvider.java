/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.wmts.GWCResourceServiceVoter;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.util.Version;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/** Provide description of GeoWebCache services for welcome page. */
public class GWCServiceDescriptionProvider extends ServiceDescriptionProvider {

    /** Service type to cross-link between service description and service link description. */
    public static final String SERVICE_TYPE = "WMTS";

    private final GWC gwc;
    GeoServer geoserver;
    Catalog catalog;

    public GWCServiceDescriptionProvider(GWC gwc, GeoServer gs) {
        super(SERVICE_TYPE);
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

    /** GWC-bases services don't have layer-specific enabling... */
    @Override
    protected boolean isAvailable(String serviceType, ServiceInfo serviceInfo, PublishedInfo layerInfo) {
        if (layerInfo != null && layerInfo instanceof LayerInfo info) {
            GWCResourceServiceVoter voter = new GWCResourceServiceVoter();
            if (voter.hideService(serviceType, info.getResource())) {
                return false;
            }
        }
        if (layerInfo != null && !gwc.hasTileLayer(layerInfo)) {
            return false;
        }
        return super.isAvailable(serviceType, serviceInfo, layerInfo);
    }

    @Override
    public List<ServiceDescription> getServices(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceDescription> descriptions = new ArrayList<>();
        WMTSInfo info = info(workspaceInfo, layerInfo);

        if (workspaceInfo != null || geoserver.getGlobal().isGlobalServices()) {
            descriptions.add(description(serviceType, info, workspaceInfo, layerInfo));
        }
        return descriptions;
    }

    private String createLinkWMSC(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        if ((workspaceInfo == null) && (layerInfo == null)) {
            return "../gwc/service/wms?service=WMS&version=1.1.1&request=GetCapabilities&tiled=true";
        }
        if ((workspaceInfo != null) && (layerInfo != null)) {
            return "../"
                    + workspaceInfo.getName()
                    + "/"
                    + layerInfo.getName()
                    + "/gwc/service/wms?service=WMS&version=1.1.1&request=GetCapabilities&tiled=true";
        }
        if ((workspaceInfo != null)) {
            return "../"
                    + workspaceInfo.getName()
                    + "/gwc/service/wms?service=WMS&version=1.1.1&request=GetCapabilities&tiled=true";
        }

        // workspaceInfo will be null
        return "../"
                + layerInfo.getName()
                + "/gwc/service/wms?service=WMS&version=1.1.1&request=GetCapabilities&tiled=true";
    }

    private String createLinkWMTS(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        if ((workspaceInfo == null) && (layerInfo == null)) {
            return "../gwc/service/wmts?service=WMTS&acceptVersions=1.0.0&request=GetCapabilities";
        }
        if ((workspaceInfo != null) && (layerInfo != null)) {
            return "../"
                    + workspaceInfo.getName()
                    + "/"
                    + layerInfo.getName()
                    + "/gwc/service/wmts?service=WMTS&acceptVersions=1.0.0&request=GetCapabilities";
        }
        if ((workspaceInfo != null)) {
            return "../"
                    + workspaceInfo.getName()
                    + "/gwc/service/wmts?service=WMTS&acceptVersions=1.0.0&request=GetCapabilities";
        }
        return "../" + layerInfo.getName()
                + "/gwc/service/wmts?service=WMTS&acceptVersions=1.0.0&request=GetCapabilities";
    }

    private String createLinkTMS(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        if ((workspaceInfo == null) && (layerInfo == null)) {
            return "../gwc/service/tms/1.0.0";
        }
        if ((workspaceInfo != null) && (layerInfo != null)) {
            return "../" + workspaceInfo.getName() + "/" + layerInfo.getName() + "/gwc/service/tms/1.0.0";
        }
        if ((workspaceInfo != null)) {
            return "../" + workspaceInfo.getName() + "/gwc/service/tms/1.0.0";
        }
        return "../" + layerInfo.getName() + "/gwc/service/tms/1.0.0";
    }

    @Override
    public List<ServiceLinkDescription> getServiceLinks(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceLinkDescription> links = new ArrayList<>();

        if (workspaceInfo == null && !geoserver.getGlobal().isGlobalServices()) {
            return links;
        }

        WMTSInfo info = info(workspaceInfo, layerInfo);
        List<Version> disabledVersions = (info != null) ? info.getDisabledVersions() : null;

        final GeoServerApplication app = GeoServerApplication.get();
        final GWCConfig gwcConfig = gwc.getConfig();

        try {
            if (gwcConfig.isWMSCEnabled() && null != app.getBean("gwcServiceWMS")) {
                Version wmscVersion = new Version("1.1.1");
                if (disabledVersions == null || !disabledVersions.contains(wmscVersion)) {
                    links.add(new ServiceLinkDescription(
                            serviceType,
                            wmscVersion,
                            createLinkWMSC(workspaceInfo, layerInfo),
                            workspaceInfo != null ? workspaceInfo.getName() : null,
                            layerInfo != null ? layerInfo.getName() : null,
                            "WMS-C"));
                }
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }

        try {
            if (info != null && info.isEnabled() && null != app.getBean("gwcServiceWMTS")) {
                Version wmtsVersion = new Version("1.0.0");
                if (disabledVersions == null || !disabledVersions.contains(wmtsVersion)) {
                    links.add(new ServiceLinkDescription(
                            serviceType,
                            wmtsVersion,
                            createLinkWMTS(workspaceInfo, layerInfo),
                            workspaceInfo != null ? workspaceInfo.getName() : null,
                            layerInfo != null ? layerInfo.getName() : null,
                            "WMTS"));
                }
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }
        try {
            if (gwcConfig.isTMSEnabled() && null != app.getBean("gwcServiceTMS")) {
                Version tmsVersion = new Version("1.0.0");
                if (disabledVersions == null || !disabledVersions.contains(tmsVersion)) {
                    links.add(new ServiceLinkDescription(
                            serviceType,
                            tmsVersion,
                            createLinkTMS(workspaceInfo, layerInfo),
                            workspaceInfo != null ? workspaceInfo.getName() : null,
                            layerInfo != null ? layerInfo.getName() : null,
                            "TMS"));
                }
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }
        return links;
    }
}
