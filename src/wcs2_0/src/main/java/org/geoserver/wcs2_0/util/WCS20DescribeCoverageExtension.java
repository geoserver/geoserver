/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.util;

import org.geoserver.catalog.CoverageInfo;

/**
 * Extension point used for the DescribeCoverage operation
 *
 * @author Nicola Lagomarsini
 */
public interface WCS20DescribeCoverageExtension {

    /**
     * Returns a new coverageId value encoded for being accepted by the {@link NCNameResourceCodec}
     * class
     *
     * @param coverageId the provided CoverageId parameter
     * @return a newly encoded coverageId
     */
    public String handleCoverageId(String coverageId);

    /**
     * Returns a new {@link CoverageInfo} object reflecting the extension point modifications
     *
     * @param coverageId the provided CoverageId parameter
     * @param ci the initial {@link CoverageInfo} object provided by WCS
     * @return a new CoverageInfo object
     */
    public CoverageInfo handleCoverageInfo(String coverageId, CoverageInfo ci);

    /**
     * Returns a new string which will be print in the describecoverage result
     *
     * @param encodedId the initial encodedId parameter
     * @param coverageId the provided CoverageId parameter
     * @return a new string
     */
    public String handleEncodedId(String encodedId, String coverageId);
}
