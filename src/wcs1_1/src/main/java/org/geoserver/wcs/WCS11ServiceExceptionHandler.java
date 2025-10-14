/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.util.List;
import org.geoserver.ows.OWS11ServiceExceptionHandler;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.Service;

public class WCS11ServiceExceptionHandler extends OWS11ServiceExceptionHandler {

    public static final String WCS11_NAMESPACE = "http://www.opengis.net/wcs/1.1.1";

    public WCS11ServiceExceptionHandler(Service service) {
        super(service);
    }

    /**
     * This handler can handle exceptions for the WCS 1.1 service, as well as requests in the
     * http://www.opengis.net/wcs/1.1.1 namespace. The extra effort is needed because the WCS 1.1 CITE tests send
     * requests without a version, and expects to get a OWS 1.1 exception report.
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
                // WCS 1.1 requests have a identifiers parameter
                return request.getRawKvp().containsKey("identifiers");
            }
        }

        return super.canHandle(service, request) || WCS11_NAMESPACE.equals(request.getNamespace());
    }
}
