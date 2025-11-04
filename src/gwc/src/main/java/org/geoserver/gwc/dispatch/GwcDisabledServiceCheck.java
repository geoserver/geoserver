/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import static java.util.Objects.requireNonNull;
import static org.geoserver.security.DisabledServiceResourceFilter.disabledServices;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.gwc.wmts.WMTSInfoImpl;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DisabledServiceCheck;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * {@link DispatcherCallback} similar to {@link DisabledServiceCheck} but tailored towards GeoWebCache services
 *
 * @since 3.0
 */
public class GwcDisabledServiceCheck extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(GwcDisabledServiceCheck.class);

    /** Used to obtain the {@link WMTSInfo} if the service being called is WMTS */
    private GeoServer geoServer;

    /**
     * Regex to match {@literal [workspace/layer/]gwc/service} to decide if {@link Request#getContext()} targets a GWC
     * service endpoint
     */
    private static final Pattern GWC_SERVICE_ENDPOINT_PATTERN = Pattern.compile("(?:[^/]+/)?(?:[^/]+/)?gwc/service");

    public GwcDisabledServiceCheck(GeoServer geoServer) {
        this.geoServer = requireNonNull(geoServer);
    }

    @Override
    public Service serviceDispatched(Request request, Service service) {

        final String requestContext = request.getContext();
        if (!StringUtils.hasText(requestContext)
                || !GWC_SERVICE_ENDPOINT_PATTERN.matcher(requestContext).matches()) {
            return service;
        }

        final String gwcServiceName = request.getPath();
        WMTSInfo wmtsInfo = geoServer.getService(WMTSInfo.class);
        if (wmtsInfo == null) {
            wmtsInfo = new WMTSInfoImpl();
            wmtsInfo.setEnabled(false);
        }

        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        boolean enabled =
                switch (gwcServiceName) {
                    case "wmts" -> wmtsInfo.isEnabled();
                    case "wms" -> config.isWMSCEnabled();
                    case "tms" -> config.isTMSEnabled();
                    case "gmaps", "ve", "kml", "mgmaps" -> wmtsInfo.isEnabled();
                    default -> throw new IllegalArgumentException("Unknown GWC service id: " + gwcServiceName);
                };

        if (!enabled) {
            throw new HttpErrorCodeException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Service is disabled");
        }

        PublishedInfo localPublished = LocalPublished.get();
        if (localPublished instanceof LayerInfo layerInfo) {
            ResourceInfo resource = layerInfo.getResource();
            List<String> disabledServices = disabledServices(resource);
            boolean disabled = disabledServices.stream().anyMatch(gwcServiceName::equalsIgnoreCase);
            if (disabled) {
                throw new HttpErrorCodeException(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service %s is disabled for layer %s".formatted(gwcServiceName, layerInfo.prefixedName()));
            }
        }

        return service;
    }
}
