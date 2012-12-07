/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import net.opengis.wcs20.DescribeCoverageType;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Web Coverage Services interface.
 * <p>
 * Each of the methods on this class corresponds to an operation as defined
 * by the Web Coverage Specification. See {@link http://www.opengeospatial.org/standards/wcs}
 * for more details.
 * </p>
 * 
 * @author Emanuele Tajariol (etj) - GeoSolutions
 *
 */
public interface WebCoverageService20 {

    /**
     * WCS service info.
     */
    WCSInfo getServiceInfo();
    
    /**
    * GetCapabilities operation.
    */
    TransformerBase getCapabilities(GetCapabilitiesType request);

    /**
     * DescribeCoverage operation.
     */
    WCS20DescribeCoverageTransformer describeCoverage(DescribeCoverageType request);

    /**
     * GetCoverage operation.
     */
    GridCoverage getCoverage(GetCoverageType request);
}
