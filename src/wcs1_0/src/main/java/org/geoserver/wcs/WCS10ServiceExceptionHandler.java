/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.Service;
import org.geoserver.wcs.response.LegacyServiceExceptionHandler;

public class WCS10ServiceExceptionHandler extends LegacyServiceExceptionHandler {

    static final String WCS10_NAMESPACE = "http://www.opengis.net/wcs";

    public WCS10ServiceExceptionHandler(Service service, GeoServer geoServer) {
        super(service, geoServer);
    }

    /**
     * This handler can handle exceptions for the WCS 1.0 service. The extra effort is needed because the WCS 1.0 CITE
     * test suite sends incomplete requests, and expects to get a OWS 1.1 exception report.
     */
    @Override
    public boolean canHandle(Service service, Request request) {
        // WCS 1.0 tests send requests without (valid) version number, but expect WCS 1.0 exceptions...
        // Try to guess the version from the request parameters instead
        if (request.isGet()
                && "WCS".equalsIgnoreCase(request.getService())
                && !"GetCapabilities".equalsIgnoreCase(request.getRequest())) {
            // do we have a version mismatch?
            List<String> supportedVersions = RequestUtils.getSupportedVersions("WCS");
            if (!supportedVersions.contains(request.getVersion())) {
                // WCS 1.0 requests have a coverage parameter
                return request.getRawKvp().containsKey("coverage");
            }
        }

        return super.canHandle(service, request) || WCS10_NAMESPACE.equals(request.getNamespace());
    }
}
