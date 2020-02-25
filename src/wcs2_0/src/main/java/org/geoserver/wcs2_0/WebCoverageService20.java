/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import net.opengis.wcs20.DescribeCoverageType;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Web Coverage Services interface.
 *
 * <p>Each of the methods on this class corresponds to an operation as defined by the Web Coverage
 * Specification. See {@link "http://www.opengeospatial.org/standards/wcs"} for more details.
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public interface WebCoverageService20 {

    /**
     * A key that can be be used to identify the originating CoverageInfo attached to the output
     * GridCoverage, which can be used to retrieve extra metadata about the coverage
     */
    public static final String ORIGINATING_COVERAGE_INFO =
            "org.geoserver.wcs.originatingCoverageInfo";

    /** WCS service info. */
    WCSInfo getServiceInfo();

    /** GetCapabilities operation. */
    TransformerBase getCapabilities(GetCapabilitiesType request);

    /** DescribeCoverage operation. */
    WCS20DescribeCoverageTransformer describeCoverage(DescribeCoverageType request);

    /**
     * The WCS EO desscribe coverage set operation (available only if the wcs-eo plugin is
     * installed)
     */
    TransformerBase describeEOCoverageSet(DescribeEOCoverageSetType request);

    /** GetCoverage operation. */
    GridCoverage getCoverage(GetCoverageType request);
}
