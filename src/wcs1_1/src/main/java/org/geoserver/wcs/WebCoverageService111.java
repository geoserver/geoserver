/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import net.opengis.wcs11.DescribeCoverageType;
import net.opengis.wcs11.GetCapabilitiesType;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.wcs.response.DescribeCoverageTransformer;
import org.geoserver.wcs.response.WCSCapsTransformer;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Web Coverage Services interface.
 *
 * <p>Each of the methods on this class corresponds to an operation as defined by the Web Coverage
 * Specification. See {@link "http://www.opengeospatial.org/standards/wcs"} for more details.
 *
 * @author Andrea Aime, TOPP
 */
public interface WebCoverageService111 {

    /** WCS service info. */
    WCSInfo getServiceInfo();

    /** GetCapabilities operation. */
    WCSCapsTransformer getCapabilities(GetCapabilitiesType request);

    /** DescribeCoverage oeration. */
    DescribeCoverageTransformer describeCoverage(DescribeCoverageType request);

    /** GetCoverage operation. */
    GridCoverage[] getCoverage(GetCoverageType request);
}
