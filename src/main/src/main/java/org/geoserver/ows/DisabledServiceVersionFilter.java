/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        LOGGER.info("DisabledServiceVersionFilter.filterVersions called for service: "
                + (service != null ? service.getId() : "null")
                + ", versions: " + versions);
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
                LOGGER.info("Could not lookup ServiceInfo from service bean");
                return versions;
            }

            // look up the fresh ServiceInfo from GeoServer configuration
            ServiceInfo serviceInfo = geoServer.getService(cachedServiceInfo.getId(), ServiceInfo.class);
            if (serviceInfo == null) {
                serviceInfo = cachedServiceInfo; // fallback to cached, if there is any
            }

            LOGGER.info("ServiceInfo found, disabled versions: " + serviceInfo.getDisabledVersions());
            List<String> filtered = ServiceVersionUtils.getEnabledVersions(service.getId(), serviceInfo);
            LOGGER.info("After filtering, enabled versions: " + filtered);
            return filtered;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception during version filtering", e);
            return versions;
        }
    }
}
