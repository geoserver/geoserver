/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * Similar to the {@link org.geoserver.ows.CiteComplianceHack}, but works on both "/wms" and "/ows"
 */
public class CiteComplianceCallback extends AbstractDispatcherCallback {

    GeoServer gs;

    public CiteComplianceCallback(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        if ("WMS".equals(request.getService())) {
            WMSInfo wms = gs.getService(WMSInfo.class);
            // version is mandatory in all requests but GetCapabilities
            if (wms.isCiteCompliant()
                    && !"GetCapabilities".equals(request.getRequest())
                    && isVersionMissing(request)) {
                throw new ServiceException(
                        "Could not determine version", "MissingParameterValue", "version");
            }
        }
        return service;
    }

    private boolean isVersionMissing(Request request) {
        // wmtver supported for backwards compatibility with WMS < 1.0
        return request.getVersion() == null && request.getRawKvp().get("wmtver") == null;
    }
}
