package org.geoserver.wcs.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.util.InternationalStringUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WebCoverageService100;
import org.geoserver.wcs.WebCoverageService111;
import org.geoserver.wcs2_0.WebCoverageService20;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServicesPanel;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/** Provide description of WMS services for welcome page. */
public class WCSServiceDescriptionProvider implements ServiceDescriptionProvider {

    static final Logger LOGGER = Logging.getLogger(WCSServiceDescriptionProvider.class);

    GeoServer geoserver;
    Catalog catalog;

    public WCSServiceDescriptionProvider(GeoServer gs) {
        this.geoserver = gs;
        catalog = gs.getCatalog();
    }

    /**
     * Lookup WFSInfo using workspaceName / layerName conotext.
     *
     * @param workspaceName Name of workspace, or global layer group
     * @param layerName Name of layer or layer group
     * @return WMSInfo if available for workspaceName, or {@code null} for global WMSInfo
     */
    protected WCSInfo info(String workspaceName, String layerName) {
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
        if (workspaceInfo != null) {
            return geoserver.getService(workspaceInfo, WCSInfo.class);
        }
        return null;
    }

    /**
     * Look up published info using page workspace / layer context (see {@code
     * LocalWorkspaceCallback}).
     *
     * @param workspaceName Name of workspace, or global layer group
     * @param layerName Name of layer or layer group
     * @return PublishedInfo representing layer info or group info, or {@code null} if not found
     */
    protected PublishedInfo layer(String workspaceName, String layerName) {
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
        if (workspaceInfo == null) {
            if (workspaceName != null) {
                LayerGroupInfo groupInfo =
                        catalog.getLayerGroupByName((WorkspaceInfo) null, workspaceName);
                if (groupInfo != null) {
                    return groupInfo;
                }
            }
        } else if (layerName != null) {
            NamespaceInfo namespaceInfo = catalog.getNamespaceByPrefix(workspaceInfo.getName());
            LayerInfo layerInfo =
                    catalog.getLayerByName(new NameImpl(namespaceInfo.getURI(), layerName));
            if (layerInfo != null) {
                return layerInfo;
            }
            LayerGroupInfo groupInfo = catalog.getLayerGroupByName(workspaceInfo, layerName);
            if (groupInfo != null) {
                return groupInfo;
            }
        }
        return null;
    }

    public List<ServicesPanel.ServiceDescription> getServices(
            String workspaceName, String layerName) {
        List<ServicesPanel.ServiceDescription> descriptions = new ArrayList<>();

        WCSInfo virtualWCS = info(workspaceName, layerName);
        WCSInfo globalWCS = geoserver.getService(WCSInfo.class);
        PublishedInfo layerInfo = layer(workspaceName, layerName);

        WCSInfo info = globalWCS;
        if (virtualWCS != null) {
            info = virtualWCS;
        }

        String serviceId = "wcs";
        boolean available = info.isEnabled();
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
                        virtualWCS != null ? workspaceName : null,
                        layerInfo != null ? layerName : null);
        descriptions.add(serviceDescription);
        return descriptions;
    }

    public List<ServicesPanel.ServiceLinkDescription> getServiceLinks(
            String workspace, String layer) {
        List<ServicesPanel.ServiceLinkDescription> links = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if ((service.getService() instanceof WebCoverageService111)
                    || (service.getService() instanceof WebCoverageService100)
                    || (service.getService() instanceof WebCoverageService20)) {
                String serviceId = service.getId();
                String namespace = service.getNamespace();

                String link = null;
                if (service.getOperations().contains("GetCapabilities")) {
                    if (workspace != null) {
                        link =
                                "../"
                                        + workspace
                                        + "/ows?service="
                                        + serviceId
                                        + "&version="
                                        + service.getVersion().toString()
                                        + "&request=GetCapabilities";
                    } else {
                        link =
                                "../ows?service="
                                        + serviceId
                                        + "&version="
                                        + service.getVersion().toString()
                                        + "&request=GetCapabilities";
                    }
                } else if (service.getCustomCapabilitiesLink() != null) {
                    link = service.getCustomCapabilitiesLink();
                }

                if (link != null) {
                    links.add(
                            new ServicesPanel.ServiceLinkDescription(
                                    serviceId.toLowerCase(),
                                    service.getVersion(),
                                    link,
                                    namespace));
                }
            }
        }
        return links;
    }
}
