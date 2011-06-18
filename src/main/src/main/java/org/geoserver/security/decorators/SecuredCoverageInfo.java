/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.util.ProgressListener;

public class SecuredCoverageInfo extends DecoratingCoverageInfo {

    WrapperPolicy policy;

    public SecuredCoverageInfo(CoverageInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public GridCoverage getGridCoverage(ProgressListener listener, Hints hints)
            throws IOException {
        if(policy.level == AccessLevel.METADATA) 
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        
        // go through the secured reader
        GridCoverageReader reader = getGridCoverageReader(listener, hints);
        return getCatalog().getResourcePool().getGridCoverage(this, reader, null, hints);
    }

    @Override
    public GridCoverage getGridCoverage(ProgressListener listener,
            ReferencedEnvelope envelope, Hints hints) throws IOException {
        if(policy.level == AccessLevel.METADATA) 
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        
        // go through the secured reader
        GridCoverageReader reader = getGridCoverageReader(listener, hints);
        return getCatalog().getResourcePool().getGridCoverage(this, reader, envelope, hints);
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener,
            Hints hints) throws IOException {
        if(policy.level == AccessLevel.METADATA)
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        GridCoverageReader reader = super.getGridCoverageReader(listener, hints);
        return (GridCoverageReader) SecuredObjects.secure(reader, policy);
    }

    @Override
    public CoverageStoreInfo getStore() {
        return new SecuredCoverageStoreInfo(super.getStore(), policy);
    }

}
