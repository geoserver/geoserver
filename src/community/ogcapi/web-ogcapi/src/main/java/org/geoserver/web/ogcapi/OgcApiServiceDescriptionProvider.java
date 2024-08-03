/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.ogcapi;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.util.InternationalStringUtils;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.api.util.InternationalString;
import org.springframework.web.util.UriUtils;

public class OgcApiServiceDescriptionProvider<SERVICEINFOTYPE extends ServiceInfo, SERVICETYPE>
        extends ServiceDescriptionProvider {

    /**
     * This is the TYPE of service (i.e "WMS", "WMTS", "WFS", etc...). This is used to categorize
     * the link into a group (i.e. OGCAPI-Features into the "WFS" category).
     */
    String serviceType;

    String specificServiceType;

    /**
     * Specific name of the service ("OGCAPI-Tiles"). The link text consists of this name and the
     * version number.
     */
    String serviceName;

    Class<SERVICEINFOTYPE> infoClass;
    Class<SERVICETYPE> serviceClass;

    GeoServer geoserver;
    Catalog catalog;

    /**
     * OGCAPI Service Descriptor with additional information to group with associated Open Web
     * Service heading.
     *
     * @param gs GeoServer configuration
     * @param serviceType Service identifier, example {@code WFS}, used to group for heading and
     *     description
     * @param serviceName OGCAPI Name
     * @param specificServiceType OGCAPI specific service type, example {@code Features}.
     */
    public OgcApiServiceDescriptionProvider(
            GeoServer gs, String serviceType, String serviceName, String specificServiceType) {
        this.geoserver = gs;
        catalog = gs.getCatalog();
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.specificServiceType = specificServiceType;
        @SuppressWarnings("unchecked")
        var infoClass =
                (Class<SERVICEINFOTYPE>)
                        ((ParameterizedType) this.getClass().getGenericSuperclass())
                                .getActualTypeArguments()[0];
        this.infoClass = infoClass;

        @SuppressWarnings("unchecked")
        var serviceClass =
                (Class<SERVICETYPE>)
                        ((ParameterizedType) this.getClass().getGenericSuperclass())
                                .getActualTypeArguments()[1];
        this.serviceClass = serviceClass;
    }

    /**
     * Lookup SERVICEINFOTYPE using workspaceInfo / layerInfo context.
     *
     * @param workspaceInfo Workspace, or null for global.
     * @param layerInfo Layer, LayerGroup, or null for any
     * @return SERVICEINFOTYPE if available for workspace, or global SERVICEINFOTYPE.
     */
    protected SERVICEINFOTYPE info(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        SERVICEINFOTYPE info = null;
        if (workspaceInfo != null) {
            info = (SERVICEINFOTYPE) geoserver.getService(workspaceInfo, infoClass);
        }
        if (info == null) {
            info = (SERVICEINFOTYPE) geoserver.getService(infoClass);
        }
        return info;
    }

    @Override
    public List<ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        List<ServiceDescription> descriptions = new ArrayList<>();
        SERVICEINFOTYPE info = info(workspaceInfo, layerInfo);

        if (workspaceInfo != null || geoserver.getGlobal().isGlobalServices()) {
            descriptions.add(description(serviceType, info, workspaceInfo, layerInfo));
        }
        return descriptions;
    }

    /**
     * Generate ServiceDescription from provided ServiceInfo.
     *
     * <p>Subclasses may use when implementing {@link #getServices(WorkspaceInfo, PublishedInfo)}.
     *
     * @param serviceType Service type, example {@code wps}, to cross-reference with service links
     * @param info ServiceInfo providing customer configured description
     * @param workspaceInfo workspace context for info lookup
     * @param layerInfo layer or layergroup context for info lookup
     * @return ServiceDescription
     */
    @Override
    protected ServiceDescription description(
            String serviceType,
            ServiceInfo info,
            WorkspaceInfo workspaceInfo,
            PublishedInfo layerInfo) {
        boolean available = isAvailable(serviceType, info, layerInfo);

        InternationalString title =
                InternationalStringUtils.growable(
                        info.getInternationalTitle(),
                        Strings.isEmpty(info.getTitle()) ? info.getName() : info.getTitle());

        InternationalString description =
                InternationalStringUtils.growable(
                        info.getInternationalAbstract(),
                        Strings.isEmpty(info.getAbstract()) ? null : info.getAbstract());

        var serviceDesc =
                new ServiceDescription(
                        serviceType,
                        title,
                        description,
                        available,
                        false,
                        workspaceInfo != null ? workspaceInfo.getName() : null,
                        layerInfo != null ? layerInfo.getName() : null);

        serviceDesc.setDescriptionPriority(10.0);

        return serviceDesc;
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
            if (service.getService().getClass() == serviceClass) {
                String link = null;
                if (service.getCustomCapabilitiesLink() != null) {
                    link =
                            ogcApiCustomCapabilitiesLinkMangler(
                                    service.getCustomCapabilitiesLink(), workspaceInfo, layerInfo);
                }

                if (link != null) {
                    links.add(
                            new ServiceLinkDescription(
                                    serviceType,
                                    service.getVersion(),
                                    link,
                                    workspaceInfo != null ? workspaceInfo.getName() : null,
                                    layerInfo != null ? layerInfo.getName() : null,
                                    serviceName,
                                    specificServiceType));
                }
            }
        }
        return links;
    }

    public String ogcApiCustomCapabilitiesLinkMangler(
            String customLink, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        if (workspaceInfo == null) return customLink;
        // add in WS info
        var wsName = UriUtils.encodePath(workspaceInfo.getName(), "UTF-8");
        customLink = customLink.replace("/ogc/", "/" + wsName + "/ogc/");
        if (layerInfo != null) {
            var layerName = UriUtils.encodePath(layerInfo.getName(), "UTF-8");
            customLink = customLink.replace("/ogc/", "/" + layerName + "/ogc/");
        }
        return customLink;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getSpecificServiceType() {
        return specificServiceType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Class<SERVICEINFOTYPE> getInfoClass() {
        return infoClass;
    }

    public Class<SERVICETYPE> getServiceClass() {
        return serviceClass;
    }
}
