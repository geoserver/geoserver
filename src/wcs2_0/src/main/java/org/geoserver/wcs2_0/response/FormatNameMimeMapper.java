/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 * An implementation of {@link CoverageMimeTypeMapper} based on the {@link GridCoverage2D} class
 * associated to the CoverageInfo
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FormatNameMimeMapper implements CoverageMimeTypeMapper {

    private String formatName;

    private String mime;

    public FormatNameMimeMapper(String formatName, String mime) {
        this.formatName = formatName;
        this.mime = mime;
    }

    @Override
    public String getMimeType(CoverageInfo ci) throws IOException {
        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        if (formatName.equals(reader.getFormat().getName())) {
            return mime;
        }

        return null;
    }
}
