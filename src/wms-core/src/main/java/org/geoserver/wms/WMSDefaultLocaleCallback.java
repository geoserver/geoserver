/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.config.GeoServer;
import org.geoserver.data.DefaultLocaleDispatcherCallback;
import org.geoserver.ows.Request;

public class WMSDefaultLocaleCallback extends DefaultLocaleDispatcherCallback<WMSInfo> {

    public WMSDefaultLocaleCallback(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    protected WMSInfo getService(Request request) {
        if (request.getService() != null && request.getService().equalsIgnoreCase("WMS"))
            return geoServer.getService(WMSInfo.class);
        return null;
    }
}
