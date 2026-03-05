/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServiceVersionUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geotools.util.logging.Logging;

/**
 * Implementation of ServiceVersionFilter that filters out disabled versions based on ServiceInfo configuration. This
 * class bridges the gap between the ows module (where version negotiation happens) and the main module (where
 * ServiceInfo configuration is stored).
 */
public class DisabledServiceVersionFilter implements ServiceVersionFilter {

    private static final Logger LOGGER = Logging.getLogger(DisabledServiceVersionFilter.class);

    @Override
    public List<String> filterVersions(Service service, List<String> versions) {
        if (service == null || versions == null || versions.isEmpty()) {
            return versions;
        }

        try {
            // look up GeoServer to get the current ServiceInfo directly from configuration
            GeoServer geoServer = GeoServerExtensions.bean(GeoServer.class);
            if (geoServer == null) {
                LOGGER.warning("GeoServer bean is null, returning all versions");
                return versions;
            }

            // get the ServiceInfo from the service bean (might be cached)
            ServiceInfo cachedServiceInfo = DisabledServiceCheck.lookupServiceInfo(service);
            if (cachedServiceInfo == null) {
                return versions;
            }

            // determine workspace context: LocalWorkspace or from the request context
            WorkspaceInfo workspace = LocalWorkspace.get();
            if (workspace == null) {
                Request owsRequest = Dispatcher.REQUEST.get();
                if (owsRequest != null && owsRequest.getContext() != null) {
                    String ctx = owsRequest.getContext();
                    String wsName = ctx.contains("/") ? ctx.substring(0, ctx.indexOf('/')) : ctx;
                    workspace = geoServer.getCatalog().getWorkspaceByName(wsName);
                }
            }

            // look up workspace-specific service
            ServiceInfo serviceInfo = null;
            if (workspace != null) {
                serviceInfo = geoServer.getServices(workspace).stream()
                        .filter(si -> si.getName().equalsIgnoreCase(cachedServiceInfo.getName()))
                        .findFirst()
                        .orElse(null);
            }
            if (serviceInfo == null) {
                serviceInfo = geoServer.getService(cachedServiceInfo.getId(), ServiceInfo.class);
            }
            if (serviceInfo == null) {
                serviceInfo = cachedServiceInfo;
            }

            List<String> filtered = ServiceVersionUtils.getEnabledVersions(service.getId(), serviceInfo);
            return filtered;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception during version filtering", e);
            return versions;
        }
    }
}
