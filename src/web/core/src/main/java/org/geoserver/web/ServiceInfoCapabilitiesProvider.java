/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.DisabledServiceCheck;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;

/**
 * Contributes standard GetCapabilities links for all the versions in all the {@link ServiceInfo} implementations
 * available to the {@link GeoServerApplication} using the GeoServer's {@code /ows} standard OpenGIS Web Service entry
 * point as the root of the GetCapabilities request.
 *
 * @author Gabriel Roldan
 * @see CapabilitiesHomePagePanel
 */
public class ServiceInfoCapabilitiesProvider implements CapabilitiesHomePageLinkProvider {
    static Logger LOGGER = Logger.getLogger("org.geoserver.web");

    /** @see org.geoserver.web.CapabilitiesHomePageLinkProvider#getCapabilitiesComponent */
    @Override
    public Component getCapabilitiesComponent(final String id) {
        Set<String> skip = skipServiceDescriptionProviders();

        @SuppressWarnings("deprecation")
        List<org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo> serviceInfoLinks = new ArrayList<>();

        for (Service service : GeoServerExtensions.extensions(Service.class)) {
            String serviceId = service.getId();
            if (skip.contains(serviceId.toLowerCase())) {
                continue;
            } else {
                try {
                    ServiceInfo serviceInfo = DisabledServiceCheck.lookupServiceInfo(service);
                    if (serviceInfo != null) {
                        if (!serviceInfo.isEnabled()) {
                            continue;
                        }

                        List<org.geotools.util.Version> disabledVersions = serviceInfo.getDisabledVersions();
                        if (disabledVersions != null && disabledVersions.contains(service.getVersion())) {
                            continue;
                        }
                    }
                } catch (Exception unexpected) {
                    LOGGER.fine("Error while looking up service info for service ");
                }

                if (service.getCustomCapabilitiesLink() != null) {
                    String capsLink = service.getCustomCapabilitiesLink();
                    @SuppressWarnings("deprecation")
                    org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo ci =
                            new org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo(
                                    serviceId, service.getVersion(), capsLink);
                    serviceInfoLinks.add(ci);
                } else if (service.getOperations().contains("GetCapabilities")) {
                    String capsLink = "../ows?service="
                            + serviceId
                            + "&version="
                            + service.getVersion().toString()
                            + "&request=GetCapabilities";
                    @SuppressWarnings("deprecation")
                    org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo ci =
                            new org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo(
                                    serviceId, service.getVersion(), capsLink);
                    serviceInfoLinks.add(ci);
                }
            }
        }
        if (serviceInfoLinks.isEmpty()) {
            return null;
        }
        return new CapabilitiesHomePagePanel(id, serviceInfoLinks);
    }

    /**
     * Check which services are available via {@link ServiceDescriptionProvider} and can be safely skipped.
     *
     * @return list of services to skip, in lowercase (for case-insensitive matching)
     */
    protected Set<String> skipServiceDescriptionProviders() {
        Set<String> skip = new HashSet<>();
        for (ServiceDescriptionProvider provider : GeoServerExtensions.extensions(ServiceDescriptionProvider.class)) {
            for (String serviceType : provider.getServiceTypes()) {
                skip.add(serviceType.toLowerCase());
            }
        }
        return skip;
    }
}
