/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import org.geoserver.config.GeoServer;
import org.geoserver.data.DefaultLocaleDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.wcs.WCSInfo;

public class WCSDefaultLocalCallback extends DefaultLocaleDispatcherCallback<WCSInfo> {

    public WCSDefaultLocalCallback(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    protected WCSInfo getService(Request request) {
        if (request.getService() != null && request.getService().equalsIgnoreCase("WCS"))
            return geoServer.getService(WCSInfo.class);
        return null;
    }
}
