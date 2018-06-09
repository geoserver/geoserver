/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import org.geoserver.catalog.CoverageInfo;

/**
 * Maps a {@link CoverageInfo} to its native mime type, if any. Used to figure out the "native mime"
 * as required by the WCS 2.0 spec.
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface CoverageMimeTypeMapper {

    String getMimeType(CoverageInfo info) throws IOException;
}
