/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.DefaultAPIExceptionHandler;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.springframework.stereotype.Component;

/** Custom exception wrapper to hide some of the WCS 2.0 exception handling oddities */
@Component
public class WCS20ExceptionHandler extends DefaultAPIExceptionHandler {

    public WCS20ExceptionHandler(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    public boolean canHandle(Throwable t, APIRequestInfo request) {
        return t instanceof WCS20Exception;
    }

    @Override
    public void handle(Throwable t, HttpServletResponse response) {
        WCS20Exception wc = (WCS20Exception) t;
        if (WCS20Exception.WCS20ExceptionCode.NoSuchCoverage.getExceptionCode()
                .equals(wc.getCode())) {
            // the one and only valid 404 usage
            response.setStatus(404);
        } else if (wc.getHttpCode() == 404) {
            // WCS 2.0 returns 404 for invalid param values...
            response.setStatus(400);
        } else {
            response.setStatus(wc.getHttpCode());
        }
        response.setContentType("application/json");

        writeResponse(response, t, wc.getCode(), getDescription(t));
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
