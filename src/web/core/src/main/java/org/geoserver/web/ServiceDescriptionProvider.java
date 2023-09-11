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
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.security.DisabledServiceResourceFilter;
import org.geoserver.util.InternationalStringUtils;
import org.geotools.api.util.InternationalString;

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
     * Is this service/layer available? This works for standard GS services like WMS/WFS - GWC-based
     * ones will need to re-implement.
     *
     * @param serviceType Service type, example {@code WPS}, to cross reference with service links
     * @param info - Info about the service (service can be on/off)
     * @param layerInfo - Info about the layer (layer can not have this service enabled)
     * @return true of service is available for this layer
     */
    protected boolean isAvailable(String serviceType, ServiceInfo info, PublishedInfo layerInfo) {
        if (layerInfo != null && !layerInfo.isEnabled()) {
            return false;
        }
        if (layerInfo instanceof LayerInfo) {
            ResourceInfo resourceInfo = ((LayerInfo) layerInfo).getResource();

            // check what services are available for this kind of resource
            ServiceResourceProvider provider =
                    GeoServerExtensions.bean(ServiceResourceProvider.class);

            List<String> layerServices = provider.getServicesForResource(resourceInfo);

            // Remove any services that were disabled for this layer
            List<String> disabledServices =
                    DisabledServiceResourceFilter.disabledServices(resourceInfo);
            layerServices.removeAll(disabledServices);

            return layerServices.contains(serviceType);
        }
        return info.isEnabled();
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

        return new ServiceDescription(
                serviceType,
                title,
                description,
                available,
                false,
                workspaceInfo != null ? workspaceInfo.getName() : null,
                layerInfo != null ? layerInfo.getName() : null);
    }

    /**
     * Gets the name of the {@code version} parameter for the service. This will usually be {@code
     * version}, but some (i.e. WCS 2+) it will be {@code acceptversions}. To overrided by
     * subclasses.
     *
     * @param service
     * @return version parameter of service, example {@code version} or {@code acceptversions}
     */
    protected String getVersionParameterName(Service service) {
        return "version";
    }

    /**
     * Generate getcapabilities url for workspace / layer context.
     *
     * @param workspace WorkspaceInfo if available
     * @param layer Layer or LayerGroup info if available
     * @return getcapabilities link
     */
    protected String getCapabilitiesURL(
            WorkspaceInfo workspace, PublishedInfo layer, Service service) {

        String serviceId = service.getId();
        String serviceVersion = service.getVersion().toString();

        String query =
                "service="
                        + serviceId.toUpperCase()
                        + "&"
                        + getVersionParameterName(service)
                        + "="
                        + serviceVersion
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
