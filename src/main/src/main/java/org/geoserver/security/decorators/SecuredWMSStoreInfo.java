/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.ows.wms.WebMapServer;
import org.opengis.util.ProgressListener;

public class SecuredWMSStoreInfo extends DecoratingWMSStoreInfo {

    WrapperPolicy policy;

    public SecuredWMSStoreInfo(WMSStoreInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public WebMapServer getWebMapServer(ProgressListener listener) throws IOException {
        WebMapServer wms = super.getWebMapServer(null);
        if (wms == null) return null;
        else if (policy.level == AccessLevel.METADATA)
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        else return (WebMapServer) SecuredObjects.secure(wms, policy);
    }
}
