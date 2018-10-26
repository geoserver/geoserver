/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.ows.wmts.WebMapTileServer;
import org.opengis.util.ProgressListener;

public class SecuredWMTSStoreInfo extends DecoratingWMTSStoreInfo {

    WrapperPolicy policy;

    public SecuredWMTSStoreInfo(WMTSStoreInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public WebMapTileServer getWebMapTileServer(ProgressListener listener) throws IOException {
        WebMapTileServer wms = super.getWebMapTileServer(null);
        if (wms == null) return null;
        else if (policy.level == AccessLevel.METADATA)
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        else return (WebMapTileServer) SecuredObjects.secure(wms, policy);
    }
}
