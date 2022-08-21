/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.Service;
import org.geoserver.web.ServicesPanel.ServiceDescription;
import org.geoserver.web.ServicesPanel.ServiceLinkDescription;

/**
 * Contributes service description and link information for global, workspace and layer services.
 *
 * @author Jody Garnett
 * @see ServicesPanel
 */
public abstract class ServiceDescriptionProvider {

    /**
     * Provides service descriptions, optionally filtered by workspace and layer.
     *
     * <p>Filtering is forgiving: provide the global services unless the workspace exactly matches;
     * provide workspace services unless the layer exactly matches.
     *
     * @param workspaceInfo Workspace context, or {@code null} for global
     * @param layerInfo Layer context, or {@code null} for all
     * @return service descriptions, may be empty if none available.
     */
    public List<ServiceDescription> getServices(
            WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return Collections.emptyList();
    }

    /**
     * Provides service links, optionally filtered by workspace and layer.
     *
     * <p>Filtering is forgiving: provide the global services unless the workspace exactly matches;
     * provide workspace services unless the layer exactly matches.
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
