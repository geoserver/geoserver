/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.coverage.grid.io.AbstractGridFormat;

public class SecuredCoverageStoreInfo extends DecoratingCoverageStoreInfo {
    WrapperPolicy policy;

    public SecuredCoverageStoreInfo(CoverageStoreInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public AbstractGridFormat getFormat() {
        Request request = Dispatcher.REQUEST.get();
        if (policy.level == AccessLevel.METADATA
                && (request == null || !"GetCapabilities".equalsIgnoreCase(request.getRequest()))) {
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        }
        return super.getFormat();
    }
}
