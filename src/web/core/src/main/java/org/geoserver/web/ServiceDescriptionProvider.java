/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.Collections;
import java.util.List;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.util.InternationalStringUtils;
import org.opengis.util.InternationalString;

/**
 * Contributes service description and link information for global, workspace and layer services.
 *
 * @author Jody Garnett
 * @see ServicesPanel
 */
public abstract class ServiceDescriptionProvider {

    /**
     * Provides service descriptions, filtered by workspace and layer.
     *
     * <p>Service info precedence: <pl>
     * <li>layer (LayerInfo), workspace (WorkspaceInfo): Description of virtual web service for
     *     individual layer.
     *
     *     <p>Service info may be constructed on the fly from layer, with default values from
     *     workspace service info (if defined), or global service info.
     * <li>layer (LayerGroup), workspace (WorkspaceInfo): Description of virtual web service for
     *     individual layer group.
     *
     *     <p>Service info may be constructed on the fly from layer group, with default values from
     *     workspace service info (if defined), or global service info.
     * <li>layer (LayerGroup), workspace (null): Description of virtual web service for global layer
     *     group.
     *
     *     <p>Service info may be constructed on the fly from layer group, with default values from
     *     global service info.
     * <li>layer (null), workspace (WorkspaceInfo): Description of virtual web virtual service for
     *     workspace.
     *
     *     <p>Service info from workspace service info (if defined), or global service info.
     * <li>layer (null), workspace (null): Description of global web service.
     *
     *     <p>Service defined by global service info.
     * </ol>
     *
     * >
     *
     * @param workspaceInfo Workspace context, or {@code null} for global
     * @param layerInfo Layer context, or {@code null} for all
     * @return Service descriptions, may be empty if none available.
     */
    public List<ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return Collections.emptyList();
    }

    /**
     * Provides service links, optionally filtered by workspace and layer.
     *
     * @param workspace Workspace context, or {@code null} for global
     * @param layer Layer context, or {@code null} for all
     * @return service links, may be empty if none available.
     */
    public List<ServiceLinkDescription> getServiceLinks(
            WorkspaceInfo workspace, PublishedInfo layer) {
        return Collections.emptyList();
    }

    /**
     * Generate ServiceDescription from provided ServiceInfo.
     *
     * <p>Subclasses may use when implementing {@link #getServices(WorkspaceInfo, PublishedInfo)}.
     *
     * @param info ServiceInfo
     * @return ServiceDescription
     */
    protected ServiceDescription description(
            ServiceInfo info, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        boolean available = info.isEnabled();

        if (layerInfo instanceof LayerInfo) {
            ServiceResourceProvider provider =
                    GeoServerExtensions.bean(ServiceResourceProvider.class);
            List<String> layerServices =
                    provider.getServicesForResource(((LayerInfo) layerInfo).getResource());

            available = layerServices.contains(info.getName().toUpperCase());
        }

        InternationalString title =
                InternationalStringUtils.growable(
                        info.getInternationalTitle(),
                        Strings.isEmpty(info.getTitle())
                                ? info.getName().toUpperCase()
                                : info.getTitle());

        InternationalString description =
                InternationalStringUtils.growable(
                        info.getInternationalAbstract(),
                        Strings.isEmpty(info.getAbstract()) ? null : info.getAbstract());

        return new ServiceDescription(
                info.getName().toLowerCase(),
                title,
                description,
                available,
                false,
                workspaceInfo != null ? workspaceInfo.getName() : null,
                layerInfo != null ? layerInfo.getName() : null);
    }

    /**
     * Generate getcapabilities url for workspace / layer context.
     *
     * @param workspace WorkspaceInfo if available
     * @param layer Layer or LayerGroup info if available
     * @param service open web service
     * @return getcapabilities link
     */
    protected String getCapabilitiesURL(
            WorkspaceInfo workspace, PublishedInfo layer, Service service) {
        String serviceId = service.getId();
        String query =
                "service="
                        + serviceId
                        + "&version="
                        + service.getVersion().toString()
                        + "&request=GetCapabilities";

        if (workspace != null && layer != null) {
            return "../" + workspace.getName() + "/" + layer.getName() + "/ows?" + query;

        } else if (workspace == null && layer != null) {
            String prefixed = layer.prefixedName();
            if (prefixed.contains(":")) {
                // use prefix to determine workspace name
                String prefix = prefixed.substring(0, prefixed.indexOf(":"));
                return "../" + prefix + "/" + layer.getName() + "/ows?" + query;
            } else {
                // global layer group
                return "../" + layer.getName() + "/ows?" + query;
            }
        } else if (workspace != null && layer == null) {
            return "../" + workspace.getName() + "/ows?" + query;
        } else {
            return "../ows?" + query;
        }
    }
}
