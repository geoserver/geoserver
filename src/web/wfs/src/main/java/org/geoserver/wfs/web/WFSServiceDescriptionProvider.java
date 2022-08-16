package org.geoserver.wfs.web;

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
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServicesPanel;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WebFeatureService;
import org.geoserver.wfs.WebFeatureService20;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/** Provide description of WMS services for welcome page. */
public class WFSServiceDescriptionProvider implements ServiceDescriptionProvider {

    static final Logger LOGGER = Logging.getLogger(WFSServiceDescriptionProvider.class);

    GeoServer geoserver;
    Catalog catalog;

    public WFSServiceDescriptionProvider(GeoServer gs) {
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
    protected WFSInfo info(String workspaceName, String layerName) {
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
        if (workspaceInfo != null) {
            return geoserver.getService(workspaceInfo, WFSInfo.class);
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

        WFSInfo virtualWFS = info(workspaceName, layerName);
        WFSInfo globalWFS = geoserver.getService(WFSInfo.class);
        PublishedInfo layerInfo = layer(workspaceName, layerName);

        WFSInfo info = globalWFS;
        if (virtualWFS != null) {
            info = virtualWFS;
        }

        String serviceId = "wfs";
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
                        virtualWFS != null ? workspaceName : null,
                        layerInfo != null ? layerName : null);
        descriptions.add(serviceDescription);
        return descriptions;
    }

    public List<ServicesPanel.ServiceLinkDescription> getServiceLinks(
            String workspace, String layer) {
        List<ServicesPanel.ServiceLinkDescription> links = new ArrayList<>();
        List<Service> extensions = GeoServerExtensions.extensions(Service.class);

        for (Service service : extensions) {
            if ((service.getService() instanceof WebFeatureService20)
                    || (service.getService() instanceof WebFeatureService)) {
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
