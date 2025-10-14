/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.exception;

import java.util.List;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.Service;

public class WCS20ServiceExceptionHandler extends OWS20ServiceExceptionHandler {

    public static final String WCS20_NAMESPACE = "http://www.opengis.net/wcs/2.0";

    /**
     * Should be fed the WCS 2.0 service
     *
     * @param service
     */
    public WCS20ServiceExceptionHandler(Service service) {
        super(service);
    }

    /**
     * This handler can handle exceptions for the WCS 2.0 service, as well as requests in the
     * http://www.opengis.net/wcs/2.0 namespace. The extra effort is needed because the WCS 2.0 CITE test suite sends
     * requests in the WCS 2.0 namespace, but the service can be an invalid name.
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
                // WCS 2.0 requests have a coverage parameter
                return request.getRawKvp().containsKey("coverageId");
            }
        }

        return super.canHandle(service, request) || WCS20_NAMESPACE.equals(request.getNamespace());
    }
}
