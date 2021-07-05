/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.config.GeoServer;
import org.geoserver.data.DefaultLocaleDispatcherCallback;
import org.geoserver.ows.Request;

public class WFSDefaultLocaleCallback extends DefaultLocaleDispatcherCallback<WFSInfo> {

    public WFSDefaultLocaleCallback(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    protected WFSInfo getService(Request request) {
        if (request.getService() != null && request.getService().equalsIgnoreCase("WFS"))
            return geoServer.getService(WFSInfo.class);
        return null;
    }
}
