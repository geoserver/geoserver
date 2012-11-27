/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.util.logging.Logger;
import net.opengis.wcs20.DescribeCoverageType;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.response.WCSDescribeCoverageTransformer;
import org.geoserver.wcs2_0.response.WCSCapsTransformer;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;

public class DefaultWebCoverageService20 implements WebCoverageService20 {

    protected Logger LOGGER = Logging.getLogger(DefaultWebCoverageService20.class);

    private Catalog catalog;

    private GeoServer geoServer;

    private CoverageResponseDelegateFinder responseFactory;

    public DefaultWebCoverageService20(GeoServer geoServer, CoverageResponseDelegateFinder responseFactory) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.responseFactory = responseFactory;
    }
    
    public WCSInfo getServiceInfo() {
        return geoServer.getService(WCSInfo.class);
    }

    @Override
    public WCSCapsTransformer getCapabilities(GetCapabilitiesType request) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WCSDescribeCoverageTransformer describeCoverage(DescribeCoverageType request) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GridCoverage[] getCoverage(GetCoverageType request) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
